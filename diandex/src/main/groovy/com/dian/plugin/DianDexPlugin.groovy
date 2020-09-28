package com.dian.plugin

import com.android.build.gradle.api.ApplicationVariant
import com.dian.extension.DianDexExtension
import com.dian.task.DianDexCleanTask
import com.dian.task.DianDexIgnoreTask
import com.dian.task.DianDexManifestTask
import com.dian.util.DianDexBuildListener
import com.dian.util.DianDexInstantRun
import com.dian.util.DianDexUtils
import com.dian.util.GradleUtils
import com.dian.variant.DianDexVariant
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException

class DianDexPlugin implements Plugin<Project>{

    @Override
    void apply(Project project) {
        println("*** Print in DianDexPlugin ***")
        project.tasks.create("dianTask", MyTask)

//        检测com.android.application是否导入
        if (!project.plugins.hasPlugin('com.android.application')) {
            throw new GradleException('Android Application plugin required')
        }

//        获取DianDex配置信息，使用project.dianDex访问
        project.extensions.create("dianDex", DianDexExtension)
//        对项目设置监听
        DianDexBuildListener.addByProject(project)

//        获取android配置信息
        def androidConfig = project.extensions.android
//        关闭此项可以加快编译
        androidConfig.dexOptions.preDexLibraries = false
//        忽略方法数限制的检查，Android设定的方法数限制是65536个，超过此数无法生成dex
        androidConfig.dexOptions.jumboMode = true

//        Android Gradle 插件 3.0.0 及更高版本默认情况下会启用 AAPT2
        if (GradleUtils.getAndroidGradlePluginVersion() >= "3.0.0"){
            //TODO 禁掉aapt2，运行时关闭失败，待调研
            reflectAapt2Flag(project)
            //TODO 禁止掉dex archive任务，运行时关闭失败，待调研
            reflectDexArchive(project)
        }

        project.afterEvaluate {
            def configuration = project.dianDex
//            用户是否启用dianDex服务
            if (!configuration.dianDexEnable){
                project.logger.error("====dianDex tasks are disabled.====")
                return
            }

//            Gradle 2.2.2 后引入build-cache机制，能提高全量打包速度
            if (GradleUtils.getAndroidGradlePluginVersion() >= "2.2.2"){
                project.logger.error("==== dianDex add dynamic property: 'android.enableBuildCache = true'")
                GradleUtils.addDynamicProperty(project, "android.enableBuildCache", "true")
            }else{
                project.logger.error("It is recommended to use gradle versions larger than 2.3")
            }

//            打开kotlin增量编译
            project.logger.error("==== dianDex add dynamic property: 'kotlin.incremental = true'")
            GradleUtils.addDynamicProperty(project, "kotlin.incremental", "true")

//            本插件最低支持Gradle2.0.0
            if (GradleUtils.getAndroidGradlePluginVersion() < "2.0.0") {
                throw new GradleException("Android gradle version too old 'com.android.tools.build:gradle:${GradleUtils.getAndroidGradlePluginVersion()}', minimum support version ${minSupportVersion}")
            }

            project.tasks.create("dianDexCleanAll", DianDexCleanTask)

//            apt功能即注解处理器是否可用
            def aptConfiguration = project.configurations.findByName("apt")
            def isAptEnabled = (project.plugins.hasPlugin("android-apt") || project.plugins.hasPlugin("com.neenbedankt.android-apt")) && aptConfiguration != null && !aptConfiguration.empty

//            release和debug两种模式
            androidConfig.applicationVariants.each { ApplicationVariant variant ->
                def variantOutput = variant.outputs.first()
                def variantName = variant.name.capitalize()

                try {
//                    与instant run有冲突需要禁掉instant run
                    def instantRunTask = project.tasks.getByName("transformClassesWithInstantRunFor${variantName}")
                    if (instantRunTask) {
                        throw new GradleException(
                                "DianDex does not support instant run mode, please trigger build"
                                        + " by assemble${variantName} or disable instant run"
                                        + " in 'File->Settings...'."
                        )
                    }
                } catch (UnknownTaskException ignored) {
                    // Not in instant run mode, continue.
                }

                boolean proguardEnable = variant.getVariantData().getVariantConfiguration().getBuildType().isMinifyEnabled()
//                当release下且onlyHookDebug为true此项为true
                boolean ignoreBuildType = configuration.onlyHookDebug && !"debug".equalsIgnoreCase(variant.buildType.name as String)
//                忽略开启混淆的buildType
                if (proguardEnable || ignoreBuildType) {
//                    创建忽略任务，输出忽略信息
                    DianDexIgnoreTask ignoreTask = project.tasks.create("dianDex${variantName}", DianDexIgnoreTask)
                    ignoreTask.androidVariant = variant
                    ignoreTask.proguardEnable = proguardEnable
                    ignoreTask.ignoreBuildType =ignoreBuildType
                }else{
                    def javaCompile = variant.getJavaCompileProvider().get()
//                    指定apt目录
                    setAptOutputDir(isAptEnabled, javaCompile, variant)

                    DianDexVariant dianDexVariant = new DianDexVariant(project, variant)
                    DianDexInstantRun dianDexInstantRun = new DianDexInstantRun(dianDexVariant)
                    dianDexVariant.dianDexInstantRun = dianDexInstantRun

                    if (GradleUtils.getAndroidGradlePluginVersion() < "3.0"){
//                        TODO 此处源码方法不存在,暂未找到合适的替代方法
//                        dianDexInstantRun.resourceApFile = variantOutput.getProcessResourcesProvider().get().packageOutputFile
//                        dianDexVariant.textSymbolOutputFile = new File(variant.getVariantData().getScope().getSymbolLocation(),"R.txt")
                    }else{
//                        TODO 此处源码方法不存在,暂未找到合适的替代方法
//                        def config = variant.getVariantData().getVariantConfiguration()
//                        dianDexInstantRun.resourceApFile = new File(variantOutput.processResources.getResPackageOutputFolder(),"resources-" + config.getFullName()  + ".ap_")
//                        dianDexVariant.textSymbolOutputFile = variantOutput.processResources.getTextSymbolOutputFile()
                    }

                    javaCompile.doLast {
                        dianDexVariant.compiledByOriginJavac = true
                    }

                    if (DianDexUtils.isDataBindingEnabled(project)){
                        configuration.useCustomCompile = false
                        project.logger.error("==== dianDex dataBinding is enabled, disable useCustomCompile...")
                    }

//                    禁用lint任务,该任务的作用待查阅
                    String taskName = "lintVital${variantName}"
                    try {
                        def lintTask = project.tasks.getByName(taskName)
                        lintTask.enabled = false
                    } catch (Throwable ignored) {

                    }

//                    创建清理指定variantName缓存的任务(用户触发)
                    DianDexCleanTask cleanTask = project.tasks.create("dianDexCleanFor${variantName}", DianDexCleanTask)
                    cleanTask.dianDexVariant = dianDexVariant

//                    先执行MergeResources任务，再执行ProcessManifest任务
                    variantOutput.getProcessManifestProvider().get().dependsOn variant.getMergeResourcesProvider().get()

//                    创建修改Manifest文件的任务
                    DianDexManifestTask manifestTask = project.tasks.create("dianDexProcess${variantName}Manifest", DianDexManifestTask)
                    manifestTask.dianDexVariant = dianDexVariant
//                    此任务在ProcessManifest之后执行
                    manifestTask.mustRunAfter variantOutput.getProcessManifestProvider().get()

//                    修复issue#8，具体作用待学习
                    def tinkerPatchManifestTask = getTinkerPatchManifestTask(project, variantName)
                    if (tinkerPatchManifestTask != null) {
                        manifestTask.mustRunAfter tinkerPatchManifestTask
                    }

//                    TODO 此处获取目录方法待调研
                    File resDir = GradleUtils.getResOutputDir(variantOutput.getProcessResourcesProvider().get())
                }
            }
        }
    }

//    对Gradle2.2之前的版本手动指定apt目录
    def setAptOutputDir(boolean isAptEnabled, Object javaCompile, ApplicationVariant variant){
        if (isAptEnabled){
            return
        }

        if (GradleUtils.getAndroidGradlePluginVersion() >= "2.2"){
            return
        }

        def aptOutputDir = new File(variant.getVariantData().getScope().getGlobalScope().getGeneratedDir(), "/source/apt")
        def aptOutput = new File(aptOutputDir, variant.dirName)

        if (variant.variantData.extraGeneratedSourceFolders == null || !variant.variantData.extraGeneratedSourceFolders.contains(aptOutput)) {
            variant.addJavaSourceFoldersToModel(aptOutput)
        }

        javaCompile.doFirst {
            if (!aptOutput.exists()) {
                aptOutput.mkdirs()
            }
        }

        if (javaCompile.options.compilerArgs == null) {
            javaCompile.options.compilerArgs = new ArrayList<>()
        }

//        拷贝一份参数
        def compilerArgs = new ArrayList<>()
        compilerArgs.addAll(javaCompile.options.compilerArgs)

        boolean discoveryAptOutput = false
        def originAptOutput = null

//        找到-s后的参数就是apt目录
        for (Object obj : compilerArgs) {
            if (discoveryAptOutput) {
                originAptOutput = obj
                break
            }
            if ("-s" == obj) {
                discoveryAptOutput = true
            }
        }

//        移除原有的参数
        if (discoveryAptOutput) {
            compilerArgs.remove("-s")
            compilerArgs.remove(originAptOutput)
        }

//        在开头添加参数
        compilerArgs.add(0, "-s")
        compilerArgs.add(1, aptOutput)

//        把参数赋值给编译器
        javaCompile.options.compilerArgs.clear()
        javaCompile.options.compilerArgs.addAll(compilerArgs)

//        把参数取出来放到开头，看不懂这一波操作
    }

//    禁掉aapt2
    def reflectAapt2Flag(Project project) {
        try {
            def booleanOptClazz = Class.forName('com.android.build.gradle.options.BooleanOption')
            def enableAAPT2Field = booleanOptClazz.getDeclaredField('ENABLE_AAPT2')
            enableAAPT2Field.setAccessible(true)
            def enableAAPT2EnumObj = enableAAPT2Field.get(null)
            def defValField = enableAAPT2EnumObj.getClass().getDeclaredField('defaultValue')
            defValField.setAccessible(true)
            defValField.set(enableAAPT2EnumObj, false)
        } catch (Throwable thr) {
            project.logger.error("relectAapt2Flag error: ${thr.getMessage()}.")
        }
    }

//    禁止掉dex archive任务
    def reflectDexArchive(Project project) {
        try {
            def booleanOptClazz = Class.forName('com.android.build.gradle.options.BooleanOption')
            def enableDexArchiveField = booleanOptClazz.getDeclaredField('ENABLE_DEX_ARCHIVE')
            enableDexArchiveField.setAccessible(true)
            def enableDexArchiveObj = enableDexArchiveField.get(null)
            def defValField = enableDexArchiveObj.getClass().getDeclaredField('defaultValue')
            defValField.setAccessible(true)
            defValField.set(enableDexArchiveObj, false)
        } catch (Throwable thr) {
            project.logger.error("reflectDexArchive error: ${thr.getMessage()}.")
        }
    }

    Task getTinkerPatchManifestTask(Project project, String variantName) {
        String taskName = "tinkerpatchSupportProcess${variantName}Manifest"
        try {
            return project.tasks.getByName(taskName)
        } catch (Throwable ignored) {
            return null
        }
    }
}