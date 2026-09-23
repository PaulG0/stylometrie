package fr.unicaen.model;

public class AnalysisResult {
    private int id;
    private int textId;
    private String metricName;
    private double metricValue;

    public AnalysisResult(int id, int textId, String metricName, double metricValue) {
        this.id = id;
        this.textId = textId;
        this.metricName = metricName;
        this.metricValue = metricValue;
    }

    public int getId() { return id; }
    public int getTextId() { return textId; }
    public String getMetricName() { return metricName; }
    public double getMetricValue() { return metricValue; }

    public void setId(int id) { this.id = id; }
    public void setTextId(int textId) { this.textId = textId; }
    public void setMetricName(String metricName) { this.metricName = metricName; }
    public void setMetricValue(double metricValue) { this.metricValue = metricValue; }
}