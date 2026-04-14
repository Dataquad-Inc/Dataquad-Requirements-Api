package com.dataquadinc.model;

import jakarta.persistence.*;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@Table(name = "BDM_Client")
public class BDM_Client {

    @Id
    private String id;  // Custom-generated ID
    @Column(unique = true, nullable = false)
    private String clientName;
    private String onBoardedBy;
    private String clientAddress;
    private String positionType;
    private int netPayment;
    private double gst;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> supportingCustomers;

    private String clientWebsiteUrl;
    @Column(length = 1000)
    private String clientLinkedInUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> clientSpocName;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> clientSpocEmailid;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> clientSpocLinkedin;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> clientSpocMobileNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> supportingDocuments;

    @Column
    private String status;

    @Transient
    private int numberOfRequirements; // 👈 won't be persisted


    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] documentedData;  // Stores actual file content

    @Column(length = 100)
    private String location;

    @PrePersist
    public void prePersist() {
        if (this.id == null || this.id.isEmpty()) {
            this.id = "BDM" + UUID.randomUUID().toString().substring(0, 8);  // Generate Unique ID
        }
    }
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private String feedBack;

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

    public String getOnBoardedBy() {
        return onBoardedBy;
    }

    public void setOnBoardedBy(String onBoardedBy) {
        this.onBoardedBy = onBoardedBy;
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

    public List<String> getSupportingDocuments() {
        return supportingDocuments;
    }

    public void setSupportingDocuments(List<String> supportingDocuments) {
        this.supportingDocuments = supportingDocuments;
    }

    public byte[] getDocumentedData() {
        return documentedData;
    }

    public void setDocumentedData(byte[] documentedData) {
        this.documentedData = documentedData;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLocation() { return location; }

    public void setLocation(String location) { this.location = location; }
}