package com.codoon.threadtracker.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

class PluginUtils {

    // 用户代码包名列表统计
    private static HashSet<String> classPathSet = new HashSet<>();
    // project 名字
    private static ArrayList<String> projectList = new ArrayList<>();
    // project 绝对路径
    static ArrayList<String> projectPathList = new ArrayList<>();
    // apt等生成的java文件名
    private static HashSet<String> generatedJavaSet = new HashSet<>();

    static boolean inProjectList(String jarName) {
        int start = jarName.lastIndexOf(":");
        if (start != -1) {
            jarName = jarName.substring(start + 1);
        }
        return projectList.contains(jarName);
    }

    static void addProjectName(String project) {
        if (project != null && !project.isEmpty())
            projectList.add(project);
    }

    static void addProjectPathName(String projectPath) {
        if (projectPath != null && !projectPath.isEmpty())
            projectList.add(projectPath);
    }

    static void addClassPath(String classPath) {
        // System.out.println("#~   "+classPath+"   ~#");
        if (classPath.startsWith("java.") ||
                classPath.startsWith("android.") ||
                classPath.startsWith("androidx.") ||
                classPath.startsWith("dalvik.") ||
                classPath.startsWith("com.android.")
        ) {
            return;
        }

        String className = "";
        int lastIndex = classPath.lastIndexOf("/");

        // 去掉类名，只要路径(包名)
        if (lastIndex != -1) {
            className = classPath.substring(lastIndex + 1);
            classPath = classPath.substring(0, lastIndex);
        } else {
            className = classPath;
        }
        // 处理classname$123这种情况，变为classname，否则generatedJavaSet可能不包含
        int index$ = className.indexOf("$");
        if (index$ != -1) {
            className = className.substring(0, index$);
        }
        if (!generatedJavaSet.contains(className)) {
            classPathSet.add(classPath);
        }
    }

    // 包名去重，以较短的为准
    static List<String> getClassList() {
        ArrayList<String> finalList = new ArrayList<>();
        // 先尽量缩短包名路径
        for (String currPath : classPathSet) {
            if (finalList.isEmpty()) {
                finalList.add(currPath);
            } else {
                boolean needAdd = true;
                // finalList从后往前遍历，这样和currPath相似的概率比较大
                for (int i = finalList.size() - 1; i >= 0; i--) {
                    String existPath = finalList.get(i);
                    if (!currPath.equals(existPath)) {
                        if (existPath.contains(currPath)) {
                            finalList.set(i, currPath);
                            needAdd = false;
                            // 继续遍历，把finalList其他较长的包名都改为较短的currPath
                        } else if (currPath.contains(existPath)) {
                            // currPath较长，直接抛弃，没有继续遍历必要
                            needAdd = false;
                            break;
                        }
                    } else {
                        needAdd = false;
                        break;
                    }
                }
                if (needAdd) {
                    finalList.add(currPath);
                }
            }
        }

        // 然后去重
        HashSet<String> set = new HashSet<>(finalList);

        finalList.clear();
        finalList.addAll(set);

        for (int i = 0; i < finalList.size(); i++) {
            finalList.set(i, finalList.get(i).replace('/', '.'));
        }
        // for (int i = 0; i < finalList.size(); i++) {
        //     System.out.println("user package " + finalList.get(i));
        // }
        return finalList;
    }

    static void log(String str) {
        // System.out.println(str);
    }

    // 记录注解生成的类，统计高亮用户代码时将其去除
    static boolean dealAptFile() {
        for (String path : projectPathList) {
            path = path + File.separator + "build" + File.separator + "generated" + File.separator + "source" + File.separator;
            getFileList(path);
        }
        // for (String ignore : generatedJavaSet) {
        //     System.out.println("ignore "+ignore);
        // }
        return true;
    }

    private static void getFileList(String directory) {
        File f = new File(directory);
        if (!(f.exists() && f.isDirectory())) {
            // System.out.println(f.getPath()+" not exists");
            return;
        }
        File[] files = f.listFiles();
        if (files == null || files.length == 0) {
            // System.out.println(f.getPath()+" empty");
            return;
        }
        for (File file : files) {
            if (file.isFile()) {
                String fileName = file.getName();
                if (fileName.endsWith(".java")) {
                    // 去掉'.java'
                    generatedJavaSet.add(fileName.substring(0, fileName.length() - 5));
                }
            } else {
                getFileList(file.getAbsolutePath());
            }
        }
    }
}
