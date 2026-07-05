import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

public class CodePackerOptimized {
    public static void main(String[] args) throws IOException {
        // HIER den Pfad zu deinem Java-Paket eintragen:
        Path packageFolder = Paths.get("./src");
        Path outputFile = Paths.get("./gesamtes_projekt_kontext.md");

        StringBuilder output = new StringBuilder();
        AtomicInteger fileCount = new AtomicInteger();

        output.append("# Java Quellcode-Export\n");
        output.append("Dieses Dokument enthält die Klassen des Pakets zur Analyse.\n\n");

        if (!Files.exists(packageFolder)) {
            System.out.println("Pfad existiert nicht: " + packageFolder.toAbsolutePath());
            return;
        }

        Files.walk(packageFolder)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(path -> {
                    try {
                        fileCount.incrementAndGet();
                        output.append("## Datei: ").append(packageFolder.relativize(path)).append("\n");
                        output.append("```java\n");
                        output.append(Files.readString(path));
                        output.append("\n```\n\n---\n\n");
                    } catch (IOException e) {
                        System.err.println("Fehler bei: " + path);
                    }
                });

        Files.writeString(outputFile, output.toString());

        // Dateigröße berechnen
        long bytes = Files.size(outputFile);
        double kilobytes = bytes / 1024.0;

        System.out.println("=================================================");
        System.out.println("ERFOLG: Datei wurde generiert!");
        System.out.println("Dateiname: " + outputFile.toAbsolutePath());
        System.out.println("Verarbeitete Klassen: " + fileCount.get());
        System.out.println(String.format("Dateigröße: %.2f KB (Limit ist 100.000 KB / 100 MB)", kilobytes));
        System.out.println("=================================================");
    }
}