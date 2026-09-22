package com.dentalclinic.itteam.dto;

public class CreateMemoryRequest {
    private String agentCode;
    private String memoryKey;
    private String memoryContent;
    private Integer priorityLevel;
    private String priority;

    public CreateMemoryRequest() {
    }

    public CreateMemoryRequest(String agentCode, String memoryKey, String memoryContent, String priority) {
        this.agentCode = agentCode;
        this.memoryKey = memoryKey;
        this.memoryContent = memoryContent;
        this.priority = priority;
    }

    public String getAgentCode() {
        return agentCode;
    }

    public void setAgentCode(String agentCode) {
        this.agentCode = agentCode;
    }

    public String getMemoryKey() {
        return memoryKey;
    }

    public void setMemoryKey(String memoryKey) {
        this.memoryKey = memoryKey;
    }

    public String getMemoryContent() {
        return memoryContent;
    }

    public void setMemoryContent(String memoryContent) {
        this.memoryContent = memoryContent;
    }

    public Integer getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(Integer priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public String getPriority() {
        if (priority != null && !priority.isBlank()) {
            return priority.toUpperCase();
        }
        if (priorityLevel != null) {
            return switch (priorityLevel) {
                case 1 -> "HIGH";
                case 2 -> "MEDIUM";
                case 3 -> "LOW";
                default -> "MEDIUM";
            };
        }
        return "MEDIUM";
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public boolean isValid() {
        return memoryKey != null && !memoryKey.trim().isEmpty()
                && memoryContent != null && !memoryContent.trim().isEmpty();
    }
}
