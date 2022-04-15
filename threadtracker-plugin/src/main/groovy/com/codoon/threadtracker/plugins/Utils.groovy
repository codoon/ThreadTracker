package com.codoon.threadtracker.plugins

class GrvUtils {
    static String extractVersion(String info) {
        // jetified-threadtracker-1.1.3-runtime_93e37613
        // threadtracker-1.1.3-runtime.jar
        def pattern = ~/[0-9]+.[0-9]+.[0-9]+/
        def matcher = info =~ pattern
        return matcher.size() == 0 ? "" : matcher[0]
    }

    static int compareVersion(String v1, String v2) {
        if (v1.equals(v2)) {
            return 0
        }

        String[] version1 = v1.split("-")
        String[] version2 = v2.split("-")
        String[] version1Array = version1[0].split("[._]")
        String[] version2Array = version2[0].split("[._]")

        String preRelease1 = new String()
        String preRelease2 = new String()
        if (version1.length > 1) {
            preRelease1 = version1[1]
        }
        if (version2.length > 1) {
            preRelease2 = version2[1]
        }

        int index = 0
        int minLen = Math.min(version1Array.length, version2Array.length)
        long diff = 0

        while (index < minLen
                && (diff = Long.parseLong(version1Array[index])
                - Long.parseLong(version2Array[index])) == 0) {
            index++
        }
        if (diff == 0) {
            for (int i = index; i < version1Array.length; i++) {
                if (Long.parseLong(version1Array[i]) > 0) {
                    return 1
                }
            }

            for (int i = index; i < version2Array.length; i++) {
                if (Long.parseLong(version2Array[i]) > 0) {
                    return -1
                }
            }
            //compare pre-release
            if (!preRelease1.isEmpty() && preRelease2.isEmpty()) {
                return -1
            } else if (preRelease1.isEmpty() && !preRelease2.isEmpty()) {
                return 1
            } else if (!preRelease1.isEmpty() && !preRelease2.isEmpty()) {
                int preReleaseDiff = preRelease1.compareTo(preRelease2);
                if (preReleaseDiff > 0) {
                    return 1
                } else if (preReleaseDiff < 0) {
                    return -1
                }
            }
            return 0
        } else {
            return diff > 0 ? 1 : -1
        }
    }
}