package com.logmonitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogMonitor {
    private MonitorDataStore database = new MonitorDataStore();

    // ActiveMQ 配置
    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String ANALYSIS_QUEUE_NAME = "AnalysisQueue";
    private static final String ALERT_QUEUE_NAME = "AlertQueue";

    public void startMonitoring() {
        try {
            // 创建连接工厂
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL);
            Connection connection = connectionFactory.createConnection();
            connection.start();

            // 创建会话
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // 消费 AnalysisResult 消息
            MessageConsumer analysisConsumer = session.createConsumer(session.createQueue(ANALYSIS_QUEUE_NAME));
            analysisConsumer.setMessageListener(message -> {
                if (message instanceof TextMessage) {
                    try {
                        String text = ((TextMessage) message).getText();
                        AnalysisResult result = AnalysisResult.fromJson(text);
                        database.addAnalysisResult(result);
                        System.out.println("收到分析结果: " + result);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // 消费 AlertMessage 消息
            MessageConsumer alertConsumer = session.createConsumer(session.createQueue(ALERT_QUEUE_NAME));
            alertConsumer.setMessageListener(message -> {
                if (message instanceof TextMessage) {
                    try {
                        String text = ((TextMessage) message).getText();
                        AlertMessage alert = AlertMessage.fromJson(text);
                        database.addAlertMessage(alert);
                        System.out.println("收到告警消息: " + alert);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // 启动 Web 服务提供RESTful接口
            startWebServer();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    private void startWebServer() {
        com.sun.net.httpserver.HttpServer server;
        try {
            server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(8080), 0);

            // 新增 API 端点 /api/monitor，返回 JSON 数据
            server.createContext("/api/monitor", exchange -> {
                if ("GET".equals(exchange.getRequestMethod())) {
                    List<Map<String, Object>> devicesData = new ArrayList<>();

                    for (int deviceId : database.getDeviceIds()) {
                        Map<String, Object> deviceData = new HashMap<>();

                        // 获取 WARN 和 ERROR 占比
                        AnalysisResult lastAnalysisResult = database.getLastAnalysisResultsHistory(deviceId);
                        double warnPercentage = lastAnalysisResult == null ? 0 : lastAnalysisResult.getWarn_percentage();
                        double errorPercentage = lastAnalysisResult == null ? 0 : lastAnalysisResult.getError_percentage();

                        // 获取最近一次 ERROR 事件时间
                        String lastErrorTimestamp = lastAnalysisResult == null ? "无数据" : lastAnalysisResult.getLast_error_timestamp();

                        // 获取分析报告发布时间
                        String anagsisTimestamp = lastAnalysisResult == null ? "无数据" : lastAnalysisResult.getAnalysis_timestamp();

                        // 获取严重告警状态和次数
                        AlertMessage alert = database.getLastAlertMessage(deviceId);
                        String alertStatus = alert != null ? alert.getAlert_message() : "无";
                        String alertTimestamp = alert != null ? alert.getTimestamp() : "0000-00-00 00:00:00";
                        int alertCount = database.getAlertCount(deviceId);

                        // 构造设备数据
                        deviceData.put("device_id", deviceId);
                        deviceData.put("warn_percentage", warnPercentage);
                        deviceData.put("error_percentage", errorPercentage);
                        deviceData.put("last_error_timestamp", lastErrorTimestamp);
                        deviceData.put("alert_status", alertStatus);
                        deviceData.put("alert_count", alertCount);
                        deviceData.put("analysis_timestamp", anagsisTimestamp);
                        deviceData.put("alert_timestamp", alertTimestamp);

                        devicesData.add(deviceData);
                    }

                    // 转换为 JSON 格式
                    ObjectMapper objectMapper = new ObjectMapper();
                    String jsonResponse = objectMapper.writeValueAsString(devicesData);

                    // 设置响应头
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); // 允许跨域访问
                    exchange.sendResponseHeaders(200, jsonResponse.getBytes("UTF-8").length);

                    // 写入响应内容
                    exchange.getResponseBody().write(jsonResponse.getBytes("UTF-8"));
                    exchange.close();
                }
            });

            server.setExecutor(null); // 使用默认线程池
            server.start();
            System.out.println("RESTful API 已启动，访问 http://localhost:8080/api/monitor 获取监控数据");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        LogMonitor logMonitor = new LogMonitor();
        logMonitor.startMonitoring();
    }
}