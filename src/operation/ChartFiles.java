package operation;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Saves the report figures as standalone SVG charts in data/figure. */
final class ChartFiles {
    private static final int WIDTH = 960;
    private static final int LEFT = 90;
    private static final int RIGHT = WIDTH - 42;
    private static final int TOP = 70;
    private static final int BOTTOM = 376;
    private static final String[] PALETTE = {"#2563eb", "#087e70", "#a94a10", "#7c3db4"};

    private ChartFiles() {}

    /** Writes a bar chart in the iteration order of its labeled values. */
    static void bar(String fileName, String title, String xLabel, String yLabel,
                    Map<String, ? extends Number> values) {
        boolean angleLabels = values.size() > 14 || values.keySet().stream().anyMatch(label -> label.length() > 14);
        int height = angleLabels ? 554 : 455;
        StringBuilder svg = start(title, height);
        AxisScale yScale = scale(values.values().stream().map(Number::doubleValue).toList());
        yAxis(svg, yLabel, yScale);
        text(svg, (LEFT + RIGHT) / 2.0, height - 22, "middle", "label", xLabel);
        if (values.isEmpty()) {
            emptyPlot(svg);
        } else {
            double groupWidth = (double) (RIGHT - LEFT) / values.size();
            double barWidth = Math.min(65, groupWidth * 0.64);
            int index = 0;
            for (Map.Entry<String, ? extends Number> entry : values.entrySet()) {
                double center = LEFT + (index++ + 0.5) * groupWidth;
                double value = entry.getValue().doubleValue();
                double barHeight = Math.max(0, value) / yScale.limit() * (BOTTOM - TOP);
                svg.append("<rect x=\"").append(number(center - barWidth / 2)).append("\" y=\"")
                        .append(number(BOTTOM - barHeight)).append("\" width=\"").append(number(barWidth))
                        .append("\" height=\"").append(number(barHeight)).append("\" rx=\"3\" fill=\"#2563eb\">")
                        .append("<title>").append(escape(entry.getKey() + " - " + number(value)))
                        .append("</title></rect>");
                if (angleLabels) {
                    svg.append("<g transform=\"translate(").append(number(center - 2)).append(' ')
                            .append(BOTTOM + 15).append(") rotate(-48)\">");
                    text(svg, 0, 0, "end", "tick", shorten(entry.getKey(), 24));
                    svg.append("</g>");
                } else {
                    text(svg, center, BOTTOM + 22, "middle", "tick", entry.getKey());
                }
            }
        }
        save(fileName, svg);
    }

    /** Writes a pie chart with counts and percentages in a legend. */
    static void pie(String fileName, String title, Map<String, ? extends Number> values) {
        StringBuilder svg = start(title, 465);
        double total = values.values().stream().mapToDouble(Number::doubleValue).sum();
        int centerX = 260;
        int centerY = 255;
        int radius = 145;
        if (total <= 0) {
            svg.append("<circle cx=\"260\" cy=\"255\" r=\"145\" fill=\"#e2e8f0\"/>");
            text(svg, centerX, centerY + 5, "middle", "label", "No data");
        }
        int index = 0;
        double angle = -Math.PI / 2;
        for (Map.Entry<String, ? extends Number> entry : values.entrySet()) {
            double value = entry.getValue().doubleValue();
            double fraction = total > 0 ? value / total : 0;
            String color = PALETTE[index % PALETTE.length];
            if (fraction >= 1.0) {
                svg.append("<circle cx=\"260\" cy=\"255\" r=\"145\" fill=\"").append(color).append("\"/>");
            } else if (fraction > 0) {
                double end = angle + fraction * 2 * Math.PI;
                svg.append("<path d=\"M ").append(centerX).append(' ').append(centerY).append(" L ")
                        .append(point(centerX, centerY, radius, angle)).append(" A ").append(radius).append(' ')
                        .append(radius).append(" 0 ").append(fraction > 0.5 ? 1 : 0).append(" 1 ")
                        .append(point(centerX, centerY, radius, end))
                        .append(" Z\" fill=\"").append(color).append("\" stroke=\"#fff\"/>");
                angle = end;
            }
            int legendY = 175 + index++ * 55;
            svg.append("<rect x=\"490\" y=\"").append(legendY - 15)
                    .append("\" width=\"19\" height=\"19\" rx=\"4\" fill=\"").append(color).append("\"/>");
            text(svg, 523, legendY, "start", "label", entry.getKey());
            text(svg, 523, legendY + 19, "start", "tick",
                    number(value) + " (" + number(fraction * 100) + "%)");
        }
        save(fileName, svg);
    }

    /** Writes paired horizontal and vertical numeric values as a scatter plot. */
    static void scatter(String fileName, String title, String xLabel, String yLabel,
                        List<Double> xValues, List<Double> yValues) {
        if (xValues.size() != yValues.size()) {
            throw new IllegalArgumentException("Scatter plot values must be paired.");
        }
        StringBuilder svg = start(title, 465);
        AxisScale xScale = scale(xValues);
        AxisScale yScale = scale(yValues);
        yAxis(svg, yLabel, yScale);
        for (int i = 0; i <= xScale.intervals(); i++) {
            double fraction = (double) i / xScale.intervals();
            double x = LEFT + (RIGHT - LEFT) * fraction;
            text(svg, x, BOTTOM + 24, "middle", "tick", number(xScale.limit() * fraction));
        }
        text(svg, (LEFT + RIGHT) / 2.0, 443, "middle", "label", xLabel);
        if (xValues.isEmpty()) {
            emptyPlot(svg);
        }
        for (int i = 0; i < xValues.size(); i++) {
            double x = xValues.get(i);
            double y = yValues.get(i);
            svg.append("<circle cx=\"").append(number(LEFT + x / xScale.limit() * (RIGHT - LEFT)))
                    .append("\" cy=\"").append(number(BOTTOM - y / yScale.limit() * (BOTTOM - TOP)))
                    .append("\" r=\"5\" fill=\"#2563eb\" fill-opacity=\"0.78\" stroke=\"#fff\"><title>")
                    .append(escape(xLabel + ": " + number(x) + ", " + yLabel + ": " + number(y)))
                    .append("</title></circle>");
        }
        save(fileName, svg);
    }

    private static StringBuilder start(String title, int height) {
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" role=\"img\" aria-labelledby=\"title\" width=\"")
                .append(WIDTH).append("\" height=\"").append(height).append("\" viewBox=\"0 0 ")
                .append(WIDTH).append(' ').append(height).append("\"><title id=\"title\">")
                .append(escape(title)).append("</title>").append("""
                        <style>
                        text {font-family:'Segoe UI',Arial,sans-serif;fill:#0f172a}
                        .title {font-size:25px;font-weight:700}
                        .label {font-size:15px;font-weight:600}
                        .tick {font-size:12px;fill:#475569}
                        </style><rect width="100%" height="100%" fill="#fff"/>
                        """);
        text(svg, 36, 40, "start", "title", title);
        return svg;
    }

    private static void save(String fileName, StringBuilder svg) {
        String name = fileName.endsWith(".svg") ? fileName : fileName + ".svg";
        DataFiles.write("data/figure/" + name, List.of(svg.append("</svg>").toString()));
    }

    private static void yAxis(StringBuilder svg, String label, AxisScale scale) {
        for (int i = 0; i <= scale.intervals(); i++) {
            double fraction = (double) i / scale.intervals();
            double y = BOTTOM - (BOTTOM - TOP) * fraction;
            svg.append("<path stroke=\"").append(i == 0 ? "#64748b" : "#e2e8f0")
                    .append("\" d=\"M ").append(LEFT).append(' ').append(number(y))
                    .append(" H ").append(RIGHT).append("\"/>");
            text(svg, LEFT - 11, y + 4, "end", "tick", number(scale.limit() * fraction));
        }
        svg.append("<g transform=\"translate(25 ").append((TOP + BOTTOM) / 2).append(") rotate(-90)\">");
        text(svg, 0, 0, "middle", "label", label);
        svg.append("</g>");
    }

    private static void text(StringBuilder svg, double x, double y, String anchor, String style, String value) {
        svg.append("<text x=\"").append(number(x)).append("\" y=\"").append(number(y))
                .append("\" text-anchor=\"").append(anchor).append("\" class=\"").append(style).append("\">")
                .append(escape(value)).append("</text>");
    }

    private static void emptyPlot(StringBuilder svg) {
        text(svg, (LEFT + RIGHT) / 2.0, (TOP + BOTTOM) / 2.0, "middle", "label", "No data");
    }

    private static AxisScale scale(List<Double> values) {
        double maximum = Math.max(1, values.stream().mapToDouble(Double::doubleValue).max().orElse(0));
        double roughStep = maximum / 5;
        double magnitude = Math.pow(10, Math.floor(Math.log10(roughStep)));
        double step = 10 * magnitude;
        for (double factor : new double[]{1, 2, 2.5, 5}) {
            if (factor * magnitude >= roughStep) {
                step = factor * magnitude;
                break;
            }
        }
        if (values.stream().allMatch(value -> value == Math.rint(value))) {
            step = Math.max(1, Math.ceil(step));
        }
        int intervals = (int) Math.ceil(maximum / step);
        return new AxisScale(step * intervals, intervals);
    }

    private static String point(int centerX, int centerY, int radius, double angle) {
        return number(centerX + Math.cos(angle) * radius) + " " + number(centerY + Math.sin(angle) * radius);
    }

    private static String number(double value) {
        String formatted = String.format(Locale.ROOT, "%.2f", value);
        int end = formatted.length();
        while (formatted.charAt(end - 1) == '0') {
            end--;
        }
        if (formatted.charAt(end - 1) == '.') {
            end--;
        }
        return formatted.substring(0, end);
    }

    private static String shorten(String value, int limit) {
        int length = value.codePointCount(0, value.length());
        return length <= limit ? value : value.substring(0, value.offsetByCodePoints(0, limit - 3)) + "...";
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replaceAll("[\\x00-\\x08\\x0b\\x0c\\x0e-\\x1f]", " ");
    }

    private record AxisScale(double limit, int intervals) {}
}
