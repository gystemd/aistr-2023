package nl.tudelft.instrumentation.fuzzing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class FuzzingLabTest {

    @Test
    void testBasicUnaryNotOperator() {
        MyVar cond = new MyVar("bla");

        assertEquals(FuzzingLab.computeDistance(cond), 1.0/2.0);
    }

    @Test
    void testAdvancedUnaryOperator() {
        MyVar cond = new MyVar(new MyVar(true), new MyVar(false), "&&");


        assertEquals(FuzzingLab.computeDistance(cond), 1.0/3.0);
        assertEquals(FuzzingLab.computeDistance(new MyVar(cond, "!")), (2.0/3.0)/(1+ 2.0/3.0));
    }
}
