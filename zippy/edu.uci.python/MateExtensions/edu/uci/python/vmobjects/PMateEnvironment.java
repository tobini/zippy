package edu.uci.python.vmobjects;

import com.oracle.truffle.api.object.DynamicObject;
import edu.uci.python.interpreter.vm.constants.ReflectiveOp;
import edu.uci.python.interpreter.vm.utils.Universe;

public class PMateEnvironment {
  private static final int Semantics_IDX = 0;
  private static final int Layout_IDX = 1;
  private static final int Message_IDX = 2;

  // Todo: Finish the PMateEnvironment type with primitives for setting it fields
  public static DynamicObject methodImplementing(final DynamicObject obj, ReflectiveOp operation) {
    int field;
    switch (operation){
      case None:
        return null;
      case MessageLookup: case MessageActivation:
        field = Message_IDX;
        break;
      case ExecutorReadField: case ExecutorWriteField: case ExecutorReturn:
      case ExecutorLocalArg: case ExecutorReadLocal: case ExecutorWriteLocal:
        field = Semantics_IDX;
        break;
      case LayoutReadField: case LayoutWriteField: case LayoutPrimReadField: case LayoutPrimWriteField:
        field = Layout_IDX;
        break;
      default:
        return null;
    }
    DynamicObject metaobject = (DynamicObject) obj.get(field);
    if (metaobject == Nil.nilObject) { return null; }
    return methodForOperation(metaobject, operation);
  }

  /*Optimize this method. It can have the definition of the symbols in a static ahead of time  way*/
  private static DynamicObject methodForOperation(DynamicObject metaobject, ReflectiveOp operation) {
    SSymbol selector;
    switch (operation){
      case MessageLookup:
        selector = Universe.getCurrent().symbolFor("find:since:");
        break;
      case MessageActivation:
        selector = Universe.getCurrent().symbolFor("activate:withArguments:");
        break;
      case ExecutorReadField:
        selector = Universe.getCurrent().symbolFor("read:");
        break;
      case ExecutorWriteField:
        selector = Universe.getCurrent().symbolFor("write:value:");
        break;
      case ExecutorReturn:
        selector = Universe.getCurrent().symbolFor("return:");
        break;
      case ExecutorLocalArg:
        selector = Universe.getCurrent().symbolFor("localArgument:inFrame:");
        break;
      case ExecutorReadLocal:
        selector = Universe.getCurrent().symbolFor("readLocal:inFrame:");
        break;
      case ExecutorWriteLocal:
        selector = Universe.getCurrent().symbolFor("writeLocal:inFrame:");
        break;
      case LayoutReadField: case LayoutPrimReadField:
        selector = Universe.getCurrent().symbolFor("read:");
        break;
      case LayoutWriteField: case LayoutPrimWriteField:
        selector = Universe.getCurrent().symbolFor("write:value:");
        break;

      default:
        selector = null;
    }
    return SClass.lookupInvokable(SObject.getSOMClass(metaobject), selector);
  }
}
