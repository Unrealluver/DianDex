package com.dian.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.dian.extension.DianDexExtension
import com.dian.task.DianDexCacheDexTask
import com.dian.task.DianDexManifestTask
import com.dian.task.DianDexPatchTask
import com.dian.task.DianDexPreCompileTask
import com.dian.transform.DianDexCompileTransform
import com.dian.transform.DianDexTransform
import com.dian.util.DianDexBuildListener
import com.dian.variant.DianDexVariant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class TestPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.logger.error("*** TestPlugin & Dex Increment ***")
        project.extensions.create("dianDex", DianDexExtension)
        def androidConfig = project.extensions.android

//        DianDexBuildListener.addByProject(project)

        AppExtension android = project.extensions.getByType(AppExtension)
//        DianDexTransform dianDexTransform = new DianDexTransform(project)
//        android.registerTransform(dianDexTransform)
        DianDexCompileTransform dianDexCompileTransform = new DianDexCompileTransform(project)
        android.registerTransform(dianDexCompileTransform)

        def dianDexManifestTask = project.tasks.create("dianDexManifestTask", DianDexManifestTask)
        def dianDexCacheDexTask = project.tasks.create("dianDexCacheDexTask", DianDexCacheDexTask)
        def dianDexPatchTask = project.tasks.create("dianDexPatchTask", DianDexPatchTask)
        def dianDexPreCompileTask = project.tasks.create("dianDexPreCompileTask", DianDexPreCompileTask)

        project.afterEvaluate {
            androidConfig.applicationVariants.each { ApplicationVariant variant ->
                def variantOutput = variant.outputs.first()
                def variantName = variant.name.capitalize()
                if ("Debug" == variantName){
                    DianDexVariant dianDexVariant = new DianDexVariant(project, variant)
                    dianDexVariant.prepareEnv()

//                    dianDexTransform.dianDexVariant = dianDexVariant
                    dianDexCompileTransform.dianDexVariant = dianDexVariant

                    dianDexManifestTask.dianDexVariant = dianDexVariant
                    dianDexManifestTask.mustRunAfter variantOutput.getProcessManifestProvider().get()
                    variantOutput.getProcessResourcesProvider().get().dependsOn dianDexManifestTask

                    dianDexPreCompileTask.dianDexVariant = dianDexVariant
                    dianDexPreCompileTask.mustRunAfter project.tasks.findByName("processDebugResources")
                    project.tasks.findByName("compileDebugJavaWithJavac").dependsOn dianDexPreCompileTask

                    Task dexTask
                    dianDexPatchTask.dianDexVariant = dianDexVariant
                    if ((dexTask = project.tasks.findByName("mergeProjectDexDebug")) != null){
                        dianDexPatchTask.mustRunAfter dexTask
                    }else if ((dexTask = project.tasks.findByName("mergeDexDebug")) != null){
                        dianDexPatchTask.mustRunAfter dexTask
                    }else if ((dexTask = project.tasks.findByName("dexBuilderDebug")) != null){
                        dianDexPatchTask.mustRunAfter dexTask
                    }
                    project.tasks.findByName("packageDebug").dependsOn dianDexPatchTask

                    dianDexCacheDexTask.dianDexVariant = dianDexVariant
                    dianDexCacheDexTask.mustRunAfter project.tasks.findByName("packageDebug")
                    project.tasks.findByName("assembleDebug").dependsOn dianDexCacheDexTask

                    if (dianDexVariant.hasSnapShoot){
                        project.tasks.findByName("dexBuilderDebug").enabled = false
                        project.tasks.findByName("mergeExtDexDebug").enabled = false
                        project.tasks.findByName("compileDebugJavaWithJavac").enabled = false
                        if ((dexTask = project.tasks.findByName("mergeDexDebug")) != null){
                            dexTask.enabled = false
                        }
                    }
                }
            }
        }
    }
}