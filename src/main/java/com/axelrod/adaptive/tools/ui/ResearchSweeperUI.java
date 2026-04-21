package com.axelrod.adaptive.tools.ui;

import com.axelrod.adaptive.evolution.EvolutionEngine;
import com.axelrod.adaptive.model.StrategyType;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ResearchSweeperUI extends JFrame {

    private final JComboBox<StrategyType> strat1Combo;
    private final JComboBox<StrategyType> strat2Combo;
    private final JLabel statusLabel;

    public ResearchSweeperUI() {
        super("Research Parameter Sweeper");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Strategy selection drop-downs
        strat1Combo = new JComboBox<>(StrategyType.values());
        strat2Combo = new JComboBox<>(StrategyType.values());
        
        // Set default selections so they aren't the same
        strat1Combo.setSelectedItem(StrategyType.TIT_FOR_TAT);
        strat2Combo.setSelectedItem(StrategyType.TIT_FOR_TWO_TATS);

        JPanel selectionPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        selectionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        selectionPanel.add(new JLabel("Strategy 1:"));
        selectionPanel.add(strat1Combo);
        selectionPanel.add(new JLabel("Strategy 2:"));
        selectionPanel.add(strat2Combo);

        statusLabel = new JLabel("Ready. Click Run to generate Heatmap CSV.", SwingConstants.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton runButton = new JButton("Run Parameter Sweep (Temptation vs Noise)");
        runButton.setFont(new Font("Arial", Font.BOLD, 14));
        runButton.addActionListener(e -> runSweep());

        add(selectionPanel, BorderLayout.NORTH);
        add(runButton, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        setSize(400, 200);
        setLocationRelativeTo(null);
    }

    private void runSweep() {
        StrategyType s1 = (StrategyType) strat1Combo.getSelectedItem();
        StrategyType s2 = (StrategyType) strat2Combo.getSelectedItem();

        if (s1 == s2) {
            JOptionPane.showMessageDialog(this, "Please select two different strategies!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        statusLabel.setText("Sweeping parameters... Please wait.");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // Use a SwingWorker so the UI doesn't freeze while calculating
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Fixed constants for the sweep
                int rounds = 100;
                int popSize = 1000;
                int generations = 200;
                double mutation = 0.05;
                int r = 3, p = 1, s = 0; // T is what we are sweeping!

                try (PrintWriter writer = new PrintWriter("sweep_heatmap_data.csv")) {
                    // Dynamically name the columns based on your dropdown choices!
                    writer.println("Temptation,Noise,Final_" + s1.name() + "_Proportion,Final_" + s2.name() + "_Proportion");

                    for (int t = 2; t <= 10; t++) {
                        for (int noiseInt = 0; noiseInt <= 20; noiseInt += 2) {
                            double currentNoise = noiseInt / 100.0;
                            
                            EvolutionEngine engine = new EvolutionEngine(Arrays.asList(s1, s2), rounds, currentNoise, popSize, mutation, r, t, s, p);
                            Map<StrategyType, List<Double>> results = engine.run(generations);

                            // Dynamically pull the results using your selected dropdown items
                            double final1 = results.get(s1).get(generations - 1);
                            double final2 = results.get(s2).get(generations - 1);

                            // Formatted safely to 2 decimal places
                            writer.println(t + "," + currentNoise + "," + String.format("%.2f", final1) + "," + String.format("%.2f", final2));
                        }
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                statusLabel.setText("Success! Data saved to 'sweep_heatmap_data.csv'.");
                setCursor(Cursor.getDefaultCursor());
            }
        }.execute();
    }

    // You can run this file directly to test it!
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ResearchSweeperUI().setVisible(true));
    }
}