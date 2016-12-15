package com.duoyi.gradle

import org.apache.commons.codec.digest.DigestUtils
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceMethodVisitor;

/**
 * Created by Lianghaibin on 2016/12/2.
 */

public class RobuClassPackVisitor extends RobuBaseClassVisitor{

    protected Map hashMap;
    protected File generateHashFile;
    private ArrayList<String> changeMethods;

    RobuClassPackVisitor(ClassNode classNode, List<ClassNode> parentNodes, ClassVisitor classVisitor, Map hashMap, File generateHashFile, ArrayList<String> changeMethods) {
        super(classNode, parentNodes, classVisitor)
        this.hashMap = hashMap;
        this.generateHashFile = generateHashFile;
        this.changeMethods = changeMethods;
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visitField(Opcodes.ACC_STATIC,"changeQuickRedirect","L" + CHANGE_TYPE.getInternalName() + ";",null,null);
        super.visit(version, access, name, signature, superName, interfaces)
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        checkMethod(name,desc);
        if (name.equals("<init>"))
        {
            return super.visitMethod(access, name, desc, signature, exceptions)
        }
        MethodVisitor original = super.visitMethod(access, name, desc, signature, exceptions);
        return new ISVisitor(Opcodes.ASM5,original, access, name, desc);
    }

    public void checkMethod(String name,String desc){
        MethodNode methodNode = getMethodByName(name,desc);
        Textifier textifier = new Textifier(Opcodes.ASM5){
            @Override
            public void visitLineNumber(final int line, final Label start) {
            }
        }
        TraceMethodVisitor traceMethodVisitor = new TraceMethodVisitor(textifier);
        methodNode.accept(traceMethodVisitor);
        String methodShaHex = DigestUtils.shaHex(textifier.getText().toListString());
        String methodKey = currentClassName+"-"+name+"-"+desc;
        if (generateHashFile){
            generateHashFile.append(RobuMapUtils.format(methodKey,methodShaHex))
        }
        if (RobuMapUtils.notSame(hashMap,methodKey,methodShaHex)){
            changeMethods.add(methodKey);
        }
    }

    public class ISVisitor extends GeneratorAdapter{

        protected MethodNode methodNode;
        protected Label startLabel;


        protected ISVisitor(int api, MethodVisitor mv, int access, String name, String desc) {

            super(api, mv, access, name, desc)
            this.methodNode = getMethodByName(name,desc);
        }

        @Override
        void visitCode() {
            super.visitCode()
            startLabel = new Label();
            visitLabel(startLabel);
            visitFieldInsn(Opcodes.GETSTATIC,currentClassName,"changeQuickRedirect","L" + CHANGE_TYPE.getInternalName() + ";");
            Label label1 = new Label();
            ifNull(label1)
            String methodIndentity = methodNode.name+":"+methodNode.desc;
            visitLdcInsn(methodIndentity);
            visitFieldInsn(Opcodes.GETSTATIC,currentClassName,"changeQuickRedirect","L" + CHANGE_TYPE.getInternalName() + ";");
            visitMethodInsn(Opcodes.INVOKESTATIC, PATCH_TYPE.getInternalName(), "isSupport", "(Ljava/lang/String;Lcom/duoyi/provider/library/ChangeQuickRedirect;)Z", false);
            visitJumpInsn(Opcodes.IFEQ,label1);
            visitLabel(new Label());
            visitLdcInsn(methodIndentity);

            Type[] args = Type.getArgumentTypes(methodNode.desc);
            loadArgArray(args)

            visitFieldInsn(Opcodes.GETSTATIC,currentClassName,"changeQuickRedirect","L" + CHANGE_TYPE.getInternalName() + ";");
            visitMethodInsn(Opcodes.INVOKESTATIC, PATCH_TYPE.getInternalName(), "accessDispatch", "(Ljava/lang/String;[Ljava/lang/Object;Lcom/duoyi/provider/library/ChangeQuickRedirect;)Ljava/lang/Object;", false);
            handleReturnType(this,methodNode.desc)
            returnValue();
            visitLabel(label1);
        }

        public void loadArgArray(Type[] args) {
            boolean isStatic = ((methodNode.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC)
            int stack = 0;
            push(isStatic ? args.length:(args.length+1));
            newArray(OBJECT_TYPE);
            if (!isStatic) {
                dup();
                push(0);
                visitVarInsn(OBJECT_TYPE.getOpcode(Opcodes.ILOAD), stack);
                stack += OBJECT_TYPE.getSize();
                arrayStore(OBJECT_TYPE);
            }

            for (int i = 0; i < args.length; i++) {
                dup();
                push(isStatic ? i:(i+1));
                visitVarInsn(args[i].getOpcode(Opcodes.ILOAD), stack);
                stack += args[i].getSize()
                box(args[i]);
                arrayStore(OBJECT_TYPE);
            }
        }

        @Override
        void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
            super.visitLocalVariable(name, desc, signature, startLabel, end, index)
        }
    }

    @Override
    void visitEnd() {
        injectSuperAccess();
        super.visitEnd()
    }

    private void injectSuperAccess(){
        if (classNode.superName == null) return;

        ArrayList<MethodNode> accessMethodList = new ArrayList<MethodNode>();
        for (MethodNode methodNode:classNode.methods){
            if ((methodNode.access & Opcodes.ACC_STATIC) == 0 && !methodNode.name.equals("<init>")){
                accessMethodList.add(methodNode);
            }
        }
        int access = Opcodes.ACC_PUBLIC;
        Method method = new Method("accessSuper","(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
        MethodVisitor mv = super.visitMethod(access,method.getName(),method.getDescriptor(),null,null);
        GeneratorAdapter generatorAdapter = new GeneratorAdapter(access, method,mv);
        generatorAdapter.visitCode();
        Label label0 = new Label();
        generatorAdapter.visitLabel(label0);
        Label label3 = new Label();
        generatorAdapter.visitVarInsn(Opcodes.ALOAD, 1);
        generatorAdapter.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);
        int  [] methodSignatures = new int[accessMethodList.size()];
        Label [] labels = new Label[accessMethodList.size()];
        Map<Integer,Integer> keyMap = new HashMap<Integer,Integer>();

        for (int i = 0; i < accessMethodList.size(); i++){
            MethodNode methodNode = accessMethodList.get(i);
            String tempDesc = methodNode.name+":"+methodNode.desc;
            methodSignatures[i] = tempDesc.hashCode();
            labels[i] = new Label();
            keyMap.put(methodSignatures[i],i);
        }
        Arrays.sort(methodSignatures)

        generatorAdapter.visitLookupSwitchInsn(label3, methodSignatures, labels);
        for (int j = 0 ; j <accessMethodList.size() ; j++){
            int position = keyMap.get(methodSignatures[j]);
            MethodNode methodNode = accessMethodList.get(position);
            Type[] args = Type.getArgumentTypes(methodNode.desc);
            int arg = 0;
            generatorAdapter.visitLabel(labels[j]);
            generatorAdapter.visitVarInsn(Opcodes.ALOAD, 0);
            for (Type type:args){
                generatorAdapter.visitVarInsn(Opcodes.ALOAD, 2);
                generatorAdapter.push(arg);
                generatorAdapter.visitInsn(Opcodes.AALOAD);
                unbox2(generatorAdapter,type);
                arg++;
            }
            generatorAdapter.visitMethodInsn(Opcodes.INVOKESPECIAL, classNode.superName, methodNode.name, methodNode.desc, false);
            Type returnType = Type.getReturnType(methodNode.desc)
            generatorAdapter.valueOf(returnType)
            generatorAdapter.visitInsn(Opcodes.ARETURN);
        }
        generatorAdapter.visitLabel(label3);
        generatorAdapter.visitInsn(Opcodes.ACONST_NULL);
        generatorAdapter.visitInsn(Opcodes.ARETURN);
        Label label4 = new Label();
        generatorAdapter.visitLabel(label4);
        mv.visitLocalVariable("this","L"+currentClassName+";", null, label0, label4, 0);
        mv.visitLocalVariable("methodSignature", "Ljava/lang/String;", null, label0, label4, 1);
        mv.visitLocalVariable("paramArrayOfObject", "[Ljava/lang/Object;", null, label0, label4, 2);
        mv.visitMaxs(0,3)
        mv.visitEnd()
    }
}