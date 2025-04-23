package com.logproducer;

import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class LogProducer {

    private static final String BROKER_URL = "tcp://localhost:61616"; // ActiveMQ 服务器地址
    private static final String QUEUE_NAME = "LogQueue"; // 使用队列模式
    private static final int T = 100; //配置多少ms发布一条日志
    private static final int N = 8; //设备有N台
    private static final String[] LOG_LEVELS = {
            "INFO", "INFO", "INFO", "INFO", "INFO", "INFO", "INFO", "INFO", "INFO", // 提高 INFO 出现频率
            "WARN", "WARN",                //  WARN 出现频率
            "ERROR"                        // 降低 ERROR 出现频率
    };
    private static final String[] LOG_MESSAGES = {
            "系统状态正常",
            "磁盘空间不足",
            "数据库连接失败"
    }; // 日志内容示例

    private final int deviceId; // 当前设备ID
    private final Random random = new Random();

    private Connection connection;
    private Session session;
    private MessageProducer producer;

    public LogProducer(int deviceId) throws JMSException {
        this.deviceId = deviceId;
        initialize();
    }

    // 初始化连接、会话和生产者
    private void initialize() throws JMSException {
        ConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
        connection = factory.createConnection();
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue(QUEUE_NAME);
        producer = session.createProducer(destination);
        producer.setDeliveryMode(jakarta.jms.DeliveryMode.NON_PERSISTENT); // 非持久化消息
    }

    // 关闭资源
    public void close() throws JMSException {
        if (connection != null) {
            connection.close();
        }
    }

    // 模拟日志生成并发送
    public void startProducing() {
        try {
            System.out.println("设备 " + deviceId + " 开始生成日志...");

            while (true) {
                // 随机生成日志消息
                String logLevel = LOG_LEVELS[random.nextInt(LOG_LEVELS.length)];
                String logMessageContent = LOG_MESSAGES[random.nextInt(LOG_MESSAGES.length)];
                String logTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                // 创建日志消息对象
                LogMessage logMessageObj = new LogMessage(deviceId, logTimestamp, logLevel, logMessageContent);
                // LogMessage logMessageObj = new LogMessage(deviceId, logTimestamp, "ERROR", logMessageContent);

                // 转换为JSON字符串
                String jsonMessage = logMessageObj.toJson();

                // 创建文本消息
                TextMessage message = session.createTextMessage(jsonMessage);

                // 发送消息
                producer.send(message);
                System.out.println("设备 " + deviceId + " 发送日志: " + jsonMessage);

                // 每隔T毫秒生成一条日志
                Thread.sleep(T);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                close(); // 确保资源关闭
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        // 启动多个日志采集节点（用不同设备ID模拟）
        for (int i = 1; i <= N; i++) {
            int deviceId = i;
            new Thread(() -> {
                try {
                    new LogProducer(deviceId).startProducing();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}