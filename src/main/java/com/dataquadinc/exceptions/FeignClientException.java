package com.dataquadinc.exceptions;

public class FeignClientException extends RuntimeException {

    private int status;

    private String message;

    private String errorMessage;

    public FeignClientException(int status, String errorMessage, String message) {
        this.status = status;
        this.errorMessage = errorMessage;
        this.message = message;
    }

    public FeignClientException() {
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
