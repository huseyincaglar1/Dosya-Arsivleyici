package org.huseyin.deneme.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ExtractController {

    @FXML
    private Label selectedFileLabel;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label percentLabel;

    @FXML
    private Label statusLabel;

    private List<File> selectedZipFiles;

    @FXML
    private void selectZipFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP Dosyaları", "*.zip"));
        fileChooser.setTitle("ZIP Dosyası Seçin");
        Stage stage = (Stage) selectedFileLabel.getScene().getWindow();
        selectedZipFiles = fileChooser.showOpenMultipleDialog(stage);

        if (selectedZipFiles != null && !selectedZipFiles.isEmpty()) {
            StringBuilder sb = new StringBuilder("Seçilen ZIP dosyaları:\n");
            for (File file : selectedZipFiles) {
                sb.append(file.getAbsolutePath()).append("\n");
            }
            selectedFileLabel.setText(sb.toString());
        } else {
            selectedFileLabel.setText("Seçilen dosya yok");
        }
    }

    @FXML
    private void decompressFiles() {
        if (selectedZipFiles == null || selectedZipFiles.isEmpty()) {
            showAlert("Hata", "Lütfen bir veya daha fazla ZIP dosyası seçin!");
            return;
        }

        // İlerleme çubuğunu ve etiketlerini görünür hale getir
        progressBar.setVisible(true);
        progressBar.setProgress(0);
        percentLabel.setVisible(true);
        percentLabel.setText("0%");
        statusLabel.setVisible(true);
        statusLabel.setText("İşlem başlatılıyor...");

        CountDownLatch latch = new CountDownLatch(selectedZipFiles.size());
        long startTime = System.currentTimeMillis();

        // Toplam giriş sayısını ve işlenen giriş sayısını saklamak için değişkenler
        int totalEntries = 0;
        AtomicInteger processedEntries = new AtomicInteger(0);

        // İlk olarak, toplam giriş sayısını say
        for (File selectedZipFile : selectedZipFiles) {
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(selectedZipFile))) {
                ZipEntry zipEntry;
                while ((zipEntry = zis.getNextEntry()) != null) {
                    totalEntries++;
                    zis.closeEntry();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Dosyaları çıkarmaya başla
        for (File selectedZipFile : selectedZipFiles) {
            int finalTotalEntries = totalEntries;
            new Thread(() -> {
                try {
                    File destDir = new File(selectedZipFile.getParent(), selectedZipFile.getName().replace(".zip", ""));
                    if (!destDir.exists() && !destDir.mkdir()) {
                        latch.countDown();
                        return;
                    }

                    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(selectedZipFile))) {
                        ZipEntry zipEntry;

                        while ((zipEntry = zis.getNextEntry()) != null) {
                            File newFile = new File(destDir, zipEntry.getName());

                            if (zipEntry.isDirectory()) {
                                newFile.mkdirs();
                            } else {
                                new File(newFile.getParent()).mkdirs();
                                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                                    byte[] buffer = new byte[1024];
                                    int length;
                                    while ((length = zis.read(buffer)) > 0) {
                                        fos.write(buffer, 0, length);
                                    }
                                }
                            }

                            // İşlem edilen giriş sayısını güncelle
                            int currentProcessed = processedEntries.incrementAndGet();
                            double overallProgress = (double) currentProcessed / finalTotalEntries;

                            Platform.runLater(() -> {
                                progressBar.setProgress(overallProgress);
                                percentLabel.setText(String.format("%.0f%%", overallProgress * 100));
                                statusLabel.setText(String.format("İşlenen: %d / %d", currentProcessed, finalTotalEntries));
                            });
                            zis.closeEntry();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        new Thread(() -> {
            try {
                latch.await();
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                Platform.runLater(() -> {
                    showAlert("Tamamlandı", "Tüm ZIP dosyaları başarıyla çıkarıldı! Süre: " + (duration / 1000.0) + " saniye.");
                    progressBar.setVisible(false);
                    percentLabel.setVisible(false);
                    statusLabel.setVisible(false);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }



    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/huseyin/deneme/hello-view.fxml"));
            Parent root = loader.load();
            Stage primaryStage = (Stage) selectedFileLabel.getScene().getWindow();
            primaryStage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
