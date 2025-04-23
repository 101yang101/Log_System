package com.logproducer;

public class LogMessage {
    private int device_id; // 设备ID，使用int类型
    private String timestamp; // 时间戳
    private String log_level; // 日志级别：INFO/WARN/ERROR
    private String message; // 日志内容

    public LogMessage(int device_id, String timestamp, String log_level, String message) {
        this.device_id = device_id;
        this.timestamp = timestamp;
        this.log_level = log_level;
        this.message = message;
    }

    // Getter 和 Setter 方法
    public int getDevice_id() {
        return device_id;
    }

    public void setDevice_id(int device_id) {
        this.device_id = device_id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getLog_level() {
        return log_level;
    }

    public void setLog_level(String log_level) {
        this.log_level = log_level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // 将对象转换为JSON字符串
    public String toJson() {
        return String.format(
                "{\"device_id\":%d,\"timestamp\":\"%s\",\"log_level\":\"%s\",\"message\":\"%s\"}",
                this.device_id, this.timestamp, this.log_level, this.message
        );
    }
}