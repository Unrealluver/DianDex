package com.dian.util

import com.android.Version
import com.android.build.api.transform.QualifiedContent.ContentType
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.pipeline.IntermediateFolderUtils
import com.android.build.gradle.internal.pipeline.TransformManager
import groovy.xml.QName
import org.gradle.api.Project
import org.xml.sax.SAXParseException
import java.lang.reflect.Field
import fastdex.common.utils.FileUtils

class GradleUtils {
    static String getBootActivity(String manifestPath) {
        def xml = parseXml(manifestPath)
        return getBootActivityByXmlNode(xml)
    }

    static String getBootActivityByXmlNode(Node xml) {
        def bootActivityName = ""
        def application = xml.application[0]

        if (application) {
            def activities = application.activity
            QName androidNameAttr = new QName("http://schemas.android.com/apk/res/android", 'name', 'android')

            try {
                activities.each { activity ->
                    def activityName = activity.attribute(androidNameAttr)

                    if (activityName) {
//                        println("activityName: " + activityName)
                        def intentFilters = activity."intent-filter"
                        if (intentFilters) {
                            intentFilters.each { intentFilter->
                                def actions = intentFilter.action
                                def categories = intentFilter.category
                                if (actions && categories) {
                                    //android.intent.action.MAIN
                                    //android.intent.category.LAUNCHER

                                    boolean hasMainAttr = false
                                    boolean hasLauncherAttr = false

                                    actions.each { action ->
                                        def attr = action.attribute(androidNameAttr)
                                        if ("android.intent.action.MAIN" == attr.toString()) {
                                            hasMainAttr = true
                                        }
                                    }

                                    categories.each { categoriy ->
                                        def attr = categoriy.attribute(androidNameAttr)
                                        if ("android.intent.category.LAUNCHER" == attr.toString()) {
                                            hasLauncherAttr = true
                                        }
                                    }

                                    if (hasMainAttr && hasLauncherAttr) {
                                        bootActivityName = activityName
                                        return bootActivityName
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {

            }
        }
        return bootActivityName
    }

//    获取dex输出目录
    static File getDexOutputDir(TransformInvocation transformInvocation) {
        if (getAndroidGradlePluginVersion() < "3.0") {
//            代码有问题
//            File location = com.android.utils.FileUtils.join(transformInvocation.getOutputProvider().rootLocation,
//                    IntermediateFolderUtils.FOLDERS,
//                    typesToString(TransformManager.CONTENT_DEX))
//            return location
            return null
        }else {
            Field folderUtilsField = transformInvocation.getOutputProvider().getClass().getDeclaredField("folderUtils")
            folderUtilsField.setAccessible(true)
            return folderUtilsField.get(transformInvocation.getOutputProvider()).getRootFolder()
        }
    }

    private static String typesToString(Set<ContentType> types) {
        int value = 0
        for (ContentType type : types) {
            value += type.getValue()
        }
        return String.format("%x", value)
    }

    static File getAptOutputDir(ApplicationVariant variant) {
//        该方法现不存在
        if (getAndroidGradlePluginVersion() >= "2.2") {
            //2.2.0以后才有getAnnotationProcessorOutputDir()这个api
            return variant.getVariantData().getScope().getAnnotationProcessorOutputDir()
        }else {
            return new File(variant.getVariantData().getScope().getGlobalScope().getGeneratedDir(), "/source/apt/${variant.dirName}")
        }
    }

    static String getPackageName(String manifestPath) {
        def xml = parseXml(manifestPath)
        String packageName = xml.attribute('package')
        return packageName
    }

    static BaseVariant getLibraryFirstVariant(Project project, String buildTypeName) {
        return project.android.libraryVariants.find { it.getBuildType().buildType.getName().equals(buildTypeName) }
    }

    static String getAndroidGradlePluginVersion() {
        return Version.ANDROID_GRADLE_PLUGIN_VERSION
    }

    static File getResOutputDir(Object processResources) {
        File resDir = null
        if (processResources.properties['resDir'] != null) {
//            TODO 旧方法不存在，替代方法待调研
//            resDir = processResources.resDir
        } else if (processResources.properties['inputResourcesDir'] != null) {
//            TODO 旧方法不存在，此替代方法得到结果为null，待调研
            resDir = processResources.getSourceOutputDir()
        }
        return resDir
    }

    static Object parseXml(String xmlPath) {
        byte[] bytes = FileUtils.readContents(new File(xmlPath))
        try {
            def xml = new XmlParser().parse(new InputStreamReader(new ByteArrayInputStream(bytes), "utf-8"))
            return xml
        } catch (SAXParseException e) {
//            String msg = e.getMessage()
//            //从eclipse转过来的项目可能会有这个问题
//            if (msg != null && msg.contains("Content is not allowed in prolog.")) {
//                BufferedReader r = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), "UTF8"))
//
//                ByteArrayOutputStream fos = new ByteArrayOutputStream()
//                Writer w = new BufferedWriter(new OutputStreamWriter(fos, "Cp1252"))
//                boolean firstLine = true
//                for (String s = ""; (s = r.readLine()) != null;) {
//                    if (firstLine) {
//                        s = removeUTF8BOM(s)
//                        firstLine = false
//                    }
//                    w.write(s + System.getProperty("line.separator"))
//                    w.flush()
//                }
//                def xml = new XmlParser().parse(new InputStreamReader(new ByteArrayInputStream(fos.toByteArray()), "utf-8"))
//                return xml
//            }
//            else {
//                throw new RuntimeException(e)
//            }
        }
    }

    static void addDynamicProperty(Project project, Object key, Object value) {
        Class defaultConventionClass = null
        try {
            defaultConventionClass = Class.forName("org.gradle.api.internal.plugins.DefaultConvention")
        } catch (ClassNotFoundException e) {

        }

        if (defaultConventionClass != null) {
            Field extensionsStorageField = null
            try {
                extensionsStorageField = defaultConventionClass.getDeclaredField("extensionsStorage")
                extensionsStorageField.setAccessible(true)
            } catch (Throwable e) {

            }

            if (extensionsStorageField != null) {
                try {
                    project.rootProject.allprojects.each {
                        Object extensionsStorage = extensionsStorageField.get(it.getAsDynamicObject().getConvention())
                        extensionsStorage.add(key,value)
                        it.getAsDynamicObject().getDynamicProperties().set(key,value)
                    }
                    project.gradle.startParameter.projectProperties.put(key,value)
                } catch (Throwable e) {

                }
            }
        }
    }
}