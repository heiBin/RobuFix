package com.duoyi.gradle

import com.android.SdkConstants
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.transforms.ProGuardTransform
import groovy.io.FileType
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile

public class RobuPlugin implements Plugin<Project>{

    public static final String GENERATE_HASH_DIR = "generate"
    public static final String COMPARSION_HASH_DIR = "compare"
    public static final String PATCH_CLASS_DIR = "patchClass"
    public static final String TEMP_DIR = "tempDir"

    public static final String HASH_TXT = "hash.txt"
    public static final String MAPPING_TXT = "mapping.txt"
    public static final String CONFIG_TXT = "config.txt";

    @Override
    void apply(Project project) {

        project.extensions.create("robufix",RobuExtension);
        project.afterEvaluate {
            def robuExtensions = project.extensions.findByName("robufix") as RobuExtension
            project.android.applicationVariants.all { variant ->
                def enablePatch = false;
                def isMinifyEnabled = variant.getBuildType().isMinifyEnabled()
                def dexTask = project.tasks.findByName("transformClassesWithDexFor${variant.name.capitalize()}")
                def proguardTask = project.tasks.findByName("transformClassesAndResourcesWithProguardFor${variant.name.capitalize()}")
                def prepareInjectName = "prepare${variant.name.capitalize()}Inject"
                def generatePatchName = "generate${variant.name.capitalize()}Patch"
                def baseDir = new File("${robuExtensions.robuDir}\\${variant.dirName}")
                def comparsionDir =  RobuFileUtils.createDirWithDelete(baseDir,COMPARSION_HASH_DIR);
                RobuFileUtils.unZip(robuExtensions.oldZip,comparsionDir.absolutePath);
                def configFile = new File(comparsionDir,CONFIG_TXT);
                def configMap = RobuMapUtils.parseMap(configFile);
                if (variant.getVersionName().equals(configMap.get("versionName"))){
                    if (isMinifyEnabled == Boolean.valueOf(configMap.get("useProguard"))){
                        enablePatch = true;
                    }
                }

                def insertConfigClosure = {
                    def configMapFile = RobuFileUtils.createFile(baseDir,TEMP_DIR,CONFIG_TXT);
                    configMapFile.append(RobuMapUtils.format("versionName",variant.getVersionName()))
                    configMapFile.append(RobuMapUtils.format("useProguard",isMinifyEnabled?"true":"false"))
                }

                //Inject Code Task
                project.task(prepareInjectName)<<{
                    RobuInject robuInject = new RobuInject(project,baseDir,isMinifyEnabled);
                    dexTask.inputs.files.files.each { File file ->
                        def fileName = file.absolutePath;
                        if (isMinifyEnabled){
                            file.eachFileRecurse FileType.FILES, { File inputFile ->
                                if (inputFile.absolutePath.endsWith(SdkConstants.DOT_JAR)) {
                                    def jarFile = new JarFile(inputFile);
                                    Enumeration enumeration = jarFile.entries();
                                    def unzipDir = new File(inputFile.getParentFile(),"unzip");
                                    if (unzipDir.exists()){
                                        unzipDir.deleteDir();
                                    }
                                    unzipDir.mkdirs();
                                    while (enumeration.hasMoreElements()){
                                        JarEntry jarEntry = (JarEntry) enumeration.nextElement();
                                        if (jarEntry.isDirectory()) {
                                            continue;
                                        }
                                        InputStream inputStream = jarFile.getInputStream(jarEntry);
                                        File outFileName = new File("${unzipDir.absolutePath}\\${jarEntry.getName()}");
                                        if (!outFileName.getParentFile().exists()){
                                            outFileName.getParentFile().mkdirs();
                                        }
                                        FileUtils.copyInputStreamToFile(inputStream,outFileName)
                                    }
                                    robuInject.injectDir(unzipDir.absolutePath);
                                    inputFile.delete();
                                    RobuFileUtils.zipFolder(unzipDir,inputFile.absolutePath);
                                    unzipDir.deleteDir()
                                }
                            }
                        }else if (fileName.endsWith("intermediates\\classes\\${variant.dirName}".toLowerCase())){
                            robuInject.injectDir(fileName);
                        }
                    }
                }

                //generate Patch Task
                project.task(generatePatchName)<<{
                    if (enablePatch){
                        RobuDexUtils.generaDexJar(project,"${baseDir}\\${PATCH_CLASS_DIR}")
                    }else {
                        throw new Exception();
                    }
                }

                def prepareInjectTask = project.tasks[prepareInjectName]
                def generatePatchTask = project.tasks[generatePatchName]
                prepareInjectTask.dependsOn dexTask.taskDependencies.getDependencies(dexTask)
                dexTask.dependsOn prepareInjectTask;
                generatePatchTask.dependsOn prepareInjectTask;
                prepareInjectTask.doFirst(insertConfigClosure)

                if (proguardTask){
                    def insertProguardClosure = {
                        if (enablePatch) {
                            def mappingFile = new File("${baseDir.absolutePath}\\${COMPARSION_HASH_DIR}\\${MAPPING_TXT}");
                            if (mappingFile.exists()) {
                                def transformTask = (TransformTask) proguardTask
                                def transform = (ProGuardTransform) (transformTask.getTransform())
                                transform.applyTestedMapping(mappingFile)
                            }
                        }
                    }

                    def copyProguardClosure = {
                            def mappingFile = new File("${project.buildDir}\\outputs\\mapping\\${variant.dirName}\\${MAPPING_TXT}")
                            if (mappingFile.exists()){
                                def copyFile = RobuFileUtils.createFile(baseDir,TEMP_DIR,MAPPING_TXT);
                                FileUtils.copyFile(mappingFile,copyFile);
                            }
                    }
                    proguardTask.doFirst(insertProguardClosure)
                    proguardTask.doLast(copyProguardClosure)
                }

            }
        }
    }
}