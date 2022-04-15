package com.codoon.threadtracker.plugins

import com.android.annotations.NonNull
import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

import static org.objectweb.asm.ClassReader.EXPAND_FRAMES

class ThreadTrackerTransform extends Transform implements Plugin<Project> {
    private final String VERSION = "1.2.0"
    private final String LIB = "threadtracker"

    private Project project;

    @Override
    void apply(Project project) {
        this.project = project;

        System.out.println("hello ThreadTrackerPlugin")
        project.getRootProject().getSubprojects().each { subProject ->
            PluginUtils.addProjectName(subProject.name)
            PluginUtils.projectPathList.add(subProject.projectDir.toString())
            // println "subProject path: $subProject.projectDir"
        }

        def android = project.extensions.getByType(AppExtension)
        android.registerTransform(this)
    }

    @Override
    String getName() {
        return "ThreadTrackerTransform"
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

    @Override
    void transform(@NonNull TransformInvocation transformInvocation) {
        println '--------------- ThreadTrackerTransform start --------------- '
        def startTime = System.currentTimeMillis()

        // 记录注解生成的java文件
        println 'deal apt file...'
        PluginUtils.dealAptFile()

        Collection<TransformInput> inputs = transformInvocation.inputs
        TransformOutputProvider outputProvider = transformInvocation.outputProvider

        if (outputProvider != null)
            outputProvider.deleteAll()

        println 'transform threads...'
        JarInput threadtrackerJarInput = null

        println("inputs size : ${inputs.size()}")
        inputs.each { TransformInput input ->

            input.directoryInputs.each { DirectoryInput directoryInput ->
                handleDirectoryInput(directoryInput, outputProvider)
            }

            input.jarInputs.each { JarInput jarInput ->

                def absolutePath = jarInput.file.getAbsolutePath()
//                println('>>> found threadtracker jar file: ' + jarInput.name + " ||  "+ absolutePath)


                if (absolutePath.contains(LIB) || jarInput.name.contains(LIB)) {
                    if (GrvUtils.compareVersion(project.gradle.gradleVersion, "4.0.0") < 0) {
                        // com.codoon.threadtracker:threadtracker:1.0.0
                        if (absolutePath.endsWith(".jar") && jarInput.name.startsWith("com.codoon.threadtracker:threadtracker")) {
                            int colonIndex = jarInput.name.lastIndexOf(":")
                            String version = jarInput.name.substring(colonIndex + 1)
                            if (version != VERSION) {
                                throw new RuntimeException("version mismatching: please use com.codoon.threadtracker:threadtracker:" + VERSION)
                            }
                            threadtrackerJarInput = jarInput
                        } else {
                            handleJarInputs(jarInput, outputProvider)
                        }
                    } else {
                        // 4.0
                        // >>> found threadtracker jar file: 18c1eae82c306df20e6e041e7687463877d700b8 ||
                        // transforms-2/files-2.1/8c26f65926dc52e4992a4618f046b585/threadtracker-1.1.3-runtime.jar

                        // 4.2
                        // Already transformed jar :
                        //  jar file: jetified-threadtracker-1.1.3-runtime_93e37613 ||
                        //  build/intermediates/transforms/WMRouter/junoUat/debug/1136.jar

                        def info = LIB
                        if (jarInput.name.contains("threadtracker")) {
                            info = jarInput.name;
                        } else {
                            def fullPath = absolutePath
                            def pos = fullPath.lastIndexOf("/")
                            if (pos >= 0) {
                                def last = fullPath.substring(pos + 1)
                                if (last.contains("threadtracker")) {
                                    info = last
                                }
                            }
                        }

                        String version = GrvUtils.extractVersion(info)
                        if (!version.isEmpty()) {
                            if (version != VERSION) {
                                throw new RuntimeException("version mismatching: please use com.codoon.threadtracker:threadtracker:" + VERSION)
                            }
                        }
                        println("applied version $version")

                        threadtrackerJarInput = jarInput
                    }
                } else {
                    handleJarInputs(jarInput, outputProvider)
                }

            }
        }

        // 最后处理threadtracker以便向UserPackage.java添加所有用户包名
        if (threadtrackerJarInput != null) {
            println 'build user package list...'
            handleJarInputs(threadtrackerJarInput, outputProvider)
        } else {
            println 'error: threadtracker-transform failed'
            throw new RuntimeException("can't find threadtracker.jar: please implementation 'com.codoon.threadtracker:threadtracker:" + VERSION + "' in your application gradle file")
        }

        def cost = (System.currentTimeMillis() - startTime) / 1000
        println "ThreadTrackerTransform cost ： $cost s"
        println '--------------- ThreadTrackerTransform end --------------- '
    }

    static void handleDirectoryInput(DirectoryInput directoryInput, TransformOutputProvider outputProvider) {
        if (directoryInput.file.isDirectory()) {
            // 遍历目录所有文件（包含子文件）
            directoryInput.file.eachFileRecurse { File file ->
                def name = file.name
                if (checkClassFile(name, false)) {
                    // println '----------- class <' + name + '> -----------'
                    ClassReader classReader = new ClassReader(file.bytes)
                    ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                    ClassVisitor cv = new ThreadTrackerClassVisitor(classWriter, null)
                    classReader.accept(cv, EXPAND_FRAMES)
                    byte[] code = classWriter.toByteArray()
                    FileOutputStream fos = new FileOutputStream(
                            file.parentFile.absolutePath + File.separator + name)
                    fos.write(code)
                    fos.close()
                }
            }
        }

        // 固定写法 把输出给下一个任务
        def dest = outputProvider.getContentLocation(directoryInput.name,
                directoryInput.contentTypes, directoryInput.scopes,
                Format.DIRECTORY)
        FileUtils.copyDirectory(directoryInput.file, dest)
    }

    static void handleJarInputs(JarInput jarInput, TransformOutputProvider outputProvider) {
        if (jarInput.file.getAbsolutePath().endsWith(".jar")) {
            def jarName = jarInput.name
            // println '----------- jarName <' + jarName + '> -----------'
            def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
            if (jarName.endsWith(".jar")) {
                jarName = jarName.substring(0, jarName.length() - 4)
            }
            JarFile jarFile = new JarFile(jarInput.file)
            Enumeration enumeration = jarFile.entries()
            File tmpFile = new File(jarInput.file.getParent() + File.separator + "classes_temp.jar")
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpFile))

            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement()
                String entryName = jarEntry.getName()
                ZipEntry zipEntry = new ZipEntry(entryName)
                InputStream inputStream = jarFile.getInputStream(jarEntry)
                // println '----------- jarClass <' + entryName + '> -----------'
                if (checkClassFile(entryName, true)) {
                    jarOutputStream.putNextEntry(zipEntry)
                    ClassReader classReader = new ClassReader(IOUtils.toByteArray(inputStream))
                    ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                    ClassVisitor cv = new ThreadTrackerClassVisitor(classWriter, jarName)
                    classReader.accept(cv, EXPAND_FRAMES)
                    byte[] code = classWriter.toByteArray()
                    jarOutputStream.write(code)
                } else {
                    jarOutputStream.putNextEntry(zipEntry)
                    jarOutputStream.write(IOUtils.toByteArray(inputStream))
                }
                jarOutputStream.closeEntry()
            }
            jarOutputStream.close()
            jarFile.close()
            def dest = outputProvider.getContentLocation(jarName + md5Name,
                    jarInput.contentTypes, jarInput.scopes, Format.JAR)
            FileUtils.copyFile(tmpFile, dest)
            tmpFile.delete()
        }
    }


    /**
     * 过滤class文件名
     */
    static boolean checkClassFile(String name, boolean isJar) {
        if (isJar) {
            int lastIndex = name.lastIndexOf('/')
            if (lastIndex != -1) {
                name = name.substring(lastIndex + 1, name.length())
            }
            // 只要/后面的文件名，以便后续调用startsWith判断
        }
        return (name.endsWith(".class") && !name.startsWith("R\$")
                && name != "R.class" && !name.startsWith("BR\$")
                && name != "BR.class" && name != "BuildConfig.class")
    }
}
