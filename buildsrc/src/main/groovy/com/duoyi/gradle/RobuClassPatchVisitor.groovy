package com.duoyi.gradle

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.*
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

public class RobuClassPatchVisitor extends RobuBaseClassVisitor{


    private ArrayList<MethodNode> arrayList = new ArrayList<MethodNode>();
    private ArrayList<String> changeMethods;

    public RobuClassPatchVisitor(ClassNode classNode, List<ClassNode> parentNodes, ClassVisitor classVisitor,ArrayList<String> changeMethods){
        super(classNode,parentNodes,classVisitor)
        this.changeMethods = changeMethods;
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        String[] strings = new String[1];
        strings[0] = CHANGE_TYPE.getInternalName();
        super.visit(version, Opcodes.ACC_PUBLIC|Opcodes.ACC_SUPER, name + "\$override", signature, "java/lang/Object",strings );

        MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLocalVariable("this","L"+currentClassName+"\$override;", null, l0, l1, 0);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    @Override
    FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        return null;
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        String methodKey = currentClassName+"-"+name+"-"+desc;
        boolean isChange = changeMethods.contains(methodKey);
        if (!isChange)return null;
        if (name.equals("<clinit>")) {
            return null;
        }

        if (name.equals("<init>"))
        {
            return null;
        }

        MethodNode methodNode = getMethodByName(name, desc);
        arrayList.add(methodNode);
        boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;

        String  newDesc = computeOverrideMethodDesc(desc,isStatic);
        String newSignature = (signature == null)?null:computeOverrideMethodDesc(signature,isStatic);
        access = Opcodes.ACC_PUBLIC;
        MethodVisitor original = super.visitMethod(access, name, newDesc, newSignature, exceptions);
        return new ISVisitor(original, access, name, newDesc);
    }

    @Override
    void visitEnd() {
        addSupportMethod();
        addDispatchMethod();
        super.visitEnd()

    }

    private String computeOverrideMethodDesc(String desc, boolean isStatic)
    {
        return "("+this.instanceToStaticDescPrefix + desc.substring(1);
    }

     class ISVisitor extends GeneratorAdapter{

        ISVisitor(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM5,mv, access, name, desc)
        }

        @Override
        void visitFieldInsn(int opcode, String owner, String name, String desc) {
            boolean isReflection;
           if (!owner.equals(currentClassName)){
               isReflection = false;
           }else {
               FieldNode fieldNode = getFieldByName(name);
               if (fieldNode == null) {
                   isReflection = false;
               } else {
                   isReflection = isUseReflection(fieldNode.access);
               }
           }
           boolean handled = false;
           switch (opcode)
           {
               case Opcodes.GETSTATIC:
               case Opcodes.PUTSTATIC:
                   handled = visitStaticFieldAccess(opcode, owner, name, desc, isReflection);
                   break;
               case Opcodes.GETFIELD:
               case Opcodes.PUTFIELD:
                   handled = visitFieldAccess(opcode, owner, name, desc, isReflection);
                   break;
               default:
                   System.out.println("Unhandled field opcode " + opcode);
           }
           if (!handled) {
               super.visitFieldInsn(opcode, owner, name, desc);
           }
        }

        private boolean visitFieldAccess(int opcode, String owner, String name, String desc, boolean isReflection) {
            if (isReflection){
                switch (opcode){
                    case Opcodes.GETFIELD:
                        visitLdcInsn(Type.getType("L"+ owner + ";"));
                        push(name);
                        invokeStatic(RUNTIME_TYPE, Method.getMethod("Object getPrivateField(Object, Class, String)"))
                        unbox2(this,Type.getType(desc))
                        break
                    case Opcodes.PUTFIELD:
                        box(Type.getType(desc));
                        visitLdcInsn(Type.getType("L" + owner + ";"));
                        push(name);
                        invokeStatic(RUNTIME_TYPE,
                                Method.getMethod("void setPrivateField(Object, Object, Class, String)"));
                        break;
                    default:
                        throw new RuntimeException("VisitFieldAccess called with wrong opcode " + opcode);
                }
                return true
            }
            return false
        }

        private boolean visitStaticFieldAccess(int opcode, String owner, String name, String desc, boolean isReflection) {
            if (isReflection){
                switch (opcode){
                    case Opcodes.GETSTATIC:
                        visitLdcInsn(Type.getType("L"+ owner + ";"));
                        push(name);
                        invokeStatic(RUNTIME_TYPE, Method.getMethod("Object getStaticPrivateField(Class, String)"))
                        unbox2(this,Type.getType(desc))
                        break;
                    case Opcodes.PUTSTATIC:
                        box(Type.getType(desc));
                        visitLdcInsn(Type.getType("L" + owner + ";"));
                        push(name);
                        invokeStatic(RUNTIME_TYPE,
                                Method.getMethod("void setStaticPrivateField(Object, Class, String)"));
                        break;
                    default:
                        throw new RuntimeException("VisitStaticFieldAccess called with wrong opcode " + opcode);
                }
                return true
            }
            return false
        }

         @Override
         void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
             boolean isReflection;
             if (!owner.equals(currentClassName)){
                 isReflection = false;
             }else {
                 MethodNode methodNode = getMethodByName(name,desc);
                 if (methodNode == null) {
                     isReflection = false;
                 } else {
                     isReflection = isUseReflection(methodNode.access);
                 }
             }
             boolean handled = false;
             switch (opcode){
                 case Opcodes.INVOKEVIRTUAL:
                     handled = visitVirtualMethodAccess(owner,name,desc,itf,isReflection);
                     break;
                 case Opcodes.INVOKESPECIAL:
                     handled = visitSpecialMethodAccess(owner,name,desc,itf,isReflection);
                     break;
                 case Opcodes.INVOKESTATIC:
                     handled = visitStaticMethodAccess(owner,name,desc,itf,isReflection);
                     break;
             }
             if (!handled) {
                 this.mv.visitMethodInsn(opcode, owner, name, desc, itf);
             }
         }

         private boolean visitVirtualMethodAccess(String owner, String name, String desc, boolean itf,boolean isReflection){
             if (isReflection){
                 Type[] parameterTypes = Type.getArgumentTypes(desc);
                 int parameters = boxParametersToNewLocalArray(parameterTypes);
                 loadLocal(parameters);
                 pushParameterTypesOnStack(parameterTypes);
                 push(name);
                 invokeStatic(RUNTIME_TYPE, Method.getMethod("Object invokeProtectedMethod(Object, Object[], Class[], String)"));
                 handleReturnType(this,desc);
                 return true;
             }
             return false;
         }

         private boolean visitSpecialMethodAccess(String owner, String name, String desc, boolean itf,boolean isReflection){
             if (name.equals("<init>"))return false;
             if (!owner.equals(currentClassName)){
                 Type[] parameterTypes = Type.getArgumentTypes(desc);
                 int parameters = boxParametersToNewLocalArray(parameterTypes);
                 String methodIndentity = name+":"+desc;
                 visitLdcInsn(methodIndentity);
                 loadLocal(parameters);
                 Method method = new Method("accessSuper","(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
                 invokeVirtual(Type.getObjectType(currentClassName),method);
                 handleReturnType(this,desc);
                 return true;
             } else if (isReflection){
                 Type[] parameterTypes = Type.getArgumentTypes(desc);
                 int parameters = boxParametersToNewLocalArray(parameterTypes);
                 loadLocal(parameters);
                 pushParameterTypesOnStack(parameterTypes);
                 push(name);
                 invokeStatic(RUNTIME_TYPE, Method.getMethod("Object invokeProtectedMethod(Object, Object[], Class[], String)"));
                 handleReturnType(this,desc);
                 return true;
             }
             return false;
         }



         private boolean visitStaticMethodAccess(String owner, String name, String desc, boolean itf,boolean isReflection){
             if (isReflection){
                 Type[] parameterTypes = Type.getArgumentTypes(desc);
                 int parameters = boxParametersToNewLocalArray(parameterTypes);
                 loadLocal(parameters);
                 pushParameterTypesOnStack(parameterTypes);
                 push(name);
                 visitLdcInsn(Type.getType("L" + owner + ";"));
                 invokeStatic(RUNTIME_TYPE, Method.getMethod("Object invokeProtectedStaticMethod(Object[], Class[], String, Class)"));
                 handleReturnType(this,desc);
                 return true
             }
             return false

         }
        @Override
         public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index)
         {
             if ("this".equals(name)) {
                 name = "\$this";
             }
             if (index == 0){
                 super.visitLocalVariable(name, desc, signature, start, end, index);
             }

             super.visitLocalVariable(name, desc, signature, start, end, index+1);
         }

         @Override
         void visitVarInsn(int opcode, int var) {
             super.visitVarInsn(opcode, var+1)
         }


         private int boxParametersToNewLocalArray(Type[] parameterTypes)
         {
             int parameters = newLocal(Type.getType("[Ljava/lang.Object;"));
             push(parameterTypes.length);
             newArray(Type.getType(Object.class));
             storeLocal(parameters);
             for (int i = parameterTypes.length - 1; i >= 0; i--)
             {
                 loadLocal(parameters);
                 swap(parameterTypes[i], Type.getType(Object.class));
                 push(i);
                 swap(parameterTypes[i], Type.INT_TYPE);
                 box(parameterTypes[i]);
                 arrayStore(Type.getType(Object.class));
             }
             return parameters;
         }

         private void pushParameterTypesOnStack(Type[] parameterTypes)
         {
             push(parameterTypes.length);
             newArray(Type.getType(Class.class));
             for (int i = 0; i < parameterTypes.length; i++)
             {
                 dup();
                 push(i);
                 switch (parameterTypes[i].getSort())
                 {
                     case Type.ARRAY:
                     case Type.OBJECT:
                         visitLdcInsn(parameterTypes[i]);
                         break;
                     case Type.BOOLEAN:
                     case Type.CHAR:
                     case Type.BYTE:
                     case Type.SHORT:
                     case Type.INT:
                     case Type.FLOAT:
                     case Type.LONG:
                     case Type.DOUBLE:
                         push(parameterTypes[i]);
                         break;
                     default:
                         throw new RuntimeException("Unexpected parameter type " + parameterTypes[i]);
                 }
                 arrayStore(Type.getType(Class.class));
             }
         }

     }

    private void addSupportMethod(){
        MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, "isSupport", "(Ljava/lang/String;)Z", null, null);
        mv.visitCode();
        Label label0 = new Label();
        mv.visitLabel(label0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);
        Label label1 = new Label();
        Label label2 = new Label();
        int  [] methodSignatures = new int[arrayList.size()];
        Label [] labels = new Label[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++){
            MethodNode methodNode = arrayList.get(i);
            String tempDesc = methodNode.name+":"+methodNode.desc;
            methodSignatures[i] = tempDesc.hashCode();
            labels[i] = label1;
        }
        Arrays.sort(methodSignatures)
        mv.visitLookupSwitchInsn(label2, methodSignatures as int[], labels);
        mv.visitLabel(label1);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitLabel(label2);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitInsn(Opcodes.IRETURN);
        Label label3 = new Label();
        mv.visitLabel(label3);
        mv.visitLocalVariable("this","L"+currentClassName+"\$override;" , null, label0, label3, 0);
        mv.visitLocalVariable("methodSignature", "Ljava/lang/String;", null, label0, label3, 1);
        mv.visitMaxs(1, 2);
        mv.visitEnd();
    }

    private void addDispatchMethod(){

        Method method = new Method("accessDispatch", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
        MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, method.getName(),method.getDescriptor(), null, null);
        GeneratorAdapter generatorAdapter = new GeneratorAdapter(Opcodes.ACC_PUBLIC,method,mv);
        generatorAdapter.visitCode();
        Label label0 = new Label();
        generatorAdapter.visitLabel(label0);
        Label label3 = new Label();
        generatorAdapter.visitVarInsn(Opcodes.ALOAD, 1);
        generatorAdapter.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);
        int  [] methodSignatures = new int[arrayList.size()];
        Label [] labels = new Label[arrayList.size()];
        Map<Integer,Integer> keyMap = new HashMap<Integer,Integer>();
        for (int i = 0; i < arrayList.size(); i++){
            MethodNode methodNode = arrayList.get(i);
            String tempDesc = methodNode.name+":"+methodNode.desc;
            methodSignatures[i] = tempDesc.hashCode();
            labels[i] = new Label();
            keyMap.put(methodSignatures[i],i);
        }
        Arrays.sort(methodSignatures)
        generatorAdapter.visitLookupSwitchInsn(label3, methodSignatures as int[], labels);
        for (int j = 0 ; j <arrayList.size() ; j++){
            int position = keyMap.get(methodSignatures[j]);
            MethodNode methodNode = arrayList.get(position);
            boolean isStatic = (methodNode.access & 0x8) != 0;
            String newDesc = computeOverrideMethodDesc(methodNode.desc, isStatic);
            Type[] args = Type.getArgumentTypes(newDesc);

            int arg = 0;
            generatorAdapter.visitLabel(labels[j]);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            generatorAdapter.visitVarInsn(Opcodes.ALOAD, 0);
            for (Type type:args){
                generatorAdapter.visitVarInsn(Opcodes.ALOAD, 2);
                generatorAdapter.push(arg);
                generatorAdapter.visitInsn(Opcodes.AALOAD);
                unbox2(generatorAdapter,type);
                arg++;
            }
            generatorAdapter.visitMethodInsn(Opcodes.INVOKEVIRTUAL, currentClassName+"\$override", methodNode.name, newDesc, false);
            Type returnType = Type.getReturnType(newDesc)
            generatorAdapter.valueOf(returnType)
            generatorAdapter.visitInsn(Opcodes.ARETURN);

        }
        generatorAdapter.visitLabel(label3);
        generatorAdapter.visitInsn(Opcodes.ACONST_NULL);
        generatorAdapter.visitInsn(Opcodes.ARETURN);
        Label label4 = new Label();
        generatorAdapter.visitLabel(label4);
        mv.visitLocalVariable("this","L"+currentClassName+"\$override;", null, label0, label4, 0);
        mv.visitLocalVariable("methodSignature", "Ljava/lang/String;", null, label0, label4, 1);
        mv.visitLocalVariable("paramArrayOfObject", "[Ljava/lang/Object;", null, label0, label4, 2);
        generatorAdapter.visitMaxs(0,3)
        generatorAdapter.visitEnd()

    }


}