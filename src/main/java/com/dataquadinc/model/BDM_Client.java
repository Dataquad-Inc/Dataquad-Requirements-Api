package com.dataquadinc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

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
    private String id;  // Existing Client ID

    @Column(unique = true, nullable = false)
    private String clientName;

    @Column(name = "vendor_id")
    private String vendorId;

    @Column(name = "vendor_name")
    private String vendorName;

    @Column(name = "vendor_address")
    private String vendorAddress;

    @Column(name = "vendor_website_url")
    private String vendorWebsiteUrl;

    @Column(name = "vendor_linked_in_url", length = 1000)
    private String vendorLinkedInUrl;

    private String onBoardedBy;

    private String positionType;

    private int netPayment;

    private String gst;


    @JdbcTypeCode(SqlTypes.JSON)
    private List<com.dataquadinc.dto.SupportingCustomerDto> supportingCustomers;


    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "netpay")
    private List<Integer> netpay;

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

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] documentedData;

    @Column
    private String status;

    @Column(name = "invoice", length = 10)
    private String invoice;

    @Column(length = 100)
    private String location;

    private String feedBack;


    @Transient
    private int numberOfRequirements;


    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;


    @PrePersist
    public void prePersist() {
        if (this.id == null || this.id.isEmpty()) {
            this.id = "BDM"
                    + UUID.randomUUID()
                    .toString()
                    .substring(0, 8);
        }
    }

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

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getOnBoardedBy() {
        return onBoardedBy;
    }

    public void setOnBoardedBy(String onBoardedBy) {
        this.onBoardedBy = onBoardedBy;
    }

    public String getVendorAddress() {return vendorAddress;}

    public void setVendorAddress(String vendorAddress) {
        this.vendorAddress = vendorAddress;
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

    public String getGst() {
        return gst;
    }

    public void setGst(String gst) {
        this.gst = gst;
    }

    public List<com.dataquadinc.dto.SupportingCustomerDto> getSupportingCustomers() {
        return supportingCustomers;
    }

    public void setSupportingCustomers(
            List<com.dataquadinc.dto.SupportingCustomerDto> supportingCustomers) {

        this.supportingCustomers = supportingCustomers;
    }

    public List<Integer> getNetpay() {
        return netpay;
    }

    public void setNetpay(List<Integer> netpay) {
        this.netpay = netpay;
    }

    public String getVendorWebsiteUrl() {
        return vendorWebsiteUrl;
    }

    public void setVendorWebsiteUrl(String vendorWebsiteUrl) {
        this.vendorWebsiteUrl = vendorWebsiteUrl;
    }

    public String getVendorLinkedInUrl() {
        return vendorLinkedInUrl;
    }

    public void setVendorLinkedInUrl(String vendorLinkedInUrl) {
        this.vendorLinkedInUrl = vendorLinkedInUrl;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getInvoice() {
        return invoice;
    }

    public void setInvoice(String invoice) {
        this.invoice = invoice;
    }
}