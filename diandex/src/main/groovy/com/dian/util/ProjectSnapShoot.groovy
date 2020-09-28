package com.dian.util

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.dependency.VariantDependencies
import com.dian.variant.DianDexVariant
import fastdex.build.lib.snapshoot.api.Node
import fastdex.build.lib.snapshoot.file.FileNode
import fastdex.build.lib.snapshoot.res.AndManifestDirectorySnapshoot
import fastdex.build.lib.snapshoot.sourceset.JavaDirectorySnapshoot
import fastdex.build.lib.snapshoot.sourceset.PathInfo
import fastdex.build.lib.snapshoot.sourceset.SourceSetDiffResultSet
import fastdex.build.lib.snapshoot.sourceset.SourceSetSnapshoot
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import fastdex.common.utils.FileUtils

class ProjectSnapShoot {
    DianDexVariant dianDexVariant

    SourceSetSnapshoot sourceSetSnapShoot
    SourceSetSnapshoot oldSourceSetSnapShoot

    AndManifestDirectorySnapshoot andManifestDirectorySnapShoot

    SourceSetDiffResultSet diffResultSet

    ProjectSnapShoot(DianDexVariant dianDexVariant){
        this.dianDexVariant = dianDexVariant
    }

    def loadSnapShoot(){
        def project = dianDexVariant.project
        File sourceSetSnapShootFile = DianDexUtils.getSourceSetSnapShootFile(project, dianDexVariant.variantName)
        if (dianDexVariant.hasSnapShoot){
            this.oldSourceSetSnapShoot = SourceSetSnapshoot.load(sourceSetSnapShootFile, SourceSetSnapshoot.class)
            println("=== Load SnapShoot.")
        }
    }

    def prepareEnv(){
        def project = dianDexVariant.project
//        project.logger.error("==== in ProjectSnapShoot prepareEnv")
//        println("projectDir: " + project.getProjectDir())
//        源码快照
        sourceSetSnapShoot = new SourceSetSnapshoot(project.getProjectDir(), getProjectSrcDirSet(project))
//        for (Node node : sourceSetSnapShoot.nodes){
//            println("sourSet node: " + node.toString())
//        }
//        for (JavaDirectorySnapshoot jds: sourceSetSnapShoot.directorySnapshootSet){
//            for (Node node: jds.nodes){
//                println("javaDir node: " + node.toString())
//            }
//        }
////        自动生成文件如R.java buildConfig.java快照
//        this.handleGeneratedSource(sourceSetSnapShoot)
////        依赖库快照
//        this.handleLibraryDependencies(sourceSetSnapShoot)
////        manifest文件快照
//        this.andManifestDirectorySnapShoot = new AndManifestDirectorySnapshoot()
//        def list = getProjectManifestFiles(project)
//        list.each {
//            println("add manifest: " + ((File)it).absolutePath)
//            andManifestDirectorySnapShoot.addFile(it)
//        }
//        for (LibDependency libDependency : dianDexVariant.getLibraryDependencies()) {
//            if (libDependency.androidLibrary) {
//                File file = libDependency.dependencyProject.android.sourceSets.main.manifest.srcFile
//                andManifestDirectorySnapShoot.addFile(file)
//            }
//        }
//        if (dianDexVariant.hasDexCache){
//            println("cache exist.")
//        }else {
//            println("can't find cache.")
//        }

        saveSourceSetSnapShoot(sourceSetSnapShoot)

        if (dianDexVariant.hasSnapShoot){
            this.diffResultSet = sourceSetSnapShoot.diff(oldSourceSetSnapShoot)
            println("=== Get diffResultSet.")
        }
    }

    def saveSourceSetSnapShoot(SourceSetSnapshoot snapShoot) {
        File file = DianDexUtils.getSourceSetSnapShootFile(
                dianDexVariant.project, dianDexVariant.variantName)
        if (!file.exists()){
            file.createNewFile()
        }
        snapShoot.serializeTo(new FileOutputStream(file))
        println("=== Save SourceSetSnapShoot to " + file.absolutePath)
    }

    def getProjectManifestFiles(Project project) {
        def manifestFiles = new LinkedHashSet()

        if (project.hasProperty("android") && project.android.hasProperty("sourceSets")) {
            manifestFiles.addAll(DianDexUtils.getManifestFile(project, "main"))

            String buildTypeName = dianDexVariant.androidVariant.getBuildType().getName()
            String flavorName = dianDexVariant.androidVariant.flavorName

            if (buildTypeName && flavorName) {
                File file = DianDexUtils.getManifestFile(project, flavorName + buildTypeName.capitalize() as String)

                if (FileUtils.isLegalFile(file)) {
                    manifestFiles.add(file)
                }
            }

            if (buildTypeName) {
                File file = DianDexUtils.getManifestFile(project, buildTypeName)

                if (FileUtils.isLegalFile(file)) {
                    manifestFiles.add(file)
                }
            }

            if (flavorName) {
                File file = DianDexUtils.getManifestFile(project, flavorName)

                if (FileUtils.isLegalFile(file)) {
                    manifestFiles.add(file)
                }
            }
        }

        return manifestFiles
    }

    def handleLibraryDependencies(SourceSetSnapshoot snapShoot) {
        for (LibDependency libDependency : dianDexVariant.getLibraryDependencies()) {
            Set<File> srcDirSet = getProjectSrcDirSet(libDependency.dependencyProject)

            for (File file : srcDirSet) {
                JavaDirectorySnapshoot javaDirectorySnapShoot = new JavaDirectorySnapshoot(file)
                javaDirectorySnapShoot.projectPath = libDependency.dependencyProject.projectDir.absolutePath
                snapShoot.addJavaDirectorySnapshoot(javaDirectorySnapShoot)
            }
        }
    }

    def handleGeneratedSource(SourceSetSnapshoot snapShoot){
        Set<LibDependency> set = dianDexVariant.getLibraryDependencies()
        println("*** set size: " + set.size())
        List<Project> projectList = new ArrayList<>()
        projectList.add(dianDexVariant.project)
        for (LibDependency libDependency : set){
//            输出结果为：D:\myAndroidStudio\GradleTest\app\build\libs\app.jar 该文件似乎并不存在
            println("libDependency => " + libDependency.jarFile.absolutePath)
            if (libDependency.androidLibrary){
                projectList.add(libDependency.dependencyProject)
            }
        }

        def libraryVariantDirName = "release"
        if (GradleUtils.getAndroidGradlePluginVersion() >= "3.0"){
            libraryVariantDirName = dianDexVariant.androidVariant.getBuildType().getName()
            println("libraryVariantDirName: " + libraryVariantDirName)
        }

        for (int i = 0 ; i < projectList.size() ; i++){
            Project project = projectList.get(i)
            String packageName = (i == 0 ? dianDexVariant.getOriginPackageName() :
                    GradleUtils.getPackageName(project.android.sourceSets.main.manifest.srcFile.absolutePath))
            String packageNamePath = packageName.split("\\.").join(File.separator)
            println("project name: " + project.getName() + "  packageName: " + packageName)

//            buildConfig
            String buildConfigJavaRelativePath = "${packageNamePath}${File.separator}BuildConfig.java"
            String rJavaRelativePath = "${packageNamePath}${File.separator}R.java"

            BaseVariant baseVariant = (i == 0 ? dianDexVariant.androidVariant :
                    GradleUtils.getLibraryFirstVariant(project, libraryVariantDirName))
            File buildConfigDir = baseVariant.getVariantData().getScope().getBuildConfigSourceOutputDir()
//            File rDir = baseVariant.getVariantData().getScope().getRClassSourceOutputDir()
            File rsDir = baseVariant.getVariantData().getScope().getRenderscriptResOutputDir()
//            File aidlDir = baseVariant.getVariantData().getScope().getAidlSourceOutputDir()
            println("buildConfigDir: " + buildConfigDir.absolutePath +
                    "\nrsDir: " + rsDir.absolutePath)

//            buildConfig
            File buildConfigJavaFile = new File(buildConfigDir, buildConfigJavaRelativePath)
            JavaDirectorySnapshoot buildConfigSnapShoot = new JavaDirectorySnapshoot(buildConfigDir,
                    true, buildConfigJavaFile.absolutePath)
            buildConfigSnapShoot.projectPath = project.projectDir.absolutePath
            snapShoot.addJavaDirectorySnapshoot(buildConfigSnapShoot)

//            rs
            JavaDirectorySnapshoot rsDirectorySnapShoot = new JavaDirectorySnapshoot(rsDir)
            rsDirectorySnapShoot.projectPath = project.projectDir.absolutePath
            snapShoot.addJavaDirectorySnapshoot(rsDirectorySnapShoot)
        }
    }

    def getProjectSrcDirSet(Project project){
        def srcDirs = new LinkedHashSet()
        if (project.hasProperty("android") && project.android.hasProperty("sourceSets")){
            println("=== Search java dir by property android & sourceSets")

//            获取main目录下的java目录路径文件
            srcDirs.addAll(DianDexUtils.getSrcDirs(project, "main"))
//            println("srcDir size : " + srcDirs.size() + "  =>  list :" + srcDirs.toList().toString())
//
////            获取buildTypeName和flavorName
//            String buildTypeName = dianDexVariant.androidVariant.getBuildType().getName()
//            String flavorName = dianDexVariant.androidVariant.flavorName
//            println("buildTypeName: " + buildTypeName + "  flavorName: " + flavorName)
//
////            添加buildTypeName && flavorName目录
//            if (buildTypeName && flavorName){
//                project.logger.error("in buildTypeName && flavorName")
//                srcDirs.addAll(DianDexUtils.getSrcDirs(project, flavorName + buildTypeName.capitalize() as String))
//            }
//
////            添加buildTypeName目录
//            if (buildTypeName){
//                project.logger.error("in buildTypeName")
//                srcDirs.addAll(DianDexUtils.getSrcDirs(project, buildTypeName))
//            }
//
////            添加flavorName目录
//            if (flavorName){
//                project.logger.error("in flavorName")
//                srcDirs.addAll(DianDexUtils.getSrcDirs(project, flavorName))
//            }
//
//            println("srcDir size : " + srcDirs.size() + "  =>  list :" + srcDirs.toList().toString())

        }else if (project.plugins.hasPlugin("java") && project.hasProperty("sourceSets")){
            println("=== Search java dir by plugin java & sourceSets")
            srcDirs.addAll(project.sourceSets.main.java.srcDirs.asList())
        }
        println("=== Get java dir: " + srcDirs.toList().toString())
        return srcDirs
    }

    def getDiffResultList(String path){
        List<String> changedList = new ArrayList<>() ;
        if (dianDexVariant.hasSnapShoot && diffResultSet.isJavaFileChanged()){
            for (List<String> list : diffResultSet.addOrModifiedClassesMap.values()){
                for (String str : list){
                    changedList.add(path + File.separator + str)
                }
            }
        }
        return changedList
    }

    def getDiffPathInfoSet(){
        Set<PathInfo> pathInfoSet = new HashSet<>()
        if (dianDexVariant.hasSnapShoot && diffResultSet.isJavaFileChanged()){
            for (Set<PathInfo> set : diffResultSet.addOrModifiedPathInfosMap.values()){
                if (set.size() > 0){
                    pathInfoSet.addAll(set)
                }
            }
        }
        return pathInfoSet
    }
}