package com.duoyi.provider.library;

import android.content.Context;

import java.io.File;
import java.util.List;
import java.util.NoSuchElementException;

import dalvik.system.DexClassLoader;

public class RobuFix {
    private static final String DEX_OPT_DIR = "robuFixOpt";


    public static void loadPatch(Context context,String patchFilePath){
        if (!new File(patchFilePath).exists()|| context == null) {
            return;
        }
        File dexOptDir = new File(context.getFilesDir(), DEX_OPT_DIR);
        dexOptDir.mkdir();
        DexClassLoader dexClassLoader = new DexClassLoader(patchFilePath,dexOptDir.getAbsolutePath(),null,context.getClassLoader());
        try {
            Class patchClassImpl = dexClassLoader.loadClass("com.duoyi.provider.robufix.PatchesInfoImpl");
            PatchesInfo patchesInfo = (PatchesInfo)patchClassImpl.newInstance();
            List<PatchedClassInfo> patchedClassInfos = patchesInfo.getPatchedClassesInfo();
            if (patchedClassInfos != null){
                for (PatchedClassInfo patchedClassInfo:patchedClassInfos){
                    Class oldClass = dexClassLoader.loadClass(patchedClassInfo.getClassName());
                    Class patchRedirectClass = dexClassLoader.loadClass(patchedClassInfo.getPatchRedirectClassName());
                    ChangeQuickRedirect changeQuickRedirect = (ChangeQuickRedirect)patchRedirectClass.newInstance();
                    ReflectUtils.setStaticPrivateField(changeQuickRedirect,oldClass,"changeQuickRedirect");
                }
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchElementException e){
            e.printStackTrace();
        } catch (VerifyError e){
            e.printStackTrace();
        }catch (RuntimeException e){
            e.printStackTrace();
        }

    }
}
