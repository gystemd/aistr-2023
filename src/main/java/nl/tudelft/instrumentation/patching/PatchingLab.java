package nl.tudelft.instrumentation.patching;

import java.util.*;
import java.util.stream.Collectors;

public class PatchingLab {

        static Random r = new Random();
        static boolean isFinished = false;

        static Map<Integer, Set<Integer>> operatorTouchedByTest = new HashMap<>();

        static List<String> operators;
        static List<String> allOperators = List.of("==", "!=", "<", "<=", ">", ">=");

        static Set<List<String>> parents = new HashSet<>();

        static void initialize() {
                // initialize the population based on OperatorTracker.operators
                operators = Arrays.asList(OperatorTracker.operators);
        }

        // encounteredOperator gets called for each operator encountered while running tests
        static boolean encounteredOperator(String operator, int left, int right, int operator_nr) {
                operatorTouchedByTest.computeIfAbsent(operator_nr, k -> new HashSet<>())
                                .add(OperatorTracker.current_test);

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
                operatorTouchedByTest.computeIfAbsent(operator_nr, k -> new HashSet<>())
                                .add(OperatorTracker.current_test);

                String replacement = operators.get(operator_nr);
                if (replacement.equals("!="))
                        return left != right;
                if (replacement.equals("=="))
                        return left == right;
                return false;
        }

        static void resetCoverage() {
                operatorTouchedByTest.clear();
        }


        /*
         * @param testResults: a list of booleans indicating whether each test passed or failed
         * 
         * @param operatorToTest: a map which contains the tests touched by each operator
         */
        static Map<Integer, Double> tarantulaScore(List<Boolean> testResults,
                        Map<Integer, Set<Integer>> operatorToTest) {

                double totalPassed = testResults.stream().filter(i -> i).count();
                double totalFailed = testResults.stream().filter(i -> !i).count();
                Map<Integer, Double> scoreForOperator = new HashMap<>();
                operatorToTest.forEach((operator, tests) -> {
                        long failed = tests.stream().filter(test -> !testResults.get(test)).count();
                        long passed = tests.size() - failed;
                        double score = ((double) failed / totalFailed)
                                        / (((double) failed / totalFailed)
                                                        + ((double) passed / totalPassed));
                        scoreForOperator.put(operator, score);
                });
                return scoreForOperator;
        }



        static void run() {

                initialize();
                System.out.println("initial operators size: " + operators.size());
                List<Boolean> testResults = OperatorTracker.runAllTests();
                Map<Integer, Double> scores = tarantulaScore(testResults, operatorTouchedByTest);

                // mutate the 10 operators with the highest score to produce the initial population
                List<Integer> top = scores.entrySet().stream().sorted((entry1,
                                entry2) -> (int) ((entry2.getValue() - entry1.getValue()) * 1000))
                                .limit(10).map(Map.Entry::getKey).collect(Collectors.toList());

                for (int i = 0; i < top.size(); i++) {
                        List<String> newOperators = new ArrayList<>(operators);
                        newOperators.set(top.get(i), allOperators.get(r.nextInt(6)));
                        parents.add(newOperators);
                }

                // Place the code here you want to run once:
                // You want to change this of course, this is just an example
                // Tests are loaded from resources/rers2020_test_cases. If you are you are using
                // your own tests, make sure you put them in the same folder with the same
                // naming convention.


                // Loop here, running your genetic algorithm until you think it is done
                while (!isFinished) {
                        System.out.println("new generation");

                        Map<List<String>, Double> passedTests = new HashMap<>();
                        System.out.println("parents");
                        for (List<String> parent : parents) {
                                resetCoverage();
                                operators = parent;
                                testResults = OperatorTracker.runAllTests();
                                passedTests.put(parent,
                                                (double) testResults.stream().filter(i -> i).count()
                                                                / testResults.size());
                                System.out.println("fitness value: " + passedTests.get(parent));
                        }

                        // tournament selection based on number of passed tests
                        List<List<String>> selected = new ArrayList<>();
                        for (int i = 0; i < 10; i++) {
                                // pick two random parents
                                List<String> parent1 = parents.stream()
                                                .skip(r.nextInt(parents.size())).findFirst().get();
                                List<String> parent2 = parents.stream()
                                                .skip(r.nextInt(parents.size())).findFirst().get();
                                // compare the number of passed tests
                                selected.add(passedTests.get(parent1) >= passedTests.get(parent2)
                                                ? parent1
                                                : parent2);
                        }

                        // single point crossover
                        List<List<String>> crossOvers = new ArrayList<>();
                        for (int i = 0; i < 10; i++) {
                                List<String> parent1 = selected.get(i);
                                List<String> parent2 = selected.get((i + 1) % 10);
                                int crossoverPoint = r.nextInt(parent1.size());
                                List<String> crossover = new ArrayList<>();
                                crossover.addAll(parent1.subList(0, crossoverPoint));
                                crossover.addAll(parent2.subList(crossoverPoint, parent2.size()));
                                crossOvers.add(crossover);
                        }

                        System.out.println("crossovers");
                        // mutation
                        passedTests.clear();
                        parents.clear();
                        for (List<String> crossOver : crossOvers) {
                                resetCoverage();
                                operators = crossOver;
                                testResults = OperatorTracker.runAllTests();
                                scores = tarantulaScore(testResults, operatorTouchedByTest);
                                Integer topS = scores
                                                .entrySet().stream().sorted((entry1,
                                                                entry2) -> (int) ((entry2.getValue()
                                                                                - entry1.getValue())
                                                                                * 1000))
                                                .limit(1).map(Map.Entry::getKey).findFirst().get();
                                List<String> newOperators2 = new ArrayList<>(crossOver);
                                newOperators2.set(topS, allOperators.get(r.nextInt(6)));
                                parents.add(newOperators2);
                        }


                }
        }

        public static void output(String out) {
                // This will get called when the problem code tries to print things,
                // the prints in the original code have been removed for your convenience

                // System.out.println(out);
        }
}
