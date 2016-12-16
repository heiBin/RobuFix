package com.duoyi.provider.library;


public class PatchProxy {
    public static boolean isSupport(String methodSignature,ChangeQuickRedirect changeQuickRedirect){
        return changeQuickRedirect.isSupport(methodSignature);
    }

    public  static Object accessDispatch(String methodSignature,Object[] paramArrayOfObject,ChangeQuickRedirect changeQuickRedirect){
        return changeQuickRedirect.accessDispatch(methodSignature,paramArrayOfObject);
    }

}
