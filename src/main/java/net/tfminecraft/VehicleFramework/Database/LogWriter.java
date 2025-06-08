package net.tfminecraft.VehicleFramework.Database;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogWriter {

    private final File logDir;
    private final File olderDir;
    private final File logFile;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");

    public LogWriter(File pluginFolder) {
        this.logDir = new File(pluginFolder, "logs");
        this.olderDir = new File(logDir, "older");

        // Ensure directories exist
        if (!olderDir.exists()) {
            olderDir.mkdirs();
        }

        this.logFile = new File(logDir, "log.txt");
        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void logEntry(String s) {
        try {
            // Write log entry
            try (FileWriter fw = new FileWriter(logFile, true);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write("[" + new Date() + "] " + s);
                bw.newLine();
            }

            // Check if rotation is needed
            long lineCount = Files.lines(logFile.toPath()).count();
            if (lineCount > 400) {
                rotateLog();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void rotateLog() {
        try {
            String dateStr = dateFormat.format(new Date());
            File archivedLog = new File(olderDir, "log_" + dateStr + ".txt");

            // If file already exists (multiple rotations in one day), add a counter
            int counter = 1;
            while (archivedLog.exists()) {
                archivedLog = new File(olderDir, "log_" + dateStr + "_" + counter + ".txt");
                counter++;
            }

            Files.move(logFile.toPath(), archivedLog.toPath());

            // Create a fresh log file
            logFile.createNewFile();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

