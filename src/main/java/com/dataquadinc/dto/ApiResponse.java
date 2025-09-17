package com.dataquadinc.dto;


import com.dataquadinc.exceptions.ErrorDto;

public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private ErrorDto error;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, String message, T data, ErrorDto error) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = error;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public ErrorDto getError() {
        return error;
    }

    public void setError(ErrorDto error) {
        this.error = error;
    }
}
