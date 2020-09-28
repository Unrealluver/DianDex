package com.dian.plugin

import fastdex.common.utils.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class MyTask extends DefaultTask{

    @TaskAction
    void sayHello(){
        project.logger.error("Hello World in MyTask.")
        File source = new File("D:\\myAndroidStudio\\GradleTest\\app\\p.dex")
        File target = new File("D:\\myAndroidStudio\\GradleTest\\app\\build\\intermediates\\dex\\debug\\out\\classes2.dex")
//        File target = new File("D:\\classes2.dex")
        FileUtils.copyFileUsingStream(source, target)
        println("*** copy finish.")
    }
}