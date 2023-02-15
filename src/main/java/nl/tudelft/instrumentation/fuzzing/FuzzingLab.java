package nl.tudelft.instrumentation.fuzzing;

import java.util.*;
import java.util.Random;

/**
 * You should write your own solution using this class.
 */
public class FuzzingLab {
        static Random r = new Random();
        static List<String> currentTrace;
        static int traceLength = 10;
        static boolean isFinished = false;

        static void initialize(String[] inputSymbols){
                // Initialise a random trace from the input symbols of the problem.
                currentTrace = generateRandomTrace(inputSymbols);
        }

        /**
         * Write your solution that specifies what should happen when a new branch has been found.
         */
        static void encounteredNewBranch(MyVar condition, boolean value, int line_nr) {
                int d = computeDistance(condition);

                // do something useful
                System.out.println(condition.toString());
                System.out.println(d);
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
                                default: break;
                        }
                }
                return 0;

        }

        // Calculate distance for (a == b) : d = abs(a-b)
        private static int computeDistanceEquals(MyVar condition) {
                if (condition.left.type == TypeEnum.INT) {
                        return Math.abs(condition.left.int_value - condition.right.int_value);
                } else if (condition.left.type == TypeEnum.STRING) {
                        // How to compare strings?
                        return 0;
                }
                return 0;
        }

        /**
         * Method for fuzzing new inputs for a program.
         * @param inputSymbols the inputSymbols to fuzz from.
         * @return a fuzzed sequence
         */
        static List<String> fuzz(String[] inputSymbols){
                /*
                 * Add here your code for fuzzing a new sequence for the RERS problem.
                 * You can guide your fuzzer to fuzz "smart" input sequences to cover
                 * more branches. Right now we just generate a complete random sequence
                 * using the given input symbols. Please change it to your own code.
                 */
                return generateRandomTrace(inputSymbols);
        }

        /**
         * Generate a random trace from an array of symbols.
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
                initialize(DistanceTracker.inputSymbols);
                DistanceTracker.runNextFuzzedSequence(currentTrace.toArray(new String[0]));

                // Place here your code to guide your fuzzer with its search.
                while(!isFinished) {
                        // Do things!
                        try {
                                System.out.println("Woohoo, looping!");
                                Thread.sleep(1000);
                        } catch (InterruptedException e) {
                                e.printStackTrace();
                        }
                }
        }

        /**
         * Method that is used for catching the output from standard out.
         * You should write your own logic here.
         * @param out the string that has been outputted in the standard out.
         */
        public static void output(String out){
                System.out.println(out);
        }
}
