package edu.uci.python.interpreter;

import com.oracle.truffle.api.nodes.Node;

public interface MateNode {
    /**
     * If necessary, this method wraps the node, and replaces it in the AST with
     * the wrapping node.
     *
     * @return
     */
    void wrapIntoMateNode();

    default Node asMateNode() {
        // do nothing!
        // only a small subset of nodes needs to implement this method.
        return null;
    }
}
