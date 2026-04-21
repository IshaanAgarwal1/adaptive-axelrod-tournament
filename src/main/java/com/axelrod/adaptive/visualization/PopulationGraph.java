package com.axelrod.adaptive.visualization;

import com.axelrod.adaptive.model.StrategyType;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class PopulationGraph {

    private static final Map<StrategyType, Color> COLOR_MAP = new EnumMap<>(StrategyType.class);

    static {
        float hue = 0f;
        final float goldenRatioConjugate = 0.618033988749895f;
        for (StrategyType st : StrategyType.values()) {
            hue = (hue + goldenRatioConjugate) % 1.0f;
            COLOR_MAP.put(st, Color.getHSBColor(hue, 0.65f, 0.95f));
        }
    }

   public static Color getColorForStrategy(StrategyType s) {
        if (s == StrategyType.EMPTY) return Color.BLACK;
        return COLOR_MAP.getOrDefault(s, Color.DARK_GRAY);
    }

    public static void display(Map<StrategyType, List<Double>> history) {
        display(history, "Line Chart");
    }

    // ==========================================
    // EXISTING STATIC GRAPH METHODS (For multi-trials)
    // ==========================================
    public static void display(Map<StrategyType, List<Double>> history, String graphType) {
        JFreeChart chart = null;

        if ("Stacked Area Chart".equals(graphType)) {
            chart = createStackedAreaChart(history);
        } else if ("Final Bar Chart".equals(graphType)) {
            chart = createBarChart(history);
        } else {
            chart = createLineChart(history); 
        }

        applyStyling(chart);

        // --- SORTED POPUP LEADERBOARD ---
        StringBuilder finalResults = new StringBuilder();
        finalResults.append("FINAL GENERATION LEADERBOARD:\n\n");

        List<Map.Entry<StrategyType, Double>> finalScores = new ArrayList<>();
        for (Map.Entry<StrategyType, List<Double>> entry : history.entrySet()) {
            List<Double> values = entry.getValue();
            if (!values.isEmpty()) {
                finalScores.add(new AbstractMap.SimpleEntry<>(entry.getKey(), values.get(values.size() - 1)));
            }
        }
        
        finalScores.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

        int rank = 1;
        for (Map.Entry<StrategyType, Double> entry : finalScores) {
            finalResults.append(rank).append(". ")
                    .append(entry.getKey().name())
                    .append(" : ")
                    .append(String.format("%.2f", entry.getValue() * 100)).append("%\n");
            rank++;
        }

        JOptionPane.showMessageDialog(null, finalResults.toString(), "Final Strategy Market Share", JOptionPane.INFORMATION_MESSAGE);

        JFreeChart finalChart = chart;
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Evolution Results - " + graphType);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(new ChartPanel(finalChart));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // ==========================================
    // NEW: LIVE GRAPHING ENGINE!
    // ==========================================
    public static LiveGraphUpdater displayLive(List<StrategyType> activeStrategies, String graphType) {
        JFreeChart chart = null;
        LiveGraphUpdater updater = new LiveGraphUpdater();

        if ("Stacked Area Chart".equals(graphType)) {
            DefaultTableXYDataset dataset = new DefaultTableXYDataset();
            for (StrategyType st : activeStrategies) {
                XYSeries series = new XYSeries(st.name(), true, false);
                updater.seriesMap.put(st, series);
                dataset.addSeries(series);
            }
            chart = ChartFactory.createStackedXYAreaChart(
                    "Live Strategy Market Share", "Generation", "Population Proportion",
                    dataset, PlotOrientation.VERTICAL, true, true, false);
        } else {
            // Default to Line Chart
            XYSeriesCollection dataset = new XYSeriesCollection();
            for (StrategyType st : activeStrategies) {
                XYSeries series = new XYSeries(st.name());
                updater.seriesMap.put(st, series);
                dataset.addSeries(series);
            }
            chart = ChartFactory.createXYLineChart(
                    "Live Strategy Evolution", "Generation", "Population Proportion",
                    dataset, PlotOrientation.VERTICAL, true, true, false);

            XYPlot plot = chart.getXYPlot();
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
            for (int i = 0; i < dataset.getSeriesCount(); i++) {
                renderer.setSeriesStroke(i, new BasicStroke(2.0f));
            }
            plot.setRenderer(renderer);
        }

        applyStyling(chart);

        // Tell the X-Axis to automatically expand as new data comes in!
        if (chart.getPlot() instanceof XYPlot) {
            NumberAxis xAxis = (NumberAxis) chart.getXYPlot().getDomainAxis();
            xAxis.setAutoRange(true);
        }

        JFreeChart finalChart = chart;
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Live Evolution - " + graphType);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(new ChartPanel(finalChart));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        return updater;
    }

    // Helper class that acts as a "remote control" to push new data to the chart
    public static class LiveGraphUpdater {
        public Map<StrategyType, XYSeries> seriesMap = new HashMap<>();

        public void addDataPoint(int generation, Map<StrategyType, Double> data) {
            for (Map.Entry<StrategyType, Double> entry : data.entrySet()) {
                XYSeries series = seriesMap.get(entry.getKey());
                if (series != null) {
                    series.add(generation, entry.getValue()); // JFreeChart auto-repaints when this happens!
                }
            }
        }
    }

    // ==========================================
    // CHART BUILDING FACTORIES
    // ==========================================
    private static void applyStyling(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        if (chart.getPlot() != null) chart.getPlot().setBackgroundPaint(Color.WHITE);

        if (chart.getPlot() instanceof XYPlot) {
            XYPlot plot = chart.getXYPlot();
            plot.setRangeGridlinesVisible(true);
            plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
            plot.setDomainGridlinesVisible(false);
            
            for (int i = 0; i < plot.getDataset().getSeriesCount(); i++) {
                Comparable<?> key = plot.getDataset().getSeriesKey(i);
                StrategyType st = StrategyType.valueOf(key.toString());
                plot.getRenderer().setSeriesPaint(i, getColorForStrategy(st));
            }
        } else if (chart.getPlot() instanceof CategoryPlot) {
            CategoryPlot plot = chart.getCategoryPlot();
            plot.setRangeGridlinesVisible(true);
            plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
            for (int i = 0; i < plot.getDataset().getRowCount(); i++) {
                Comparable<?> key = plot.getDataset().getRowKey(i);
                StrategyType st = StrategyType.valueOf(key.toString());
                plot.getRenderer().setSeriesPaint(i, getColorForStrategy(st));
            }
        }

        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(Color.WHITE);
            chart.getLegend().setItemFont(chart.getTitle().getFont().deriveFont(Font.PLAIN, 12f));
        }
    }

    private static JFreeChart createLineChart(Map<StrategyType, List<Double>> history) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        int totalGen = history.values().iterator().next().size();
        int step = (totalGen > 100) ? 10 : 1; 

        for (Map.Entry<StrategyType, List<Double>> entry : history.entrySet()) {
            XYSeries series = new XYSeries(entry.getKey().name());
            List<Double> values = entry.getValue();
            for (int i = 0; i < values.size(); i += step) series.add(i, values.get(i));
            if ((values.size() - 1) % step != 0) series.add(values.size() - 1, values.get(values.size() - 1));
            dataset.addSeries(series);
        }
        
        JFreeChart chart = ChartFactory.createXYLineChart("Strategy Evolution", "Generation", "Population Proportion", dataset, PlotOrientation.VERTICAL, true, true, false);
        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        for (int i = 0; i < dataset.getSeriesCount(); i++) renderer.setSeriesStroke(i, new BasicStroke(2.0f)); 
        plot.setRenderer(renderer);
        
        if (totalGen > 100) {
            NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
            xAxis.setTickUnit(new NumberTickUnit(Math.max(10, (totalGen / 100) * 10)));
        }
        return chart;
    }

    private static JFreeChart createStackedAreaChart(Map<StrategyType, List<Double>> history) {
        DefaultTableXYDataset dataset = new DefaultTableXYDataset();
        int totalGen = history.values().iterator().next().size();
        int step = (totalGen > 100) ? 10 : 1;

        for (Map.Entry<StrategyType, List<Double>> entry : history.entrySet()) {
            XYSeries series = new XYSeries(entry.getKey().name(), true, false);
            List<Double> values = entry.getValue();
            for (int i = 0; i < values.size(); i += step) series.add(i, values.get(i));
            if ((values.size() - 1) % step != 0) series.add(values.size() - 1, values.get(values.size() - 1));
            dataset.addSeries(series);
        }
        return ChartFactory.createStackedXYAreaChart("Strategy Market Share", "Generation", "Population Proportion", dataset, PlotOrientation.VERTICAL, true, true, false);
    }

    private static JFreeChart createBarChart(Map<StrategyType, List<Double>> history) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<StrategyType, List<Double>> entry : history.entrySet()) {
            List<Double> values = entry.getValue();
            if (!values.isEmpty()) dataset.addValue(values.get(values.size() - 1), entry.getKey().name(), "Final Generation");
        }
        return ChartFactory.createBarChart("Final Population Distribution", "Strategy", "Population Proportion", dataset, PlotOrientation.VERTICAL, true, true, false);
    }
}