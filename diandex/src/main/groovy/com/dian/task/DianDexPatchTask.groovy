package com.dian.task

import com.android.utils.FileUtils
import com.dian.util.DianDexUtils
import com.dian.variant.DianDexVariant
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class DianDexPatchTask extends DefaultTask{
    DianDexVariant dianDexVariant

    @TaskAction
    void patchDex(){
        project.logger.error("*** Print in DianDexPatchTask ***")

        File dexCacheFile = DianDexUtils.getDexDir(project, dianDexVariant.variantName)

        String dexPath = null
        String normalOut = dianDexVariant.dexPath + File.separator + "out"
        String mergeProjectOut = dianDexVariant.dexPath + File.separator +
                "mergeProjectDexDebug" + File.separator + "out"

        println("=== normalOut: " + normalOut)
        println("=== mergeProjectOut: " + mergeProjectOut)

        // D:\myAndroidStudio\GradleTest\app\build\intermediates\dex\debug\out\classes.dex

        if (new File(normalOut).exists()){
            dexPath = normalOut
        }else if (new File(mergeProjectOut).exists()){
            dexPath = mergeProjectOut
        }

        println("=== DianDexVariant.dexPath: " + dianDexVariant.dexPath)
        println("=== Dex path: " + dexPath)
        println("=== Cache dex path: " + dexCacheFile)


        if (dianDexVariant.hasSnapShoot){
            if (dianDexVariant.getDiffResultList().size() > 0){
                File dexPathFile = new File(dexPath)
                if (dexPathFile.exists()){
                    dexPathFile.deleteDir()
                    dexPathFile.mkdirs()
                }

                String dx = DianDexUtils.getDxCmdPath(project)
                String cmd = dx + " --dex --output=" + dexPath + File.separator +
                        "classes.dex " + dianDexVariant.classPath
                println("=== Execute cmd: " + cmd)

                long beginTime = System.currentTimeMillis()
                Process process = cmd.execute()
                int status = process.waitFor()
                process.destroy()
                long endTime = System.currentTimeMillis()

                project.logger.error("*** Generate dex spend:" + (endTime-beginTime) + "ms")

                if (status == 0){
                    println("=== Generate patch.dex success.")
                }else{
                    println("=== Generate patch.dex failed.")
                }
//                增量
                for (File file : dexCacheFile.listFiles()){
                    String fileName = file.getName().split("\\.")[0]
                    if (fileName.endsWith("s")){
                        println("=== process: " + fileName + " skipped")
//                        FileUtils.copyFile(file,
//                                new File(dexPath + File.separator + "classes3.dex"))
                    }else{
                        String num = fileName.substring(fileName.size()-1)
                        int index = Integer.valueOf(num) + 1
                        FileUtils.copyFile(file,
                                new File(dexPath + File.separator + "classes" + index + ".dex"))
                        println("=== process: " + fileName + " rename to classes" + index)
                    }
                }
                FileUtils.copyFile(new File(dexPath + File.separator + "classes.dex"),
                        new File(dexPath + File.separator + "classes2.dex"))
                FileUtils.copyFile(new File(project.getProjectDir().absolutePath + File.separator + "hack.dex"),
                        new File(dexPath + File.separator + "classes.dex"))
                project.logger.error("=== Increment Package")
            }else{
//                不变
                File dexPathFile = new File(dexPath)
                if (dexPathFile.exists()){
                    dexPathFile.deleteDir()
                }
                FileUtils.copyDirectory(dexCacheFile, dexPathFile)
                project.logger.error("=== No Change Package")
            }
        }else{
//            全量
//            File file = new File(dexPath + File.separator + "classes.dex")
//            file.renameTo(new File(dexPath + File.separator + "classes2.dex"))
            FileUtils.copyFile(new File(dexPath + File.separator + "classes.dex"),
                    new File(dexPath + File.separator + "classes2.dex"))

            FileUtils.copyFile(new File(project.getProjectDir().absolutePath + File.separator + "hack.dex"),
                    new File(dexPath + File.separator + "classes.dex"))

            project.logger.error("=== Full Package")
        }
    }
}