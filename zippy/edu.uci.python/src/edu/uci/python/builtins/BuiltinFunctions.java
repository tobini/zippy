/*
 * Copyright (c) 2013, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.uci.python.builtins;

import java.math.*;
import java.util.*;

import org.python.core.*;

import edu.uci.python.nodes.*;
import edu.uci.python.nodes.call.*;
import edu.uci.python.nodes.expression.*;
import edu.uci.python.nodes.expression.CastToBooleanNodeFactory.YesNodeFactory;
import edu.uci.python.nodes.function.*;
import edu.uci.python.nodes.truffle.*;
import edu.uci.python.runtime.*;
import edu.uci.python.runtime.datatype.*;
import edu.uci.python.runtime.exception.*;
import edu.uci.python.runtime.function.*;
import edu.uci.python.runtime.iterator.*;
import edu.uci.python.runtime.misc.*;
import edu.uci.python.runtime.object.*;
import edu.uci.python.runtime.sequence.*;
import edu.uci.python.runtime.sequence.storage.*;
import edu.uci.python.runtime.standardtype.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

/**
 * @author Gulfem
 * @author zwei
 */
public final class BuiltinFunctions extends PythonBuiltins {

    @Override
    protected List<com.oracle.truffle.api.dsl.NodeFactory<? extends PythonBuiltinNode>> getNodeFactories() {
        return BuiltinFunctionsFactory.getFactories();
    }

    // abs(x)
    @Builtin(name = "abs", hasFixedNumOfArguments = true, fixedNumOfArguments = 1)
    @GenerateNodeFactory
    public abstract static class AbsNode extends PythonBuiltinNode {

        @Specialization
        public int absInt(int arg) {
            return Math.abs(arg);
        }

        @Specialization
        public double absDouble(double arg) {
            return Math.abs(arg);
        }

        @Specialization
        public double absPComplex(PComplex arg) {
            return FastMathUtil.hypot(arg.getReal(), arg.getImag());
        }

        @Specialization
        public Object absObject(PythonObject object) {
            Object absAttribute = object.getAttribute("__abs__");
            if (absAttribute != null && absAttribute instanceof PFunction) {
                PMethod method = new PMethod(object, (PFunction) absAttribute);
                return method.call(null, null);
            } else {
                throw Py.TypeError("bad operand type for abs(): '" + object + "'");
            }
        }

        @Specialization
        public double absObject(Object arg) {
            throw Py.TypeError("bad operand type for abs(): '" + PythonTypesUtil.getPythonTypeName(arg) + "'");
        }
    }

    // all(iterable)
    @Builtin(name = "all", hasFixedNumOfArguments = true, fixedNumOfArguments = 1)
    @GenerateNodeFactory
    public abstract static class AllNode extends PythonBuiltinNode {

        @Child protected CastToBooleanNode toBoolean;

        @Specialization
        public boolean all(PIterable iterable) {
            if (iterable.len() == 0) {
                return false;
            }

            PIterator iterator = iterable.__iter__();

            try {
                while (true) {
                    if (!toBoolean(iterator.__next__())) {
                        return false;
                    }
                }
            } catch (StopIterationException e) {
                // fall through
            }

            return true;
        }

        @Specialization
        public boolean all(Object object) {
            throw new RuntimeException("all does not support iterable object " + object);
        }

        private boolean toBoolean(Object value) {
            if (toBoolean == null) {
                CompilerDirectives.transferToInterpreter();
                toBoolean = insert(YesNodeFactory.create(EmptyNode.create()));
            }
            return toBoolean.executeBoolean(null, value);
        }
    }

    // any(iterable)
    @Builtin(name = "any", hasFixedNumOfArguments = true, fixedNumOfArguments = 1)
    @GenerateNodeFactory
    public abstract static class AnyNode extends PythonBuiltinNode {

        @Child protected CastToBooleanNode toBoolean;

        private boolean toBoolean(Object value) {
            if (toBoolean == null) {
                CompilerDirectives.transferToInterpreter();
                toBoolean = insert(YesNodeFactory.create(EmptyNode.create()));
            }
            return toBoolean.executeBoolean(null, value);
        }

        @Specialization
        public boolean any(PIterable iterable) {
            if (iterable.len() == 0) {
                return false;
            }

            PIterator iterator = iterable.__iter__();

            try {
                while (true) {
                    if (toBoolean(iterator.__next__())) {
                        return true;
                    }
                }
            } catch (StopIterationException e) {
                // fall through
            }

            return false;
        }

        @Specialization
        public boolean any(Object object) {
            throw new RuntimeException("any does not support iterable object " + object);
        }
    }

    // callable(object)
    @Builtin(name = "callable", hasFixedNumOfArguments = true, fixedNumOfArguments = 1)
    @GenerateNodeFactory
    public abstract static class CallableNode extends PythonBuiltinNode {

        @SuppressWarnings("unused")
        @Specialization
        public boolean callable(PythonCallable callable) {
            return true;
        }

        @Specialization
        public boolean callable(Object object) {
            /**
             * Added temporarily to skip translation/execution errors in unit testing
             */

            if (object.equals(ZippyTranslationErrorNode.MESSAGE)) {
                return true;
            }

            return object instanceof PythonCallable;
        }
    }

    // chr(i)
    @Builtin(name = "chr", hasFixedNumOfArguments = true, fixedNumOfArguments = 1)
    @GenerateNodeFactory
    public abstract static class ChrNode extends PythonBuiltinNode {

        @TruffleBoundary
        @Specialization
        public String charFromInt(int arg) {
            if (arg >= 0 && arg < 1114111) {
                return Character.toString((char) arg);
            } else {
                throw Py.ValueError("chr() arg not in range(0x110000)");
            }
        }

        @TruffleBoundary
        @Specialization
        public char charFromObject(BigInteger arg) {
            if (arg.longValue() > Integer.MAX_VALUE) {
                throw Py.OverflowError("integer is greater than maximum");
            } else {
                throw new RuntimeException("chr does not support BigInteger " + arg);
            }
        }

        @SuppressWarnings("unused")
        @TruffleBoundary
        @Specialization
        public Object charFromObject(double arg) {
            throw Py.TypeError("integer argument expected, got float");
        }

        @TruffleBoundary
        @Specialization
        public char charFromObject(Object arg) {
            if (arg instanceof Double) {
                throw Py.TypeError("integer argument expected, got float");
            }

            throw Py.TypeError("an integer is required");
        }
    }

    // dir([object])
    @Builtin(name = "dir", minNumOfArguments = 0, maxNumOfArguments = 1)
    @GenerateNodeFactory
    public abstract static class DirNode extends PythonBuiltinNode {

        @Specialization
        public Object dir(PythonModule module) {
            List<String> attributes = module.getAttributeNames();
            return new PTuple(attributes.toArray());
        }

        @Specialization
        public Object dir(PythonClass clazz) {
            List<String> attributes = clazz.getAttributeNames();

            if (clazz.getSuperClass() != null) {
                /**
                 * TODO should add all the attributes in the class hierarchy
                 */
                List<String> superClassAttributes = clazz.getSuperClass().getAttributeNames();
                attributes.addAll(superClassAttributes);
            }

            return new PTuple(attributes.toArray());
        }

        public Object dir(Object object) {
            throw new RuntimeException("dir is not supported for " + object + " " + object.getClass());
        }
    }

    // divmod(a, b)
    @Builtin(name = "divmod", hasFixedNumOfArguments = true, fixedNumOfArguments = 2)
    @GenerateNodeFactory
    public abstract static class DivModNode extends PythonBuiltinNode {

        @Specialization
        public PTuple doInt(int a, int b) {
            return new PTuple(new Object[]{a / b, a % b});
        }

        @Specialization
        public PTuple doBigInteger(BigInteger a, BigInteger b) {
            return new PTuple(divideAndRemainderSlowStub(a, b));
        }

        @TruffleBoundary
        private static BigInteger[] divideAndRemainderSlowStub(BigInteger a, BigInteger b) {
            return a.divideAndRemainder(b);
        }

        @Specialization
        public PTuple doDouble(double a, double b) {
            double q = Math.floor(a / b);
            return new PTuple(new Object[]{q, a % b});
        }
    }

    // eval(expression, globals=None, locals=None)
    @Builtin(name = "eval", hasFixedNumOfArguments = true, fixedNumOfArguments = 1)
    @GenerateNodeFactory
    public abstract static class EvalNode extends PythonBuiltinNode {

        @Specialization
        public Object eval(String expression) {
            return evalExpression(expression);
        }

        @TruffleBoundary
        private Object evalExpression(String expression) {
            PythonParser parser = getContext().getParser();
            PythonParseResult parsed = parser.parse(getContext(), new PythonModule(getContext(), "<eval>", null), expression);
            RootNode root = parsed.getModuleRoot();
            VirtualFrame frame = Truffle.getRuntime().createVirtualFrame(null, root.getFrameDescriptor());
            Truffle.getRuntime().createCallTarget(root);
            return root.execute(frame);
        }
    }

    // filter(function, iterable)
    @Builtin(name = "filter", hasFixedNumOfArguments = true, fixedNumOfArguments = 2)
    @GenerateNodeFactory
    public abstract static class FilterNode extends PythonBuiltinNode {

        @Specialization
        public PTuple filter(PythonCallable function, PIterable iterable) {
            PIterator iter = iterable.__iter__();
            List<Object> filteredElements = new ArrayList<>();

            try {
                while (true) {
                    Object item = iter.__next__();
                    Object[] args = new Object[]{item};
                    Object result = function.call(args);
                    if (result instanceof Boolean) {
                        boolean booleanResult = (Boolean) result;
                        if (booleanResult) {
                            filteredElements.add(item);
                        }
                    }
                }
            } catch (StopIterationException e) {
                // fall through
            }

            return new PTuple(filteredElements.toArray());
        }
    }

    // getattr(object, name[, default])
    @Builtin(name = "getattr", minNumOfArguments = 2, maxNumOfArguments = 3)
    @GenerateNodeFactory
    public abstract static class GetAttrNode extends PythonBuiltinNode {

        @Specialization
        public Object getAttrFromModule(PythonModule module, String name, Object defaultValue) {
            Object attrValue = module.getAttribute(name);
            if ((attrValue == PNone.NONE) && defaultValue != PNone.NONE) {
                return defaultValue;
            }

            return attrValue;
        }

        @Specialization
        public Object getAttrFromClass(PythonClass clazz, String name, Object defaultValue) {
            Object attrValue = clazz.getAttribute(name);
            if ((attrValue == PNone.NONE) && defaultValue != PNone.NONE) {
                return defaultValue;
            }

            return attrValue;
        }

        @Specialization
        public Object getAttrFromObject(PythonObject object, String name, Object defaultValue) {
            Object attrValue = object.getAttribute(name);

            if ((attrValue == PNone.NONE) && defaultValue != PNone.NONE) {
                return defaultValue;
            }

            if (attrValue instanceof PFunction) {
                return new PMethod(object, (PFunction) attrValue);
            }

            return attrValue;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isForJSON(list,attributeId,defaultValue)")
        public Object doPythonBuiltinObjectForJSON(PList list, String attributeId, Object defaultValue) {
            return defaultValue;
        }

        @Specialization
        public Object doPythonBuiltinObject(PythonBuiltinObject obj, String attributeId, Object defaultValue) {
            CompilerAsserts.neverPartOfCompilation();

            Object attribute = obj.__class__().getAttribute(attributeId);
            return attribute != null ? defaultValue : attribute;
        }

        @Specialization
        public Object getAttr(Object object, Object name, Object defaultValue) {
            throw new RuntimeException("getAttr is not supported for " + object + " " + object.getClass() + " name " + name + " defaultValue " + defaultValue);
        }
    }

    // hasattr(object, name)
    @Builtin(name = "hasattr", hasFixedNumOfArguments = true, fixedNumOfArguments = 2)
    @GenerateNodeFactory
    public abstract static class HasAttrNode extends PythonBuiltinNode {

        @Specialization
        public Object hasAttr(PythonObject object, String name) {
            List<String> attributes = object.getAttributeNames();
            if (attributes.contains(name)) {
                return true;
            }

            attributes = object.getPythonClass().getAttributeNames();
            if (attributes.contains(name)) {
                return true;
            }

            return false;
        }

        @Specialization
        public Object hasAttr(Object object, Object name) {
            throw new RuntimeException("hasAttr is not supported for " + object + " " + object.getClass() + " name " + name);
        }
    }

    // id(object)
    @Builtin(name = "id", hasFixedNumOfArguments = true, fixedNumOfArguments = 1)
    @GenerateNodeFactory
    public abstract static class IdNode extends PythonBuiltinNode {

        /**
         * zwei: This is not completely compliant with the id() implementation in CPython, which
         * returns the internal address of the object. In Java, the internal address of an object
         * changes during its lifetime. Therefore, using the internal address does not guarantee a
         * consistent id during an object's lifetime. Hash code of two objects however are the same.
         * This is conflicting with the 'identity' specification of id().
         */
        @Specialization
        int doId(Object obj) {
            return obj.hashCode();
        }
    }

    // isinstance(object, classinfo)
    @Builtin(name = "isinstance", hasFixedNumOfArguments = true, fixedNumOfArguments = 2)
    @GenerateNodeFactory
    public abstract static class IsIntanceNode extends PythonBuiltinNode {

        @SuppressWarnings("unused")
        @Specialization
        public boolean isinstance(String str, PythonClass clazz) {
            if (clazz.getName().equals("str")) {
                return true;
            } else {
                return false;
            }
        }

        @Specialization
        public boolean isinstance(PythonClass object, PythonClass clazz) {
            return isInstancePythonClass(object, clazz);
        }

        @Specialization
        public boolean isinstance(PythonObject object, PythonClass clazz) {
            return isInstancePythonClass(object, clazz);
        }

        private static boolean isInstancePythonClass(PythonObject object, PythonClass clazz) {
            if (object.getPythonClass().equals(clazz)) {
                return true;
            }

            PythonClass superClass = object.getPythonClass().getSuperClass();

            while (superClass != null) {
                if (superClass.equals(clazz)) {
                    return true;
                }

                superClass = superClass.getSuperClass();
            }

            if (object instanceof PythonClass) {
                if (clazz.getName().equals("type")) {
                    return true;
                }
            }

            return false;
        }

        @Specialization(guards = "is2ndNotTuple(val,cls)")
        public boolean isinstance(@SuppressWarnings("unused") int val, Object cls) {
            return PInt.__class__ == cls;
        }

        @Specialization
        public boolean isinstance(@SuppressWarnings("unused") int val, PTuple classTuple) {
            for (int i = 0; i < classTuple.len(); i++) {
                if (PInt.__class__ == classTuple.getItem(i)) {
                    return true;
                }
            }

            return false;
        }

        @Specialization
        public boolean isinstance(@SuppressWarnings("unused") String val, PTuple classTuple) {
            for (int i = 0; i < classTuple.len(); i++) {
                if (PString.__class__ == classTuple.getItem(i)) {
                    return true;
                }
            }

            return false;
        }

        @Specialization(guards = "is2ndNotTuple(obj,cls)")
        public boolean isinstance(PythonBuiltinObject obj, Object cls) {
            return obj.__class__() == cls;
        }

        @Specialization
        public boolean isinstance(PythonBuiltinObject obj, PTuple classTuple) {
            for (int i = 0; i < classTuple.len(); i++) {
                if (obj.__class__() == classTuple.getItem(i)) {
                    return true;
                }
            }

            return false;
        }

        @Specialization
        public boolean isinstance(PythonObject obj, PTuple classTuple) {
            for (int i = 0; i < classTuple.len(); i++) {
                if (obj.getPythonClass() == classTuple.getItem(i)) {
                    return true;
                }
            }

            return false;
        }
    }

    // issubclass(class, classinfo)
    @Builtin(name = "issubclass", hasFixedNumOfArguments = true, fixedNumOfArguments = 2)
    @GenerateNodeFactory
    public abstract static class IsSubClassNode extends PythonBuiltinNode {

        @SuppressWarnings("unused")
        @Specialization
        public Object issubclass(PythonModule clazz, PythonClass clazzinfo) {
            return false;
        }

        @Specialization
        public Object issubclass(PythonClass clazz, PythonClass clazzinfo) {
            /**
             * TODO How do you check two classes are equal? Name comparison can't be true all the
             * time.
             */
            if (clazz.getName().equals(clazzinfo.getName())) {
                return true;
            } else {
                PythonClass superClass = clazz.getSuperClass();
                while (superClass != null) {
                    if (superClass.getName().equals(clazzinfo.getName())) {
                        return true;
                    }
                    superClass = superClass.getSuperClass();
                }
            }

            return false;
        }

        @Specialization
        public Object issubclass(PyObject clazz, PyObject clazzinfo) {
            return Py.isSubClass(clazz, clazzinfo);
        }

        @Specialization
        public Object issubclass(Object clazz, Object clazzinfo) {
            throw new RuntimeException("issubclass is not supported for " + clazz + " " + clazz.getClass() + ", " + clazzinfo + " " + clazzinfo.getClass());
        }
    }

    // iter(object[, sentinel])
    @Builtin(name = "iter", minNumOfArguments = 1, maxNumOfArguments = 2)
    @GenerateNodeFactory
    public abstract static class IterNode extends PythonBuiltinNode {

        @SuppressWarnings("unused")
        @Specialization
        public Object iter(String str, PNone sentinel) {
            return new PStringIterator(str);
        }

        @SuppressWarnings("unused")
        @Specialization
        public Object iter(PIterable iterable, PNone sentinel) {
            return iterable.__iter__();
        }

        @Specialization
        public Object iter(Object object, Object sentinel) {
            throw new RuntimeException("Not supported sentinel case object " + object + " sentinel " + sentinel);
        }
    }

    // len(s)
    @Builtin(name = "len", hasFixedNumOfArguments = true, fixedNumOfArguments = 1)
    @GenerateNodeFactory
    public abstract static class LenNode extends PythonBuiltinNode {

        @CompilationFinal @Child protected CallDispatchSpecialNode dispatch;

        @Specialization
        public int len(String arg) {
            return arg.length();
        }

        @Specialization
        public int len(PTuple tuple) {
            return tuple.len();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isEmptyStorage(list)")
        public int lenPListEmpty(PList list) {
            return 0;
        }

        @Specialization(guards = "isIntStorage(list)")
        public int lenPListInt(PList list) {
            IntSequenceStorage store = (IntSequenceStorage) list.getStorage();
            return store.length();
        }

        @Specialization(guards = "isLongStorage(list)")
        public int lenPListLong(PList list) {
            LongSequenceStorage store = (LongSequenceStorage) list.getStorage();
            return store.length();
        }

        @Specialization(guards = "isDoubleStorage(list)")
        public int lenPListDouble(PList list) {
            DoubleSequenceStorage store = (DoubleSequenceStorage) list.getStorage();
            return store.length();
        }

        @Specialization(guards = "isObjectStorage(list)")
        public int lenPListObject(PList list) {
            ObjectSequenceStorage store = (ObjectSequenceStorage) list.getStorage();
            return store.length();
        }

        @Specialization(guards = "isBasicStorage(list)")
        public int lenPList(PList list) {
            BasicSequenceStorage store = (BasicSequenceStorage) list.getStorage();
            return store.length();
        }

        @TruffleBoundary
        @Specialization
        public int lenPList(PyList list) {
            return list.__len__();
        }

        @Specialization
        public int len(PIterable iterable) {
            return iterable.len();
        }

        @Specialization
        public Object len(VirtualFrame frame, PythonObject obj) {
            if (dispatch == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                dispatch = insert(new CallDispatchSpecialNode.UninitializedDispatchSpecialNode("__len__"));
            }

            return dispatch.executeCall(frame, obj, PNone.NONE);
        }

        @Fallback
        public int len(Object arg) {
            throw Py.TypeError("object of type '" + PythonTypesUtil.getPythonTypeName(arg) + "' has no len()");
        }
    }

    // max(iterable, *[, key])
    // max(arg1, arg2, *args[, key])
    @Builtin(name = "max", minNumOfArguments = 1, takesKeywordArguments = true, takesVariableArguments = true, keywordNames = {"key"})
    @GenerateNodeFactory
    public abstract static class MaxNode extends PythonBuiltinNode {

        @SuppressWarnings("unused")
        @Specialization(guards = "hasOneArgument(arg1,args,keywordArg)")
        public Object maxSequence(PSequence arg1, PTuple args, Object keywordArg) {
            return arg1.getMax();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "hasOneArgument(arg1,args,keywordArg)")
        public Object maxBaseSet(PBaseSet arg1, PTuple args, Object keywordArg) {
            return arg1.getMax();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "hasOneArgument(arg1,args,keywordArg)")
        public Object maxDictionary(PDict arg1, PTuple args, Object keywordArg) {
            return arg1.getMax();
        }

        /**
         * Incomplete. Only deals with ints now.
         */
        @SuppressWarnings("unused")
        @Specialization
        public Object maxPIterator(PIterator arg1, PTuple args, PNone keywordArg) {
            int max = Integer.MIN_VALUE;

            try {
                while (true) {
                    int item = (int) arg1.__next__();
                    max = Math.max(max, item);
                }
            } catch (StopIterationException e) {
            }

            return max;
        }

        @Specialization
        public Object maxGeneric(Object arg1, PTuple args, Object keywordArg) {
            if (keywordArg instanceof PNone) {
                if (args.len() == 1) {
                    return getMax(arg1, args.getItem(0));
                } else {
                    Object[] argsArray = new Object[args.len() + 1];
                    argsArray[0] = arg1;
                    System.arraycopy(args.getArray(), 0, argsArray, 1, args.len());
                    Object max = getMax(argsArray);
                    return max;
                }
            } else {
                throw new RuntimeException("Optional keyword-only key argument is not supported");
            }
        }

        private static Object getMax(Object arg1, Object arg2) {

            if (arg1 instanceof Double) {
                double arg1Double = (Double) arg1;
                if (arg2 instanceof Integer || arg2 instanceof Double) {
                    double arg2Double = (Double) arg2;
                    return Math.max(arg1Double, arg2Double);
                }
            }

            BigInteger a = null;
            if (arg1 instanceof Integer)
                a = BigInteger.valueOf(Long.valueOf((int) arg1));
            else if (arg1 instanceof Long)
                a = BigInteger.valueOf((long) arg1);
            else if (arg1 instanceof BigInteger)
                a = (BigInteger) arg1;

            BigInteger b = null;
            if (arg2 instanceof Integer)
                b = BigInteger.valueOf(Long.valueOf((int) arg2));
            else if (arg2 instanceof Long)
                b = BigInteger.valueOf((long) arg2);
            else if (arg2 instanceof BigInteger)
                b = (BigInteger) arg2;

            if (a != null && b != null)
                return a.max(b);

            throw new RuntimeException("Unsupported max operation");
        }

        private static Object getMax(Object[] args) {
            Arrays.sort(args);
            return args[args.length - 1];
        }

    }

    // min(iterable, *[, key])
    // min(arg1, arg2, *args[, key])
    @Builtin(name = "min", minNumOfArguments = 1, takesKeywordArguments = true, takesVariableArguments = true, keywordNames = {"key"})
    @GenerateNodeFactory
    public abstract static class MinNode extends PythonBuiltinNode {

        @SuppressWarnings("unused")
        @Specialization(guards = "hasOneArgument(arg1,args,keywordArg)")
        public Object minString(String arg1, PTuple args, Object keywordArg) {
            PString pstring = new PString(arg1);
            return pstring.getMin();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "hasOneArgument(arg1,args,keywordArg)")
        public Object minSequence(PSequence arg1, PTuple args, Object keywordArg) {
            return arg1.getMin();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "hasOneArgument(arg1,args,keywordArg)")
        public Object minBaseSet(PBaseSet arg1, PTuple args, Object keywordArg) {
            return arg1.getMin();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "hasOneArgument(arg1,args,keywordArg)")
        public Object minDictionary(PDict arg1, PTuple args, Object keywordArg) {
            return arg1.getMin();
        }

        @Specialization
        public Object minGeneric(Object arg1, PTuple args, Object keywordArg) {
            if (keywordArg instanceof PNone) {
                if (arg1 instanceof Iterable) {
                    throw new RuntimeException("Multiple iterables are not supported");
                } else if (args.len() == 1) {
                    return getMin(arg1, args.getItem(0));
                } else {
                    Object[] argsArray = new Object[args.len() + 1];
                    argsArray[0] = arg1;
                    System.arraycopy(args.getArray(), 0, argsArray, 1, args.len());
                    Object min = getMin(argsArray);
                    return min;
                }
            } else {
                throw new RuntimeException("Optional keyword-only key argument is not supported");
            }
        }

        private static Object getMin(Object arg1, Object arg2) {
            if (arg1 instanceof Double) {
                double arg1Double = (Double) arg1;
                if (arg2 instanceof Integer || arg2 instanceof Long || arg2 instanceof Double) {
                    double arg2Double = (Double) arg2;
                    return Math.min(arg1Double, arg2Double);
                }
            }
            BigInteger a = null;
            if (arg1 instanceof Integer)
                a = BigInteger.valueOf(Long.valueOf((int) arg1));
            else if (arg1 instanceof Long)
                a = BigInteger.valueOf((long) arg1);
            else if (arg1 instanceof BigInteger)
                a = (BigInteger) arg1;

            BigInteger b = null;
            if (arg2 instanceof Integer)
                b = BigInteger.valueOf(Long.valueOf((int) arg2));
            else if (arg2 instanceof Long)
                b = BigInteger.valueOf((long) arg2);
            else if (arg2 instanceof BigInteger)
                b = (BigInteger) arg2;

            if (a != null && b != null)
                return a.min(b);

            throw new RuntimeException("Unsupported min operation");
        }

        private static Object getMin(Object[] args) {
            Object[] copy = args;
            Arrays.sort(copy);
            return copy[0];
        }

    }

    // next(iterator[, default])
    @SuppressWarnings("unused")
    @Builtin(name = "next", minNumOfArguments = 1, maxNumOfArguments = 2)
    @GenerateNodeFactory
    public abstract static class NextNode extends PythonBuiltinNode {

        @Specialization
        public int next(PRangeIterator iterator, PNone defaultObject) {
            return iterator.__nextInt__();
        }

        @Specialization
        public int next(PIntegerIterator iterator, PNone defaultObject) {
            return iterator.__nextInt__();
        }

        @Specialization
        public double next(PDoubleIterator iterator, PNone defaultObject) {
            return iterator.__nextDouble__();
        }

        @Specialization
        public Object next(PIterator iterator, PNone defaultObject) {
            return iterator.__next__();
        }

        @Specialization
        public Object next(Object iterator, Object defaultObject) {
            throw new RuntimeException("Unsupported iterator " + iterator);
        }
    }

    // ord(c)
    @Builtin(name = "ord", hasFixedNumOfArguments = true, fixedNumOfArguments = 1)
    @GenerateNodeFactory
    public abstract static class OrdNode extends PythonBuiltinNode {

        @Specialization
        public int ord(String chr) {
            if (chr.length() != 1) {
                typeError("ord() expected a character, but string of length " + chr.length() + " found");
            }

            return chr.charAt(0);
        }
    }

    // print(*objects, sep=' ', end='\n', file=sys.stdout, flush=False)
    @Builtin(name = "print", minNumOfArguments = 0, takesKeywordArguments = true, takesVariableArguments = true, takesVariableKeywords = true, keywordNames = {"sep", "end", "file",
                    "flush"}, requiresContext = true)
    @GenerateNodeFactory
    public abstract static class PrintNode extends PythonBuiltinNode {

        @TruffleBoundary
        @Specialization
        public Object print(PTuple values, Object[] keywords) {
            String sep = null;
            String end = null;

            if (keywords != null) {
                for (int i = 0; i < keywords.length; i++) { // not support file
                    PKeyword keyword = (PKeyword) keywords[i];
                    if (keyword.getName().equals("end")) {
                        end = (String) keyword.getValue();
                    } else if (keyword.getName().equals("sep")) {
                        sep = (String) keyword.getValue();
                    }
                }
            }

            return print(values, sep, end);
        }

        @TruffleBoundary
        private Object print(PTuple values, String possibleSep, String possibleEnd) {
            String sep = possibleSep;
            String end = possibleEnd;

            if (values.len() == 0) {
                getContext().getStandardOut().print(System.getProperty("line.separator"));
            } else {
                if (sep == null) {
                    sep = "";
                }

                if (end == null) {
                    end = System.getProperty("line.separator");
                }

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < values.len() - 1; i++) {
                    sb.append(stringifyElement(values.getItem(i)) + " ");
                }

                sb.append(stringifyElement(values.getItem(values.len() - 1)));
                getContext().getStandardOut().print(sb.toString() + sep + end);
            }

            return PNone.NONE;
        }

        @TruffleBoundary
        private static String stringifyElement(Object element) {
            if (element instanceof Boolean) {
                return ((boolean) element ? "True" : "False");
            } else if (element instanceof PythonObject) {
                return PythonBuiltinNode.callAttributeSlowPath((PythonObject) element, "__repr__");
            } else if (element instanceof Double) {
                return JavaTypeConversions.doubleToString((double) element);
            } else {
                return element.toString();
            }
        }
    }

    // repr(object)
    @Builtin(name = "repr", hasFixedNumOfArguments = true, fixedNumOfArguments = 1)
    @GenerateNodeFactory
    public abstract static class ReprNode extends PythonBuiltinNode {

        @Specialization
        public String repr(PythonObject obj) {
            return PythonBuiltinNode.callAttributeSlowPath(obj, "__repr__");
        }

        @Specialization
        public String repr(Object obj) {
            return obj.toString();
        }
    }

    // reversed(seq)
    @Builtin(name = "reversed", hasFixedNumOfArguments = true, fixedNumOfArguments = 1)
    @GenerateNodeFactory
    public abstract static class ReversedNode extends PythonBuiltinNode {

        @Specialization
        public PIterator reversed(PRange range) {
            return new PRangeIterator.PRangeReverseIterator(range);
        }

        @Specialization
        public PIterator reversed(PSequence sequence) {
            return new PSequenceReverseIterator(sequence);
        }
    }

    // round(number[, ndigits])
    @Builtin(name = "round", minNumOfArguments = 1, maxNumOfArguments = 2)
    @GenerateNodeFactory
    public abstract static class RoundNode extends PythonBuiltinNode {

        @SuppressWarnings("unused")
        @Specialization
        public double round(int arg, int n) {
            return arg;
        }

        /**
         * The logic is borrowed from Jython.
         *
         */
        @TruffleBoundary
        @Specialization
        public double round(double x, Object nn) {
            int n = 0;
            if (nn != PNone.NONE)
                n = (int) nn;
            if (Double.isNaN(x) || Double.isInfinite(x) || x == 0.0) {
                // nans, infinities and zeros round to themselves
                return x;

            } else {

                // (Slightly less than) n*log2(10).
                float nlog2_10 = 3.3219f * n;

                // x = a * 2^b and a<2.
                int b = Math.getExponent(x);

                if (nlog2_10 > 52 - b) {
                    // When n*log2(10) > nmax, the lsb of abs(x) is >1, so x rounds to itself.
                    return x;
                } else if (nlog2_10 < -(b + 2)) {
                    // When n*log2(10) < -(b+2), abs(x)<0.5*10^n so x rounds to (signed) zero.
                    return Math.copySign(0.0, x);
                } else {
                    // We have to work it out properly.
                    BigDecimal xx = new BigDecimal(x);
                    BigDecimal rr = xx.setScale(n, RoundingMode.HALF_UP);
                    return rr.doubleValue();
                }
            }
        }
    }

    // setattr(object, name, value)
    @Builtin(name = "setattr", hasFixedNumOfArguments = true, fixedNumOfArguments = 3)
    @GenerateNodeFactory
    public abstract static class SetAttrNode extends PythonBuiltinNode {

        @Specialization
        public Object setAttrInModule(PythonModule module, String name, Object value) {
            module.setAttribute(name, value);
            return null;
        }

        @Specialization
        public Object setAttrInClass(PythonClass clazz, String name, Object value) {
            clazz.setAttribute(name, value);
            return null;
        }

        @Specialization
        public Object setAttrInObject(PythonObject object, String name, Object value) {
            object.setAttribute(name, value);
            return null;
        }

        @Specialization
        public Object setAttr(PyObject object, String name, Object value) {
            object.__setitem__(name, PythonTypesUtil.adaptToPyObject(value));
            return null;
        }

        @Specialization
        public Object setAttr(Object object, Object name, Object value) {
            throw new RuntimeException("setAttr is not supported for " + object + " " + object.getClass() + " name " + name + " value " + value);
        }
    }

    // sum(iterable[, start])
    @Builtin(name = "sum", minNumOfArguments = 1, takesKeywordArguments = true, maxNumOfArguments = 2, keywordNames = {"start"})
    @GenerateNodeFactory
    public abstract static class SumNode extends PythonBuiltinNode {
        /**
         * Incomplete. Only support ints.
         */

        @Specialization(guards = "isIntStorage(list)")
        public int doPIteratorInt(PList list) {
            PIterator iterator = list.__iter__();
            int sum = 0;
            try {
                while (true) {
                    sum += (int) iterator.__next__();
                }
            } catch (StopIterationException e) {
            }

            return sum;
        }

        @Specialization(guards = "isLongStorage(list)")
        public long doPIteratorLong(PList list) {
            PIterator iterator = list.__iter__();
            long sum = 0;
            try {
                while (true) {
                    sum += (long) iterator.__next__();
                }
            } catch (StopIterationException e) {
            }

            return sum;
        }

        @Specialization(guards = "isDoubleStorage(list)")
        public double doPIteratorDouble(PList list) {
            PIterator iterator = list.__iter__();
            double sum = 0.0;
            try {
                while (true) {
                    sum += (double) iterator.__next__();
                }
            } catch (StopIterationException e) {
            }

            return sum;
        }

        @Specialization
        public int doPIterable(PIterable iterable) {
            return doPIterator(iterable.__iter__());
        }

        @Specialization
        public int doPIterator(PIterator iterator) {
            int sum = 0;
            try {
                while (true) {
                    sum += (int) iterator.__next__();
                }
            } catch (StopIterationException e) {
            }

            return sum;
        }
    }

    // super([type[, object-or-type]])
    @Builtin(name = "super", minNumOfArguments = 1, maxNumOfArguments = 2)
    @GenerateNodeFactory
    public abstract static class SuperNode extends PythonBuiltinNode {

        @SuppressWarnings("unused")
        @Specialization
        public Object applySuper(PythonClass type, Object object) {
            return type.getSuperClass();
        }

        @Specialization
        public Object applySuperGeneric(Object type, Object object) {
            throw new RuntimeException("super is not supported for type " + type + " object " + object);
        }
    }

    // type(object)
    @Builtin(name = "type", hasFixedNumOfArguments = true, fixedNumOfArguments = 1, isConstructor = true)
    @GenerateNodeFactory
    public abstract static class TypeNode extends PythonBuiltinNode {

        @Specialization
        public Object type(PythonObject object) {
            return object.getPythonClass();
        }

        @Specialization
        @SuppressWarnings("unused")
        public Object type(int value) {
            return PInt.__class__;
        }

        @Specialization
        @SuppressWarnings("unused")
        public Object type(double value) {
            return PFloat.__class__;
        }

        @Specialization
        @SuppressWarnings("unused")
        public Object type(String value) {
            return PString.__class__;
        }

        @Specialization
        @SuppressWarnings("unused")
        public Object type(PList value) {
            return PList.__class__;
        }

        @Specialization
        @SuppressWarnings("unused")
        public Object type(PTuple value) {
            return PTuple.__class__;
        }

        @Specialization
        @SuppressWarnings("unused")
        public Object type(PSet value) {
            return PSet.__class__;
        }

        @Specialization
        @SuppressWarnings("unused")
        public Object type(PDict value) {
            return PDict.__class__;
        }

        @Fallback
        public Object type(Object object) {
            throw new RuntimeException("type is not supported for object " + object + " " + object.getClass());
        }
    }

    // __import__(name, globals=None, locals=None, fromlist=(), level=0)
    @Builtin(name = "__import__", hasFixedNumOfArguments = true, fixedNumOfArguments = 1)
    @GenerateNodeFactory
    public abstract static class ImportNode extends PythonBuiltinNode {

        @Specialization
        public Object __import__(String name) {
            if (name.equals("__main__")) {
                Object importedModule = getContext().getMainModule();
                return importedModule;
            } else {
                Object importedModule = getContext().getPythonBuiltinsLookup().lookupModule(name);
                return importedModule;
            }
        }
    }

    @TruffleBoundary
    private static void typeError(String message) {
        throw Py.TypeError(message);
    }

}
