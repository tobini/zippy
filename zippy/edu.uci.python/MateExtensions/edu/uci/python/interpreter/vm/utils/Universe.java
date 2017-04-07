package edu.uci.python.interpreter.vm.utils;

import com.oracle.truffle.api.nodes.Node;
import edu.uci.python.interpreter.MateifyVisitor;

public class Universe {
    public void mateifyNode(Node node) {
        MateifyVisitor visitor = new MateifyVisitor();
        node.accept(visitor);
    }
}
