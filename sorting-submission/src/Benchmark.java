import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Runs in two roles: a controller schedules experiments and writes CSV; each
 * worker is a fresh Java process that warms up and measures exactly one sort.
 * Isolation gives measurements the same heap limits and avoids retaining arrays
 * from earlier trials. Workers run one at a time, not concurrently.
 */
public final class Benchmark {
    static final int MAX_N = 30_000_000; // Elements, not bits or bytes.
    static final long CAP = 15_000_000_000L; // 15 seconds in nanoseconds.

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT); // Decimal points must not become CSV commas.
        if (args.length > 0 && args[0].equals("worker")) {
            worker(args);
            return;
        }
        if (args.length > 0 && args[0].equals("--help")) {
            System.out.println("Benchmark [quick|full] [output.csv] [repeats] [method,method,...] [size,size,...]");
            return;
        }

        // Optional positional arguments have defaults, so "Benchmark" alone works.
        String mode = args.length > 0 ? args[0] : "quick";
        if (!mode.equals("quick") && !mode.equals("full")) {
            throw new IllegalArgumentException("quick or full");
        }
        Path output = Path.of(args.length > 1 ? args[1] : "results.csv");
        int repeats = args.length > 2 ? Integer.parseInt(args[2]) : 3;
        if (repeats < 1) {
            throw new IllegalArgumentException("repeats >= 1");
        }
        String[] methods = args.length > 3 ? args[3].split(",") : SortSupport.METHODS;
        for (String method : methods) {
            if (!Arrays.asList(SortSupport.METHODS).contains(method)) {
                throw new IllegalArgumentException(method);
            }
        }
        String defaultSizes = mode.equals("full")
            ? "100000,1000000,2000000,4000000,8000000,16000000,30000000"
            : "100000,1000000";
        String sizesText = args.length > 4 ? args[4] : defaultSizes;
        // Split text, parse integers and sort sizes so failures can stop larger runs.
        int[] sizes = Arrays.stream(sizesText.split(","))
            .mapToInt(Integer::parseInt).sorted().toArray();
        for (int size : sizes) {
            if (size < 0 || size > MAX_N) {
                throw new IllegalArgumentException("n must be 0.." + MAX_N);
            }
        }

        // The sidecar records the environment alongside the measurements. Both
        // it and the output CSV replace existing files at the chosen paths.
        Files.writeString(Path.of(output + ".environment.txt"),
            "date=" + java.time.Instant.now() + "\njava=" + System.getProperty("java.runtime.version") +
            "\nvm=" + System.getProperty("java.vm.name") + "\nos=" + System.getProperty("os.name") +
            " " + System.getProperty("os.arch") + "\nhostProcessors=" + Runtime.getRuntime().availableProcessors() +
            "\nworkerProcessors=4\nheap=640MiB; SerialGC; NewRatio=8; codeCache=64MiB; metaspace=96MiB; stack=512KiB\n" +
            "arguments=" + Arrays.toString(args) + "\n");

        // A job is one method/pattern pair. After its first non-OK result, skip
        // its remaining trials and larger sizes; other pairs continue normally.
        Set<String> stopped = new HashSet<>();
        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(output))) {
            csv.println("method,pattern,n,trial,seed,setup_ms,sort_ms,status");
            csv.flush(); // Preserve each completed observation if a later run fails.
            for (int size : sizes) {
                for (int trial = 0; trial < repeats; trial++) {
                    List<String> jobs = new ArrayList<>();
                    for (String method : methods) {
                        for (String pattern : SortSupport.PATTERNS) {
                            jobs.add(method + "," + pattern);
                        }
                    }
                    // Reproducible shuffling reduces systematic execution-order bias.
                    Collections.shuffle(jobs, new Random(42L + size + trial));
                    for (String job : jobs) {
                        if (stopped.contains(job)) {
                            continue;
                        }
                        String[] methodAndPattern = job.split(",");
                        long seed = 42L + trial; // Same input seed across methods.
                        // Use this JDK and classpath for every child process.
                        // Heap starts at 32 MiB and can grow to 640 MiB. NewRatio=8
                        // favours the old generation so two large arrays can fit.
                        // Remaining flags bound JVM resources; see README for why
                        // these settings do not enforce a total process RAM limit.
                        List<String> command = new ArrayList<>(List.of(
                            Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                            "-Xms32m", "-Xmx640m", "-XX:+UseSerialGC", "-XX:NewRatio=8", "-XX:ActiveProcessorCount=4",
                            "-XX:ReservedCodeCacheSize=64m", "-XX:MaxMetaspaceSize=96m", "-Xss512k",
                            "-cp", System.getProperty("java.class.path"), "Benchmark", "worker",
                            methodAndPattern[0], methodAndPattern[1],
                            Integer.toString(size), Integer.toString(trial), Long.toString(seed)));

                        // Capture only stdout (the CSV row); errors remain visible
                        // on stderr. A file also avoids a child blocking on a full pipe.
                        Path capture = Files.createTempFile("sort-benchmark-", ".txt");
                        try {
                            Process process = new ProcessBuilder(command)
                                .redirectError(ProcessBuilder.Redirect.INHERIT)
                                .redirectOutput(capture.toFile()).start();
                            // This outer deadline includes startup, warm-up, input
                            // generation and validation as well as the measured sort.
                            boolean finished = process.waitFor(90, TimeUnit.SECONDS);
                            if (!finished) {
                                process.destroyForcibly();
                                process.waitFor();
                            }
                            String row = Files.readString(capture).trim();
                            if (row.isEmpty()) {
                                // A crashed/killed worker may never print a row.
                                // Leave unknown times blank instead of inventing them.
                                row = job + "," + size + "," + trial + "," + seed + ",,," +
                                    (finished ? "ERROR_EXIT_" + process.exitValue() : "PROCESS_TIMEOUT");
                            }
                            csv.println(row);
                            csv.flush();
                            System.out.println(row);
                            if (!row.endsWith(",OK")) {
                                stopped.add(job);
                            }
                        } finally {
                            Files.deleteIfExists(capture); // Also clean up after exceptions.
                        }
                    }
                }
            }
        } // try-with-resources closes the CSV writer, including on exceptions.
    }

    /** Internal entry point launched by the controller, not normally typed by hand. */
    static void worker(String[] args) throws Exception {
        String method = args[1];
        String pattern = args[2];
        int size = Integer.parseInt(args[3]);
        int trial = Integer.parseInt(args[4]);
        long seed = Long.parseLong(args[5]);
        if (size < 0 || size > MAX_N) {
            throw new IllegalArgumentException("size limit");
        }

        // Run the same warm-up workload for every method. Repeated calls allow
        // the JVM's just-in-time compiler to optimize frequently executed code.
        for (int i = 0; i < 16; i++) {
            long[] warmup = SortSupport.input(100000, SortSupport.PATTERNS[i % 4], 1000 + i);
            SortSupport.sort(method, warmup);
            // This post-sort fingerprint only makes the warm-up check test order;
            // the measured input below uses a true before/after content check.
            SortSupport.validate(warmup, SortSupport.fingerprint(warmup));
        }
        System.gc(); // Request cleanup outside timing; the JVM need not honour it.

        long setupStart = System.nanoTime();
        long[] values = SortSupport.input(size, pattern, seed);
        double setupMs = (System.nanoTime() - setupStart) / 1e6; // ns -> milliseconds.
        String prefix = method + "," + pattern + "," + size + "," + trial + "," + seed + ","
            + String.format("%.3f", setupMs);
        if (setupMs > 15000) {
            System.out.println(prefix + ",,SETUP_CAP");
            return;
        }
        long[] before = SortSupport.fingerprint(values); // Excluded from sort timing.

        // The main thread sorts; a daemon thread watches the deadline. Shared
        // state is protected by the same lock to resolve finishing/deadline races.
        // One-element arrays allow the lambda to update captured state: Java
        // requires captured local variable references to remain effectively final.
        Object lock = new Object();
        boolean[] done = {false};
        long[] started = {0};
        Thread watchdog = new Thread(() -> {
            synchronized (lock) {
                while (!done[0]) {
                    long remaining = CAP - (System.nanoTime() - started[0]);
                    if (remaining <= 0) {
                        System.out.println(prefix + ",,SORT_CAP");
                        System.out.flush();
                        // A sort need not respond to interrupts. Halt this worker
                        // process, leaving the separate controller free to continue.
                        Runtime.getRuntime().halt(124);
                    }
                    try {
                        // Waiting releases the lock so the sorting thread can
                        // mark completion. Loop because waits can wake spuriously.
                        TimeUnit.NANOSECONDS.timedWait(lock, remaining);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }, "sort-deadline");
        watchdog.setDaemon(true); // The watchdog alone must not keep the JVM alive.
        synchronized (lock) {
            started[0] = System.nanoTime();
            watchdog.start();
        }

        // Only the dispatch and sort are timed, including its allocations and GC.
        // The watchdog starts slightly earlier, so its deadline also includes
        // thread-launch overhead. Scheduling is best effort, not real-time.
        long sortStart = System.nanoTime();
        SortSupport.sort(method, values);
        long elapsed = System.nanoTime() - sortStart;
        synchronized (lock) {
            done[0] = true;
            lock.notifyAll(); // Wake the watchdog so it can exit its loop.
        }
        SortSupport.validate(values, before); // Validation is outside timing.
        System.out.printf("%s,%.3f,%s%n", prefix, elapsed / 1e6, elapsed <= CAP ? "OK" : "SORT_CAP");
    }
}
