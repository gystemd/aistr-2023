package nl.tudelft.instrumentation.fuzzing;

import java.util.*;
import java.util.function.Function;

import com.github.javaparser.utils.Pair;

/**
 * You should write your own solution using this class.
 */
public class FuzzingLab {
        static final int randomTimer = 5 * 60 * 1000;
        static final int hillClimberTimer = 5 * 60 * 1000;
        static Random r = new Random();

        static List<String> currentTrace, currentBestTrace;
        static int traceLength = 10;

        static Set<Pair<Integer, Boolean>> uniqueVisitedBranches = new HashSet<>();
        static Set<Integer> currentTraceBranches = new HashSet<>();
        static double currentBranchDistanceSum = 0;
        static int currentBranchCount = 0;

        static Set<DiscoveredBranch> visitedBranches = new HashSet<>();

        // keep tracked of the branches that have been visited in the current trace
        static Set<Pair<Integer, Boolean>> currentTraceVisitedBranches = new HashSet<>();

        /* keep track of the errors that have been found */
        static Set<String> errors = new HashSet<>();

        static double currentSmallestDistance = Double.MAX_VALUE;
        private static final Queue<List<String>> mutationsQueue = new LinkedList<>();

        private static final Set<List<String>> coveredTraces = new HashSet<>();

        static long startTime;

        static void initialize(String[] inputSymbols) {
                // Initialise a random trace from the input symbols of the problem.
                currentTrace = generateRandomTrace(inputSymbols);
        }

        /**
         * Write your solution that specifies what should happen when a new branch has been found.
         */
        static void encounteredNewBranch(MyVar condition, boolean value, int line_nr) {
                double d = computeDistance(condition);

                uniqueVisitedBranches.add(new Pair<>(line_nr, value));
                currentTraceBranches.add(line_nr);
                visitedBranches.add(new DiscoveredBranch(line_nr, value, d));
        }


        public static double computeDistance(MyVar condition) {
                double distance = 0;
                switch (condition.type) {
                        case BOOL:
                                distance = condition.value ? 0 : 1;
                                break;
                        case INT:
                                distance = condition.int_value;
                                break;
                        case STRING:
                                for (int i = 0; i < condition.str_value.length(); i++) {
                                        distance += condition.str_value.charAt(i);
                                }
                                break;
                        case UNARY:
                                distance = computeUnaryDistance(condition);
                                break;
                        case BINARY:
                                distance = computeBinaryDistance(condition);
                                break;
                        default:
                                System.out.println("Unknown branch type: " + condition.type);
                }

                return distance / (distance + 1);
        }

        private static double computeBinaryDistance(MyVar condition) {
                double left = computeDistance(condition.left);
                double right = computeDistance(condition.right);
                switch (condition.operator) {
                        // a == b : d = abs(a-b)
                        case "==":
                                return computeDistanceEquals(condition);
                        // a < b : d = {0 if a < b; a-b + K otherwise}
                        case "!=":
                                return computeDistanceNotEqual(condition);
                        // a < b : d = {0 if a < b; a-b + K otherwise}
                        case "<":
                                // Let k be 1 for now? Can we optimize this?
                                return condition.left.int_value < condition.right.int_value ? 0.0
                                                : condition.left.int_value
                                                                - condition.right.int_value + 1;
                        // a <= b : d = {0 if a <= b; a-b otherwise}
                        case "<=":
                                return condition.left.int_value <= condition.right.int_value ? 0.0
                                                : condition.left.int_value
                                                                - condition.right.int_value + 1;
                        // a > b : d = {0 if a > b; b-a+K otherwise}
                        case ">":
                                return condition.left.int_value > condition.right.int_value ? 0.0
                                                : condition.right.int_value
                                                                - condition.left.int_value + 1;
                        // a >= b : b = {0 if a >= b; b - a otherwise}
                        case ">=":
                                return condition.left.int_value >= condition.right.int_value ? 0.0
                                                : condition.right.int_value
                                                                - condition.left.int_value + 1;
                        // P1 | P2 : d = min(d(P1), d(p2))
                        case "||":
                                return Math.min(left, right);
                        // P1 && P2 : d = d(p1) + d(p2)
                        case "&&":
                                return left + right;
                        default:
                                System.out.println("Unkown operator: " + condition.operator);
                                return 0;
                }
        }

        private static double computeUnaryDistance(MyVar condition) {
                if (condition.left.type == TypeEnum.BOOL && condition.left.left == null) {
                        return condition.left.value ? 1 : 0;
                } else {
                        double distance = computeDistance(condition.left);
                        return 1 - distance;
                }
        }

        private static double computeDistanceNotEqual(MyVar condition) {
                switch (condition.left.type) {
                        case INT:
                                return condition.left.int_value != condition.right.int_value ? 0
                                                : 1;
                        case STRING:
                                return editDistance(condition.left.str_value,
                                                condition.right.str_value);
                        case BOOL:
                                return condition.left.value != condition.right.value ? 0 : 1;
                        default:
                                System.out.println("Unknown type for not equals: " + condition);
                }
                return 0;
        }

        // Calculate distance for (a == b) : d = abs(a-b)
        private static double computeDistanceEquals(MyVar condition) {
                switch (condition.left.type) {
                        case INT:
                                return Math.abs(condition.left.int_value
                                                - condition.right.int_value);
                        case STRING:
                                return editDistance(condition.left.str_value,
                                                condition.right.str_value);
                        case BOOL:
                                return condition.left.value == condition.right.value ? 0.0 : 1.0;
                        default:
                                System.out.println("Unknown type for equals: " + condition);
                }
                return 0;
        }

        // compute the edit distance between two strings
        private static int editDistance(String s1, String s2) {
                int[][] dp = new int[s1.length() + 1][s2.length() + 1];

                for (int i = 0; i <= s1.length(); i++) {
                        for (int j = 0; j <= s2.length(); j++) {
                                if (i == 0) {
                                        dp[i][j] = j;
                                } else if (j == 0) {
                                        dp[i][j] = i;
                                } else {
                                        dp[i][j] = Math.min(dp[i - 1][j - 1] + 1, Math
                                                        .min(dp[i - 1][j] + 1, dp[i][j - 1] + 1));
                                }
                        }
                }

                return dp[s1.length()][s2.length()];
        }

        /**
         * Method for fuzzing new inputs for a program.
         *
         * @param inputSymbols the inputSymbols to fuzz from.
         * @return a fuzzed sequence
         */
        static List<String> fuzz(String[] inputSymbols) {
                /*
                 * Add here your code for fuzzing a new sequence for the RERS problem. You can guide
                 * your fuzzer to fuzz "smart" input sequences to cover more branches. Right now we
                 * just generate a complete random sequence using the given input symbols. Please
                 * change it to your own code.
                 */
                return generateRandomTrace(inputSymbols);
        }

        /**
         * Generate a random trace from an array of symbols.
         *
         * @param symbols the symbols from which a trace should be generated from.
         * @return a random trace that is generated from the given symbols.
         */
        static List<String> generateRandomTrace(String[] symbols) {
                ArrayList<String> trace = new ArrayList<>();
                do {
                        traceLength = r.nextInt(symbols.length * 5)+1;
                        for (int i = 0; i < traceLength; i++) {
                                trace.add(symbols[r.nextInt(symbols.length)]);
                        }
                        System.out.println("generating random trace");
                } while (coveredTraces.contains(trace));
                coveredTraces.add(trace);
                return trace;
        }

        static void smartFuzzer() {
                // 1. Create random trace & evaluate the distance for this trace.
                // 2. Create several variations of this trace
                // 3. Evaluate each trace and see repeat
                Map<List<String>, Double> traces = new HashMap<>();
                initialize(DistanceTracker.inputSymbols);
                DistanceTracker.runNextFuzzedSequence(currentTrace.toArray(new String[0]));
                currentSmallestDistance = currentBranchDistanceSum / currentBranchCount;
                traces.put(currentTrace, currentSmallestDistance);
                coveredTraces.add(currentTrace);
                currentBestTrace = currentTrace;

                List<String> previousBestTrace = new ArrayList<>();

                /* repeat for 5 minutes */
                startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < hillClimberTimer) {

                        // Check if we are stuck in a local optimum?
                        if (previousBestTrace.equals(currentBestTrace)) {
                                // If we are stuck in a local minimum, it's time to start
                                // searching somewhere else.
                                System.out.println("Local optimum detected, generating a new "
                                                + "starting point");
                                currentBestTrace =
                                                generateRandomTrace(DistanceTracker.inputSymbols);
                                DistanceTracker.runNextFuzzedSequence(
                                                currentBestTrace.toArray(new String[0]));
                                currentSmallestDistance = getAverageDistance();
                                traces.put(currentBestTrace, currentSmallestDistance);
                                coveredTraces.add(currentBestTrace);
                        } else {
                                previousBestTrace = currentBestTrace;
                        }

                        // Calculate mutations
                        generateAlternatives();

                        while (!mutationsQueue.isEmpty()) {
                                currentBranchCount = 0;
                                currentBranchDistanceSum = 0;
                                currentTrace = mutationsQueue.poll();

                                System.out.println("Current trace: " + currentTrace);
                                DistanceTracker.runNextFuzzedSequence(
                                                currentTrace.toArray(new String[0]));
                                double distance = getAverageDistance();

                                traces.put(currentTrace, distance);
                                coveredTraces.add(currentTrace);
                                if (distance < currentSmallestDistance) {
                                        currentSmallestDistance = distance;
                                        currentBestTrace = currentTrace;
                                }
                        }


                }

                System.out.println("global number of distinct branches activated: "
                                + (uniqueVisitedBranches.size()));
                // Use different method to calculate best trace.
                System.out.println("the latest best trace is: " + currentBestTrace + " with "
                                + traces.get(currentBestTrace) + " average branch distance");
                Map.Entry<List<String>, Double> finalTrace =
                                traces.entrySet().stream().min(Map.Entry.comparingByValue()).get();
                System.out.println("The overall best trace is " + finalTrace.getKey() + " with "
                                + "average branch distance: " + finalTrace.getValue());
                System.out.println("In total " + errors.size() + " errors were discovered");

        }

        static void randomFuzzer() {
                int max = 0;

                startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < randomTimer) {
                        System.out.println("Current trace: " + currentTrace);

                        initialize(DistanceTracker.inputSymbols);
                        DistanceTracker.runNextFuzzedSequence(currentTrace.toArray(new String[0]));

                        if (currentTraceVisitedBranches.size() > max) {
                                max = currentTraceVisitedBranches.size();
                                currentBestTrace = currentTrace;
                        }
                        currentTraceVisitedBranches.clear();
                }

                System.out.println("global number of distinct branches activated: "
                                + (uniqueVisitedBranches.size()));
                System.out.println("the best trace is: " + currentBestTrace + " with " + max
                                + " branches activated");
                System.out.println("In total " + errors.size() + " errors were discovered");
        }

        static void run() {
                smartFuzzer();
                // randomFuzzer();
        }

        private static void generateSpecificAlternative(int alternativeTraces,
                                                        Function<List<String>, Object> run) {
                int maxTries = alternativeTraces;
                List<String> trace;
                do {
                        trace = new ArrayList<>(currentBestTrace);
                        trace.remove(r.nextInt(currentBestTrace.size()));
                        run.apply(trace);
                        maxTries--;
                } while (coveredTraces.contains(trace) && maxTries > 0);
                mutationsQueue.add(trace);
                coveredTraces.add(trace);
        }

        /**
         * Generate mutations based on the current best trace.
         */
        private static void generateAlternatives() {
                int alternativeTraces = DistanceTracker.inputSymbols.length;
                for(int i =0; i<alternativeTraces; i++) {
                        System.out.println("generating new traces");

                        Function<List<String>, Object> func = currTrace -> {
                                currTrace = new ArrayList<>(currentBestTrace);
                                currTrace.remove(r.nextInt(currentBestTrace.size()));
                                return null;
                        };
                        generateSpecificAlternative(alternativeTraces, func);


                        func = currTrace -> {
                                currTrace = new ArrayList<>(currentBestTrace);
                                currTrace.set(r.nextInt(currTrace.size()),
                                        DistanceTracker.inputSymbols[r.nextInt(
                                                DistanceTracker.inputSymbols.length)]);
                                return null;
                        };
                        generateSpecificAlternative(alternativeTraces, func);


                        func = currTrace -> {
                                currTrace = new ArrayList<>(currentBestTrace);
                                currTrace.add(DistanceTracker.inputSymbols[r.nextInt(
                                        DistanceTracker.inputSymbols.length)]);
                                return null;
                        };
                        generateSpecificAlternative(alternativeTraces, func);


                        func = currTrace -> {
                                currTrace = new ArrayList<>(currentBestTrace);
                                // swap two elements

                                int index1 = r.nextInt(currTrace.size());
                                int index2 = r.nextInt(currTrace.size());
                                String temp = currTrace.get(index1);
                                currTrace.set(index1, currTrace.get(index2));
                                currTrace.set(index2, temp);
                                return null;
                        };
                        generateSpecificAlternative(alternativeTraces, func);

                        func = currTrace -> {
                                currTrace = new ArrayList<>(currentBestTrace);
                                Collections.shuffle(currTrace);
                                return null;
                        };
                        generateSpecificAlternative(alternativeTraces, func);
                }

        }

        private static double getAverageDistance() {
                return visitedBranches.stream()
                                .mapToDouble(DiscoveredBranch::getBranchDistance).sum()
                                / visitedBranches.size();
        }

        /**
         * Method that is used for catching the output from standard out. You should write your own
         * logic here.
         *
         * @param out the string that has been outputted in the standard out.
         */
        public static void output(String out) {
                long runningTime = System.currentTimeMillis() - startTime;
                if (out.contains("error")) {
                        errors.add(out);
                        System.out.println(runningTime + " : " + out);
                }
        }

        static class DiscoveredBranch {
                private final int line_nr;
                private final double branchDistance;
                private final boolean condition;

                DiscoveredBranch(int line_nr, boolean condition, double branchDistance) {
                        this.line_nr = line_nr;
                        this.branchDistance = branchDistance;
                        this.condition = condition;
                }

                double getBranchDistance() {
                        return this.branchDistance;
                }

        }
}

