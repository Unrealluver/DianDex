package com.dian.task

import com.android.utils.FileUtils
import com.dian.util.DianDexUtils
import com.dian.variant.DianDexVariant
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class DianDexCacheDexTask extends DefaultTask{
    DianDexVariant dianDexVariant

    @TaskAction
    void saveDexCache(){
        project.logger.error("*** Print in DianDexCacheDexTask ***")

        File dexCacheFile = DianDexUtils.getDexDir(project, dianDexVariant.variantName)

        String dexPath = null
        String normalOut = dianDexVariant.dexPath + File.separator + "out"
        String mergeProjectOut = dianDexVariant.dexPath + File.separator +
                "mergeProjectDexDebug" + File.separator + "out"
        if (new File(normalOut).exists()){
            dexPath = normalOut
        }else if (new File(mergeProjectOut).exists()){
            dexPath = mergeProjectOut
        }

        println("=== DianDexVariant.dexPath: " + dianDexVariant.dexPath)
        println("=== Dex path: " + dexPath)
        println("=== Cache dex path: " + dexCacheFile)

        if (dexCacheFile.exists()){
            dexCacheFile.deleteDir()
        }

        FileUtils.copyDirectory(new File(dexPath), dexCacheFile)
    }
}