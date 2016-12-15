package com.duoyi.provider.library;

/**
 * Created by duoyi on 2016/11/14.
 */

public interface ChangeQuickRedirect {
    boolean isSupport(String methodSignature);
    Object accessDispatch(String methodSignature, Object[] paramArrayOfObject) ;
}
