package com.dian.task

import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class DianDexIgnoreTask extends DefaultTask {
    ApplicationVariant androidVariant
    boolean proguardEnable
    boolean ignoreBuildType

    DianDexIgnoreTask(){
        group = "dianDex"
    }

    @TaskAction
    def instantRun() {
        String buildTypeName = androidVariant.getBuildType().buildType.getName()
        project.logger.error("--------------------dianDex--------------------")
        if (ignoreBuildType) {
            project.logger.error("onlyHookDebug = true, build-type = ${buildTypeName}, just ignore")
        } else if (proguardEnable) {
            project.logger.error("dianDex android.buildTypes.${buildTypeName}.minifyEnabled=true, just ignore")
        }
        project.logger.error("--------------------dianDex--------------------")
    }
}