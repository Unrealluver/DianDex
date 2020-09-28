package com.dian.task

import com.dian.util.GradleUtils
import com.dian.variant.DianDexVariant
import groovy.xml.Namespace
import groovy.xml.QName
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class DianDexManifestTask extends DefaultTask{
    static final String DIAN_ORIGIN_APPLICATION_CLASSNAME = "DIAN_ORIGIN_APPLICATION_CLASSNAME"
    static final String DIAN_BOOT_ACTIVITY_CLASSNAME = "DIAN_BOOT_ACTIVITY_CLASSNAME"

    DianDexVariant dianDexVariant

    DianDexManifestTask(){
        group = 'dianDex'
    }

    @TaskAction
    def updateManifest(){
        project.logger.error("*** Print in DianDexManifestTask ***")
        def ns = new Namespace("http://schemas.android.com/apk/res/android", "android")
        def xml = GradleUtils.parseXml(dianDexVariant.manifestPath)
        def application = xml.application[0]
        if (application){
            println("=== Change Boot Application.")

            QName nameAttr = new QName("http://schemas.android.com/apk/res/android", 'name', 'android')
            def applicationName = application.attribute(nameAttr)
            if (applicationName == null || applicationName.isEmpty()) {
                applicationName = "android.app.Application"
            }else{
                println("applicationName = " + applicationName)
            }
            application.attributes().put(nameAttr, "dianDex.runtime.MyApplication")
            String bootActivityName = GradleUtils.getBootActivity(dianDexVariant.manifestPath)

            println("=== Get Boot ActivityName: " + bootActivityName)

            def metaDataTags = application['meta-data']

//            对原始Application的Name做记录
//            remove any old DIAN_ORIGIN_APPLICATION_CLASSNAME elements
            metaDataTags.findAll {
                (it.attributes()[ns.name] == DIAN_ORIGIN_APPLICATION_CLASSNAME)
            }.each {
                it.parent().remove(it)
            }
//            Add the new DIAN_ORIGIN_APPLICATION_CLASSNAME element
            application.appendNode('meta-data', [(ns.name): DIAN_ORIGIN_APPLICATION_CLASSNAME, (ns.value): applicationName])

//            对原始的启动Activity做记录
//            remove any old DIAN_BOOT_ACTIVITY_CLASSNAME elements
            metaDataTags.findAll {
                (it.attributes()[ns.name] == DIAN_BOOT_ACTIVITY_CLASSNAME)
            }.each {
                it.parent().remove(it)
            }
//            Add the new DIAN_BOOT_ACTIVITY_CLASSNAME element
            application.appendNode('meta-data', [(ns.name): DIAN_BOOT_ACTIVITY_CLASSNAME, (ns.value): bootActivityName])


//            写回文件
            def xmlPrinter = new XmlNodePrinter(new PrintWriter(dianDexVariant.manifestPath, "utf-8"))
            xmlPrinter.preserveWhitespace = true
            xmlPrinter.print(xml)
            println("=== Change Manifest file: " + dianDexVariant.manifestPath)
        }
    }
}