package com.axelrod.adaptive.ui;

import com.axelrod.adaptive.evolution.EvolutionEngine;
import com.axelrod.adaptive.model.StrategyType;
import com.axelrod.adaptive.visualization.PopulationGraph;
import com.axelrod.adaptive.visualization.DataExporter;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StrategyEvolutionUI extends JFrame {

    private final java.util.Map<StrategyType, JCheckBox> checkboxes = new java.util.HashMap<>();

    private final JTextField roundsField = new JTextField("100", 6);
    private final JTextField noiseField = new JTextField("0.05", 6);
    private final JTextField mutationField = new JTextField("0.05", 6);
    private final JTextField popSizeField = new JTextField("2500", 6); // Defaulted to 2500 for a nice 50x50 grid!
    private final JTextField generationsField = new JTextField("500", 6);
    private final JTextField trialsField = new JTextField("1", 6);
    
    // Payoff Matrix Text Fields
    private final JTextField tField = new JTextField("5", 6);
    private final JTextField rField = new JTextField("3", 6);
    private final JTextField pField = new JTextField("1", 6);
    private final JTextField sField = new JTextField("0", 6);

    // NEW: Starvation Threshold
    private final JTextField starvationField = new JTextField("150", 6);
    
    // UI Additions for the new features
    private final JComboBox<String> graphTypeCombo = new JComboBox<>(new String[]{"Line Chart", "Stacked Area Chart", "Final Bar Chart", "Grid Only (Spatial)"});
    private final JCheckBox spatialModeCheckBox = new JCheckBox("Enable Spatial Mode (Grid World)");

    private final JLabel statusLabel = new JLabel("Ready");

    public StrategyEvolutionUI() {
        super("Strategy Evolution — Configuration");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel configPanel = new JPanel(new GridLayout(14, 2, 5, 5)); // Increased grid rows to 14
        configPanel.setBorder(BorderFactory.createTitledBorder("Simulation Parameters"));
        
        configPanel.add(new JLabel("Rounds per Match:"));
        configPanel.add(roundsField);
        configPanel.add(new JLabel("Noise Rate (e.g., 0.05):"));
        configPanel.add(noiseField);
        configPanel.add(new JLabel("Mutation Rate (e.g., 0.05):"));
        configPanel.add(mutationField);
        configPanel.add(new JLabel("Population Size:"));
        configPanel.add(popSizeField);
        configPanel.add(new JLabel("Generations:"));
        configPanel.add(generationsField);
        configPanel.add(new JLabel("Trials (Averaging):"));
        configPanel.add(trialsField);
        
        // Custom payoff matrix inputs
        configPanel.add(new JLabel("Temptation (Defect vs Coop):"));
        configPanel.add(tField);
        configPanel.add(new JLabel("Reward (Mutual Coop):"));
        configPanel.add(rField);
        configPanel.add(new JLabel("Punishment (Mutual Defect):"));
        configPanel.add(pField);
        configPanel.add(new JLabel("Sucker (Coop vs Defect):"));
        configPanel.add(sField);

        // Starvation Threshold Input
        configPanel.add(new JLabel("Starvation Threshold:"));
        configPanel.add(starvationField);

        configPanel.add(new JLabel("Graph Type:"));
        configPanel.add(graphTypeCombo);
        
        configPanel.add(new JLabel("Spatial Evolution:"));
        configPanel.add(spatialModeCheckBox);

        JPanel strategyPanel = new JPanel(new GridLayout(0, 2));
        strategyPanel.setBorder(BorderFactory.createTitledBorder("Include Strategies"));
        for (StrategyType st : StrategyType.values()) {
            // NEW: Skip the EMPTY strategy so users can't check it manually!
            if (st == StrategyType.EMPTY) continue; 
            
            JCheckBox cb = new JCheckBox(st.name(), true);
            checkboxes.put(st, cb);
            strategyPanel.add(cb);
        }

        JButton runButton = new JButton("Run Simulation");
        runButton.addActionListener(e -> runSimulation());

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(runButton, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        add(configPanel, BorderLayout.NORTH);
        add(new JScrollPane(strategyPanel), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private void runSimulation() {
        List<StrategyType> activeStrategies = new ArrayList<>();
        for (Map.Entry<StrategyType, JCheckBox> entry : checkboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                activeStrategies.add(entry.getKey());
            }
        }

        if (activeStrategies.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select at least one strategy!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int rounds = parseIntOrDefault(roundsField.getText(), 100);
        double noise = parseDoubleOrDefault(noiseField.getText(), 0.05);
        double mutation = parseDoubleOrDefault(mutationField.getText(), 0.05);
        int popSize = parseIntOrDefault(popSizeField.getText(), 2500);
        int generations = parseIntOrDefault(generationsField.getText(), 500);
        int trials = parseIntOrDefault(trialsField.getText(), 1); 
        String selectedGraph = (String) graphTypeCombo.getSelectedItem();
        
        // Grab Payoff Matrix and Starvation variables as integers
        int t = parseIntOrDefault(tField.getText(), 5);
        int r = parseIntOrDefault(rField.getText(), 3);
        int p = parseIntOrDefault(pField.getText(), 1);
        int s = parseIntOrDefault(sField.getText(), 0);

        statusLabel.setText("Running Simulation...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            runStandardSimulation(activeStrategies, rounds, noise, popSize, generations, mutation, trials, selectedGraph, r, t, s, p);
    }

    private void runStandardSimulation(List<StrategyType> activeStrategies, int rounds, double noise, int popSize, int generations, double mutation, int trials, String selectedGraph, int r, int t, int s, int p) {
        new SwingWorker<Map<StrategyType, List<Double>>, Void>() {
            @Override
            protected Map<StrategyType, List<Double>> doInBackground() {
                Map<StrategyType, List<Double>> aggregatedHistory = new HashMap<>();

                for (int trial = 0; trial < trials; trial++) {
                    // Standard EvolutionEngine doesn't use starvation yet, so we don't pass it here
                    EvolutionEngine engine = new EvolutionEngine(activeStrategies, rounds, noise, popSize, mutation, r, t, s, p);
                    Map<StrategyType, List<Double>> result = engine.run(generations);

                    if (trial == 0) {
                        for (Map.Entry<StrategyType, List<Double>> entry : result.entrySet()) {
                            aggregatedHistory.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                        }
                    } else {
                        for (Map.Entry<StrategyType, List<Double>> entry : result.entrySet()) {
                            List<Double> aggList = aggregatedHistory.get(entry.getKey());
                            List<Double> newVals = entry.getValue();
                            for (int i = 0; i < aggList.size(); i++) {
                                aggList.set(i, aggList.get(i) + newVals.get(i));
                            }
                        }
                    }
                }

                if (trials > 1) {
                    for (List<Double> aggList : aggregatedHistory.values()) {
                        for (int i = 0; i < aggList.size(); i++) {
                            aggList.set(i, aggList.get(i) / trials);
                        }
                    }
                }
                return aggregatedHistory;
            }

            @Override
            protected void done() {
                try {
                    Map<StrategyType, List<Double>> result = get();
                    
                    DataExporter.exportHistoryToCSV(result, "simulation_results.csv");
                    
                    PopulationGraph.display(result, selectedGraph); 
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                statusLabel.setText("Finished");
                setCursor(Cursor.getDefaultCursor());
            }
        }.execute();
    }

    private int parseIntOrDefault(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception ex) { return def; }
    }

    private double parseDoubleOrDefault(String s, double def) {
        try { return Double.parseDouble(s); } catch (Exception ex) { return def; }
    }
}