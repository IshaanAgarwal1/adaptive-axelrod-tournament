package com.axelrod.adaptive.visualization;
import com.axelrod.adaptive.model.StrategyType;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataExporter {

    public static void exportHistoryToCSV(Map<StrategyType, List<Double>> history, String filename) {
        try (PrintWriter writer = new PrintWriter(filename)) {
            List<StrategyType> strategies = new ArrayList<>(history.keySet());
            
            writer.print("Generation");
            for (StrategyType st : strategies) {
                writer.print("," + st.name());
            }
            writer.println(); // Move to next line

            // 3. Write the data row by row
            int totalGenerations = history.get(strategies.get(0)).size();
            for (int g = 0; g < totalGenerations; g++) {
                writer.print(g); 
                
                for (StrategyType st : strategies) {
                    writer.print("," + history.get(st).get(g)); 
                }
                writer.print("," + String.format("%.2f", history.get(strategies.get(0)).get(g) + history.get(strategies.get(1)).get(g) + history.get(strategies.get(2)).get(g) + history.get(strategies.get(3)).get(g)));
                writer.println();
            }
            System.out.println("SUCCESS: Raw data exported to " + filename);
        } catch (Exception e) {
            System.out.println("ERROR: Could not write CSV file.");
            e.printStackTrace();
        }
    }
}