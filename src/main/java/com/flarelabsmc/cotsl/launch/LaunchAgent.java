package com.flarelabsmc.cotsl.launch;

import com.sun.management.OperatingSystemMXBean;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class LaunchAgent {
    static final CountDownLatch LAUNCH_LATCH = new CountDownLatch(1);
    private static final String RELAUNCHED_PROP = "cotsl.relaunched";

    public static void premain(String agentArgs, Instrumentation inst) throws InterruptedException, IOException {
        if (System.getProperty(RELAUNCHED_PROP) != null) return;
        LauncherWindow.create(LAUNCH_LATCH);
        LAUNCH_LATCH.await();
        tryRelaunch();
    }

    private static void tryRelaunch() throws IOException {
        long totalRamMB = getTotalSystemRamMB();
        long recommended = totalRamMB > 0 ? computeMaxHeap(totalRamMB)
                : Runtime.getRuntime().maxMemory() / (1024 * 1024);
        try { doRelaunch(recommended); }
        catch (Exception e) { System.err.println("[CotSL] Could not relaunch with stock JVM args, continuing as is: " + e.getMessage()); }
    }

    private static void doRelaunch(long maxHeapMB) throws Exception {
        String javaExecutable = ProcessHandle.current().info().command()
                .orElseGet(() -> System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        List<String> args = new ArrayList<>();
        boolean hasClassPath = false;
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (arg.startsWith("-Xmx") || arg.startsWith("-Xms")) continue;
            args.add(arg);
        }
        if (!hasClassPath) {
            String cp = System.getProperty("java.class.path");
            if (cp != null && !cp.isEmpty()) {
                args.add("-classpath");
                args.add(cp);
            }
        }
        args.add("-Xms512M");
        args.add("-Xmx" + maxHeapMB + "M");
        args.add("-D" + RELAUNCHED_PROP + "=true");
        File argFile = File.createTempFile("cotsl-jvmargs-", ".txt");
        argFile.deleteOnExit();
        try (PrintWriter pw = new PrintWriter(new FileWriter(argFile))) {
            for (String arg : args) pw.println(quoteForArgFile(arg));
        }
        List<String> programArgs = ProcessHandle.current().info().arguments()
                .map(LaunchAgent::extractProgramArgs)
                .filter(l -> !l.isEmpty())
                .orElseGet(() -> Arrays.asList(System.getProperty("sun.java.command", "").split(" ")));
        if (programArgs.isEmpty()) throw new IllegalStateException("Could not determine program arguments for relaunch");
        List<String> command = new ArrayList<>();
        command.add(javaExecutable);
        command.add("@" + argFile.getAbsolutePath());
        command.addAll(programArgs);
        int exitCode = new ProcessBuilder(command)
                .inheritIO()
                .start()
                .waitFor();
        System.exit(exitCode);
    }

    private static List<String> extractProgramArgs(String[] allArgs) {
        Set<String> jvmArgSet = new HashSet<>(ManagementFactory.getRuntimeMXBean().getInputArguments());
        for (int i = 0; i < allArgs.length; i++) {
            String a = allArgs[i];
            if (jvmArgSet.contains(a)) continue;
            if (a.startsWith("-X") || a.startsWith("-D") || a.startsWith("-ea") || a.startsWith("-da")) continue;
            if (a.startsWith("--add-") || a.startsWith("--enable-") || a.startsWith("--sun-")) continue;
            if (a.startsWith("@")) continue;
            return Arrays.asList(Arrays.copyOfRange(allArgs, i, allArgs.length));
        }
        return Collections.emptyList();
    }

    private static long getTotalSystemRamMB() {
        try {
            OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            return os.getTotalMemorySize() / (1024 * 1024);
        } catch (Exception e) { return -1; }
    }

    private static long computeMaxHeap(long totalRamMB) throws IOException {
        InputStream is = LaunchAgent.class.getResourceAsStream("/rec_mem_values.txt");
        if (is == null) throw new FileNotFoundException("rec_mem_values.txt not found in classpath");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            List<long[]> entries = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length != 2) continue;
                entries.add(new long[]{
                        Long.parseLong(parts[0].trim()),
                        Long.parseLong(parts[1].trim())
                });
            }
            for (long[] entry : entries) {
                if (totalRamMB < entry[0]) return entry[1];
            }
            return entries.get(entries.size() - 1)[1];
        }
    }


    private static String quoteForArgFile(String arg) {
        String escaped = arg.replace("\\", "\\\\").replace("\"", "\\\"");
        if (arg.contains(" ") || arg.contains("\"")) return "\"" + escaped + "\"";
        return escaped;
    }
}
