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

        static Set<String> errors = new HashSet<>();
        private static long startTime;

        static void initialize(String[] inputSymbols) {
                // Initialise a random trace from the input symbols of the problem.
                currentTrace = generateRandomTrace(inputSymbols);
        }

        /**
         * Write your solution that specifies what should happen when a new branch has been found.
         */
        static void encounteredNewBranch(MyVar condition, boolean value, int line_nr) {
                double d = computeDistance(condition);
//                d = d / (d + 1); // normalize to [0,1]

                if (value) {
                        trueBranches.add(line_nr);
                } else {
                        falseBranches.add(line_nr);
                }
//                System.out.println(condition.toString());
//                System.out.println(d);
//                System.out.println("line: " + line_nr);
                currentTraceBranches.add(line_nr);
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
                                // Sum of all the chars values??
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
                                //Let k be 1 for now? Can we optimize this?
                                return condition.left.int_value < condition.right.int_value ? 0.0 :
                                        condition.left.int_value - condition.right.int_value + 1;
                        // a <= b : d = {0 if a <= b; a-b otherwise}
                        case "<=":
                                return condition.left.int_value <= condition.right.int_value ? 0.0 :
                                        condition.left.int_value - condition.right.int_value + 1;
                        // a > b : d = {0 if a > b; b-a+K otherwise}
                        case ">":
                                return condition.left.int_value > condition.right.int_value ? 0.0 :
                                        condition.right.int_value - condition.left.int_value + 1;
                        // a >= b : b = {0 if a >= b; b - a otherwise}
                        case ">=": return condition.left.int_value >= condition.right.int_value ?
                                0.0 : condition.right.int_value - condition.left.int_value + 1;
                        // P1 | P2 : d = min(d(P1), d(p2))
                        case "||": return Math.min(left, right);
                        // P1 && P2 : d = d(p1) + d(p2)
                        case "&&": return left + right;
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
                        return 1-distance;
                }
        }

        private static double computeDistanceNotEqual(MyVar condition) {
                switch (condition.left.type) {
                        case INT:
                                return condition.left.int_value != condition.right.int_value ? 0 : 1;
                        case STRING:
                                return !condition.left.str_value.equals(condition.right.str_value) ? 0 : 1;
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
                                return Math.abs(condition.left.int_value - condition.right.int_value);
                        case STRING:
                                return editDistance(condition.left.str_value, condition.right.str_value);
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
                for (int i = 0; i < traceLength; i++) {
                        trace.add(symbols[r.nextInt(symbols.length)]);
                }
                return trace;
        }

        static void run() {
                int max = 0;

                /* repeat for 5 minutes */
                startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < 5 * 60 * 1000) {
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
                System.out.println("In total " + errors.size() + " errors were discovered");
        }

        /**
         * Method that is used for catching the output from standard out. You should write your own
         * logic here.
         *
         * @param out the string that has been outputted in the standard out.
         */
        public static void output(String out) {
                if (out.contains("error")) {
                        System.out.println(startTime + " : " + out);
                        errors.add(out);
                }
        }
}
