package com.dataquadinc.dto;

public class InterviewCandidateDto {
    private String candidateId;
    private String candidateName;
    private String email;
    private String interviewStatus;
    private String interviewLevel;
    private String interviewDateTime;

    public InterviewCandidateDto(String candidateId, String candidateName, String email, String interviewStatus, String interviewLevel, String interviewDateTime) {
        this.candidateId = candidateId;
        this.candidateName = candidateName;
        this.email = email;
        this.interviewStatus = interviewStatus;
        this.interviewLevel = interviewLevel;
        this.interviewDateTime = interviewDateTime;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }


    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getInterviewStatus() {
        return interviewStatus;
    }

    public void setInterviewStatus(String interviewStatus) {
        this.interviewStatus = interviewStatus;
    }

    public String getInterviewLevel() {
        return interviewLevel;
    }

    public void setInterviewLevel(String interviewLevel) {
        this.interviewLevel = interviewLevel;
    }

    public String getInterviewDateTime() {
        return interviewDateTime;
    }

    public void setInterviewDateTime(String interviewDateTime) {
        this.interviewDateTime = interviewDateTime;
    }

// ✅ Getters & Setters


}

