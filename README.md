# 分布式日志采集分析与异常检测系统

## 项目介绍

本项目基于 MOM（Message-Oriented Middleware）消息队列技术（使用 ActiveMQ），实现了一个分布式日志采集、分析和实时展示的系统。系统包括多个日志采集节点、一个异常检测与统计分析微服务，以及一个实时日志可视化监控微服务。

## 功能特点

- **日志采集节点**：模拟多个设备生成日志，并将日志发布到消息队列。
- **异常检测与统计分析微服务**：订阅日志消息，进行日志分析并生成分析结果或告警消息。
- **实时日志可视化监控微服务**：提供 RESTful 接口，供前端获取日志分析结果和告警信息。
- **前端可视化展示**：实时展示日志分析结果，并绘制趋势图。

## 项目结构
```commandline
log-system/
├── log-producer/            # 日志生产者模块
│   ├── src/
│   │   └── com.logproducer/
│   │       ├── LogProducer.java          # 日志生产者主类
│   │       └── LogMessage.java           # 日志消息实体类
│   └── pom.xml                           # Maven 配置文件
│
├── log-analyzer/            # 异常检测与统计分析模块
│   ├── src/
│   │   └── com.loganalyzer/
│   │       ├── LogAnalyzer.java          # 日志分析器主类
│   │       ├── AnalysisResult.java       # 分析结果实体类
│   │       ├── AlertMessage.java         # 告警消息实体类
│   │       └── LogMessage.java           # 日志消息实体类
│   └── pom.xml                           # Maven 配置文件
│
├── log-monitor/             # 实时日志监控模块
│   ├── src/
│   │   └── com.logmonitor/
│   │       ├── LogMonitor.java           # 日志监控器主类
│   │       ├── AnalysisResult.java       # 分析结果实体类
│   │       ├── AlertMessage.java         # 告警消息实体类
│   │       └── MonitorDataStore.java     # 数据存储类
│   └── pom.xml                           # Maven 配置文件
│
└── front.html               # 前端展示界面
```

1. **log-producer**
- 模拟多个设备（用线程模拟）生成日志。
- 每隔 T 毫秒（可配置）生成一条日志消息，并发布到消息队列。

2. **log-analyzer**
- 订阅所有日志消息，按 device_id 对日志进行独立分析。
- 维护最近 N 条（可配置）日志记录，计算以下指标：ERROR 级别日志占比、WARN 级别日志占比、最近一次 ERROR 事件及其时间戳。
- 每隔 T 秒（可配置）将分析结果打包成新消息，并发布到消息队列。
- 若在 S 秒（可配置）内某设备 ERROR 占比超过 50%，生成严重告警消息并发布到消息队列。

3. **log-monitor**
- 订阅 log-analyzer 发布的分析结果和告警消息。
- 提供 RESTful 接口，供前端获取以下信息：：WARN/ERROR 占比、最近一次 ERROR 事件及其时间戳、严重告警状态、时间及次数。

4. **front.html**
- 提供实时监控界面，展示所有设备的日志分析结果和告警信息。
- 调用 log-monitor 提供的 RESTful 接口，动态更新数据。
- 使用 Chart.js 库绘制 WARN/ERROR 占比变化趋势图。


## 效果展示


## 技术栈
- **消息队列: ActiveMQ
- **编程语言: Java
- **构建工具: Maven
- **前端: HTML + JavaScript（Chart.js 用于绘图）
- **其他依赖:Jackson (JSON 序列化/反序列化)

## 安装指南

### 环境依赖

- JDK 3.8+
- Maven 3.x
- ActiveMQ（需提前安装并启动）

### 安装步骤

1. 启动 ActiveMQ

2. 配置 ActiveMQ 服务器地址：分别在LogProducer.java、LogAnalyzer.java、LogMonitor.java中配置服务器地址，格式如下：
   ```bash
   "tcp://serverhost:61616"
   ```

2. 启动 log-producer，在 log-producer 目录下，依次执行以下命令：
   ```bash
   mvn clean install
   mvn exec:java -Dexec.mainClass="com.logproducer.LogProducer"
   ```

3. 启动 log-analyzer，在 log-analyzer 目录下，依次执行以下命令：
   ```bash
   mvn clean install
   mvn exec:java -Dexec.mainClass="com.loganalyzer.LogAnalyzer"
   ```

4. 启动 log-monitor，在 log-monitor 目录下，依次执行以下命令：
   ```bash
   mvn clean install
   mvn exec:java -Dexec.mainClass="com.logmonitor.LogMonitor"
   ```

5. 打开 front.html 即可查看可视化信息


## 改进方向
- 支持更多日志级别和复杂分析逻辑。
- 增加分布式部署支持。


## 联系方式
- 邮箱：yang189256@163.com