package com.axelrod.adaptive.model;

import java.util.Random;

public class Individual {

    private StrategyType strategyType;

    private Move lastMove;
    private Move lastOpponentMove;

    private boolean opponentDefectedEver = false;
    private int opponentConsecutiveDefections = 0;

    private boolean testerMode = true;

    private double runningScore = 0;
    private int round = 0;
    private double explorationRate = 0.05; // Starts at 5% to probe, then decays
    private static final Random rand = new Random();

    // =====================
    // ADAPTIVE ML FIELDS
    // =====================

    // EXPANDED TO 8 STATES!
    // Bit 4 (value 4): My Last Move (0=Coop, 4=Defect)
    // Bit 2 (value 2): Opponent Last Move (0=Coop, 2=Defect)
    // Bit 1 (value 1): Opponent Defected EVER (0=No, 1=Yes)
    // 2 Actions: 0 (Coop), 1 (Defect)
    private double[][] qTable = new double[8][2]; 
    
    private int tranquilizerState = 0; 
    private boolean proberRetaliated = false;
    private int opponentDefectionsTotal = 0;
    private int opponentTotalCooperations = 0;
    public Individual(StrategyType type) {
        this.strategyType = type;
    }

    // 8-State calculation
    private int getCurrentState() {
        if (lastMove == null || lastOpponentMove == null) return 0; 
        int myBit = (lastMove == Move.DEFECT) ? 4 : 0;
        int oppBit = (lastOpponentMove == Move.DEFECT) ? 2 : 0;
        int everBit = (opponentDefectedEver) ? 1 : 0; 
        return myBit + oppBit + everBit;
    }

    public StrategyType getStrategyType() {
        return strategyType;
    }

    public double getRunningScore() {
        return runningScore;
    }

    public void resetScore() {
        runningScore = 0;
        round = 0;
    }

    public void addScore(double s) {
        runningScore += s;
    }

    public void resetForMatch() {
        lastMove = null;
        lastOpponentMove = null;
        opponentDefectedEver = false;
        opponentConsecutiveDefections = 0;
        testerMode = true;
        round = 0;
        
        // Start exploration at 5% for early probing
        explorationRate = 0.05; 

        if (this.strategyType == StrategyType.ADAPTIVE) {
            for (int i = 0; i < 8; i++) {
                qTable[i][0] = 60.0; // Optimism for cooperation
                qTable[i][1] = 50.0; // Defection is slightly lower initially
            }
        }
    }

    public Move play() {
        Move decision = Move.COOPERATE;

        switch (strategyType) {
            case ALWAYS_COOPERATE: decision = Move.COOPERATE; break;
            case ALWAYS_DEFECT: decision = Move.DEFECT; break;
            case TIT_FOR_TAT: decision = lastOpponentMove == null ? Move.COOPERATE : lastOpponentMove; break;
            case COPY_DEFECT: decision = lastOpponentMove == null ? Move.DEFECT : lastOpponentMove; break;
            case TIT_FOR_TWO_TATS: decision = opponentConsecutiveDefections >= 2 ? Move.DEFECT : Move.COOPERATE; break;
            case JOSS:
                if (round == 0) decision = Move.COOPERATE;
                else if (rand.nextDouble() < 0.1) decision = Move.DEFECT;
                else decision = lastOpponentMove;
                break;
            case GRIM_TRIGGER: decision = opponentDefectedEver ? Move.DEFECT : Move.COOPERATE; break;
            case TESTER:
                if (round == 0) decision = Move.DEFECT;
                else {
                    if (lastOpponentMove == Move.DEFECT) testerMode = false;
                    decision = testerMode ? Move.DEFECT : (lastOpponentMove != null ? lastOpponentMove : Move.COOPERATE);
                }
                break;
            case PAVLOV:
                if (lastMove == null) decision = Move.COOPERATE;
                else decision = (lastMove == lastOpponentMove) ? lastMove : (lastMove == Move.COOPERATE ? Move.DEFECT : Move.COOPERATE);
                break;
            case RANDOM: decision = rand.nextDouble() < 0.5 ? Move.COOPERATE : Move.DEFECT; break;
            case RANDOM_70_COOP: decision = rand.nextDouble() < 0.7 ? Move.COOPERATE : Move.DEFECT; break;
            case RANDOM_30_COOP: decision = rand.nextDouble() < 0.3 ? Move.COOPERATE : Move.DEFECT; break;
            case BULLY: decision = lastOpponentMove == Move.DEFECT ? Move.COOPERATE : Move.DEFECT; break;
            case BOURGEOIS: decision = rand.nextBoolean() ? Move.DEFECT : Move.COOPERATE; break;
            case Tranquilizer: 
                if (round == 0) decision = Move.COOPERATE; 
                if (tranquilizerState == 1) { tranquilizerState = 0; decision = Move.COOPERATE; } 
                if (opponentConsecutiveDefections >= 2) { tranquilizerState = 1; decision = Move.DEFECT; } 
                double avgScore = (round == 0) ? 3.0 : (double) runningScore / round; 
                if (avgScore >= 2.25) { 
                    double probDefect = Math.min(0.25, 0.05 + round * 0.01); 
                    if (Math.random() < probDefect) { tranquilizerState = 1; decision = Move.DEFECT; } 
                    decision = Move.COOPERATE; 
                } 
                if (avgScore >= 1.75) { 
                    if (Math.random() < 0.15) { tranquilizerState = 1; decision = Move.DEFECT; } 
                    decision = Move.COOPERATE; 
                } 
                decision = lastOpponentMove == null ? Move.COOPERATE : lastOpponentMove; 
                break; 
            case First_By_Davis: decision = round <= 10 ? Move.COOPERATE : (opponentDefectedEver ? Move.DEFECT : Move.COOPERATE); break; 
            case PROBER: 
                if (round == 0) decision = Move.DEFECT; 
                else if (round <= 2) decision = Move.COOPERATE;
                else if (!proberRetaliated && opponentDefectionsTotal == 0) { decision = Move.DEFECT; proberRetaliated = true; } 
                else { decision = lastOpponentMove; } 
                break;
            case ADAPTIVE:
                int state = getCurrentState();

                if (rand.nextDouble() < explorationRate) {
                    decision = rand.nextBoolean() ? Move.COOPERATE : Move.DEFECT;
                } else {
                    double coopValue = qTable[state][0];
                    double defectValue = qTable[state][1];
                    decision = (coopValue >= defectValue) ? Move.COOPERATE : Move.DEFECT;
                }
                break;
            case GENEROUS_TIT_FOR_TAT:
    if (lastOpponentMove == null) decision = Move.COOPERATE;
    else if (lastOpponentMove == Move.DEFECT) {
        // 20% chance to forgive a defection to break noise spirals
        decision = rand.nextDouble() < 0.20 ? Move.COOPERATE : Move.DEFECT;
    } else {
        decision = Move.COOPERATE;
    } 
    break;
                case SOFT_MAJORITY:
            if (round == 0) decision = Move.COOPERATE;
            else {
                double coopRate = (double) opponentTotalCooperations / round;
                decision = coopRate >= 0.50 ? Move.COOPERATE : Move.DEFECT;
            }
            break;
            case OLIVE_BRANCH:
    if (round == 0) decision = Move.COOPERATE;
    else {
        double currentAvg = runningScore / round;
        if (currentAvg < 2.0) {
            decision = Move.COOPERATE;
        } else {
            decision = lastOpponentMove; 
        }
    }
    break;
        case DEFECTS_EVERY_THREE:
            if (round % 3 == 0) {
                decision = Move.DEFECT;
            } else {
                decision = Move.COOPERATE;
            }
            break;
        case EMPTY:
            decision = Move.DEFECT;
            break;
        }
        return decision;
    }

    public void updateAfterRound(Move myMove, int myPayoff, Move opponentMove) {
        
        int prevState = getCurrentState();
        int action = (myMove == Move.COOPERATE) ? 0 : 1;

        lastMove = myMove;
        lastOpponentMove = opponentMove;

        if (opponentMove == Move.DEFECT) {
            opponentDefectedEver = true;
            opponentConsecutiveDefections++;
            opponentDefectionsTotal++;
        } else {
            opponentConsecutiveDefections = 0;
            opponentTotalCooperations++;
        }

        if (strategyType == StrategyType.ADAPTIVE) {
            double alpha = 0.2;   
            double gamma = 0.95;  

            int newState = getCurrentState();
            double maxFuture = Math.max(qTable[newState][0], qTable[newState][1]);

            qTable[prevState][action] = 
                qTable[prevState][action] + 
                alpha * (myPayoff + gamma * maxFuture - qTable[prevState][action]);
                
            // Decay the exploration rate so it acts perfectly later in the match
            explorationRate *= .99; 
        }

        round++;
    }
}