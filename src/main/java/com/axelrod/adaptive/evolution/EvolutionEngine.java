package com.axelrod.adaptive.evolution;

import com.axelrod.adaptive.model.*;
import com.axelrod.adaptive.game.Game;

import java.util.*;
import java.util.stream.Collectors;

public class EvolutionEngine {

    private List<Individual> population;
    private List<StrategyType> allowedStrategies;
    private Map<StrategyType, List<Double>> history = new HashMap<>();

    private final int roundsPerMatch;
    private final int popSize;
    private final double mutationRate;
    private final double noiseRate;
    private final Integer r;
    private final Integer t;
    private final Integer s;
    private final Integer p;

    public EvolutionEngine(List<StrategyType> allowed,
                           int roundsPerMatch,
                           double noiseRate,
                           int popSize,
                           double mutationRate,
                           Integer r,
                           Integer t,
                           Integer s,
                           Integer p) {

        this.allowedStrategies = allowed;
        this.population = new ArrayList<>();

        this.roundsPerMatch = roundsPerMatch;
        this.noiseRate = noiseRate;
        this.popSize = popSize;
        this.mutationRate = mutationRate;
        this.r = r;
        this.t = t; 
        this.s = s;
        this.p = p;

        for (StrategyType strategy : allowed)
            history.put(strategy, new ArrayList<>());

        initializePopulation();
    }

    private void initializePopulation() {
        Random rand = new Random();

        for (int i = 0; i < popSize; i++) {
            StrategyType s = allowedStrategies.get(rand.nextInt(allowedStrategies.size()));
            population.add(new Individual(s));
        }
    }

    public Map<StrategyType, List<Double>> run(int generations) {

        for (int g = 0; g < generations; g++) {

            // Reset scores
            for (Individual ind : population)
                ind.resetScore();

            // Round-robin matches
            for (int i = 0; i < population.size(); i++) {
                for (int j = i + 1; j < population.size(); j++) {

                    Individual a = population.get(i);
                    Individual b = population.get(j);

                    a.resetForMatch();
                    b.resetForMatch();

                    for (int round = 0; round < roundsPerMatch; round++) {
                        double[] result = Game.play(a, b, noiseRate, t, r, s, p);
                        a.addScore(result[0]);
                        b.addScore(result[1]);
                    }
                }
            }

            recordPopulation();
            reproduce();
        }

        return history;
    }

    private void reproduce() {

    List<Individual> newPop = new ArrayList<>();
    Random rand = new Random();

    double totalFitness = population.stream()
            .mapToDouble(Individual::getRunningScore)
            .sum();

    // 🛡️ SAFETY FIX
    if (totalFitness <= 0) {
        // If everyone scored 0, choose randomly instead of breaking selection
        for (int i = 0; i < popSize; i++) {
            StrategyType randomStrategy = allowedStrategies.get(
                    rand.nextInt(allowedStrategies.size()));
            newPop.add(new Individual(randomStrategy));
        }
        population = newPop;
        return;
    }

    for (int i = 0; i < popSize; i++) {

        double pick = rand.nextDouble() * totalFitness;
        double cumulative = 0;

        for (Individual ind : population) {
            cumulative += ind.getRunningScore();
            if (cumulative >= pick) {

                StrategyType child = ind.getStrategyType();

                // mutation
                if (rand.nextDouble() < mutationRate) {
                    child = allowedStrategies.get(
                            rand.nextInt(allowedStrategies.size()));
                }

                newPop.add(new Individual(child));
                break;
            }
        }
    }

    population = newPop;
}

    private void recordPopulation() {

        Map<StrategyType, Long> counts =
                population.stream().collect(Collectors.groupingBy(
                        Individual::getStrategyType,
                        Collectors.counting()));

        for (StrategyType s : allowedStrategies) {
            double proportion = counts.getOrDefault(s, 0L) / (double) popSize;
            history.get(s).add(proportion);
        }
    }
}