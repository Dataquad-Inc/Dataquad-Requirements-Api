package com.dataquadinc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class InProgressRequirementDTO {
    private String recruiterId;
    private String recruiterName;
    private String jobId;
    private String clientName;
    private String bdm;
    private String teamlead;
    private String technology;
    private LocalDate postedDate;
    private LocalDateTime updatedDateTime;
    private long numberOfSubmissions;
    private long numberOfScreenReject;
    private String jobTitle;
    private String jobMode;
    private String jobType;
    private String experienceRequired;
    private String relevantExperience;


    public InProgressRequirementDTO(
            String recruiterId,
            String recruiterName,
            String jobId,
            String clientName,
            String bdm,
            String teamlead,
            String technology,
            LocalDate postedDate,
            LocalDateTime updatedDateTime,
            long numberOfSubmissions,
            long numberOfScreenReject,
            String jobTitle,
            String jobMode,
            String jobType,
            String experienceRequired,
            String relevantExperience

    ) {
        this.recruiterId = recruiterId;
        this.recruiterName = recruiterName;
        this.jobId = jobId;
        this.clientName = clientName;
        this.bdm = bdm;
        this.teamlead = teamlead;
        this.technology = technology;
        this.postedDate = postedDate;
        this.updatedDateTime = updatedDateTime;
        this.numberOfSubmissions = numberOfSubmissions;
        this.numberOfScreenReject = numberOfScreenReject;
        this.jobTitle = jobTitle;
        this.jobMode = jobMode;
        this.jobType = jobType;
        this.experienceRequired = experienceRequired;
        this.relevantExperience = relevantExperience;
    }


    public String getRecruiterId() {
        return recruiterId;
    }

    public void setRecruiterId(String recruiterId) {
        this.recruiterId = recruiterId;
    }

    public String getRecruiterName() {
        return recruiterName;
    }

    public void setRecruiterName(String recruiterName) {
        this.recruiterName = recruiterName;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getBdm() {
        return bdm;
    }

    public void setBdm(String bdm) {
        this.bdm = bdm;
    }

    public String getTeamlead() {
        return teamlead;
    }

    public void setTeamlead(String teamlead) {
        this.teamlead = teamlead;
    }

    public String getTechnology() {
        return technology;
    }

    public void setTechnology(String technology) {
        this.technology = technology;
    }

    public LocalDate getPostedDate() {
        return postedDate;
    }

    public void setPostedDate(LocalDate postedDate) {
        this.postedDate = postedDate;
    }

    public LocalDateTime getUpdatedDateTime() {
        return updatedDateTime;
    }

    public void setUpdatedDateTime(LocalDateTime updatedDateTime) {
        this.updatedDateTime = updatedDateTime;
    }

    public long getNumberOfSubmissions() {
        return numberOfSubmissions;
    }

    public void setNumberOfSubmissions(long numberOfSubmissions) {
        this.numberOfSubmissions = numberOfSubmissions;
    }

    public long getNumberOfScreenReject() {
        return numberOfScreenReject;
    }

    public void setNumberOfScreenReject(long numberOfScreenReject) {
        this.numberOfScreenReject = numberOfScreenReject;
    }

    public String getJobMode() {
        return jobMode;
    }

    public void setJobMode(String jobMode) {
        this.jobMode = jobMode;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getExperienceRequired() {
        return experienceRequired;
    }

    public void setExperienceRequired(String experienceRequired) {
        this.experienceRequired = experienceRequired;
    }

    public String getRelevantExperience() {
        return relevantExperience;
    }

    public void setRelevantExperience(String relevantExperience) {
        this.relevantExperience = relevantExperience;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }
}
