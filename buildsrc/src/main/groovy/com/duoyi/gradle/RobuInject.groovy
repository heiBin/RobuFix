package com.duoyi.gradle

import org.gradle.api.Project
import org.objectweb.asm.*
import org.objectweb.asm.tree.ClassNode

import java.text.SimpleDateFormat

public class RobuInject {

    private  File generateHashFile
    private  Map hashMap
    private File baseDir;
    private  File patchDir;
    private  ArrayList<String> changeMethods = new ArrayList<String>();
    private  RobuExtension robuExtensions;
    private boolean useProguard = false;
    private static List excludePath = ['R.class','BuildConfig.class','R$',]
    private List excludeContent = ['android.support','com.duoyi.provider.library' ]

    public RobuInject(Project project,File baseDir,boolean isMinifyEnabled){
        robuExtensions = project.extensions.findByName("robufix") as RobuExtension
        this.baseDir = baseDir;
        this.useProguard = isMinifyEnabled
        generateHashFile = RobuFileUtils.createFile(baseDir,RobuPlugin.TEMP_DIR,RobuPlugin.HASH_TXT)
        excludeContent.add(RobuDexUtils.getApplicationName(project))
        excludeContent.add(robuExtensions.excludeClass)
        excludeContent.add(robuExtensions.excludePackage)
        initPatchDir()
        initComparsionHashFile()
    }

    private void initComparsionHashFile(){
        def hashFile = new File("${baseDir.absolutePath}\\${RobuPlugin.COMPARSION_HASH_DIR}\\${RobuPlugin.HASH_TXT}");
        if (hashFile.exists()){
            hashMap = RobuMapUtils.parseMap(hashFile)
        }
    }

    private void initPatchDir(){
        patchDir = new File(baseDir,RobuPlugin.PATCH_CLASS_DIR);
        patchDir.deleteDir()
        patchDir.mkdirs();
    }

    private void generateZip(){
        def tempDir = new File(baseDir,RobuPlugin.TEMP_DIR);
        SimpleDateFormat simpleFormatter = new SimpleDateFormat("yyMMdd-HHmmss",Locale.CHINA)
        String generateFileName = "generate-"+simpleFormatter.format(new Date())+".zip"
        def generateFile = RobuFileUtils.createFile(baseDir,RobuPlugin.GENERATE_HASH_DIR,generateFileName)
        if (!generateFile.getParentFile().exists()){
            generateFile.getParentFile().mkdirs();
        }
        RobuFileUtils.zipFolder(tempDir,generateFile.absolutePath)
        tempDir.deleteDir();
    }




    public  void injectDir(String path) {
        File dir = new File(path)
        if (dir.isDirectory()) {
            dir.eachFileRecurse { File file ->
                String filePath = file.absolutePath
                if (filePath.endsWith(".class")){
                    boolean exclude = false;
                    for (String excludeClass:excludeContent){
                        excludeClass = excludeClass.replace(".","\\");
                        if (filePath.contains(excludeClass)){
                            exclude = true;
                            break;
                        }
                    }
                    for (String excludeClass:excludePath){
                        if (filePath.contains(excludeClass)){
                            exclude = true;
                            break;
                        }
                    }
                    if (exclude == false) {
                        println(filePath)
                        processClass(file)
                    }
                }
            }
            createPatchedInfos();
            generateZip();
        }
    }


    public void processClass(File file) {
        def optClass = new File(file.getParentFile(), file.name+".opt")
        optClass.createNewFile();

        String className = referHackWhenInit(file,optClass);
        for (String methodKey:changeMethods){
            String[] strings = methodKey.split("-");
            String classKey = strings[0];
            if (classKey.equals(className)){
                referPatch(file,className)
                break;
            }
        }
        if (file.exists()) {
            file.delete()
        }
        optClass.renameTo(file)
    }

    private String referHackWhenInit(File file,File optFile) {
        FileInputStream inputStream = new FileInputStream(file);
        FileOutputStream outputStream = new FileOutputStream(optFile)
        ClassReader cr = new ClassReader(inputStream);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, ClassReader.EXPAND_FRAMES);
        String className = classNode.name;
        RobuBaseClassVisitor robuClassVisitor = new RobuClassPackVisitor(classNode,null,cw,hashMap,generateHashFile,changeMethods);
        classNode.accept(robuClassVisitor);
        byte [] bytes = cw.toByteArray()
        outputStream.write(bytes);
        outputStream.close();
        inputStream.close()
        return className;
    }

    private  void referPatch(File file , String className){
        FileInputStream inputStream = new FileInputStream(file);
        def optClass = new File(patchDir.absolutePath+"/"+className+"\$override.class");
        if(!optClass.getParentFile().exists()){
            optClass.getParentFile().mkdirs();
        }
        optClass.createNewFile()
        FileOutputStream outputStream = new FileOutputStream(optClass)
        ClassReader cr = new ClassReader(inputStream);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, ClassReader.EXPAND_FRAMES);
        RobuClassPatchVisitor robuClassVisitor = new RobuClassPatchVisitor(classNode,null,cw,changeMethods);
        classNode.accept(robuClassVisitor);
        byte [] bytes = cw.toByteArray()
        outputStream.write(bytes);
        outputStream.close();
        inputStream.close()
    }

    private  void createPatchedInfos(){
        def optClass = new File(patchDir.absolutePath+"/com/duoyi/provider/robufix/PatchesInfoImpl.class");
        if(!optClass.getParentFile().exists()){
            optClass.getParentFile().mkdirs();
        }
        FileOutputStream outputStream = new FileOutputStream(optClass)
        ClassWriter cw = new ClassWriter(0)
        cw.visit(Opcodes.V1_7,Opcodes.ACC_PUBLIC|Opcodes.ACC_SUPER,"com/duoyi/provider/robufix/PatchesInfoImpl",null,"java/lang/Object",["com/duoyi/provider/library/PatchesInfo"] as String[]);
        cw.visitSource("PatchesInfoImpl.java", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC,"<init>", "()V", null, null);

        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitVarInsn(Opcodes.ALOAD,0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>","()V" ,false);
        mv.visitInsn(Opcodes.RETURN);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLocalVariable("this", "Lcom/duoyi/provider/robufix/PatchesInfoImpl;", null, l0, l1, 0);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC,"getPatchedClassesInfo", "()Ljava/util/List;", "()Ljava/util/List<Lcom/duoyi/provider/library/PatchedClassInfo;>;", null);
        mv.visitCode();
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitTypeInsn(Opcodes.NEW, "java/util/ArrayList");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
        mv.visitVarInsn(Opcodes.ASTORE, 1);
        ArrayList<String> classNameList = new ArrayList<String>();
        for (String methodKey:changeMethods) {
            String[] strings = methodKey.split("-");
            String className = strings[0];
            if (!classNameList.contains(className)) {
                classNameList.add(className);
            }
        }
        for (String className:classNameList){
            mv.visitLabel(new Label())
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitTypeInsn(Opcodes.NEW, "com/duoyi/provider/library/PatchedClassInfo");
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(className);
            mv.visitLdcInsn(className + "\$override");
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/duoyi/provider/library/PatchedClassInfo", "<init>","(Ljava/lang/String;Ljava/lang/String;)V", false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "add", "(Ljava/lang/Object;)Z", false);
            mv.visitInsn(Opcodes.POP);
        }
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ARETURN);
        Label l6 = new Label();
        mv.visitLabel(l6);
        mv.visitLocalVariable("this", "Lcom/duoyi/provider/robufix/PatchesInfoImpl;", null, l2, l6, 0);
        mv.visitLocalVariable("localArrayList", "Ljava/util/ArrayList;", null, l2, l6, 1);
        mv.visitMaxs(4+classNameList.size(), 2);
        mv.visitEnd();
        cw.visitEnd();
        outputStream.write(cw.toByteArray());
        outputStream.close()
    }



}