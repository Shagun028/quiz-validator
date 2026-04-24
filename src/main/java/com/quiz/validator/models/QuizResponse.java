package com.quiz.validator.models;

import java.util.List;

public class QuizResponse {
    private String regNo;
    private String setId;
    private int pollIndex;
    private List<Event> events;
    
    public String getRegNo() { return regNo; }
    public void setRegNo(String regNo) { this.regNo = regNo; }
    public String getSetId() { return setId; }
    public void setSetId(String setId) { this.setId = setId; }
    public int getPollIndex() { return pollIndex; }
    public void setPollIndex(int pollIndex) { this.pollIndex = pollIndex; }
    public List<Event> getEvents() { return events; }
    public void setEvents(List<Event> events) { this.events = events; }
}
