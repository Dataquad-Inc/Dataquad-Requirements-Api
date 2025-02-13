package com.dataquadinc.model;



import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.Set;

@Entity
@Data
public class RequirementsModel_prod {

    @Id
    private String jobId;

    @NotNull(message = "Job Title cannot be null")
    @Size(min = 3, max = 100, message = "Job Title must be between 3 and 100 characters")
    private String jobTitle;

    @NotNull(message = "Client Name cannot be null")
    @Size(min = 3, max = 100, message = "Client Name must be between 3 and 100 characters")
    private String  clientName;

    @NotNull(message = "Job Description cannot be null")
    @Column( columnDefinition = "LONGTEXT" )
//    @Size(min = 10, max = 15000, message = "Job Description must be between 10 and 15000 characters")
    private String jobDescription;

    @NotNull(message = "Job Type cannot be null")
    private String jobType;

    @NotNull(message = "Location cannot be null")
    private String location;

    @NotNull(message = "Job Mode cannot be null")
    private String jobMode;

    @NotNull(message = "Experience Required cannot be null")
    private String experienceRequired;

    @NotNull(message = "Notice Period cannot be null")
    private String noticePeriod;

    @NotNull(message = "Relevant Experience cannot be null")
    private String relevantExperience;

    @NotNull(message = "Qualification cannot be null")
    private String qualification;

    private String salaryPackage;

    private int noOfPositions;

    private LocalDateTime requirementAddedTimeStamp;



    @ElementCollection
    @CollectionTable(
            name = "job_recruiters_prod",







            joinColumns = @JoinColumn(name = "job_id")
    )

    @Column(name = "recruiter_id")
    private Set<String> recruiterIds;


//    private String recruiterId;   // Add recruiterId if you want to track the main recruiter for the job
//    private String recruiterEmail;

    private String status;
    private Set<String> recruiterName;



    @PrePersist
    public void prePersist() {
        if (this.jobId == null || this.jobId.isEmpty()) {
            generateJobId();
        }
        System.out.println("Generated Job ID: " + this.jobId); // Debug log
    }



    // Method to generate the jobId using a random number
    private void generateJobId() {
        Random random = new Random();
        int randomNumber = 1000 + random.nextInt(9000);  // Generates a random number between 1000 and 9999
        this.jobId = "JOB" + randomNumber;  // Combine the prefix "JOB" with the random number
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public @NotNull(message = "Job Title cannot be null") @Size(min = 3, max = 100, message = "Job Title must be between 3 and 100 characters") String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(@NotNull(message = "Job Title cannot be null") @Size(min = 3, max = 100, message = "Job Title must be between 3 and 100 characters") String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getSalaryPackage() {
        return salaryPackage;
    }

    public void setSalaryPackage(String salaryPackage) {
        this.salaryPackage = salaryPackage;
    }

    public int getNoOfPositions() {
        return noOfPositions;
    }

    public void setNoOfPositions(int noOfPositions) {
        this.noOfPositions = noOfPositions;
    }

    public @NotNull(message = "Client Name cannot be null") @Size(min = 3, max = 100, message = "Client Name must be between 3 and 100 characters") String getClientName() {
        return clientName;
    }

    public void setClientName(@NotNull(message = "Client Name cannot be null") @Size(min = 3, max = 100, message = "Client Name must be between 3 and 100 characters") String clientName) {
        this.clientName = clientName;
    }

    public @NotNull(message = "Job Description cannot be null") String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(@NotNull(message = "Job Description cannot be null") String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public @NotNull(message = "Job Type cannot be null") String getJobType() {
        return jobType;
    }

    public void setJobType(@NotNull(message = "Job Type cannot be null") String jobType) {
        this.jobType = jobType;
    }

    public @NotNull(message = "Location cannot be null") String getLocation() {
        return location;
    }

    public void setLocation(@NotNull(message = "Location cannot be null") String location) {
        this.location = location;
    }

    public @NotNull(message = "Job Mode cannot be null") String getJobMode() {
        return jobMode;
    }

    public void setJobMode(@NotNull(message = "Job Mode cannot be null") String jobMode) {
        this.jobMode = jobMode;
    }

    public @NotNull(message = "Experience Required cannot be null") String getExperienceRequired() {
        return experienceRequired;
    }

    public void setExperienceRequired(@NotNull(message = "Experience Required cannot be null") String experienceRequired) {
        this.experienceRequired = experienceRequired;
    }

    public @NotNull(message = "Notice Period cannot be null") String getNoticePeriod() {
        return noticePeriod;
    }

    public void setNoticePeriod(@NotNull(message = "Notice Period cannot be null") String noticePeriod) {
        this.noticePeriod = noticePeriod;
    }

    public @NotNull(message = "Relevant Experience cannot be null") String getRelevantExperience() {
        return relevantExperience;
    }

    public void setRelevantExperience(@NotNull(message = "Relevant Experience cannot be null") String relevantExperience) {
        this.relevantExperience = relevantExperience;
    }

    public @NotNull(message = "Qualification cannot be null") String getQualification() {
        return qualification;
    }

    public void setQualification(@NotNull(message = "Qualification cannot be null") String qualification) {
        this.qualification = qualification;
    }

    public LocalDateTime getRequirementAddedTimeStamp() {
        return requirementAddedTimeStamp;
    }

    public void setRequirementAddedTimeStamp(LocalDateTime requirementAddedTimeStamp) {
        this.requirementAddedTimeStamp = requirementAddedTimeStamp;
    }

    public Set<String> getRecruiterIds() {
        return recruiterIds;
    }

    public void setRecruiterIds(Set<String> recruiterIds) {
        this.recruiterIds = recruiterIds;
    }

//    public String getRecruiterId() {
//        return recruiterId;
//    }
//
//    public void setRecruiterId(String recruiterId) {
//        this.recruiterId = recruiterId;
//    }
//
//    public String getRecruiterEmail() {
//        return recruiterEmail;
//    }
//
//    public void setRecruiterEmail(String recruiterEmail) {
//        this.recruiterEmail = recruiterEmail;
//    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Set<String> getRecruiterName() {
        return recruiterName;
    }

    public void setRecruiterName(Set<String> recruiterName) {
        this.recruiterName = recruiterName;
    }
}
