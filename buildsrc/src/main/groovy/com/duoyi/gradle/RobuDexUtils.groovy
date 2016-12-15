package com.duoyi.gradle

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project

public class RobuDexUtils {

    public static void generaDexJar(Project project, String filPath) {
        File classDir = new File(filPath)
        if (classDir.listFiles().size()) {
            def sdkDir

            Properties properties = new Properties()
            File localProps = project.rootProject.file("local.properties")
            if (localProps.exists()) {
                properties.load(localProps.newDataInputStream())
                sdkDir = properties.getProperty("sdk.dir")
            } else {
                sdkDir = System.getenv("ANDROID_HOME")
            }
            if (sdkDir) {
                def cmdExt = Os.isFamily(Os.FAMILY_WINDOWS) ? '.bat' : ''
                def stdout = new ByteArrayOutputStream()
                project.exec {
                    commandLine "${sdkDir}/build-tools/${project.android.buildToolsVersion}/dx${cmdExt}",
                            '--dex',
                            "--output=${new File(classDir.getParent(), "patch.jar").absolutePath}",
                            "${classDir.absolutePath}"
                    standardOutput = stdout
                }
                def error = stdout.toString().trim()
                if (error) {
                    println "dex error:" + error
                }
            } else {
                throw new InvalidUserDataException('$ANDROID_HOME is not defined')
            }
        }
    }

    public static String getApplicationName(Project project) {
        String applicationName = 'Application'
        project.android.applicationVariants.all { variant ->
            def manifestPath = "${project.buildDir}/intermediates/manifests/full/${variant.dirName}/AndroidManifest.xml"
            File file = new File(manifestPath)
            if (file.exists()) {
                XmlSlurper xmlSlurper = new XmlSlurper()
                def manifest = xmlSlurper.parse(file)
                def application = manifest.application
                String name = application['@android:name']
                applicationName = name
                return null
            }
        }
        return applicationName
    }
}