package com.duoyi.gradle

public class RobuExtension {
    HashSet<String> excludeClass = [ ]
    HashSet<String> excludePackage = []
    String robuDir
    String oldZip
    public RobuExtension(){
        robuDir = ""
    }
}