package nl.tudelft.instrumentation.symbolic;

import java.util.*;
import com.github.javaparser.utils.Pair;
import com.microsoft.z3.*;
import nl.tudelft.instrumentation.fuzzing.DistanceTracker;
import java.io.FileWriter;
import java.io.IOException;

/**
 * You should write your solution using this class.
 */
public class SymbolicExecutionLab {

    static Random r = new Random();
    static Boolean isFinished = false;
    static List<String> currentTrace;
    static int traceLength = 1;
    static Set<String> traceQueue = new HashSet<>();
    static Set<Pair<Integer, Boolean>> distinctBranches = new HashSet<>();

    static void initialize(String[] inputSymbols) {
        // Initialise a random trace from the input symbols of the problem.
        currentTrace = generateRandomTrace(inputSymbols);
    }

    static MyVar createVar(String name, Expr value, Sort s) {
        Context c = PathTracker.ctx;
        /**
         * Create var, assign value and add to path constraint. We show how to do it for creating
         * new symbols, please add similar steps to the functions below in order to obtain a path
         * constraint.
         */
        Expr z3var = c.mkConst(c.mkSymbol(name + "_" + PathTracker.z3counter++), s);
        PathTracker.addToModel(c.mkEq(z3var, value));
        return new MyVar(z3var, name);
    }

    static MyVar createInput(String name, Expr value, Sort s) {
        // Create an input var, these should be free variables!
        // how to create a free variable in Z3?
        Context c = PathTracker.ctx;
        Expr z3var = c.mkConst(c.mkSymbol(name + "_" + PathTracker.z3counter++), s);
        MyVar input = new MyVar(z3var, name);
        PathTracker.inputs.add(input);
        return input;
    }

    static MyVar createBoolExpr(BoolExpr var, String operator) {
        // Any unary expression (!)
        Context ctx = PathTracker.ctx;
        if (operator.equals("!")) {
            return new MyVar(ctx.mkNot(var));
        }
        throw new IllegalArgumentException("Unknown boolean operator " + operator);
    }

    static MyVar createBoolExpr(BoolExpr left_var, BoolExpr right_var, String operator) {
        // Any binary expression (&, &&, |, ||)
        Context ctx = PathTracker.ctx;
        switch (operator) {
            case "&": // Should there be a difference between & and &&?
            case "&&":
                return new MyVar(ctx.mkAnd(left_var, right_var));
            case "|": // Should there be a difference between | and ||??
            case "||":
                return new MyVar(ctx.mkOr(left_var, right_var));
            default:
                throw new IllegalArgumentException(
                        "Unknown boolean expression operator" + operator);

        }
    }

    static MyVar createIntExpr(IntExpr var, String operator) {
        // Any unary expression (+, -)
        Context ctx = PathTracker.ctx;
        switch (operator) {
            case "+":
                return new MyVar(var);
            case "-":
                return new MyVar(ctx.mkUnaryMinus(var));
            default:
                throw new IllegalArgumentException("Unknown int operator: " + operator);

        }
    }

    static MyVar createIntExpr(IntExpr left_var, IntExpr right_var, String operator) {
        // Any binary expression (+, -, /, *, %, ^)
        Context ctx = PathTracker.ctx;
        switch (operator) {
            case "+":
                return new MyVar(ctx.mkAdd(left_var, right_var));
            case "-":
                return new MyVar(ctx.mkSub(left_var, right_var));
            case "/":
                return new MyVar(ctx.mkDiv(left_var, right_var));
            case "*":
                return new MyVar(ctx.mkMul(left_var, right_var));
            case "%":
                return new MyVar(ctx.mkMod(left_var, right_var));
            case "^":
                return new MyVar(ctx.mkPower(left_var, right_var));
            case "==":
                return new MyVar(ctx.mkEq(left_var, right_var));
            case "!=":
                return new MyVar(ctx.mkNot(ctx.mkEq(left_var, right_var)));
            case ">":
                return new MyVar(ctx.mkGt(left_var, right_var));
            case ">=":
                return new MyVar(ctx.mkGe(left_var, right_var));
            case "<":
                return new MyVar(ctx.mkLt(left_var, right_var));
            case "<=":
                return new MyVar(ctx.mkLe(left_var, right_var));
            default:
                throw new IllegalArgumentException("Unknown int operator: " + operator);
        }
    }

    static MyVar createStringExpr(SeqExpr left_var, SeqExpr right_var, String operator) {
        // We only support String.equals
        if (operator.equals("==")) {
            return new MyVar(PathTracker.ctx.mkEq(left_var, right_var));
        }
        throw new IllegalArgumentException("Unknown string expression: " + operator);
    }

    static void assign(MyVar var, String name, Expr value, Sort s) {
        // All variable assignments, use single static assignment
        Context c = PathTracker.ctx;
        Expr z3var = PathTracker.ctx.mkConst(name + "-" + PathTracker.z3counter++, s);
        PathTracker.addToModel(c.mkEq(z3var, value));
    }

    static void encounteredNewBranch(MyVar condition, boolean value, int line_nr) {

        Context c = PathTracker.ctx;
        if (!distinctBranches.contains(new Pair<>(line_nr, value))) {
            BoolExpr condTrue = c.mkEq(condition.z3var, c.mkBool(value));
            PathTracker.solve(condTrue, false);
            distinctBranches.add(new Pair<>(line_nr, value));
        }


        if (!distinctBranches.contains(new Pair<>(line_nr, !value))) {
            BoolExpr condFalse = c.mkEq(condition.z3var, c.mkBool(!value));
            PathTracker.solve(condFalse, false);
        }

        BoolExpr condTrue = c.mkEq(condition.z3var, c.mkBool(value));
        // compute the traces to trigger the other branch
        PathTracker.addToBranches(condTrue);
    }

    static void newSatisfiableInput(LinkedList<String> new_inputs) {
        // Hurray! found a new branch using these new inputs!
        System.out.println("Found new inputs: " + new_inputs);
        new_inputs.stream().filter(input -> !traceQueue.contains(input)).forEach(input -> {
            traceQueue.add(input);
        });
    }

    /**
     * Method for fuzzing new inputs for a program.
     * 
     * @param inputSymbols the inputSymbols to fuzz from.
     * @return a fuzzed sequence
     */
    static List<String> fuzz(String[] inputSymbols) {
        /*
         * Add here your code for fuzzing a new sequence for the RERS problem. You can guide your
         * fuzzer to fuzz "smart" input sequences to cover more branches using symbolic execution.
         * Right now we just generate a complete random sequence using the given input symbols.
         * Please change it to your own code.
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
        initialize(PathTracker.inputSymbols);

        System.out.println("inital trace: " + currentTrace);
        PathTracker.runNextFuzzedSequence(currentTrace.toArray(new String[0]));
        // run some random traces to obtain some path constraints
        for (int i = 0; i < 100; i++) {
            PathTracker
                    .runNextFuzzedSequence(fuzz(PathTracker.inputSymbols).toArray(new String[0]));
        }

        for (String l : traceQueue) {
            System.out.println("running next trace:" + l);
            PathTracker.runNextFuzzedSequence(l.split(" "));
            PathTracker.reset();
        }
        System.out.println("visited branches: " + distinctBranches.size());

        while (!isFinished) {
            // Do things!
            try {
                System.out.println("looping!");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void output(String out) {
        System.out.println(out);
    }

}
