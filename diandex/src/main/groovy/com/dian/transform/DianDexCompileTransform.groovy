package com.dian.transform

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.dian.util.ClassInject
import com.dian.util.DianDexCompileUtils
import com.dian.variant.DianDexVariant
import fastdex.build.lib.snapshoot.sourceset.PathInfo
import org.gradle.api.Project

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

class DianDexCompileTransform extends Transform {
    Project project

    DianDexVariant dianDexVariant

    DianDexCompileTransform(Project project){
        this.project = project
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        project.logger.error("*** Print in DianDexCompileTransform ***")

        if (dianDexVariant.hasSnapShoot && dianDexVariant.getDiffResultList().size() == 0){
            println("=== No change & skip compile.")
            return
        }

        Collection<TransformInput> inputs = transformInvocation.getInputs()
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider()

        File classSource = null
        File classDest = null

        Set<File> jarSet = new HashSet<>()

        long beginTime = System.currentTimeMillis()
        for(TransformInput input : inputs) {
            for(JarInput jarInput : input.getJarInputs()) {
                File dest = outputProvider.getContentLocation(
                        jarInput.getFile().getAbsolutePath(),
                        jarInput.getContentTypes(),
                        jarInput.getScopes(),
                        Format.JAR)
//                收集依赖包
                jarSet.add(jarInput.getFile())
                FileUtils.copyFile(jarInput.getFile(), dest)
            }
            for(DirectoryInput directoryInput : input.getDirectoryInputs()) {
                File dest = outputProvider.getContentLocation(
                        directoryInput.getFile().getAbsolutePath(),
                        directoryInput.getContentTypes(),
                        directoryInput.getScopes(),
                        Format.DIRECTORY)
//                获取编译输出目录
                classSource = directoryInput.getFile()
                classDest = dest
                dianDexVariant.classPath = dest.absolutePath
//                全量编译下直接复制目录
                if (!dianDexVariant.hasSnapShoot){
                    FileUtils.copyDirectory(directoryInput.getFile(), dest)
                }
            }
        }
        long endTime = System.currentTimeMillis()
        project.logger.error("*** Copy jar & Directory spend: " + (endTime-beginTime) + "ms")


        println("=== ClassDestPath: " + classDest.absolutePath)

//        增量编译下
        long begin = System.currentTimeMillis()
        if (dianDexVariant.hasSnapShoot && dianDexVariant.getDiffResultList().size() > 0){
            Set<PathInfo> diffJavaSet = dianDexVariant.getDiffPathInfoSet()
            println("=== DiffJavaSet: " + diffJavaSet)

            DianDexCompileUtils.compileJava(project, diffJavaSet, jarSet, classDest, dianDexVariant.javaCompile)
        }
        long end = System.currentTimeMillis()
        project.logger.error("*** Compile java spend: " + (end-begin) + "ms")

        long beginInject = System.currentTimeMillis()
        if (classDest.exists()){
            println("=== Transform result exist & begin inject.")
//            插桩注入
            Files.walkFileTree(classDest.toPath(), new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    File classFile = file.toFile()
                    if (classFile.name != "BuildConfig.class" && classFile.name.endsWith("class")){
                        byte[] code = ClassInject.inject(file.bytes)
                        FileOutputStream fos = new FileOutputStream(classFile)
                        fos.write(code)
                        fos.close()
//                        println("=== inject file: " + classFile.absolutePath)
                    }
                    return FileVisitResult.CONTINUE
                }
            })
        }
        long endInject = System.currentTimeMillis()
        project.logger.error("*** Inject spend: " + (endInject-beginInject) + "ms")

        long beginJar = System.currentTimeMillis()
        DianDexCompileUtils.generateJar(project, classSource, classDest)
        long endJar = System.currentTimeMillis()
        project.logger.error("*** Generate jar spend: " + (endJar-beginJar) + "ms")
    }

    @Override
    String getName() {
        return "DianDexCompileTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }
}