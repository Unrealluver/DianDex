package com.dian.task

import com.dian.util.DianDexUtils
import com.dian.variant.DianDexVariant
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class DianDexCleanTask extends DefaultTask {
    DianDexVariant dianDexVariant

    DianDexCleanTask(){
        group = 'dianDex'
    }

    @TaskAction
    def clean(){
        println("*** Print in DianDexCleanTask ***")
        if (dianDexVariant == null){
            DianDexUtils.cleanAllCache(project)
        }else{
            DianDexUtils.cleanCache(project, dianDexVariant.variantName)
        }
    }
}