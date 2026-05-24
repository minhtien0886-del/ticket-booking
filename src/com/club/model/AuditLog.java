package com.club.model;

import java.util.Objects;

/**
 * Represents a system audit log entry tracking user actions for security and compliance.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class AuditLog {

    private static final long serialVersionUID = 1L;

    private String logId;
    private String timestamp;
    private String username;
    private String action;
    private String resource;
    private String resourceId;
    private String ipAddress;
    private String result;
    private String details;

    public AuditLog() {}

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditLog auditLog = (AuditLog) o;
        return Objects.equals(logId, auditLog.logId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logId);
    }

    public String toCsv() {
        return String.join(",",
            safe(logId), safe(timestamp), safe(username), safe(action),
            safe(resource), safe(resourceId), safe(ipAddress), safe(result), safe(details)
        );
    }

    private String safe(String s) {
        return s == null ? "" : s.contains(",") ? "\"" + s + "\"" : s;
    }

    public static AuditLog fromCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        String[] parts = csv.split(",", -1);
        AuditLog al = new AuditLog();
        al.setLogId(parts.length > 0 ? parts[0] : null);
        al.setTimestamp(parts.length > 1 ? parts[1] : null);
        al.setUsername(parts.length > 2 ? parts[2] : null);
        al.setAction(parts.length > 3 ? parts[3] : null);
        al.setResource(parts.length > 4 ? parts[4] : null);
        al.setResourceId(parts.length > 5 ? parts[5] : null);
        al.setIpAddress(parts.length > 6 ? parts[6] : null);
        al.setResult(parts.length > 7 ? parts[7] : null);
        al.setDetails(parts.length > 8 ? parts[8] : null);
        return al;
    }

    @Override
    public String toString() {
        return String.format("AuditLog{timestamp=%s, user='%s', action='%s', resource='%s', result='%s'}",
            timestamp, username, action, resource, result);
    }
}
