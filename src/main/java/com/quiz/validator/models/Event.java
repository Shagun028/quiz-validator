package com.quiz.validator.models;

public class Event {
    private String roundId;
    private String participant;
    private int score;
    
    public String getRoundId() { return roundId; }
    public void setRoundId(String roundId) { this.roundId = roundId; }
    public String getParticipant() { return participant; }
    public void setParticipant(String participant) { this.participant = participant; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
}
