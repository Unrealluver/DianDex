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
import com.dian.util.GradleUtils
import com.dian.variant.DianDexVariant
import org.gradle.api.Project

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

class DianDexTransform extends Transform {
    Project project

    DianDexVariant dianDexVariant

    DianDexTransform(Project project){
        this.project = project
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        project.logger.error("*** Print in Transform ***")

        if (dianDexVariant.hasSnapShoot && dianDexVariant.getDiffResultList().size() == 0){
            println("=== No change & skip compile.")
            return
        }

//        File dexOutputDir = GradleUtils.getDexOutputDir(transformInvocation)
//        if (dexOutputDir == null){
//            println("dexDir null.")
//        }else{
//            println("dexOutputDir: " + dexOutputDir.absolutePath)
//        }

        //消费型输入，可以从中获取jar包和class文件夹路径。需要输出给下一个任务
        Collection<TransformInput> inputs = transformInvocation.getInputs()
        //OutputProvider管理输出路径，如果消费型输入为空，你会发现OutputProvider == null
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider()

        for(TransformInput input : inputs) {
            long beginTime = System.currentTimeMillis()
            for(JarInput jarInput : input.getJarInputs()) {
                File dest = outputProvider.getContentLocation(
                        jarInput.getFile().getAbsolutePath(),
                        jarInput.getContentTypes(),
                        jarInput.getScopes(),
                        Format.JAR)
//                println("=== JarInput: " + jarInput.getFile().getAbsolutePath() +
//                        " dest: " + dest.absolutePath)
                //将修改过的字节码copy到dest，就可以实现编译期间干预字节码的目的了
                FileUtils.copyFile(jarInput.getFile(), dest)
            }
            long endTime = System.currentTimeMillis()
            project.logger.error("*** Copy jar spend:" + (endTime-beginTime) + "ms")


//            println("=== Diff java path info: " + dianDexVariant.getDiffPathInfoSet())


            for(DirectoryInput directoryInput : input.getDirectoryInputs()) {
                File dest = outputProvider.getContentLocation(
                        directoryInput.getFile().getAbsolutePath(),
                        directoryInput.getContentTypes(),
                        directoryInput.getScopes(),
                        Format.DIRECTORY)
                println("=== DirectoryInput: " + directoryInput.getFile().getAbsolutePath() +
                        " dest: " + dest.absolutePath)
                dianDexVariant.classPath = dest.absolutePath
//                dianDexVariant.classPath = directoryInput.getFile().getAbsolutePath()
//                //将修改过的字节码copy到dest，就可以实现编译期间干预字节码的目的了
//                FileUtils.copyDirectory(directoryInput.getFile(), dest)

//                获取改变的class文件信息
                List<String> list = dianDexVariant.getDiffResultList(directoryInput.getFile().absolutePath)

                println("=== Change list: " + list)
//                删除未改变的class文件
                if (list.size() > 0){
                    Files.walkFileTree(directoryInput.getFile().toPath(), new SimpleFileVisitor<Path>(){
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            println("=== visit file: " + file.toString())
                            if (!list.contains(file.toString())){
                                println("### delete file: " + file.toString())
                                file.toFile().delete()
                            }
                            return FileVisitResult.CONTINUE
                        }
                    })
                }
//                注入代码
                long beginWalk = System.currentTimeMillis()
                Files.walkFileTree(directoryInput.getFile().toPath(), new SimpleFileVisitor<Path>(){
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        File classFile = file.toFile()
                        if (classFile.name != "BuildConfig.class" && classFile.name.endsWith("class")){
//                            byte[] classByte = fastdex.common.utils.FileUtils.readContents(classFile)
//                            classByte = ClassInject.inject(classByte)
//                            fastdex.common.utils.FileUtils.write2file(classByte, classFile)
                            byte[] code = ClassInject.inject(file.bytes)
                            FileOutputStream fos = new FileOutputStream(classFile)
                            fos.write(code)
                            fos.close()
                            println("=== inject file: " + classFile.absolutePath)
                        }
                        return FileVisitResult.CONTINUE
                    }
                })
                long endWalk = System.currentTimeMillis()
                project.logger.error("*** Walk file tree to inject spend:" + (endWalk-beginWalk) + "ms")

                //将修改过的字节码copy到dest，就可以实现编译期间干预字节码的目的了
                if (dest.exists()){
                    dest.deleteDir()
                }
                long begin = System.currentTimeMillis()
                FileUtils.copyDirectory(directoryInput.getFile(), dest)
                long end = System.currentTimeMillis()
                project.logger.error("*** Copy directory spend:" + (end-begin) + "ms")
            }
        }
    }

    @Override
    String getName() {
        return "DianDexTransform"
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