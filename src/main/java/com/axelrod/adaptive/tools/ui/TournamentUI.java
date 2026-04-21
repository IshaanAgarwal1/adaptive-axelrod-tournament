package com.axelrod.adaptive.tools.ui;

import com.axelrod.adaptive.model.Individual;
import com.axelrod.adaptive.model.StrategyType;
import com.axelrod.adaptive.game.Game;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class TournamentUI extends JFrame {

    private final Map<StrategyType, JCheckBox> checkboxes = new HashMap<>();

    // Simulation Parameters
    private final JTextField roundsField = new JTextField("200", 6);
    private final JTextField trialsField = new JTextField("10", 6); // NEW: Trials option!
    private final JTextField noiseField = new JTextField("0.0", 6);
    
    // Payoff Matrix
    private final JTextField tField = new JTextField("5", 6);
    private final JTextField rField = new JTextField("3", 6);
    private final JTextField pField = new JTextField("1", 6);
    private final JTextField sField = new JTextField("0", 6);

    // Leaderboard Display
    private final JTextArea leaderboardArea;

    public TournamentUI() {
        super("Round Robin Tournament");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- 1. CONFIGURATION PANEL ---
        // Increased grid rows to 8 to fit the new Trials field
        JPanel configPanel = new JPanel(new GridLayout(8, 2, 5, 5)); 
        configPanel.setBorder(BorderFactory.createTitledBorder("Tournament Rules"));
        
        configPanel.add(new JLabel("Rounds per Match:"));
        configPanel.add(roundsField);
        configPanel.add(new JLabel("Trials per Matchup:")); // NEW UI Element
        configPanel.add(trialsField);
        configPanel.add(new JLabel("Noise Rate:"));
        configPanel.add(noiseField);
        
        configPanel.add(new JLabel("Temptation (T):"));
        configPanel.add(tField);
        configPanel.add(new JLabel("Reward (R):"));
        configPanel.add(rField);
        configPanel.add(new JLabel("Punishment (P):"));
        configPanel.add(pField);
        configPanel.add(new JLabel("Sucker (S):"));
        configPanel.add(sField);

        JButton runButton = new JButton("Run Tournament");
        runButton.setFont(new Font("Arial", Font.BOLD, 14));
        runButton.addActionListener(e -> runTournament());
        configPanel.add(new JLabel("")); // Spacer
        configPanel.add(runButton);

        // --- 2. STRATEGY SELECTION PANEL ---
        JPanel strategyPanel = new JPanel(new GridLayout(0, 2));
        strategyPanel.setBorder(BorderFactory.createTitledBorder("Entrants"));
        for (StrategyType st : StrategyType.values()) {
            if (st == StrategyType.EMPTY) continue; // Skip empty space!
            JCheckBox cb = new JCheckBox(st.name(), true);
            checkboxes.put(st, cb);
            strategyPanel.add(cb);
        }

        // --- 3. LEADERBOARD DISPLAY ---
        leaderboardArea = new JTextArea(15, 40);
        leaderboardArea.setEditable(false);
        leaderboardArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(leaderboardArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Final Leaderboard"));

        add(configPanel, BorderLayout.NORTH);
        add(new JScrollPane(strategyPanel), BorderLayout.WEST);
        add(scrollPane, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }

    private void runTournament() {
        List<StrategyType> entrants = new ArrayList<>();
        for (Map.Entry<StrategyType, JCheckBox> entry : checkboxes.entrySet()) {
            if (entry.getValue().isSelected()) entrants.add(entry.getKey());
        }

        if (entrants.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select at least one entrant!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int rounds = parseIntOrDefault(roundsField.getText(), 200);
        int trials = parseIntOrDefault(trialsField.getText(), 10); // Parse new trials field
        double noise = parseDoubleOrDefault(noiseField.getText(), 0.0);
        int t = parseIntOrDefault(tField.getText(), 5);
        int r = parseIntOrDefault(rField.getText(), 3);
        int p = parseIntOrDefault(pField.getText(), 1);
        int s = parseIntOrDefault(sField.getText(), 0);

        leaderboardArea.setText("Running Tournament...\nCalculating " + trials + " trials per matchup. Please wait.");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // Use SwingWorker to prevent the UI from freezing during heavy calculation
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                Map<StrategyType, Double> tournamentScores = new HashMap<>();
                for (StrategyType st : entrants) tournamentScores.put(st, 0.0);

                // Round Robin: Everyone plays everyone
                for (int i = 0; i < entrants.size(); i++) {
                    for (int j = i; j < entrants.size(); j++) {
                        StrategyType s1 = entrants.get(i);
                        StrategyType s2 = entrants.get(j);

                        double totalTrialScore1 = 0;
                        double totalTrialScore2 = 0;

                        // NEW: Loop through the number of trials!
                        for (int trial = 0; trial < trials; trial++) {
                            // Fresh individuals so their memory resets
                            Individual p1 = new Individual(s1);
                            Individual p2 = new Individual(s2);

                            double matchScore1 = 0;
                            double matchScore2 = 0;

                            for (int round = 0; round < rounds; round++) {
                                double[] matchScores = Game.play(p1, p2, noise, r, t, s, p);
                                matchScore1 += matchScores[0];
                                matchScore2 += matchScores[1];
                            }

                            totalTrialScore1 += matchScore1;
                            totalTrialScore2 += matchScore2;
                        }

                        // Calculate the AVERAGE score across all trials for this matchup
                        double avgScore1 = totalTrialScore1 / trials;
                        double avgScore2 = totalTrialScore2 / trials;

                        // Tally the scores
                        if (i == j) {
                            // Mirror Match: Average the two clones
                            tournamentScores.put(s1, tournamentScores.get(s1) + ((avgScore1 + avgScore2) / 2.0));
                        } else {
                            tournamentScores.put(s1, tournamentScores.get(s1) + avgScore1);
                            tournamentScores.put(s2, tournamentScores.get(s2) + avgScore2);
                        }
                    }
                }

                // Sort the results from Highest Score to Lowest
                List<Map.Entry<StrategyType, Double>> sortedLeaderboard = new ArrayList<>(tournamentScores.entrySet());
                sortedLeaderboard.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

                // Format Leaderboard String
                StringBuilder sb = new StringBuilder();
                sb.append("🏆 TOURNAMENT RESULTS 🏆\n");
                sb.append("Total Entrants: ").append(entrants.size()).append("\n");
                sb.append("Averaged across: ").append(trials).append(" trials per matchup.\n");
                sb.append("------------------------------------------\n");
                sb.append(String.format("%-4s %-25s %s\n", "Rank", "Strategy", "Avg Total Score"));
                sb.append("------------------------------------------\n");

                int rank = 1;
                for (Map.Entry<StrategyType, Double> entry : sortedLeaderboard) {
                    sb.append(String.format("#%-3d %-25s %.1f\n", rank, entry.getKey().name(), entry.getValue()));
                    rank++;
                }

                return sb.toString();
            }

            @Override
            protected void done() {
                try {
                    // Get the string generated by doInBackground and put it in the text area
                    leaderboardArea.setText(get());
                } catch (Exception ex) {
                    leaderboardArea.setText("Error running tournament.");
                    ex.printStackTrace();
                }
                setCursor(Cursor.getDefaultCursor());
            }
        }.execute();
    }

    private int parseIntOrDefault(String str, int def) {
        try { return Integer.parseInt(str); } catch (Exception e) { return def; }
    }

    private double parseDoubleOrDefault(String str, double def) {
        try { return Double.parseDouble(str); } catch (Exception e) { return def; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TournamentUI().setVisible(true));
    }
}