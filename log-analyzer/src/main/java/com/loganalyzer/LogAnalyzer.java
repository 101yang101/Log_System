package com.loganalyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LogAnalyzer {
    private static final String BROKER_URL = "tcp://localhost:61616"; // ActiveMQ 服务器地址
    private static final String LOG_QUEUE_NAME = "LogQueue"; // 日志队列名称
    private static final String ANALYSIS_QUEUE_NAME = "AnalysisQueue"; // 分析结果队列名称
    private static final String ALERT_QUEUE_NAME = "AlertQueue"; // 告警队列名称

    private static final int N = 100; // 最近N条日志
    private static final int T = 5; // 每隔T秒发布分析结果
    private static final int S = 10; // 在S秒内ERROR占比超过50%

    private Connection connection;
    private Session session;
    private MessageConsumer consumer;
    private MessageProducer analysisProducer;
    private MessageProducer alertProducer;

    private Map<Integer, Deque<LogMessage>> logBufferMap = new ConcurrentHashMap<>(); // 存储每个设备的日志缓冲区
    private Map<Integer, LogMessage> lastErrorMessageMap = new ConcurrentHashMap<>(); // 存储每个设备的最近一次 ERROR 日志

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public LogAnalyzer() throws JMSException {
        ConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
        this.connection = factory.createConnection();
        this.connection.start();
        this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // 创建消费者和生产者
        Destination logQueue = session.createQueue(LOG_QUEUE_NAME);
        this.consumer = session.createConsumer(logQueue);

        Destination analysisQueue = session.createQueue(ANALYSIS_QUEUE_NAME);
        this.analysisProducer = session.createProducer(analysisQueue);

        Destination alertQueue = session.createQueue(ALERT_QUEUE_NAME);
        this.alertProducer = session.createProducer(alertQueue);

        System.out.println("日志分析器已启动...");
    }

    public void startAnalyzing() throws JMSException, InterruptedException {
        long lastAnalysisTime = System.currentTimeMillis(); // 上次分析的时间戳

        int count = 0;
        while (true) {
            // 阻塞等待接收一条日志消息
            Message message = consumer.receive(T * 1000 - System.currentTimeMillis() + lastAnalysisTime); // 阻塞模式

            // 如果超时（未接收到消息），直接跳转到分析
            if (message == null) {
                long currentTime = System.currentTimeMillis();
                publishAnalysisResults();

                System.out.println("处理了：" + count);

                // 更新上次分析的时间戳
                lastAnalysisTime = currentTime;

                // 重置计数器
                count = 0;

                continue; // 跳过后续逻辑，进入下一次循环
            }

            count += 1;
            if (message instanceof TextMessage) {
                String jsonMessage = ((TextMessage) message).getText();

                // 解析日志消息
                LogMessage logMessage = parseLogMessage(jsonMessage);

                // 处理日志消息
                processLogMessage(logMessage);
            }

            // 获取当前时间
            long currentTime = System.currentTimeMillis();

            // 检查是否需要执行分析
            if (currentTime - lastAnalysisTime >= T * 1000) {
                publishAnalysisResults();

                System.out.println("处理了：" + count);
                count = 0;

                // 更新上次分析的时间戳
                lastAnalysisTime = currentTime;
            }
        }
    }

    public LogMessage parseLogMessage(String jsonMessage) {
        try {
            // 使用Jackson将JSON字符串解析为JsonNode对象
            JsonNode jsonNode = objectMapper.readTree(jsonMessage);

            // 提取字段值
            int deviceId = jsonNode.get("device_id").asInt();
            String timestamp = jsonNode.get("timestamp").asText();
            String logLevel = jsonNode.get("log_level").asText();
            String message = jsonNode.get("message").asText();

            // 创建并返回LogMessage对象
            return new LogMessage(deviceId, timestamp, logLevel, message);
        } catch (Exception e) {
            // 捕获任何异常并抛出自定义异常
            throw new IllegalArgumentException("Failed to parse log message: " + jsonMessage, e);
        }
    }

    private void processLogMessage(LogMessage logMessage) throws JMSException{
        int device_id = logMessage.getDevice_id();
        logBufferMap.putIfAbsent(device_id, new ArrayDeque<>(N));
        Deque<LogMessage> buffer = logBufferMap.get(device_id);

        // 添加日志到缓冲区
        if (buffer.size() >= N) {
            buffer.poll(); // 移除最早的日志
        }
        buffer.add(logMessage);

        // 如果是ERROR日志，更新最近一次ERROR日志
        if (logMessage.getLog_level().equals("ERROR")) {
            lastErrorMessageMap.put(device_id, logMessage);
        }

        // 检测是否需要告警
        checkAndPublishAlerts(device_id);
    }

    private void publishAnalysisResults() throws JMSException {
        for (Map.Entry<Integer, Deque<LogMessage>> entry : logBufferMap.entrySet()) {
            int device_id = entry.getKey();
            Deque<LogMessage> buffer = entry.getValue();

            // 计算ERROR和WARN占比
            long totalLogs = buffer.size();
            long errorCount = buffer.stream().filter(log -> log.getLog_level().equals("ERROR")).count();
            long warnCount = buffer.stream().filter(log -> log.getLog_level().equals("WARN")).count();

            double errorPercentage = totalLogs > 0 ? (double) errorCount / totalLogs * 100 : 0;
            double warnPercentage = totalLogs > 0 ? (double) warnCount / totalLogs * 100 : 0;

            // 获取最近一次ERROR事件的时间戳和日志内容
            LogMessage lastErrorMessage = lastErrorMessageMap.get(device_id); // 可能为 null

            // 如果没有最近的 ERROR 日志，提供默认值
            String errorMessageContent = lastErrorMessage != null
                    ? lastErrorMessage.getMessage() // 提取日志内容
                    : "无"; // 默认值

            String lastErrorTimestamp = lastErrorMessage != null
                    ? lastErrorMessage.getTimestamp() // 提取日志内容
                    : "0000-00-00 00:00:00"; // 默认值

            String analysisTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // 创建分析结果
            AnalysisResult result = new AnalysisResult(device_id, errorPercentage, warnPercentage, lastErrorTimestamp, errorMessageContent, analysisTimestamp);
            TextMessage analysisMessage = session.createTextMessage(result.toJson());
            analysisProducer.send(analysisMessage);
            System.out.println("设备 " + device_id + " 分析结果已发布: " + result.toJson());
        }
    }

    private void checkAndPublishAlerts(int device_id) throws JMSException {
        // 检查指定设备的日志缓冲区是否存在
        if (!logBufferMap.containsKey(device_id)) {
            System.out.println("设备 " + device_id + " 无日志记录，无需分析。");
            return;
        }

        // 获取指定设备的日志缓冲区
        Deque<LogMessage> buffer = logBufferMap.get(device_id);

        // 检查最近 S 秒内的 ERROR 占比
        long recentLogs = buffer.stream()
                .filter(log -> isWithinTimeRange(log.getTimestamp(), S))
                .count();
        long recentErrorLogs = buffer.stream()
                .filter(log -> isWithinTimeRange(log.getTimestamp(), S) && log.getLog_level().equals("ERROR"))
                .count();

        double errorPercentage = recentLogs > 0 ? (double) recentErrorLogs / recentLogs * 100 : 0;

        // 如果 ERROR 占比超过 50%，生成告警消息
        if (errorPercentage > 50) {
            // 生成告警消息
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            AlertMessage alertMessage = new AlertMessage(device_id, timestamp, "ERROR占比超过50%");
            TextMessage alert = session.createTextMessage(alertMessage.toJson());
            alertProducer.send(alert);
            System.out.println("设备 " + device_id + " 告警已发布: " + alertMessage.toJson());
        }
    }


    private boolean isWithinTimeRange(String timestamp, int seconds) {
        LocalDateTime logTime = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime now = LocalDateTime.now();
        return java.time.Duration.between(logTime, now).getSeconds() <= seconds;
    }

    public void close() throws JMSException {
        if (connection != null) {
            connection.close();
        }
    }

    public static void main(String[] args) throws JMSException, InterruptedException {
        LogAnalyzer analyzer = new LogAnalyzer();
        analyzer.startAnalyzing();
    }
}