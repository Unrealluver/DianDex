package com.dian.util

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project

import java.nio.file.Files
import fastdex.common.utils.FileUtils

class DianDexUtils {
//    获取javac命令
    static final String getJavacCmdPath() {
        StringBuilder cmd = new StringBuilder(getCurrentJdk())
        if (!cmd.toString().endsWith(File.separator)) {
            cmd.append(File.separator)
        }
        cmd.append("bin${File.separator}javac")
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            cmd.append(".exe")
        }
        return new File(cmd.toString()).absolutePath
    }

//    获取jar命令
    static final String getJarCmdPath() {
        StringBuilder cmd = new StringBuilder(getCurrentJdk())
        if (!cmd.toString().endsWith(File.separator)) {
            cmd.append(File.separator)
        }
        cmd.append("bin${File.separator}jar")
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            cmd.append(".exe")
        }
        return new File(cmd.toString()).absolutePath
    }

//    获取jdk路径
    static final String getCurrentJdk() {
        String javaHomeProp = System.properties.'java.home'
        if (javaHomeProp) {
            int jreIndex = javaHomeProp.lastIndexOf("${File.separator}jre")
            if (jreIndex != -1) {
                return javaHomeProp.substring(0, jreIndex)
            } else {
                return javaHomeProp
            }
        } else {
            return System.getenv("JAVA_HOME")
        }
    }

//    获取dx命令路径
    static final String getDxCmdPath(Project project) {
        File dx = new File(getSdkDirectory(project),"build-tools${File.separator}${project.android.buildToolsVersion.toString()}${File.separator}dx")
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return "${dx.absolutePath}.bat"
        }
        return dx.getAbsolutePath()
    }

//    获取sdk目录
    static final String getSdkDirectory(Project project) {
        String sdkDirectory = project.android.getSdkDirectory()
        if (sdkDirectory.contains("\\")) {
            sdkDirectory = sdkDirectory.replace("\\", "/")
        }
        return sdkDirectory
    }

//    检查是否有快照
    static boolean hasSnapShoot(Project project, String variantName){
        File file = new File(getBuildDir(project, variantName), Constants.SOURCESET_SNAPSHOOT_FILENAME)
        println("=== Has SnapShoot: " + file.exists())
        return file.exists()
    }

//    获取源码快照文件路径
    static final File getSourceSetSnapShootFile(Project project, String variantName) {
        File file = new File(getBuildDir(project, variantName), Constants.SOURCESET_SNAPSHOOT_FILENAME)
        if (!file.parentFile.exists()){
            file.parentFile.mkdirs()
            println("=== mkdirs: " + file.parentFile.absolutePath)
        }
        return file
    }

//    获取dex目录
    static getDexDir(Project project,String variantName) {
        File file = new File(getBuildDir(project, variantName),"dex")
        if (!file.exists()){
            file.mkdirs()
        }
        return file
    }

//    获取jar目录
    static getJarDir(Project project,String variantName) {
        File file = new File(getBuildDir(project, variantName),"jar")
        if (!file.exists()){
            file.mkdirs()
        }
        return file
    }


//    获取dex缓存目录
    static final File getDexCacheDir(Project project, String variantName) {
        File file = new File(getDexDir(project, variantName), "cache")
        if (!file.exists()){
            file.mkdirs()
        }
        return file
    }

//    检查dex缓存
    static boolean hasDexCache(Project project, String variantName) {
        File cacheDexDir = getDexCacheDir(project, variantName)
        println("cacheDexDir: " + cacheDexDir.absolutePath)
        if (!FileUtils.dirExists(cacheDexDir.absolutePath)) {
            return false
        }

        FindDexFileVisitor visitor = new FindDexFileVisitor()
        Files.walkFileTree(cacheDexDir.toPath(), visitor)
        return visitor.hasDex
    }

//    获取sourceSetKey目录下的manifest文件
    static File getManifestFile(Project project, String sourceSetKey) {
        def sourceSetsValue = project.android.sourceSets.findByName(sourceSetKey)
        if (sourceSetsValue) {
            return sourceSetsValue.manifest.srcFile
        }
        return null
    }

//    获取sourceSetKey目录下的java文件
    static LinkedHashSet<File> getSrcDirs(Project project, String sourceSetKey) {
        def srcDirs = new LinkedHashSet()
        def sourceSetsValue = project.android.sourceSets.findByName(sourceSetKey)
        if (sourceSetsValue) {
            srcDirs.addAll(sourceSetsValue.java.srcDirs.asList())
        }
        return srcDirs
    }

//    获取dianDex的build目录
    static final File getBuildDir(Project project) {
        File file = new File(project.getBuildDir(), Constants.BUILD_DIR)
        return file
    }

//    获取指定variantName的build目录
    static final File getBuildDir(Project project, String variantName) {
        File file = new File(getBuildDir(project), variantName)
        return file
    }

//    清除所有缓存
    static boolean cleanAllCache(Project project) {
        File dir = getBuildDir(project)
        project.logger.error("==== dianDex clean dir: ${dir}")
        return FileUtils.deleteDir(dir)
    }

//    清除指定缓存
    static boolean cleanCache(Project project, String variantName){
        File dir = getBuildDir(project, variantName)
        project.logger.error("==== dianDex clean dir: ${dir}")
        return FileUtils.deleteDir(dir)
    }

    static boolean isDataBindingEnabled(Project project) {
        return project.android.dataBinding && project.android.dataBinding.enabled
    }

    static void runCommand(Project project, List<String> cmdArgs) {
        runCommand(project, cmdArgs, false)
    }

    static void runCommand(Project project, List<String> cmdArgs,boolean background) {
        runCommand(project,cmdArgs,null, background)
    }

    static void runCommand(Project project, List<String> cmdArgs, File directory, boolean background) {
        if (!background) {
            StringBuilder cmd = new StringBuilder()
            for (int i = 0; i < cmdArgs.size(); i++) {
                if (i != 0) {
                    cmd.append(" ")
                }
                cmd.append(cmdArgs.get(i))
            }
            project.logger.error("\n${cmd}")
        }

        int status = -1
        try {
            ProcessBuilder processBuilder = new ProcessBuilder((String[])cmdArgs.toArray())
            if (directory != null) {
                processBuilder.directory(directory)
            }
            def process = processBuilder.start()
            InputStream is = process.getInputStream()
            BufferedReader reader = new BufferedReader(new InputStreamReader(is))
            String line = null
            while ((line = reader.readLine()) != null) {
                println(line)
            }
            reader.close()
            status = process.waitFor()
            reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))
            while ((line = reader.readLine()) != null) {
                System.out.println(line)
            }
            reader.close()
            process.destroy()
        } catch (Throwable e) {

        }
        if (status != 0 && !background) {
            throw new Exception("Command exec fail....")
        }
    }
}