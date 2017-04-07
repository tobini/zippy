package edu.uci.python.test.mazippy;

import edu.uci.python.runtime.PythonOptions;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static edu.uci.python.test.PythonTests.assertPrints;

public class ReadAndWriteLocalVariableTests {
    @Test
    public void mazippy() {
        PythonOptions.OptimizeGeneratorExpressions = false;
        Path script = Paths.get("read-and-write-local-variable.py");
        assertPrints("True\n", script);
    }
}
