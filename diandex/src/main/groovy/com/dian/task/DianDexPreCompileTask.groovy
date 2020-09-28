package com.dian.task

import com.dian.variant.DianDexVariant
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class DianDexPreCompileTask extends DefaultTask{
    DianDexVariant dianDexVariant

    @TaskAction
    void compileJava(){
        project.logger.error("*** Print in DianDexPreCompileTask ***")

        long beginTime = System.currentTimeMillis()

//        为了让transform能够运行删除输出目录
        String transformPath = project.getBuildDir().absolutePath + File.separator + "intermediates" +
                File.separator + "transforms" + File.separator + "DianDexCompileTransform"
        println("=== Delete DianDexCompileTransform output dir: " + transformPath)
        File file = new File(transformPath)
        if (file.exists()){
            file.deleteDir()
        }

        String transformPath2 = project.getBuildDir().absolutePath + File.separator + "intermediates" +
                File.separator + "transforms" + File.separator + "DianDexTransform"
        println("=== Delete DianDexTransform output dir: " + transformPath2)
        File file2 = new File(transformPath2)
        if (file2.exists()){
            file2.deleteDir()
        }
        long endTime = System.currentTimeMillis()

        project.logger.error("*** Delete Transform output spend: " + (endTime-beginTime) + "ms")

    }
}