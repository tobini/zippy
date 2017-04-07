package edu.uci.python.interpreter.vm.constants;

public enum ReflectiveOp {
    ExecutorReadField, ExecutorWriteField, ExecutorLocalArg, ExecutorNonLocalArg,
    ExecutorLocalSuperArg, ExecutorNonLocalSuperArg, ExecutorReadLocal, ExecutorWriteLocal, ExecutorReturn,
    MessageLookup, MessageActivation,
    LayoutReadField, LayoutWriteField, LayoutPrimReadField, LayoutPrimWriteField,
    None
}
