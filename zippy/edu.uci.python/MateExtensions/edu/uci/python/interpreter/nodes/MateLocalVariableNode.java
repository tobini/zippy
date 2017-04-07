package edu.uci.python.interpreter.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import edu.uci.python.nodes.frame.ReadLocalVariableNode;
import edu.uci.python.interpreter.matenodes.IntercessionHandling;
import edu.uci.python.interpreter.vm.constants.ReflectiveOp;

public abstract class MateLocalVariableNode {
    public static class MateReadLocalVariableNode extends ReadLocalVariableNode {

        public MateReadLocalVariableNode(ReadLocalVariableNode node) {
            super(node);
            this.local = node;
            ih = IntercessionHandling.createForOperation(ReflectiveOp.ExecutorReadLocal);
            this.adoptChildren();
        }

        @Node.Child
        private IntercessionHandling ih;
        @Node.Child
        ReadLocalVariableNode local;

        @Override
        public Object execute(VirtualFrame frame) {
            // TODO
//      Object value = ih.doMateSemantics(frame, new Object[] {SArguments.rcvr(frame)});
            Object value = ih.doMateSemantics(frame, new Object[]{null});
            if (value == null) {
                value = local.execute(frame);
            }
            return value;
        }
    }
/*
  public static class MateLocalVariableWriteNode extends LocalVariableWriteNode {

    @Node.Child
    private IntercessionHandling ih;
    @Node.Child
    LocalVariableWriteNode local;

    public MateLocalVariableWriteNode(LocalVariableWriteNode node) {
      super(node);
      this.local = node;
      ih = IntercessionHandling.createForOperation(ReflectiveOp.ExecutorWriteLocal);
      this.adoptChildren();
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
      Object value = ih.doMateSemantics(frame, new Object[] {SArguments.rcvr(frame)});
      if (value == null) {
       value = local.executeGeneric(frame);
      }
      return value;
    }

    @Override
    public ExpressionNode getExp() {
      return local.getExp();
    }
  }*/
}
