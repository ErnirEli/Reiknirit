import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Reads Benchmark's CSV and prints a Markdown table to standard output. */
public final class Summarize {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: Summarize results/full.csv");
        }
        // A case is one method + pattern + size. LinkedHashMap retains the order
        // in which cases first appear in the file, rather than alphabetical order.
        Map<String, List<Double>> times = new LinkedHashMap<>();
        Map<String, Integer> failures = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(Path.of(args[0]));
        for (String line : lines.subList(1, lines.size())) { // Skip the header.
            // -1 preserves empty fields, including missing times in failure rows.
            // This handles our simple CSV format, which has no quoted commas.
            String[] columns = line.split(",", -1);
            if (columns.length != 8) {
                throw new IllegalArgumentException("Malformed CSV: " + line);
            }
            String key = columns[0] + " | " + columns[1] + " | " + columns[2];
            // Keep a case in the table even if all its trials failed.
            times.computeIfAbsent(key, ignored -> new ArrayList<>());
            if (columns[7].equals("OK")) {
                times.get(key).add(Double.parseDouble(columns[6]));
            } else {
                failures.merge(key, 1, Integer::sum); // Insert 1, or add 1.
            }
        }
        System.out.println("| Method | Pattern | n | Successful trials | Median ms | Min ms | Max ms | Failed/capped |\n" +
            "|---|---|---:|---:|---:|---:|---:|---:|");
        for (Map.Entry<String, List<Double>> entry : times.entrySet()) {
            List<Double> successfulTimes = entry.getValue();
            Collections.sort(successfulTimes);
            int trialCount = successfulTimes.size();
            if (trialCount == 0) {
                // Missing times are not zero-millisecond sorts.
                System.out.println("| " + entry.getKey() + " | 0 | — | — | — | " + failures.get(entry.getKey()) + " |");
            } else {
                // Odd count: both indexes select the middle. Even count: take
                // the average of the two middle values. Integer division rounds down.
                double median = (successfulTimes.get((trialCount - 1) / 2)
                    + successfulTimes.get(trialCount / 2)) / 2;
                System.out.printf(Locale.ROOT, "| %s | %d | %.3f | %.3f | %.3f | %d |%n",
                    entry.getKey(), trialCount, median, successfulTimes.get(0),
                    successfulTimes.get(trialCount - 1), failures.getOrDefault(entry.getKey(), 0));
            }
        }
    }
}
