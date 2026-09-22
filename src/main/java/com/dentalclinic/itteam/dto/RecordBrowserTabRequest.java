package com.dentalclinic.itteam.dto;

public class RecordBrowserTabRequest {
    private String agentCode;
    private String tabTitle;
    private String urlRoute;
    private String tabCategory;
    private String status;

    public RecordBrowserTabRequest() {
    }

    public RecordBrowserTabRequest(String agentCode, String tabTitle, String urlRoute, String tabCategory, String status) {
        this.agentCode = agentCode;
        this.tabTitle = tabTitle;
        this.urlRoute = urlRoute;
        this.tabCategory = tabCategory;
        this.status = status;
    }

    public String getAgentCode() {
        return agentCode;
    }

    public void setAgentCode(String agentCode) {
        this.agentCode = agentCode;
    }

    public String getTabTitle() {
        return tabTitle;
    }

    public void setTabTitle(String tabTitle) {
        this.tabTitle = tabTitle;
    }

    public String getUrlRoute() {
        return urlRoute;
    }

    public void setUrlRoute(String urlRoute) {
        this.urlRoute = urlRoute;
    }

    public String getTabCategory() {
        return tabCategory;
    }

    public void setTabCategory(String tabCategory) {
        this.tabCategory = tabCategory;
    }

    public String getStatus() {
        return (status != null && !status.isBlank()) ? status : "OPEN";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isValid() {
        return urlRoute != null && !urlRoute.trim().isEmpty();
    }
}
