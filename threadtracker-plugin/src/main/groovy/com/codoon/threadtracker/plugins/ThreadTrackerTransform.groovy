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
    private final String VERSION = "1.1.0"

    @Override
    void apply(Project project) {
        System.out.println("hello ThreadTrackerPlugin:" + project.name)
        project.getRootProject().getSubprojects().each { subProject ->
            PluginUtils.addProjectName(subProject.name)
            PluginUtils.projectPathList.add(subProject.projectDir.toString())
             println "subProject path: $subProject.projectDir"
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

        println 'transform threads ...'
        JarInput threadtrackerJarInput = null

        inputs.each { TransformInput input ->

            input.directoryInputs.each { DirectoryInput directoryInput ->
                handleDirectoryInput(directoryInput, outputProvider)
            }

            input.jarInputs.each { JarInput jarInput ->
                // com.codoon.threadtracker:threadtracker:1.0.0
                if (jarInput.file.getAbsolutePath().endsWith(".jar") && jarInput.name.startsWith("com.codoon.threadtracker:threadtracker")) {
                    int colonIndex = jarInput.name.lastIndexOf(":")
                    String version = jarInput.name.substring(colonIndex + 1)
                    if (version != VERSION) {
                        throw new RuntimeException("version mismatching: please use com.codoon.threadtracker:threadtracker:" + VERSION)
                    }
                    threadtrackerJarInput = jarInput
                } else if (handleJarInputs(jarInput, outputProvider, false)) {
                    threadtrackerJarInput = jarInput
                }
            }
        }

        // 最后处理threadtracker以便向UserPackage.java添加所有用户包名
        if (threadtrackerJarInput != null) {
            println 'build user package list...'
            handleJarInputs(threadtrackerJarInput, outputProvider, true)
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
                    ClassVisitor cv = new ThreadTrackerClassVisitor(classWriter, true)
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

    // return true if the jarInput is threadtracker.jar
    static boolean handleJarInputs(JarInput jarInput, TransformOutputProvider outputProvider,  boolean handleThreadTrackerJar) {
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
            boolean isThreadTrackerJar = false;
            boolean isUserCode = false;
            if (jarInput.getScopes().contains(QualifiedContent.Scope.PROJECT)
                    || jarInput.getScopes().contains(QualifiedContent.Scope.SUB_PROJECTS)) {
                isUserCode = true;
            } else if (jarInput.file.absolutePath.contains("/transforms/ajx") && jarInput.name.equals("include")) {
                // if aspectjx plugin is applied before this plugin
                isUserCode = true;
            }
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement()
                String entryName = jarEntry.getName()
                ZipEntry zipEntry = new ZipEntry(entryName)
                InputStream inputStream = jarFile.getInputStream(jarEntry)
                if (!handleThreadTrackerJar && entryName.contains("com/codoon/threadtracker/")) {
                    isThreadTrackerJar = true
                    println 'entryName:' + entryName + "," + jarInput.file.absolutePath
                    break
                }
                // println '----------- jarClass <' + entryName + '> -----------'
                if (checkClassFile(entryName, true)) {
                    jarOutputStream.putNextEntry(zipEntry)
                    ClassReader classReader = new ClassReader(IOUtils.toByteArray(inputStream))
                    ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                    ClassVisitor cv = new ThreadTrackerClassVisitor(classWriter, isUserCode)
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
            if (isThreadTrackerJar && !handleThreadTrackerJar) {
                return isThreadTrackerJar
            }
            def dest = outputProvider.getContentLocation(jarName + md5Name,
                    jarInput.contentTypes, jarInput.scopes, Format.JAR)
            FileUtils.copyFile(tmpFile, dest)
            tmpFile.delete()
            return false
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
