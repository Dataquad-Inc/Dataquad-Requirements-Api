package com.dataquadinc.exceptions;

import java.util.List;

public class ErrorResponseClient {


    private boolean success;
    private String message;
    private List data;
    private ErrorDto error; ;


    public ErrorResponseClient() {

    }

    public ErrorResponseClient(boolean success, String message, List data, ErrorDto error) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = error;
    }

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

    public List getData() {
        return data;
    }

    public void setData(List data) {
        this.data = data;
    }

    public ErrorDto getError() {
        return error;
    }

    public void setError(ErrorDto error) {
        this.error = error;
    }
}
