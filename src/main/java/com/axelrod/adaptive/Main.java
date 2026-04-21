package com.axelrod.adaptive;

import com.axelrod.adaptive.model.StrategyType;
import com.axelrod.adaptive.evolution.EvolutionEngine;
import com.axelrod.adaptive.ui.StrategyEvolutionUI;
import com.axelrod.adaptive.visualization.PopulationGraph;

import javax.swing.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        // If user explicitly requests console mode, keep old behavior
        if (args != null && args.length > 0 && "--console".equalsIgnoreCase(args[0])) {
            runConsoleMode();
            return;
        }

        // Launch GUI
        SwingUtilities.invokeLater(() -> {
            StrategyEvolutionUI ui = new StrategyEvolutionUI();
            ui.setVisible(true);
        });
    }

    private static void runConsoleMode() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Available strategies:");
        for (StrategyType s : StrategyType.values())
            System.out.println(s.name() + ",");

        System.out.println("\nEnter strategies separated by commas:");
        String input = scanner.nextLine();

        List<StrategyType> selected = new ArrayList<>();
        for (String s : input.split(",")) {
            selected.add(StrategyType.valueOf(s.trim()));
        }

        System.out.println("\nEnter rounds per match (blank for default 100):");
        int roundsPerMatch = 100;
        String roundsInput = scanner.nextLine();
        if (roundsInput != null && !roundsInput.trim().isEmpty()) {
            try {
                roundsPerMatch = Integer.parseInt(roundsInput.trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid number, using default 5 rounds.");
            }
        }

        System.out.println("\nEnter noise rate between 0.0 and 1.0 (blank for default 0.05):");
        double noiseRate = 0.05;
        String noiseInput = scanner.nextLine();
        if (noiseInput != null && !noiseInput.trim().isEmpty()) {
            try {
                noiseRate = Double.parseDouble(noiseInput.trim());
                if (noiseRate < 0.0 || noiseRate > 1.0) {
                    System.out.println("Noise rate out of range, using default 0.05.");
                    noiseRate = 0.05;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid number, using default 0.05.");
                noiseRate = 0.05;
            }
        }
        System.out.println("Both Coop");
        Integer r = 3;
        String RewardInput = scanner.nextLine();
        if (RewardInput != null && !RewardInput.trim().isEmpty()) {
            try {
                r = Integer.parseInt(RewardInput.trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid number, using default 3.");
                r = 3;
            }
        }
        System.out.println("The one who defects while other coops");
        Integer t = 3;
        String TemptationInput = scanner.nextLine();
        if (TemptationInput != null && !TemptationInput.trim().isEmpty()) {
            try {
                t = Integer.parseInt(TemptationInput.trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid number, using default 5.");
                t = 5;
            }
        }
        System.out.println("The one who coops while other defects");
        Integer s = 0;
        String SuckerInput = scanner.nextLine();
        if (SuckerInput != null && !SuckerInput.trim().isEmpty()) {
            try {
                s = Integer.parseInt(SuckerInput.trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid number, using default 0.");
                s = 0;
            }
        }
        System.out.println("Both defect");
        Integer p = 1;
        String punishementString = scanner.nextLine();
        if (punishementString != null && !punishementString.trim().isEmpty()) {
            try {
                p = Integer.parseInt(punishementString.trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid number, using default 1.");
                p = 1;
            }
        }

        System.out.println("\nEnter number of trials to run (blank for default 1):");
        int trials = 1;
        String trialsInput = scanner.nextLine();
        if (trialsInput != null && !trialsInput.trim().isEmpty()) {
            try {
                trials = Integer.parseInt(trialsInput.trim());
                if (trials < 1) {
                    System.out.println("Invalid trial count, using 1.");
                    trials = 1;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid number, using default 1 trial.");
                trials = 1;
            }
        }

        Map<com.axelrod.adaptive.model.StrategyType, List<Double>> aggregate = null;
        for (int a = 0; a < trials; a++) {
            EvolutionEngine engine = new EvolutionEngine(selected, roundsPerMatch, noiseRate, 100, 0.05, r, t, s, p);
            Map<com.axelrod.adaptive.model.StrategyType, List<Double>> hist = engine.run(100);
            if (aggregate == null) {
                aggregate = new java.util.HashMap<>();
                for (Map.Entry<com.axelrod.adaptive.model.StrategyType, List<Double>> e : hist.entrySet()) {
                    aggregate.put(e.getKey(), new ArrayList<>(e.getValue()));
                }
            } else {
                for (Map.Entry<com.axelrod.adaptive.model.StrategyType, List<Double>> e : hist.entrySet()) {
                    List<Double> dest = aggregate.get(e.getKey());
                    if (dest == null) {
                        dest = new ArrayList<>(Collections.nCopies(e.getValue().size(), 0.0));
                        aggregate.put(e.getKey(), dest);
                    }
                    List<Double> src = e.getValue();
                    for (int i = 0; i < src.size(); i++) {
                        double prev = i < dest.size() ? dest.get(i) : 0.0;
                        double sum = prev + src.get(i);
                        if (i < dest.size()) {
                            dest.set(i, sum);
                        } else {
                            dest.add(sum);
                        }
                    }
                }
            }
        }
        if (aggregate != null && trials > 1) {
            for (List<Double> vals : aggregate.values()) {
                for (int i = 0; i < vals.size(); i++) {
                    vals.set(i, vals.get(i) / trials);
                }
            }
        }
        if (aggregate != null) {
            PopulationGraph.display(aggregate);
        }
        scanner.close();
    }
}
