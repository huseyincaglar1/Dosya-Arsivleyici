package org.huseyin.deneme.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CompressController {

    @FXML
    private Label selectedFileLabel;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label progressLabel; // İlerleme yüzdesini göstermek için etiket

    private List<File> selectedDirectories;
    private long startTime;

    @FXML
    private void selectFiles() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Klasör Seçin");

        selectedDirectories = new ArrayList<>();

        Stage stage = (Stage) selectedFileLabel.getScene().getWindow();
        boolean selecting = true;

        while (selecting) {
            File selectedDirectory = directoryChooser.showDialog(stage);
            if (selectedDirectory != null) {
                if (!selectedDirectories.contains(selectedDirectory)) {
                    selectedDirectories.add(selectedDirectory);
                    selectedFileLabel.setText("Seçilen klasörler:\n" + getSelectedDirectoriesText());
                } else {
                    showAlert("Bilgi", "Bu klasör zaten seçildi!");
                }
            } else {
                selecting = false;
            }
        }

        if (selectedDirectories.isEmpty()) {
            selectedFileLabel.setText("Seçilen klasör yok");
        }
    }

    private String getSelectedDirectoriesText() {
        StringBuilder sb = new StringBuilder();
        for (File directory : selectedDirectories) {
            sb.append(directory.getAbsolutePath()).append("\n");
        }
        return sb.toString();
    }

    @FXML
    private void compressFiles() {
        startTime = System.currentTimeMillis();

        if (selectedDirectories == null || selectedDirectories.isEmpty()) {
            showAlert("Hata", "Lütfen en az bir klasör seçin!");
            return;
        }

        progressBar.setVisible(true);
        progressLabel.setVisible(true);
        progressBar.setProgress(0);
        progressLabel.setText("0%");

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<?>> futures = new ArrayList<>();
        AtomicInteger currentFileCount = new AtomicInteger(0);
        int totalFiles = calculateTotalFiles(selectedDirectories); // Toplam dosya sayısını hesapla

        for (File directory : selectedDirectories) {
            futures.add(executor.submit(() -> {
                String zipFileName = directory.getAbsolutePath() + ".zip";
                try (FileOutputStream fos = new FileOutputStream(zipFileName);
                     ZipOutputStream zos = new ZipOutputStream(fos)) {

                    zipDirectory(directory, directory.getName(), zos, currentFileCount, totalFiles);

                } catch (IOException e) {
                    Platform.runLater(() -> showAlert("Hata", "Bir hata oluştu: " + e.getMessage()));
                }
            }));
        }

        executor.shutdown();

        // Tüm görevlerin bitmesini bekle
        new Thread(() -> {
            try {
                for (Future<?> future : futures) {
                    future.get(); // Her bir görevin bitmesini bekle
                }
                long duration = (System.currentTimeMillis() - startTime) / 1000;
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    progressLabel.setVisible(false);
                    showAlert("Tamamlandı", "Tüm klasörler başarıyla sıkıştırıldı.\nSüre: " + duration + " saniye.");
                });
            } catch (InterruptedException | ExecutionException e) {
                Platform.runLater(() -> showAlert("Hata", "Bir hata oluştu: " + e.getMessage()));
            }
        }).start();
    }
    private int calculateTotalFiles(List<File> directories) {
        int totalFiles = 0;
        for (File directory : directories) {
            totalFiles += countFiles(directory);
        }
        return totalFiles;
    }

    private int countFiles(File directory) {
        int count = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    count += countFiles(file);
                } else {
                    count++;
                }
            }
        }
        return count;
    }





    private void zipDirectory(File directory, String entryName, ZipOutputStream zos, AtomicInteger currentFileCount, int totalFiles) throws IOException {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Bu dosya bir dizin değil: " + directory.getName());
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectory(file, entryName + "/" + file.getName(), zos, currentFileCount, totalFiles);
            } else {
                zipFile(file, zos, entryName, currentFileCount, totalFiles);
            }
        }
    }

    private void zipFile(File file, ZipOutputStream zos, String entryName, AtomicInteger currentFileCount, int totalFiles) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(entryName + "/" + file.getName());
            zos.putNextEntry(zipEntry);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            zos.closeEntry();

            // İlerleme güncellemesi
            int currentFile = currentFileCount.incrementAndGet();
            double progress = (double) currentFile / totalFiles;
            Platform.runLater(() -> {
                progressBar.setProgress(progress);
                progressLabel.setText((int) (progress * 100) + "%");
            });
        }
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
            Scene scene = new Scene(root);
            Stage primaryStage = (Stage) selectedFileLabel.getScene().getWindow();
            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
