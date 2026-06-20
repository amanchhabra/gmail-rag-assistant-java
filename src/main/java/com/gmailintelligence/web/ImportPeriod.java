package com.gmailintelligence.web;

public enum ImportPeriod {
    SEVEN_DAYS("Last 7 days", "newer_than:7d"),
    THIRTY_DAYS("Last 30 days", "newer_than:30d"),
    NINETY_DAYS("Last 90 days", "newer_than:90d"),
    ONE_YEAR("Last year", "newer_than:1y"),
    ALL_MAIL("All mail", "");

    private final String label;
    private final String gmailQuery;

    ImportPeriod(String label, String gmailQuery) {
        this.label = label;
        this.gmailQuery = gmailQuery;
    }

    public String getLabel() {
        return label;
    }

    String gmailQuery() {
        return gmailQuery;
    }
}
