package nl.tudelft.instrumentation.fuzzing;

import java.util.*;

/**
 * You should write your own solution using this class.
 */
public class FuzzingLab {
        static Random r = new Random();
        static List<String> currentTrace, bestTrace;
        static int traceLength = 10;
        static boolean isFinished = false;

        static Set<Integer> trueBranches = new HashSet<>();
        static Set<Integer> falseBranches = new HashSet<>();
        static Set<Integer> currentTraceBranches = new HashSet<>();


        static void initialize(String[] inputSymbols) {
                // Initialise a random trace from the input symbols of the problem.
                currentTrace = generateRandomTrace(inputSymbols);
        }

        /**
         * Write your solution that specifies what should happen when a new branch has been found.
         */
        static void encounteredNewBranch(MyVar condition, boolean value, int line_nr) {
                double d = computeDistance(condition);
                d = (double) d / (d + 1); // normalize to [0,1]

                if (value) {
                        trueBranches.add(line_nr);
                } else {
                        falseBranches.add(line_nr);
                }
                System.out.println(condition.toString());
                System.out.println(d);
                System.out.println("line: " + line_nr);
                currentTraceBranches.add(line_nr);
        }

        // P1 && P2 : d = d(p1) + d(p2)
        private static int computeDistanceAndOp(MyVar condition) {
                return computeDistance(condition.left) + computeDistance(condition.right);
        }

        private static int computeDistance(MyVar condition) {
                if (condition.type == TypeEnum.BINARY) {
                        switch (condition.operator) {
                                case "&&":
                                        return computeDistanceAndOp(condition);
                                case "==":
                                        return computeDistanceEquals(condition);
                                case "!=":
                                        return computeDistanceNotEqual(condition);
                                default:
                                        break;
                        }
                } else if (condition.type == TypeEnum.UNARY) {
                        if (Objects.equals(condition.operator, "!")) {
                                return 1 - computeDistance(condition.left);
                        }
                } else if (condition.type == TypeEnum.BOOL) {
                        return condition.value ? 0 : 1;
                }
                return 0;

        }

        private static int computeDistanceNotEqual(MyVar condition) {
                if (condition.left.type == TypeEnum.INT) {
                        return condition.left.int_value != condition.right.int_value ? 0 : 1;
                } else if (condition.left.type == TypeEnum.STRING) {
                        return !condition.left.str_value.equals(condition.right.str_value) ? 0 : 1;
                } else if (condition.left.type == TypeEnum.BOOL) {
                        return condition.left.value != condition.right.value ? 0 : 1;
                }
                return 0;
        }

        // Calculate distance for (a == b) : d = abs(a-b)
        private static int computeDistanceEquals(MyVar condition) {
                if (condition.left.type == TypeEnum.INT) {
                        return Math.abs(condition.left.int_value - condition.right.int_value);
                } else if (condition.left.type == TypeEnum.STRING) {
                        return editDistance(condition.left.str_value, condition.right.str_value);
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
                for (int i = 0; i < traceLength; i++) {
                        trace.add(symbols[r.nextInt(symbols.length)]);
                }
                return trace;
        }

        static void run() {
                int max = 0;

                /* repeat for 5 minutes */
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < 0.1 * 60 * 1000) {
                        System.out.println("Current trace: " + currentTrace);

                        initialize(DistanceTracker.inputSymbols);
                        DistanceTracker.runNextFuzzedSequence(currentTrace.toArray(new String[0]));

                        if (currentTraceBranches.size() > max) {
                                max = currentTraceBranches.size();
                                bestTrace = currentTrace;
                        }
                        currentTraceBranches = new HashSet<>();
                }

                System.out.println("global number of distinct branches activated: "
                                + (trueBranches.size() + falseBranches.size()));
                System.out.println("the best trace is: " + bestTrace + " with " + max
                                + " branches activated");
        }

        /**
         * Method that is used for catching the output from standard out. You should write your own
         * logic here.
         * 
         * @param out the string that has been outputted in the standard out.
         */
        public static void output(String out) {
                System.out.println(out);
        }
}
