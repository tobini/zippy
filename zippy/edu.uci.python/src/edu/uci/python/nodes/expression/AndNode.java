package edu.uci.python.nodes.expression;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;

import edu.uci.python.ast.VisitorIF;
import edu.uci.python.nodes.*;
import edu.uci.python.nodes.expression.CastToBooleanNode.*;
import edu.uci.python.nodes.expression.CastToBooleanNodeFactory.*;

@NodeInfo(shortName = "and")
public class AndNode extends BinaryOpNode {

    @Child private PNode leftNode;
    @Child private PNode rightNode;
    @Child private YesNode booleanCast = YesNodeFactory.create(EmptyNode.create());

    public AndNode(PNode left, PNode right) {
        this.leftNode = left;
        this.rightNode = right;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object left = this.getLeftNode().execute(frame);
        if (booleanCast.executeBoolean(frame, left)) {
            return this.getRightNode().execute(frame);
        }
        return left;
    }

    @Override
    public PNode getLeftNode() {
        return leftNode;
    }

    @Override
    public PNode getRightNode() {
        return rightNode;
    }

    @Override
    public <R> R accept(VisitorIF<R> visitor) throws Exception {
        return visitor.visitAndNode(this);
    }

}
