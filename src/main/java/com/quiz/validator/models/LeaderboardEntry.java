package com.quiz.validator.models;

public class LeaderboardEntry {
    private String participant;
    private int totalScore;
    
    public LeaderboardEntry() {}
    
    public LeaderboardEntry(String participant, int totalScore) {
        this.participant = participant;
        this.totalScore = totalScore;
    }
    
    public String getParticipant() { return participant; }
    public void setParticipant(String participant) { this.participant = participant; }
    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
}
