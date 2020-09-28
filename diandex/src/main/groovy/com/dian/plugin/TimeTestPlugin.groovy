package com.dian.plugin

import com.dian.util.DianDexUtils
import fastdex.build.lib.snapshoot.sourceset.PathInfo
import org.gradle.api.Plugin
import org.gradle.api.Project

class TimeTestPlugin implements Plugin<Project>{

    @Override
    void apply(Project project) {
        project.logger.error("*** TimeTestPlugin ***")

        String javacCmd = DianDexUtils.getJavacCmdPath()
        println("=== Javac cmd path: " + javacCmd)

        List<String> cmdArgs = new ArrayList<>()
        cmdArgs.add(javacCmd)
        cmdArgs.add("-encoding")
        cmdArgs.add("UTF-8")
        cmdArgs.add("-d")
        cmdArgs.add("D:\\tmp\\compile")
        cmdArgs.add("D:\\myAndroidStudio\\GradleTest\\app\\src\\main\\java\\com\\example\\gradletest\\Hello.java")

        long begin = System.currentTimeMillis()
        DianDexUtils.runCommand(project, cmdArgs)
        long end = System.currentTimeMillis()
        project.logger.error("*** Compile java spend: " + (end-begin) + "ms")
    }
}