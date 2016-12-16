package com.duoyi.provider.library;

public interface ChangeQuickRedirect {
    boolean isSupport(String methodSignature);
    Object accessDispatch(String methodSignature, Object[] paramArrayOfObject) ;
}
