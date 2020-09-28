package com.dian.plugin

import com.android.build.api.transform.Transform
import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.pipeline.TransformTask
import com.dian.extension.DianDexExtension
import com.dian.task.DianDexManifestTask
import com.dian.transform.DianDexTransform
import com.dian.util.ProjectSnapShoot
import com.dian.variant.DianDexVariant
import fastdex.common.utils.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.execution.TaskExecutionGraphListener

class MyPlugin implements Plugin<Project>{

    @Override
    void apply(Project project) {
        project.logger.error("*** apply my Plugin ***")
        project.extensions.create("dianDex", DianDexExtension)
        def androidConfig = project.extensions.android

        def myTask = project.tasks.create("TestDianTask", MyTask)
        def dianDexManifestTask = project.tasks.create("dianDexManifestTask", DianDexManifestTask)
        project.afterEvaluate {
            androidConfig.applicationVariants.each { ApplicationVariant variant ->
                project.logger.error("variant: " + variant.name)
                def variantOutput = variant.outputs.first()
                def variantName = variant.name.capitalize()


                if ("Debug" == variantName){
                    DianDexVariant dianDexVariant = new DianDexVariant(project, variant)
                    dianDexManifestTask.dianDexVariant = dianDexVariant
                    dianDexManifestTask.mustRunAfter variantOutput.getProcessManifestProvider().get()
                    variantOutput.getProcessResourcesProvider().get().dependsOn dianDexManifestTask
//                    dianDexVariant.prepareEnv()
                }

//                def dexBuilderDebugTask = project.tasks.findByName("mergeDexDebug")
//                def mergeExtDexDebugTask = project.tasks.findByName("packageDebug")
//                myTask.mustRunAfter dexBuilderDebugTask
//                mergeExtDexDebugTask.dependsOn myTask

//                project.getGradle().getTaskGraph().addTaskExecutionGraphListener(new TaskExecutionGraphListener() {
//                    @Override
//                    void graphPopulated(TaskExecutionGraph taskGraph) {
//                        println("*** in graphPopulated")
//                        for (Task task : taskGraph.getAllTasks()) {
//                            if (task instanceof  TransformTask){
//                                println("=== task: " + task.name)
//                            }
//                            if (task.getProject() == project
//                                    && task instanceof TransformTask
//                                    && task.name.startsWith("transform")
//                                    && task.name.endsWith("For" + variantName)) {
//                                Transform transform = ((TransformTask) task).getTransform()
//                                println("*** task: " + task.name + "transform: " + transform.getClass())
//                            }
//                        }
//                    }
//                })
            }
        }

//        AppExtension android = project.extensions.getByType(AppExtension)
//        android.registerTransform(new DianDexTransform(project))

//        project.afterEvaluate {
//            def assembleDebugTask = project.tasks.findByName("assembleDebug")
//            def packageDebugTask = project.tasks.findByName("packageDebug")
//            Task myTask = project.tasks.create("myTask", MyTask)
//            myTask.mustRunAfter packageDebugTask
//            assembleDebugTask.dependsOn myTask
//        }

//        project.getGradle().getTaskGraph().addTaskExecutionGraphListener(new TaskExecutionGraphListener() {
//            @Override
//            void graphPopulated(TaskExecutionGraph taskExecutionGraph) {
//                project.logger.error("==== in graphPopulated")
//            }
//        })
    }
}