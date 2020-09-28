package com.dian.util

import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskState

class DianDexBuildListener implements BuildListener, TaskExecutionListener {

    private final Project project
    private long startMillis
    private times = []

    DianDexBuildListener(Project project){
        this.project = project ;
    }

    void beforeExecute(Task task) {
        startMillis = System.currentTimeMillis()
    }


    void afterExecute(Task task, TaskState taskState) {
        def ms = System.currentTimeMillis() - startMillis
        times.add([ms, task.path])
//        task.project.logger.warn "${task.path} took ${ms}ms"
    }

    @Override
    void buildFinished(BuildResult buildResult) {
        long totals = 0
        println "Task timings:"
        for (timing in times) {
            if (timing[0] >= 50) {
                printf "%7sms  %s\n", timing
            }
            totals += timing[0]
        }
        println("Total times: " + totals + "ms")
    }

    @Override
    void buildStarted(Gradle gradle) {

    }

    @Override
    void settingsEvaluated(Settings settings) {

    }

    @Override
    void projectsLoaded(Gradle gradle) {

    }

    @Override
    void projectsEvaluated(Gradle gradle) {

    }

    static void addByProject(Project pro){
        DianDexBuildListener listener = new DianDexBuildListener(pro) ;
        pro.gradle.addListener(listener) ;
    }
}