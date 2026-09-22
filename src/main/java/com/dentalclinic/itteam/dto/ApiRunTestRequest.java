package com.dentalclinic.itteam.dto;

public class ApiRunTestRequest {
    private String endpoint;
    private String httpMethod;
    private String requestPayload;

    public ApiRunTestRequest() {
    }

    public ApiRunTestRequest(String endpoint, String httpMethod, String requestPayload) {
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
        this.requestPayload = requestPayload;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getHttpMethod() {
        return (httpMethod != null && !httpMethod.isBlank()) ? httpMethod.toUpperCase() : "GET";
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public boolean isValid() {
        return endpoint != null && !endpoint.trim().isEmpty();
    }
}
