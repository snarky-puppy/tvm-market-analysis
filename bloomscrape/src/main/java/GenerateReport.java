import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by matt on 25/10/16.
 */
public class GenerateReport {

    public static void main(String[] args) throws IOException {
        final String outputPath = "data";
        ObjectMapper mapper = new ObjectMapper();
        List<BloomData> list = new ArrayList<>();

        Files.walk(Paths.get(outputPath))
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(p -> {
                    try {
                        list.add(mapper.readValue(p.toFile(), BloomData.class));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        System.out.printf("Loaded %d objects\n", list.size());

        File output = new File("report.csv");

        PrintWriter pw = new PrintWriter(new FileOutputStream(output));

        pw.write("Code, Symbol,Ticker,Company Name, Market, Sector, Industry, Sub-Industry, ");
        pw.write("Acquired by Code, Acquired by Symbol,Acquired by Ticker,Acquired by Company Name,Acquired by Market,Acquired by Sector,Acquired by Industry,Acquired by Sub-Industry\n");

        for(BloomData data : list) {
            pw.write(data.toString()+"\n");
        }

        pw.close();

    }
}
