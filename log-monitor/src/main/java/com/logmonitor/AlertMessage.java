package com.logmonitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AlertMessage {
    private int device_id; // 设备ID
    private String alert_message; // 告警信息
    private String timestamp; // 告警时间戳

    public AlertMessage(int device_id, String timestamp, String alert_message) {
        this.device_id = device_id;
        this.alert_message = alert_message;
        this.timestamp = timestamp;
    }

    // Getter 和 Setter 方法
    public int getDevice_id() {
        return device_id;
    }

    public void setDevice_id(int device_id) {
        this.device_id = device_id;
    }

    public String getAlert_message() {
        return alert_message;
    }

    public void setAlert_message(String alert_message) {
        this.alert_message = alert_message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    // 将对象转换为JSON字符串
    public String toJson() {
        return String.format(
                "{\"device_id\":%d,\"alert_message\":\"%s\",\"timestamp\":\"%s\"}",
                this.device_id, this.alert_message, this.timestamp
        );
    }

    // 从JSON字符串解析为AlertMessage对象
    public static AlertMessage fromJson(String jsonMessage) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // 使用Jackson将JSON字符串解析为JsonNode对象
            JsonNode jsonNode = objectMapper.readTree(jsonMessage);

            // 提取字段值
            int device_id = jsonNode.get("device_id").asInt();
            String alert_message = jsonNode.get("alert_message").asText();
            String timestamp = jsonNode.get("timestamp").asText();

            // 创建并返回AlertMessage对象
            return new AlertMessage(device_id, timestamp, alert_message);
        } catch (Exception e) {
            // 捕获任何异常并抛出自定义异常
            throw new IllegalArgumentException("Failed to parse AlertMessage from JSON: " + jsonMessage, e);
        }
    }
}