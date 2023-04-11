package nl.tudelft.instrumentation.patching;

import java.util.*;
import java.util.stream.Collectors;
import com.github.javaparser.utils.Pair;
import com.google.common.collect.Streams;

public class PatchingLab {

        static Random r = new Random();
        static boolean isFinished = false;
        static Map<Integer, Set<Integer>> operatorTouchedByTest = new HashMap<>();

        static Map<Integer, String> operatorType = new HashMap<>();
        static List<String> operators;
        static List<String> allOperators = List.of("==", "!=", "<", "<=", ">", ">=");

        static Pair<List<String>, Double> bestGene = new Pair<>(new ArrayList<>(), 0.0);

        static Set<List<String>> parents = new HashSet<>();

        static void initialize() {
                // initialize the population based on OperatorTracker.operators
                operators = Arrays.asList(OperatorTracker.operators);
        }

        // encounteredOperator gets called for each operator encountered while running tests
        static boolean encounteredOperator(String operator, int left, int right, int operator_nr) {
                operatorTouchedByTest.computeIfAbsent(operator_nr, k -> new HashSet<>())
                                .add(OperatorTracker.current_test);
                operatorType.putIfAbsent(operator_nr, "integer");

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
                operatorType.putIfAbsent(operator_nr, "boolean");

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
                System.out.println("Initial fitness"
                                + (double) testResults.stream().filter(i -> i).count()
                                                / testResults.size());

                // mutate the 10 operators with the highest score to produce the initial population
                List<Integer> top = scores.entrySet().stream()
                                .sorted((entry1, entry2) -> Double.compare(entry2.getValue(),
                                                entry1.getValue()))
                                .filter(en -> en.getValue() != 0).limit(200).map(Map.Entry::getKey)
                                .collect(Collectors.toList());
                //
                // for (int j = 0; j < 10; j++) {
                for (Integer integer : top) {
                        List<String> newOperators = new ArrayList<>(operators);
                        newOperators.set(integer, allOperators.get(r.nextInt(6)));
                        parents.add(newOperators);
                }
                // }

                // Place the code here you want to run once:
                // You want to change this of course, this is just an example
                // Tests are loaded from resources/rers2020_test_cases. If you are you are using
                // your own tests, make sure you put them in the same folder with the same
                // naming convention.


                // Loop here, running your genetic algorithm until you think it is done
                while (!isFinished) {
                        System.out.println("new cycle");

                        Map<List<String>, Double> passedTests = new HashMap<>();
                        // Run the tests with each parent.
                        double fitnessSum = 0;
                        for (List<String> parent : parents) {
                                resetCoverage();
                                operators = parent;
                                testResults = OperatorTracker.runAllTests();
                                passedTests.put(parent,
                                                (double) testResults.stream().filter(i -> i).count()
                                                                / testResults.size());
                                System.out.println("fitness value: " + passedTests.get(parent));
                                fitnessSum += passedTests.get(parent);
                        }
                        System.out.println("Fitness average " + fitnessSum / passedTests.size());


                        /* keep the individuals with the highest fitness */
                        List<List<String>> theBestResults = passedTests.entrySet().stream()
                                        .sorted((e1, e2) -> Double.compare(e2.getValue(),
                                                        e1.getValue()))
                                        .map(Map.Entry::getKey).limit(15)
                                        .collect(Collectors.toList());

                        if (passedTests.getOrDefault(theBestResults.get(0), 0.0) > bestGene.b) {
                                bestGene = new Pair<>(theBestResults.get(0), passedTests
                                                .getOrDefault(theBestResults.get(0), 0.0));

                        } else {
                                // Always use the best gene in our gene pool.
                                theBestResults.add(bestGene.a);
                        }

                        /* perform crossover only among best individuals */
                        List<List<String>> crossOvers = new ArrayList<>();
                        for (int i = 0; i < theBestResults.size(); i++) {

                                List<String> parent1 = theBestResults
                                                .get(r.nextInt(theBestResults.size()));
                                List<String> parent2 = theBestResults
                                                .get(r.nextInt(theBestResults.size()));

                                int crossoverPoint = r.nextInt(parent1.size());
                                List<String> crossover = new ArrayList<>();

                                // Add one way crossover
                                crossover.addAll(parent1.subList(0, crossoverPoint));
                                crossover.addAll(parent2.subList(crossoverPoint, parent2.size()));
                                crossOvers.add(crossover);

                                // Otherway crossover
                                List<String> crossover2 = new ArrayList<>();
                                crossover2.addAll(parent2.subList(0, crossoverPoint));
                                crossover2.addAll(parent1.subList(crossoverPoint, parent2.size()));
                                crossOvers.add(crossover2);
                        }


                        Set<List<String>> children = new HashSet<>();
                        for (List<String> crossOver : crossOvers) {

                                /*
                                 * recompute the tarantula score for each crossover to locate a
                                 * suspicious operator to mutate
                                 */
                                resetCoverage();
                                passedTests.clear();
                                operators = crossOver;
                                testResults = OperatorTracker.runAllTests();
                                scores = tarantulaScore(testResults, operatorTouchedByTest);

                                // find the top 100 suspicious operators
                                List<Integer> topS = scores.entrySet().stream()
                                                .sorted((entry1, entry2) -> Double.compare(
                                                                entry2.getValue(),
                                                                entry1.getValue()))
                                                .limit(100).map(Map.Entry::getKey)
                                                .collect(Collectors.toList());

                                // and mutate one of them at random
                                List<String> newOperators = new ArrayList<>(crossOver);
                                children.add(typeSafeMutation(newOperators,
                                                topS.get(r.nextInt(topS.size()))));

                                /* try to mutate a random operator */
                                if (r.nextInt(10) > 5) {
                                        List<String> randomOperator = new ArrayList<>(newOperators);
                                        typeSafeMutation(randomOperator,
                                                        r.nextInt(newOperators.size()));
                                        children.add(randomOperator);
                                }
                        }
                        System.out.println("New population size: " + children.size());
                        System.out.println("The current best is: " + bestGene.b);
                        parents = new HashSet<>(children);
                }
        }

        private static List<String> typeSafeMutation(List<String> newOperators, int mutateIndex) {
                if (operatorType.getOrDefault(mutateIndex, "").equals("boolean")) {
                        if (newOperators.get(mutateIndex).equals("==")) {
                                newOperators.set(mutateIndex, "!=");
                        } else {
                                newOperators.set(mutateIndex, "==");
                        }
                } else {
                        newOperators.set(mutateIndex,
                                        allOperators.get(r.nextInt(allOperators.size())));
                }
                return newOperators;
        }

        public static void output(String out) {
                // This will get called when the problem code tries to print things,
                // the prints in the original code have been removed for your convenience

                // System.out.println(out);
        }
}
