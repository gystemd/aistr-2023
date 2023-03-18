package nl.tudelft.instrumentation.patching;

import java.util.*;
import java.util.stream.Collectors;

public class PatchingLab {

        static Random r = new Random();
        static boolean isFinished = false;

        static Map<Integer, Set<Integer>> lineTouchedByTest = new HashMap<>();

        static Map<Integer, Double> scoreForLine = new HashMap<>();

        static List<String> operators;
        static List<String> allOperators = List.of("==", "!=", "<", "<=", ">", ">=");

        static Queue<List<String>> operatorQueue = new LinkedList<>();

        static void initialize() {
                // initialize the population based on OperatorTracker.operators
                operators = Arrays.asList(OperatorTracker.operators);

        }

        // encounteredOperator gets called for each operator encountered while running tests
        static boolean encounteredOperator(String operator, int left, int right, int operator_nr) {
                updateCoverage(operator_nr);


                String replacement = operators.get(operator_nr);
                if (replacement.equals("!="))
                        return left != right;
                if (replacement.equals("=="))
                        return left == right;
                if (replacement.equals("<"))
                        return left < right;
                if (replacement.equals(">"))
                        return left > right;
                if (replacement.equals("<="))
                        return left <= right;
                if (replacement.equals(">="))
                        return left >= right;
                return false;
        }

        static boolean encounteredOperator(String operator, boolean left, boolean right,
                        int operator_nr) {
                // Do something useful
                updateCoverage(operator_nr);


                String replacement = operators.get(operator_nr);
                if (replacement.equals("!="))
                        return left != right;
                if (replacement.equals("=="))
                        return left == right;
                return false;
        }

        static void updateCoverage(int operator_nr) {
                // The tests which touched this operator (operator_nr)
                lineTouchedByTest.computeIfAbsent(operator_nr, k -> new HashSet<>())
                                .add(OperatorTracker.current_test);
        }

        static void resetCoverage() {
                lineTouchedByTest = new HashMap<>();
        }

        static double calculateScore(List<Boolean> testResults) {

                double numberOfTests = testResults.size();
                double totalPassed = testResults.stream().filter(i -> i).count();
                double totalFailed = testResults.stream().filter(i -> !i).count();
                System.out.println("Number of tests: " + numberOfTests);
                lineTouchedByTest.forEach((line, tests) -> {
                        int passed = 0;
                        int failed = 0;
                        for (Integer test : tests) {
                                if (testResults.get(test)) {
                                        passed++;
                                } else {
                                        failed++;
                                }
                        }
                        double score = ((double) failed / totalFailed)
                                        / (((double) failed / totalFailed)
                                                        + ((double) passed / totalPassed));
                        scoreForLine.put(line, score);
                });
                System.out.println(scoreForLine);
                System.out.println("Number of failed tests: " + totalFailed);
                return totalFailed;
        }

        static void doMutation() {
                // Get to the id of the operation with the highest score
                Map.Entry<Integer, Double> operator_nr = scoreForLine.entrySet().stream().max((
                                entry1,
                                entry2) -> (int) ((entry1.getValue() - entry2.getValue()) * 1000))
                                .get();

                List<String> newOperators = new ArrayList<>(operators);
                newOperators.set(operator_nr.getKey(), allOperators.get(r.nextInt(6)));
                operatorQueue.add(newOperators);
        }

        static void run() {
                initialize();
                List<Boolean> testResults = OperatorTracker.runAllTests();
                calculateScore(testResults);

                // Place the code here you want to run once:
                // You want to change this of course, this is just an example
                // Tests are loaded from resources/rers2020_test_cases. If you are you are using
                // your own tests, make sure you put them in the same folder with the same
                // naming convention.


                // Loop here, running your genetic algorithm until you think it is done
                while (!isFinished) {
                        doMutation();
                        resetCoverage();
                        Map<List<String>, Double> operatorScore = new HashMap<>();
                        while (!operatorQueue.isEmpty()) {
                                operators = operatorQueue.poll();
                                resetCoverage();
                                testResults = OperatorTracker.runAllTests();
                                double score = calculateScore(testResults);
                                operatorScore.put(operators, score);
                        }
                        operators = operatorScore.entrySet().stream()
                                        .min((entry1, entry2) -> (int) ((entry1.getValue()
                                                        - entry2.getValue()) * 1000))
                                        .get().getKey();
                        resetCoverage();
                        testResults = OperatorTracker.runAllTests();
                        calculateScore(testResults);
                }
        }

        public static void output(String out) {
                // This will get called when the problem code tries to print things,
                // the prints in the original code have been removed for your convenience

                // System.out.println(out);
        }
}
