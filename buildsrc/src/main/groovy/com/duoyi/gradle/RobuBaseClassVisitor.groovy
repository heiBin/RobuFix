package com.duoyi.gradle

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

public class RobuBaseClassVisitor extends ClassVisitor{

    protected static final Type RUNTIME_TYPE = Type.getObjectType("com/duoyi/provider/library/ReflectUtils");
    protected static final Type CHANGE_TYPE = Type.getObjectType("com/duoyi/provider/library/ChangeQuickRedirect");
    protected static final Type PATCH_TYPE = Type.getObjectType("com/duoyi/provider/library/PatchProxy");
    protected static final Type OBJECT_TYPE = Type.getObjectType("java/lang/Object");
    private static final Type NUMBER_TYPE = Type.getObjectType("java/lang/Number");
    private static final Method SHORT_VALUE = Method.getMethod("short shortValue()");
    private static final Method BYTE_VALUE = Method.getMethod("byte byteValue()");
    protected String currentClassName;
    protected String instanceToStaticDescPrefix;
    protected  ClassNode classNode;
    protected  List<ClassNode> parentNodes;




    static boolean isUseReflection(int nodeAccess)
    {
        if ((nodeAccess & Opcodes.ACC_PRIVATE) != 0) {
            return true;
        }
        if ((nodeAccess & Opcodes.ACC_PROTECTED) != 0) {
            return true;
        }
        if ((nodeAccess & Opcodes.ACC_PUBLIC) != 0) {
            return false;
        }
        return true;
    }
    public RobuBaseClassVisitor(ClassNode classNode, List<ClassNode> parentNodes, ClassVisitor classVisitor){
        super(Opcodes.ASM5, classVisitor)
        this.classNode = classNode;
        this.parentNodes = parentNodes;
        currentClassName = classNode.name;
        instanceToStaticDescPrefix = "L" + currentClassName + ";";
    }

    public FieldNode getFieldByName(String fieldName)
    {
        List<FieldNode> fields = classNode.fields;
        for (FieldNode field : fields) {
            if (field.name.equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    public MethodNode getMethodByName(String methodName, String desc){
        List<MethodNode> methods = classNode.methods;
        for (MethodNode method : methods) {
            if ((method.name.equals(methodName)) && (method.desc.equals(desc))) {
                return method;
            }
        }
        return null;
    }

    protected static void unbox2(GeneratorAdapter generatorAdapter, Type type)
    {
        if (type.equals(Type.SHORT_TYPE))
        {
            generatorAdapter.checkCast(NUMBER_TYPE);
            generatorAdapter.invokeVirtual(NUMBER_TYPE, SHORT_VALUE);
        }
        else if (type.equals(Type.BYTE_TYPE))
        {
            generatorAdapter.checkCast(NUMBER_TYPE);
            generatorAdapter.invokeVirtual(NUMBER_TYPE, BYTE_VALUE);
        }
        else
        {
            generatorAdapter.unbox(type);
        }
    }

    protected static void handleReturnType(GeneratorAdapter generatorAdapter,String desc)
    {
        Type ret = Type.getReturnType(desc);
        if (ret.getSort() == 0) {
            generatorAdapter.pop();
        } else {
            unbox2(generatorAdapter,ret);
        }
    }


}
