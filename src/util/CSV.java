package util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CSV {
    public static void CSVFile(String action) {
        Date timestamp = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestampString = dateFormat.format(timestamp);
        try {
            FileWriter fileWriter = new FileWriter("audit.csv", true);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.printf("%s,%s\n", action, timestampString);
            printWriter.close();
        } catch (IOException e) {
            System.out.println("Eroare la scrierea in fisierul CSV: " + e.getMessage());
        }
    }
}
