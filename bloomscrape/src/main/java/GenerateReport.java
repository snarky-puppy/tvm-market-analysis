import com.fasterxml.jackson.annotation.JsonInclude;
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
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
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

        pw.write("Code\t Symbol\tTicker\tCompany Name\t Market\t Sector\t Industry\t Sub-Industry\t Delisted\t Changed\t ");
        pw.write("Acquired by Code\t Acquired by Symbol\tAcquired by Ticker\tAcquired by Company Name\tAcquired by Market\tAcquired by Sector\tAcquired by Industry\tAcquired by Sub-Industry\t Acquired by Delisted\t Acquired by Changed\t\n");

        for(BloomData data : list) {
            pw.write(data.toString()+"\n");
        }

        pw.close();

    }
}
