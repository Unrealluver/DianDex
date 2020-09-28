package com.dian.util

import fastdex.build.lib.snapshoot.sourceset.PathInfo
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

class DianDexCompileUtils {
    static void compileJava(Project project, Set<PathInfo> diffJavaSet, Set<File> jarSet, File classDest, JavaCompile javaCompile){
        File androidJar = new File(DianDexUtils.getSdkDirectory(project) + File.separator + "platforms" +
                File.separator + project.android.getCompileSdkVersion() + File.separator +
                "android.jar")
        jarSet.add(androidJar)
        println("=== Android jar path: " + androidJar.absolutePath)

        File jarCacheFile = DianDexUtils.getJarDir(project, "debug")
        File sourceJar = new File(jarCacheFile, "source.jar")
        jarSet.add(sourceJar)

        String javacCmd = DianDexUtils.getJavacCmdPath()
        println("=== Javac cmd path: " + javacCmd)

        List<String> cmdArgs = new ArrayList<>()
        cmdArgs.add(javacCmd)
        cmdArgs.add("-encoding")
        cmdArgs.add("UTF-8")
        cmdArgs.add("-g")
        cmdArgs.add("-target")
        cmdArgs.add(javaCompile.targetCompatibility)
        cmdArgs.add("-source")
        cmdArgs.add(javaCompile.sourceCompatibility)
        cmdArgs.add("-cp")
        cmdArgs.add(joinJarSet(jarSet))
        cmdArgs.add("-d")
        cmdArgs.add(classDest.absolutePath)
        for (PathInfo info : diffJavaSet){
            cmdArgs.add(info.absoluteFile.absolutePath)
        }
        classDest.mkdirs()
        DianDexUtils.runCommand(project, cmdArgs)
    }

    static String joinJarSet(Set<File> jarSet){
        StringBuilder sb = new StringBuilder()

        boolean window = Os.isFamily(Os.FAMILY_WINDOWS)
        for (File file : jarSet){
            sb.append(file)
            if (window) {
                sb.append(";")
            }else {
                sb.append(":")
            }
        }
        return sb
    }

    static void generateJar(Project project, File classSource, File classDest){
        project.logger.error("*** Generate jar ***")
        println("=== Source path: " + classSource.absolutePath)
        String jarCmd = DianDexUtils.getJarCmdPath()
        println("=== Jar cmd: " + jarCmd)
        File jarCacheFile = DianDexUtils.getJarDir(project, "debug")
        println("=== Jar cache path: " + jarCacheFile.absolutePath)

        ArrayList<String> cmdArgs = new ArrayList<>()
        Process process
//        初次生成
        if (jarCacheFile.listFiles().size() == 0){
            cmdArgs.add(jarCmd)
            cmdArgs.add("cvf")
            cmdArgs.add(jarCacheFile.absolutePath + File.separator + "source.jar")
            cmdArgs.add("./")
            println("=== Jar cmd: " + cmdArgs)
            process = Runtime.getRuntime().exec((String[])cmdArgs.toArray(), null, classSource)
        }else{
//            增量合成
            cmdArgs.add(jarCmd)
            cmdArgs.add("uvf")
            cmdArgs.add(jarCacheFile.absolutePath + File.separator + "source.jar")
            cmdArgs.add("./")
            println("=== Jar cmd: " + cmdArgs)
            process = Runtime.getRuntime().exec((String[])cmdArgs.toArray(), null, classDest)
        }

        InputStream is = process.getInputStream()
        BufferedReader reader = new BufferedReader(new InputStreamReader(is))
        String line = null
        while ((line = reader.readLine()) != null) {
            println(line)
        }
        reader.close()
        int status = process.waitFor()
        reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))
        while ((line = reader.readLine()) != null) {
            System.out.println(line)
        }
        reader.close()

        process.destroy()
        if (status == 0){
            println("=== Generate source.jar success.")
        }else{
            println("=== Generate source.jar failed.")
        }

    }
}