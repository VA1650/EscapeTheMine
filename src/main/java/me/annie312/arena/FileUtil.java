package me.annie312.arena;

import java.io.*;
import java.nio.file.Files;
import java.util.Objects;

public class FileUtil {
    public static void copy(File source, File target) throws IOException {
        if (source.isDirectory()) {
            if (!target.exists()) target.mkdirs();
            for (String child : Objects.requireNonNull(source.list())) {
                copy(new File(source, child), new File(target, child));
            }
        } else {
            try (InputStream in = Files.newInputStream(source.toPath());
                 OutputStream out = Files.newOutputStream(target.toPath())) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            }
        }
    }

    public static void delete(File file) {
        if (file.isDirectory()) {
            for (File child : Objects.requireNonNull(file.listFiles())) delete(child);
        }
        file.delete();
    }
}
