package com.example.kafka_test;

import com.example.kafka_test.integration.IntegrationOrchestrator;
import com.example.kafka_test.integration.model.Notification;
import com.example.kafka_test.integration.model.PeriodInfo;
import com.example.kafka_test.integration.model.DownloadResult;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MainApp {

    public static void main(String[] args) {
        JFrame frame = new JFrame("Kafka + ПУМП (интерактивно)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
        frame.setLocationRelativeTo(null);

        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        frame.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Файл");

        JMenuItem checkItem = new JMenuItem("1. Проверить уведомления (Kafka)");
        JMenuItem selectItem = new JMenuItem("2. Выбрать период и получить информацию");
        JMenuItem exitItem = new JMenuItem("Выход");

        // ============ ШАГ 1. Просмотр уведомлений из Kafka ============
        checkItem.addActionListener(e -> new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> logArea.append("Читаем Kafka...\n"));

                List<Notification> notifications =
                        new KafkaNotificationReader().readNotification();

                int total = notifications.size();

                // Показываем в диалоге: всего + последние 10
                StringBuilder sb = new StringBuilder();
                if (total == 0) {
                    sb.append("Накопленных уведомлений не найдено");
                } else {
                    sb.append("Всего сообщений: ").append(total).append("\n");
                    sb.append("Последние ").append(Math.min(10, total)).append(":\n\n");
                    int start = Math.max(0, total - 10);
                    for (int i = start; i < total; i++) {
                        sb.append(i + 1).append(") ")
                                .append(notifications.get(i).getRawMessage())
                                .append("\n");
                    }
                }

                String text = sb.toString();
                SwingUtilities.invokeLater(() -> {
                    logArea.append("Получено уведомлений: " + total + "\n\n");
                    JTextArea dialogArea = new JTextArea(text);
                    dialogArea.setEditable(false);
                    dialogArea.setRows(15);
                    dialogArea.setColumns(50);
                    JOptionPane.showMessageDialog(frame,
                            new JScrollPane(dialogArea),
                            "Уведомления из Kafka",
                            JOptionPane.INFORMATION_MESSAGE);
                });

            } catch (Exception ex) {
                showError(frame, ex);
            }
        }).start());

        selectItem.addActionListener(e -> new Thread(() -> {
            try {
                IntegrationOrchestrator orch = new IntegrationOrchestrator();

                // 2.1. Ввод периода — через массив
                final String[] periodHolder = new String[1];
                SwingUtilities.invokeAndWait(() -> {
                    periodHolder[0] = JOptionPane.showInputDialog(frame,
                            "Введите период (YYYY-MM-DD):", "2026-01-01");
                });

                String periodId = periodHolder[0];
                if (periodId == null || periodId.isBlank()) {
                    SwingUtilities.invokeLater(() ->
                            logArea.append("Период не выбран\n\n"));
                    return;
                }
                periodId = periodId.trim();
                final String finalPeriod = periodId;

                SwingUtilities.invokeLater(() ->
                        logArea.append("Запрос метаданных за период: " + finalPeriod + "\n"));

                // 2.2. Метаданные — без скачивания
                PeriodInfo info = orch.fetchPeriodInfo(finalPeriod);
                SwingUtilities.invokeLater(() ->
                        logArea.append("  Сумма: " + info.getSumm()
                                + ", счетов: " + info.getCount()
                                + ", файлов: " + info.getFileIds().size() + "\n"));

                // 2.3. Подтверждение — через массив
                String message = "Период: " + finalPeriod + "\n"
                        + "Сумма: " + info.getSumm() + "\n"
                        + "Количество счетов: " + info.getCount() + "\n"
                        + "Файлов к скачиванию: " + info.getFileIds().size() + "\n\n"
                        + "Скачать файлы?";

                final int[] confirmHolder = new int[1];
                SwingUtilities.invokeAndWait(() -> {
                    confirmHolder[0] = JOptionPane.showConfirmDialog(frame, message, "Подтверждение",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                });

                if (confirmHolder[0] != JOptionPane.OK_OPTION) {
                    SwingUtilities.invokeLater(() ->
                            logArea.append("Скачивание отменено\n\n"));
                    return;
                }

                // 2.4. Скачивание
                SwingUtilities.invokeLater(() -> logArea.append("Скачиваем...\n"));
                DownloadResult result = orch.downloadFiles(info);
                SwingUtilities.invokeLater(() -> {
                    logArea.append("  Скачано: " + result.getSavedPaths().size() + "\n");
                    for (String p : result.getSavedPaths()) {
                        logArea.append("    → " + p + "\n");
                    }
                    if (!result.getErrors().isEmpty()) {
                        logArea.append("  Ошибки: " + result.getErrors() + "\n");
                    }
                    logArea.append("\n");
                });

            } catch (Exception ex) {
                showError(frame, ex);
            }
        }).start());

        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(checkItem);
        fileMenu.add(selectItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        frame.setJMenuBar(menuBar);
        frame.setVisible(true);
    }

    private static void showError(JFrame frame, Exception ex) {
        ex.printStackTrace();
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(frame,
                        "Ошибка: " + (ex.getMessage() != null ? ex.getMessage() : ex.toString())));
    }
}