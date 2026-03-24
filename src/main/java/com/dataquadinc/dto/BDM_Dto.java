package com.dataquadinc.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BDM_Dto {
    private String id;
    private String clientName;
    private String clientAddress;
    private String positionType;
    private int netPayment;
    private double gst;
    private List<String> supportingCustomers;
    @Pattern(regexp = "^(https?:\\/\\/)?([\\w.-]+)+(:\\d+)?(\\/.*)?$",
            message = "Invalid website URL format")
    private String clientWebsiteUrl;
    @Pattern(regexp = "^(https?:\\/\\/)?([\\w.-]+)+(:\\d+)?(\\/.*)?$",
            message = "Invalid LinkedIn URL format")
    private String clientLinkedInUrl;
    private List<String> clientSpocName;
    private List<String> clientSpocEmailid;
    private List<String> supportingDocuments =new ArrayList<>(); ; // Stores file names only
    private List<String> clientSpocLinkedin;
    private List<String> clientSpocMobileNumber;
    private String onBoardedBy;
    private String assignedTo;
    private String status;
    private String feedBack;
    private int numberOfRequirements;
    private String location;

    public int getNumberOfRequirements() {
        return numberOfRequirements;
    }

    public void setNumberOfRequirements(int numberOfRequirements) {
        this.numberOfRequirements = numberOfRequirements;
    }

    public String getFeedBack() {
        return feedBack;
    }

    public void setFeedBack(String feedBack) {
        this.feedBack = feedBack;
    }



    @JsonIgnore // Prevents file data from being included in API responses
    private byte[] documentData;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientAddress() {
        return clientAddress;
    }

    public void setClientAddress(String clientAddress) {
        this.clientAddress = clientAddress;
    }

    public String getPositionType() {
        return positionType;
    }

    public void setPositionType(String positionType) {
        this.positionType = positionType;
    }

    public int getNetPayment() {
        return netPayment;
    }

    public void setNetPayment(int netPayment) {
        this.netPayment = netPayment;
    }

    public double getGst() {
        return gst;
    }

    public void setGst(double gst) {
        this.gst = gst;
    }

    public List<String> getSupportingCustomers() {
        return supportingCustomers;
    }

    public void setSupportingCustomers(List<String> supportingCustomers) {
        this.supportingCustomers = supportingCustomers;
    }

    public String getClientWebsiteUrl() {
        return clientWebsiteUrl;
    }

    public void setClientWebsiteUrl(String clientWebsiteUrl) {
        this.clientWebsiteUrl = clientWebsiteUrl;
    }

    public String getClientLinkedInUrl() {
        return clientLinkedInUrl;
    }

    public void setClientLinkedInUrl(String clientLinkedInUrl) {
        this.clientLinkedInUrl = clientLinkedInUrl;
    }

    public List<String> getClientSpocName() {
        return clientSpocName;
    }

    public void setClientSpocName(List<String> clientSpocName) {
        this.clientSpocName = clientSpocName;
    }

    public List<String> getClientSpocEmailid() {
        return clientSpocEmailid;
    }

    public void setClientSpocEmailid(List<String> clientSpocEmailid) {
        this.clientSpocEmailid = clientSpocEmailid;
    }

    public List<String> getSupportingDocuments() {
        return supportingDocuments;
    }

    public void setSupportingDocuments(List<String> supportingDocuments) {
        this.supportingDocuments = supportingDocuments;
    }

    public List<String> getClientSpocLinkedin() {
        return clientSpocLinkedin;
    }

    public void setClientSpocLinkedin(List<String> clientSpocLinkedin) {
        this.clientSpocLinkedin = clientSpocLinkedin;
    }

    public List<String> getClientSpocMobileNumber() {
        return clientSpocMobileNumber;
    }

    public void setClientSpocMobileNumber(List<String> clientSpocMobileNumber) {
        this.clientSpocMobileNumber = clientSpocMobileNumber;
    }

    public String getOnBoardedBy() {
        return onBoardedBy;
    }

    public void setOnBoardedBy(String onBoardedBy) {
        this.onBoardedBy = onBoardedBy;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public byte[] getDocumentData() {
        return documentData;
    }

    public void setDocumentData(byte[] documentData) {
        this.documentData = documentData;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLocation() { return location; }

    public void setLocation(String location) { this.location = location;}



}