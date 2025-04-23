package com.logmonitor;

import java.util.*;

public class MonitorDataStore {
    private Map<Integer, List<AnalysisResult>> analysisResultsHistory = new HashMap<>();
    private Map<Integer, List<AlertMessage>> alertMessagesHistory = new HashMap<>();

    public synchronized void addAnalysisResult(AnalysisResult result) {
        int device_id = result.getDevice_id();
        analysisResultsHistory.putIfAbsent(device_id, new ArrayList<>());
        analysisResultsHistory.get(device_id).add(result);
    }

    public synchronized List<AnalysisResult> getAnalysisResultsHistory(int device_id) {
        return analysisResultsHistory.getOrDefault(device_id, new ArrayList<>());
    }

    public synchronized AnalysisResult getLastAnalysisResultsHistory(int device_id) {
        // 获取指定设备的历史分析结果列表
        List<AnalysisResult> results = analysisResultsHistory.getOrDefault(device_id, new ArrayList<>());

        // 如果列表为空，返回 null 或抛出异常（根据需求）
        if (results.isEmpty()) {
            System.out.println("No analysis results found for device ID: " + device_id);
            return null;
        }

        // 返回列表中的最后一个元素（即最后一次分析结果）
        return results.get(results.size() - 1);
    }

    // 添加告警信息
    public synchronized void addAlertMessage(AlertMessage alert) {
        int device_id = alert.getDevice_id();
        alertMessagesHistory.putIfAbsent(device_id, new ArrayList<>());
        alertMessagesHistory.get(device_id).add(alert);
    }

    // 获取告警信息列表
    public synchronized List<AlertMessage> getAlertMessagesHistory(int device_id) {
        return alertMessagesHistory.getOrDefault(device_id, new ArrayList<>());
    }

    // 获取某设备最后一条告警信息
    public synchronized AlertMessage getLastAlertMessage(int device_id) {
        List<AlertMessage> alerts = alertMessagesHistory.getOrDefault(device_id, new ArrayList<>());

        if (alerts.isEmpty()) {
            System.out.println("No alert messages found for device ID: " + device_id);
            return null;
        }

        return alerts.get(alerts.size() - 1); // 返回最后一个元素
    }

    // 获取某设备的告警次数
    public synchronized int getAlertCount(int device_id) {
        List<AlertMessage> alerts = alertMessagesHistory.getOrDefault(device_id, new ArrayList<>());
        return alerts.size(); // 返回告警信息的数量
    }

    // 获取所有设备ID列表
    public synchronized List<Integer> getDeviceIds() {
        Set<Integer> deviceIds = new HashSet<>();

        // 添加来自 analysisResultsHistory 的设备 ID
        deviceIds.addAll(analysisResultsHistory.keySet());

        // 添加来自 alertMessagesHistory 的设备 ID
        deviceIds.addAll(alertMessagesHistory.keySet());

        // 返回排序后的设备 ID 列表
        List<Integer> sortedDeviceIds = new ArrayList<>(deviceIds);
        Collections.sort(sortedDeviceIds);
        return sortedDeviceIds;
    }
}