package com.dian.variant

import com.android.build.gradle.api.ApplicationVariant
import com.dian.extension.DianDexExtension
import com.dian.plugin.DianDexPlugin
import com.dian.util.DianDexInstantRun
import com.dian.util.DianDexUtils
import com.dian.util.LibDependency
import com.dian.util.ProjectSnapShoot
import fastdex.build.lib.snapshoot.sourceset.PathInfo
import fastdex.build.lib.snapshoot.sourceset.SourceSetDiffResultSet
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

class DianDexVariant {
    Project project
    ApplicationVariant androidVariant
    DianDexExtension configuration
    String variantName
    String manifestPath
    String dexPath
    String classPath
    File rootBuildDir
    File buildDir
    ProjectSnapShoot projectSnapShoot
    DianDexInstantRun dianDexInstantRun
    boolean compiledByOriginJavac
    Set<LibDependency> libraryDependencies
    boolean hasSnapShoot
    JavaCompile javaCompile

    DianDexVariant(Project project, ApplicationVariant androidVariant){
        this.project = project
        this.androidVariant = androidVariant

        this.configuration = project.dianDex
        this.variantName = androidVariant.name


//        获取manifest文件路径
        def processManifest = androidVariant.outputs.first().getProcessManifestProvider().get()
        def processResources = androidVariant.outputs.first().getProcessResourcesProvider().get()
        if (processManifest.properties['manifestOutputFile'] != null){
            this.manifestPath = processManifest.aaptFriendlyManifestOutputFile.absolutePath
        }else if (processResources.properties['manifestFile'] != null){
            this.manifestPath = processResources.manifestFile.absolutePath
        }

//        获取build文件路径
        this.rootBuildDir = DianDexUtils.getBuildDir(project)
        this.buildDir = DianDexUtils.getBuildDir(project, this.variantName)


//        获取dex文件路径
        this.dexPath = project.getBuildDir().absolutePath + File.separator +
                "intermediates" + File.separator + "dex" + File.separator +
                this.variantName

        this.javaCompile = this.androidVariant.getJavaCompileProvider().get()

//        新建快照工具
        this.projectSnapShoot = new ProjectSnapShoot(this)
    }

    def prepareEnv(){
//        this.hasDexCache = DianDexUtils.hasDexCache(project, variantName)
        this.hasSnapShoot = DianDexUtils.hasSnapShoot(project, variantName)
        this.projectSnapShoot.loadSnapShoot()
        this.projectSnapShoot.prepareEnv()
        if (hasSnapShoot){
            SourceSetDiffResultSet diffResultSet = projectSnapShoot.diffResultSet
            println("=== Is file changed: " + diffResultSet.isJavaFileChanged())
            if (diffResultSet.isJavaFileChanged()){
                for (List<String> list : diffResultSet.addOrModifiedClassesMap.values()){
                    for (String str : list){
                        println("=== Changed file: " + str)
                    }
                }
            }
        }
    }

//    没有传参数的时候path为null
    def getDiffResultList(String path){
        return projectSnapShoot.getDiffResultList(path)
    }

    def getDiffPathInfoSet(){
        return projectSnapShoot.getDiffPathInfoSet()
    }

//    获取项目依赖项
    def getLibraryDependencies() {
        if (libraryDependencies == null) {
            libraryDependencies = LibDependency.resolveProjectDependency(project, androidVariant)
        }
        return libraryDependencies
    }

//    获取原始manifest文件的package节点的值
    def getOriginPackageName() {
        return androidVariant.getVariantData().getVariantConfiguration().getOriginalApplicationId()
    }
}