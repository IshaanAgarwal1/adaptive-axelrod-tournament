package com.axelrod.adaptive.game;

import com.axelrod.adaptive.model.*;

public class Game {

    public static double[] play(Individual a, Individual b, double noiseRate, Integer r, Integer t, Integer s, Integer p) {
        if (a.getStrategyType() == StrategyType.EMPTY || b.getStrategyType() == StrategyType.EMPTY) {
            return new double[]{0, 0};
        }
        Move moveA = a.play();
        Move moveB = b.play();

        // apply GRse: with probability `noiseRate` flip the intended move
        if (Math.random() < noiseRate) {
            moveA = (moveA == Move.COOPERATE) ? Move.DEFECT : Move.COOPERATE;
        }
        if (Math.random() < noiseRate) {
            moveB = (moveB == Move.COOPERATE) ? Move.DEFECT : Move.COOPERATE;
        }

        double scoreA = 0;
        double scoreB = 0;

        if (moveA == Move.COOPERATE && moveB == Move.COOPERATE) {
            scoreA = r;
            scoreB = r;
        } else if (moveA == Move.COOPERATE && moveB == Move.DEFECT) {
            scoreA = s;
            scoreB = t;
        } else if (moveA == Move.DEFECT && moveB == Move.COOPERATE) {
            scoreA = t;
            scoreB = s;
        } else {
            scoreA = p;
            scoreB = p;
        }

        a.updateAfterRound(moveA, (int) scoreA, moveB);
        b.updateAfterRound(moveB, (int) scoreB, moveA);
        // ----------------------------

        return new double[]{scoreA, scoreB};
    }
}