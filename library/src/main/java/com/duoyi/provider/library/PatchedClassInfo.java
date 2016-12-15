package com.duoyi.provider.library;

/**
 * Created by duoyi on 2016/11/14.
 */

public class PatchedClassInfo {
    private String className;
    private String patchRedirectClassName;
    public PatchedClassInfo(String className,String patchRedirectClassName){
        this.className = className;
        this.patchRedirectClassName = patchRedirectClassName;
    }

    public String getClassName() {
        return className;
    }

    public String getPatchRedirectClassName() {
        return patchRedirectClassName;
    }

}
