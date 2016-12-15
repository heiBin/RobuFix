##RobuFix热修复方案
> 结合美团Robust原理和Instant Run框架开发而成。

###RobuFix简介
RobuFix通过对项目所有方法进行代码预注入，然后加载生成的补丁包，替换指定方法内容，从而修复问题。目前只支持方法级别的修复，暂不支持新增类，方法，类变量。

主要包括两个部分：

- 核心代码库(com.duoyi.provider.library):负责加载补丁包，反射替换类对象等。
- gradle插件(buildsrc):负责应用包代码预注入，打补丁包。

###RobuFix修复原理
详细原理可以查看[美团热更新方案](http://tech.meituan.com/android_robust.html )

###RobuFix接入说明

#####Gradle接入

在项目的build.gradle中应用Gradle插件（目前暂时没有使用依赖库，直接用buildsrc）

	apply plugin: com.duoyi.gradle.RobuPlugin


同时在build.gradle中加入robufix自定义变量，robuDir为补丁包和其他文件的输出目录，oldZip为比对包（每次打包都会生成一个比对包，放在robuDir目录下），excludeClass为排除代码注入的类，excludeClass为排除的包名。

	robufix{
	    robuDir = "E:\\robufix\\RobuFix\\app\\build\\robufix"
	    oldZip = "E:\\robufix\\RobuFix\\app\\build\\robufix\\debug\\generate\\generate-161215-161221.zip"
	    excludeClass = [ ]
	    excludePackage = [ ]
	}

#####核心代码接入

在Application中加入这段代码，建议在attachBaseContext方法中加入。

 	RobuFix.loadPatch(context, Environment.getExternalStorageDirectory().getAbsolutePath().concat("/patch.jar"));


###RobuFix使用说明

#####打应用包

直接使用Android Studio默认打包工具就可以了，打包后会在之前指定的`robuDir`目录下生成以下文件:

- generate会在打包时生成一个对比压缩包，包括mapping.txt（混淆文件），hash.txt（方法签名对照表），config.txt（配置文件）。
- compare存放解压出来的对比包文件，打补丁包时候自动生成。
- patchClass存放打出的补丁文件（.class文件）

![image](https://github.com/heiBin/RobuFix/tree/master/image/filePicture.png)

#####打补丁包

 打补丁包前需要在build.gradle中配置`oldZip`参数，为原应用对应的对比包路径。

目前是直接使用`gradlew  generate${variant.name}Patch`指令打补丁包，` ${variant.name}`为构建类型（例如Debug或Release），然后会在`robuDir`目录下生成`patch.jar`文件

#####导入补丁包

可以利用adb push指令导入补丁包到模拟器中，例如：

	adb push ./app/build/robufix/patch.jar /sdcard/

`./app/build/robufix/patch.jar`要改成补丁包所在的文件路径，然后重启应用，就可以看到效果了:)

![image](https://github.com/heiBin/RobuFix/tree/master/image/patch.gif)



