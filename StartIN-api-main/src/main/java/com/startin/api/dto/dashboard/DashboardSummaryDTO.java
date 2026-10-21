package com.startin.api.dto.dashboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DashboardSummaryDTO {

    private String startupName;
    private String greeting;
    private int savedOpportunitiesCount;
    private int newMatchesCount;
    private int deadlinesThisWeekCount;
    private int trackingCount;
    private int pendingActionsCount;
    private String pendingActionsText;
    private List<Map<String, String>> upcomingDeadlines = new ArrayList<>();
    private List<Map<String, String>> recentActivities = new ArrayList<>();

    public DashboardSummaryDTO() {
    }

    public String getStartupName() { return startupName; }
    public void setStartupName(String startupName) { this.startupName = startupName; }
    public String getGreeting() { return greeting; }
    public void setGreeting(String greeting) { this.greeting = greeting; }
    public int getSavedOpportunitiesCount() { return savedOpportunitiesCount; }
    public void setSavedOpportunitiesCount(int savedOpportunitiesCount) { this.savedOpportunitiesCount = savedOpportunitiesCount; }
    public int getNewMatchesCount() { return newMatchesCount; }
    public void setNewMatchesCount(int newMatchesCount) { this.newMatchesCount = newMatchesCount; }
    public int getDeadlinesThisWeekCount() { return deadlinesThisWeekCount; }
    public void setDeadlinesThisWeekCount(int deadlinesThisWeekCount) { this.deadlinesThisWeekCount = deadlinesThisWeekCount; }
    public int getTrackingCount() { return trackingCount; }
    public void setTrackingCount(int trackingCount) { this.trackingCount = trackingCount; }
    public int getPendingActionsCount() { return pendingActionsCount; }
    public void setPendingActionsCount(int pendingActionsCount) { this.pendingActionsCount = pendingActionsCount; }
    public String getPendingActionsText() { return pendingActionsText; }
    public void setPendingActionsText(String pendingActionsText) { this.pendingActionsText = pendingActionsText; }
    public List<Map<String, String>> getUpcomingDeadlines() { return upcomingDeadlines; }
    public void setUpcomingDeadlines(List<Map<String, String>> upcomingDeadlines) { this.upcomingDeadlines = upcomingDeadlines; }
    public List<Map<String, String>> getRecentActivities() { return recentActivities; }
    public void setRecentActivities(List<Map<String, String>> recentActivities) { this.recentActivities = recentActivities; }
}
