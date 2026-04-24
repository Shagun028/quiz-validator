package com.quiz.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quiz.validator.models.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ValidatorService {
    private static final String BASE_URL = "https://devapigw.vidalhealthtpa.com/srm-quiz-task";
    private static final int TOTAL_POLLS = 10;
    private static final int DELAY_SECONDS = 5;
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Set<String> processedKeys;
    private final Map<String, Integer> participantScores;
    private final AtomicBoolean isPolling;
    private final AtomicInteger currentPoll;
    private String currentRegNo;
    private SubmitResponse lastSubmitResponse;
    
    public ValidatorService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.processedKeys = ConcurrentHashMap.newKeySet();
        this.participantScores = new ConcurrentHashMap<>();
        this.isPolling = new AtomicBoolean(false);
        this.currentPoll = new AtomicInteger(0);
    }
    
    public void startPolling(String regNo) throws Exception {
        if (isPolling.get()) {
            throw new IllegalStateException("Polling already in progress");
        }
        
        reset();
        this.currentRegNo = regNo;
        this.isPolling.set(true);
        
        try {
            for (int i = 0; i < TOTAL_POLLS; i++) {
                currentPoll.set(i);
                QuizResponse response = poll(i);
                processEvents(response.getEvents());
                System.out.println("Poll " + i + " completed - Unique events so far: " + processedKeys.size());
                
                if (i < TOTAL_POLLS - 1) {
                    Thread.sleep(DELAY_SECONDS * 1000);
                }
            }
            System.out.println("All polls completed! Total unique events: " + processedKeys.size());
        } finally {
            this.isPolling.set(false);
        }
    }
    
    private QuizResponse poll(int pollIndex) throws Exception {
        String url = String.format("%s/quiz/messages?regNo=%s&poll=%d", 
            BASE_URL, currentRegNo, pollIndex);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to poll API. Status: " + response.statusCode());
        }
        
        return objectMapper.readValue(response.body(), QuizResponse.class);
    }
    
    private void processEvents(List<Event> events) {
        for (Event event : events) {
            String key = event.getRoundId() + "|" + event.getParticipant();
            if (processedKeys.add(key)) {
                participantScores.merge(event.getParticipant(), event.getScore(), Integer::sum);
            }
        }
    }
    
    public List<LeaderboardEntry> getLeaderboard() {
        return participantScores.entrySet().stream()
            .map(entry -> new LeaderboardEntry(entry.getKey(), entry.getValue()))
            .sorted((a, b) -> Integer.compare(b.getTotalScore(), a.getTotalScore()))
            .collect(Collectors.toList());
    }
    
    public SubmitResponse submitLeaderboard() throws Exception {
        List<LeaderboardEntry> leaderboard = getLeaderboard();
        SubmitRequest submitRequest = new SubmitRequest(currentRegNo, leaderboard);
        String requestBody = objectMapper.writeValueAsString(submitRequest);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/quiz/submit"))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to submit. Status: " + response.statusCode());
        }
        
        lastSubmitResponse = objectMapper.readValue(response.body(), SubmitResponse.class);
        return lastSubmitResponse;
    }
    
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalParticipants", participantScores.size());
        summary.put("totalUniqueEvents", processedKeys.size());
        summary.put("totalScore", getLeaderboard().stream().mapToInt(LeaderboardEntry::getTotalScore).sum());
        summary.put("submitted", lastSubmitResponse != null);
        if (lastSubmitResponse != null) {
            summary.put("submissionCorrect", lastSubmitResponse.isCorrect());
        }
        return summary;
    }
    
    public void reset() {
        processedKeys.clear();
        participantScores.clear();
        currentPoll.set(0);
        lastSubmitResponse = null;
    }
    
    public boolean isPolling() { return isPolling.get(); }
    public int getCurrentPoll() { return currentPoll.get(); }
    public int getUniqueEventCount() { return processedKeys.size(); }
    public int getParticipantCount() { return participantScores.size(); }
}
