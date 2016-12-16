package com.duoyi.provider.library;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.util.Objects;

public class ReflectUtils {
    public static void Check(Object object){
        int a = 2;
        boolean b = true;
    }

    public static Object getStaticPrivateField(Class targetClass, String fieldName) {
        return getPrivateField(null, targetClass, fieldName);
    }

    public static void setStaticPrivateField(
            Object value, Class targetClass,  String fieldName) {
        setPrivateField(null, value, targetClass, fieldName);
    }

    public static Object getPrivateField(){
        return new Object();
    }

    public static Object getPrivateField(
            Object targetObject,
            Class targetClass,
            String fieldName) {
        try {
            Field declaredField = getField(targetClass, fieldName);
            return declaredField.get(targetObject);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setPrivateField(
            Object targetObject,
            Object value,
            Class targetClass,
            String fieldName) {

        try {
            Field declaredField = getField(targetClass, fieldName);
            declaredField.set(targetObject, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object invokeProtectedMethod(Object receiver,
                                               Object[] params,
                                               Class[] parameterTypes,
                                               String methodName) throws Throwable {
        try {
            Method toDispatchTo = getMethodByName(receiver.getClass(), methodName, parameterTypes);
            if (toDispatchTo == null) {
                throw new RuntimeException(new NoSuchMethodException(methodName));
            }
            toDispatchTo.setAccessible(true);
            return toDispatchTo.invoke(receiver, params);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object invokeProtectedStaticMethod(
            Object[] params,
            Class[] parameterTypes,
            String methodName,
            Class receiverClass) throws Throwable {
        try {
            Method toDispatchTo = getMethodByName(receiverClass, methodName, parameterTypes);
            if (toDispatchTo == null) {
                throw new RuntimeException(new NoSuchMethodException(
                        methodName + " in class " + receiverClass.getName()));
            }
            toDispatchTo.setAccessible(true);
            return toDispatchTo.invoke(null /* target */, params);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Field getField(Class target, String name) {
        Field declareField = getFieldByName(target, name);
        if (declareField == null) {
            throw new NoSuchElementException(name);
        }
        declareField.setAccessible(true);
        return declareField;
    }

    private static Field getFieldByName(Class<?> aClass, String name) {
        Class<?> currentClass = aClass;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                // ignored.
            }
            currentClass = currentClass.getSuperclass();
        }
        return null;
    }

    private static Method getMethodByName(Class<?> aClass, String name, Class[] paramTypes) {

        if (aClass == null) {
            return null;
        }

        Class<?> currentClass = aClass;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredMethod(name, paramTypes);
            } catch (NoSuchMethodException e) {
                // ignored.
            }
        }
        return null;
    }

}
