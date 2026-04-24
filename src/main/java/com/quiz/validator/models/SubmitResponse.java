package com.quiz.validator.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SubmitResponse {
    private boolean isCorrect;
    private boolean isIdempotent;
    private int submittedTotal;
    private int expectedTotal;
    private String message;
    
    @JsonProperty("isCorrect")
    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }
    
    @JsonProperty("isIdempotent")
    public boolean isIdempotent() { return isIdempotent; }
    public void setIdempotent(boolean idempotent) { isIdempotent = idempotent; }
    
    public int getSubmittedTotal() { return submittedTotal; }
    public void setSubmittedTotal(int submittedTotal) { this.submittedTotal = submittedTotal; }
    
    public int getExpectedTotal() { return expectedTotal; }
    public void setExpectedTotal(int expectedTotal) { this.expectedTotal = expectedTotal; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
