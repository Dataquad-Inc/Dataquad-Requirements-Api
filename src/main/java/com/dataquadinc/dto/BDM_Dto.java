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



    private String vendorId;

    private String vendorName;


    private String vendorAddress;

    private String positionType;

    private String currency;


    @Pattern(
            regexp = "^(https?:\\/\\/)?([\\w.-]+)+(:\\d+)?(\\/.*)?$",
            message = "Invalid website URL format"
    )
    private String vendorWebsiteUrl;


    @Pattern(
            regexp = "^(https?:\\/\\/)?([\\w.-]+)+(:\\d+)?(\\/.*)?$",
            message = "Invalid LinkedIn URL format"
    )
    private String vendorLinkedInUrl;




    private int netPayment;

    private String gst;

    private String invoice;

    private List<SupportingCustomerDto> supportingCustomers;




    private List<String> clientSpocName;

    private List<String> clientSpocEmailid;

    private List<String> clientSpocLinkedin;

    private List<String> clientSpocMobileNumber;




    private List<String> supportingDocuments = new ArrayList<>();



    private String onBoardedBy;

    private String assignedTo;



    private String status;

    private String feedBack;

    private int numberOfRequirements;

    private String location;

    private String accountManager;


    @JsonIgnore
    private byte[] documentData;


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

    public String getVendorAddress() {
        return vendorAddress;
    }

    public void setVendorAddress(String vendorAddress) {
        this.vendorAddress = vendorAddress;
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

    public String getInvoice() {
        return invoice;
    }

    public void setInvoice(String invoice) {
        this.invoice = invoice;
    }

    public List<SupportingCustomerDto> getSupportingCustomers() {
        return supportingCustomers;
    }

    public void setSupportingCustomers(
            List<SupportingCustomerDto> supportingCustomers) {

        this.supportingCustomers = supportingCustomers;
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

    public void setClientSpocMobileNumber(
            List<String> clientSpocMobileNumber) {

        this.clientSpocMobileNumber = clientSpocMobileNumber;
    }

    public List<String> getSupportingDocuments() {
        return supportingDocuments;
    }

    public void setSupportingDocuments(
            List<String> supportingDocuments) {

        this.supportingDocuments = supportingDocuments;
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

    public byte[] getDocumentData() {
        return documentData;
    }

    public void setDocumentData(byte[] documentData) {
        this.documentData = documentData;
    }

    public String getCurrency() { return currency; }

    public void setCurrency(String currency) { this.currency = currency; }

    public String getAccountManager() {return accountManager;}

    public void setAccountManager(String accountManager) {this.accountManager = accountManager;}
}