package com.duoyi.provider.library;


import java.util.Objects;

/**
 * Created by duoyi on 2016/11/14.
 */

public class PatchProxy {
    public static boolean isSupport(String methodSignature,ChangeQuickRedirect changeQuickRedirect){
        return changeQuickRedirect.isSupport(methodSignature);
    }

    public  static Object accessDispatch(String methodSignature,Object[] paramArrayOfObject,ChangeQuickRedirect changeQuickRedirect){
        return changeQuickRedirect.accessDispatch(methodSignature,paramArrayOfObject);
    }

    public  static Object accessDispatch(String methodSignature,ChangeQuickRedirect changeQuickRedirect){
        return new Object();
    }

}
