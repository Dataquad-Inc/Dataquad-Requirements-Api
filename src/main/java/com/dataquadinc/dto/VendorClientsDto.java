package com.dataquadinc.dto;

import lombok.Data;

import java.util.List;

@Data
public class VendorClientsDto {

    private String vendorId;
    private String vendorName;
    private List<String> clients;
}