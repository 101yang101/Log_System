package com.loganalyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AnalysisResult {
    private int device_id; // 设备ID
    private double error_percentage; // ERROR 日志占比
    private double warn_percentage; // WARN 日志占比
    private String last_error_timestamp; // 最近一次 ERROR 时间戳
    private String last_error_message; // 最近一次 ERROR 的日志内容
    private String analysis_timestamp;  // 分析报告发布时间

    public AnalysisResult(int device_id, double error_percentage, double warn_percentage, String last_error_timestamp, String last_error_message, String analysis_timestamp) {
        this.device_id = device_id;
        this.error_percentage = error_percentage;
        this.warn_percentage = warn_percentage;
        this.last_error_timestamp = last_error_timestamp;
        this.last_error_message = last_error_message;
        this.analysis_timestamp = analysis_timestamp;
    }

    // Getter 和 Setter 方法
    public int getDevice_id() {
        return device_id;
    }

    public void setDevice_id(int device_id) {
        this.device_id = device_id;
    }

    public double getError_percentage() {
        return error_percentage;
    }

    public void setError_percentage(double error_percentage) {
        this.error_percentage = error_percentage;
    }

    public double getWarn_percentage() {
        return warn_percentage;
    }

    public void setWarn_percentage(double warn_percentage) {
        this.warn_percentage = warn_percentage;
    }

    public String getLast_error_timestamp() {
        return last_error_timestamp;
    }

    public void setLast_error_timestamp(String last_error_timestamp) {
        this.last_error_timestamp = last_error_timestamp;
    }

    public String getLast_error_message() {
        return last_error_message;
    }

    public void setLast_error_message(String last_error_message) {
        this.last_error_message = last_error_message;
    }

    public String getAnalysis_timestamp() {
        return analysis_timestamp;
    }

    public void setAnalysis_timestamp(String analysis_timestamp) {
        this.analysis_timestamp = analysis_timestamp;
    }

    // 将对象转换为JSON字符串
    public String toJson() {
        return String.format(
                "{\"device_id\":%d,\"error_percentage\":%.2f,\"warn_percentage\":%.2f,\"last_error_timestamp\":\"%s\",\"last_error_message\":\"%s\",\"analysis_timestamp\":\"%s\"}",
                this.device_id, this.error_percentage, this.warn_percentage, this.last_error_timestamp, this.last_error_message, this.analysis_timestamp
        );
    }

    // 从JSON字符串解析为AnalysisResult对象
    public static AnalysisResult fromJson(String jsonMessage) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // 使用Jackson将JSON字符串解析为JsonNode对象
            JsonNode jsonNode = objectMapper.readTree(jsonMessage);

            // 提取字段值
            int device_id = jsonNode.get("device_id").asInt();
            double error_percentage = jsonNode.get("error_percentage").asDouble();
            double warn_percentage = jsonNode.get("warn_percentage").asDouble();
            String last_error_timestamp = jsonNode.get("last_error_timestamp").asText();
            String last_error_message = jsonNode.get("last_error_message").asText();
            String analysis_timestamp = jsonNode.get("analysis_timestamp").asText();

            // 创建并返回AnalysisResult对象
            return new AnalysisResult(device_id, error_percentage, warn_percentage, last_error_timestamp, last_error_message, analysis_timestamp);
        } catch (Exception e) {
            // 捕获任何异常并抛出自定义异常
            throw new IllegalArgumentException("Failed to parse AnalysisResult from JSON: " + jsonMessage, e);
        }
    }
}