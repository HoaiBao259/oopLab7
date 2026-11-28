package operation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/** Reads and replaces the application's UTF-8, one-record-per-line data files. */
final class DataFiles {
    private DataFiles() {}

    /**
     * Reads a file, treating a missing file as an empty collection.
     *
     * @param path path relative to the project root
     * @return records in file order
     */
    static synchronized List<String> read(String path) {
        File file = new File(path);
        List<String> lines = new ArrayList<>();
        try {
            recover(file);
            if (!file.exists()) {
                return lines;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            return lines;
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read " + path, exception);
        }
    }

    /**
     * Writes complete replacement data before moving it into place.
     *
     * @param path path relative to the project root
     * @param lines complete replacement records
     */
    static synchronized void write(String path, List<String> lines) {
        File file = new File(path);
        File parent = file.getParentFile();
        File directory = parent == null ? new File(".") : parent;
        File temporary = null;
        try {
            if (!directory.isDirectory() && !directory.mkdirs()) {
                throw new IOException("Could not create " + directory);
            }
            recover(file);
            if (file.exists() && !file.isFile()) {
                throw new IOException("Expected a regular file at " + file);
            }
            temporary = File.createTempFile(file.getName() + ".", ".tmp", directory);
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(temporary), "UTF-8"))) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            replace(file, temporary);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not write " + path, exception);
        } finally {
            if (temporary != null && temporary.exists() && !temporary.delete()) {
                temporary.deleteOnExit();
            }
        }
    }

    /** Restores the old file if an interrupted replacement left only its backup. */
    static synchronized boolean exists(String path) {
        File file = new File(path);
        try {
            recover(file);
            return file.exists();
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not check " + path, exception);
        }
    }

    private static void replace(File file, File temporary) throws IOException {
        File backup = backupOf(file);
        if (file.exists() && !file.renameTo(backup)) {
            throw new IOException("Could not back up " + file);
        }
        if (!temporary.renameTo(file)) {
            recover(file);
            throw new IOException("Could not install replacement for " + file);
        }
        // Leave a backup that cannot be removed for recover() to handle on the next access.
        backup.delete();
    }

    private static void recover(File file) throws IOException {
        File backup = backupOf(file);
        if (!backup.isFile()) {
            return;
        }
        if (!file.exists() && !backup.renameTo(file)) {
            throw new IOException("Could not restore " + file);
        }
        if (backup.exists() && !backup.delete()) {
            throw new IOException("Could not remove backup " + backup);
        }
    }

    private static File backupOf(File file) {
        return new File(file.getParentFile(), file.getName() + ".bak");
    }
}
