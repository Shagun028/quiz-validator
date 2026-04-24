package com.quiz.validator;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

public class WebServer {
    private static final int PORT = 8080;
    private final HttpServer server;
    private final ValidatorService validatorService;
    private final ObjectMapper objectMapper;
    
    public WebServer() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(PORT), 0);
        this.validatorService = new ValidatorService();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        setupRoutes();
        server.setExecutor(Executors.newCachedThreadPool());
    }
    
    private void setupRoutes() {
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/start", new StartPollingHandler());
        server.createContext("/api/status", new StatusHandler());
        server.createContext("/api/submit", new SubmitHandler());
        server.createContext("/api/reset", new ResetHandler());
        server.createContext("/api/leaderboard", new LeaderboardHandler());
    }
    
    public void start() {
        server.start();
        System.out.println("========================================");
        System.out.println("Quiz Validator Server Started!");
        System.out.println("Open your browser and visit:");
        System.out.println("http://localhost:" + PORT);
        System.out.println("========================================");
    }
    
    private class StartPollingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            
            Map<String, Object> response = new HashMap<>();
            try {
                String regNo = extractRegNo(exchange);
                
                // Start polling in background
                new Thread(() -> {
                    try {
                        validatorService.startPolling(regNo);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
                
                response.put("status", "started");
                response.put("message", "Polling started successfully");
                sendJsonResponse(exchange, 200, response);
            } catch (Exception e) {
                response.put("status", "error");
                response.put("message", e.getMessage());
                sendJsonResponse(exchange, 500, response);
            }
        }
    }
    
    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> status = new HashMap<>();
            status.put("isPolling", validatorService.isPolling());
            status.put("currentPoll", validatorService.getCurrentPoll());
            status.put("totalPolls", 10);
            status.put("uniqueEvents", validatorService.getUniqueEventCount());
            status.put("participants", validatorService.getParticipantCount());
            status.put("summary", validatorService.getSummary());
            
            sendJsonResponse(exchange, 200, status);
        }
    }
    
    private class SubmitHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            
            try {
                com.quiz.validator.models.SubmitResponse response = validatorService.submitLeaderboard();
                Map<String, Object> result = new HashMap<>();
                result.put("success", response.isCorrect());
                result.put("submittedTotal", response.getSubmittedTotal());
                result.put("expectedTotal", response.getExpectedTotal());
                result.put("message", response.getMessage());
                result.put("isIdempotent", response.isIdempotent());
                
                sendJsonResponse(exchange, 200, result);
            } catch (Exception e) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", e.getMessage());
                sendJsonResponse(exchange, 500, error);
            }
        }
    }
    
    private class ResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            validatorService.reset();
            Map<String, String> response = Map.of("status", "reset", "message", "Application reset successfully");
            sendJsonResponse(exchange, 200, response);
        }
    }
    
    private class LeaderboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<com.quiz.validator.models.LeaderboardEntry> leaderboard = validatorService.getLeaderboard();
            Map<String, Object> response = new HashMap<>();
            response.put("leaderboard", leaderboard);
            response.put("totalScore", leaderboard.stream().mapToInt(com.quiz.validator.models.LeaderboardEntry::getTotalScore).sum());
            
            sendJsonResponse(exchange, 200, response);
        }
    }
    
    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            
            String filePath = "/static" + path;
            InputStream is = getClass().getResourceAsStream(filePath);
            
            if (is == null) {
                String response = "404 Not Found";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }
            
            String mimeType = getMimeType(path);
            exchange.getResponseHeaders().set("Content-Type", mimeType);
            exchange.sendResponseHeaders(200, 0);
            
            OutputStream os = exchange.getResponseBody();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.close();
            is.close();
        }
        
        private String getMimeType(String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "application/javascript";
            return "text/plain";
        }
    }
    
    private String extractRegNo(HttpExchange exchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            body.append(line);
        }
        
        Map<String, String> data = objectMapper.readValue(body.toString(), Map.class);
        return data.getOrDefault("regNo", "2024CS101");
    }
    
    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String response = objectMapper.writeValueAsString(data);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
    
    public static void main(String[] args) throws IOException {
        WebServer webServer = new WebServer();
        webServer.start();
    }
}
