package com.example.kafka_test.integration.model;


import java.util.List;

public class PeriodInfo {
    private String periodId;
    private double summ;
    private int count;
    private List<String> fileIds;

    // геттеры/сеттеры
    public String getPeriodId() { return periodId; }
    public void setPeriodId(String periodId) { this.periodId = periodId; }
    public double getSumm() { return summ; }
    public void setSumm(double summ) { this.summ = summ; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public List<String> getFileIds() { return fileIds; }
    public void setFileIds(List<String> fileIds) { this.fileIds = fileIds; }
}