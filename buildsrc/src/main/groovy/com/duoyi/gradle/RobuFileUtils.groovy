package com.duoyi.gradle

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class RobuFileUtils {

    public static File createDirWithDelete(File dir, String folderName){
        File file  = new File(dir,folderName);
        file.deleteDir()
        file.mkdirs();
        return file;
    }

    public static File createFile(File dir, String folderName, String fileName){
        File file  = new File("${dir.absolutePath}/${folderName}/${fileName}");
        file.getParentFile().mkdirs();
        file.createNewFile();
        return file;
    }

    public static void zipFolder(File dirFile, String zipFilePath) throws Exception {
        // 创建Zip包
        ZipOutputStream outZip = new ZipOutputStream(new FileOutputStream(zipFilePath));
        for (File file : dirFile.listFiles()){
            // 压缩
            zipFiles(file.getParent() + File.separator, file.getName(), outZip);
        }
        // 完成,关闭
        outZip.finish();
        outZip.close();
    }

    private static void zipFiles(String folderPath, String filePath, ZipOutputStream zipOut) throws Exception {
        if (zipOut == null) {
            return;
        }
        File file = new File(folderPath + filePath);
        // 判断是不是文件
        if (file.isFile()) {
            ZipEntry zipEntry = new ZipEntry(filePath);
            FileInputStream inputStream = new FileInputStream(file);
            zipOut.putNextEntry(zipEntry);
            int len;
            byte[] buffer = new byte[100000];
            while ((len = inputStream.read(buffer)) != -1) {
                zipOut.write(buffer, 0, len);
            }
            inputStream.close();
            zipOut.closeEntry();
        } else {
            // 文件夹的方式,获取文件夹下的子文件
            String [] fileList = file.list();
            // 如果没有子文件, 则添加进去即可
            if (fileList.length <= 0) {
                ZipEntry zipEntry = new ZipEntry(filePath + java.io.File.separator);
                zipOut.putNextEntry(zipEntry);
                zipOut.closeEntry();
            }
            // 如果有子文件, 遍历子文件
            for (int i = 0; i < fileList.length; i++) {
                zipFiles(folderPath, filePath + java.io.File.separator + fileList[i], zipOut);
            }
        }
    }

    public static void unZip(String unZipfileName, String mDestPath) {
        if (unZipfileName != null && unZipfileName != "") {
            if (!mDestPath.endsWith("/")) {
                mDestPath = mDestPath + "/";
            }
            FileOutputStream fileOut = null;
            ZipInputStream zipIn = null;
            ZipEntry zipEntry = null;
            File file = null;
            int readedBytes = 0;
            byte[] buf = new byte[4096];
            try {
                zipIn = new ZipInputStream(new BufferedInputStream(new FileInputStream(unZipfileName)));
                while ((zipEntry = zipIn.getNextEntry()) != null) {
                    file = new File(mDestPath + zipEntry.getName());
                    if (zipEntry.isDirectory()) {
                        file.mkdirs();
                    } else {
                        // 如果指定文件的目录不存在,则创建之.
                        File parent = file.getParentFile();
                        if (!parent.exists()) {
                            parent.mkdirs();
                        }
                        fileOut = new FileOutputStream(file);
                        while ((readedBytes = zipIn.read(buf)) > 0) {
                            fileOut.write(buf, 0, readedBytes);
                        }
                        fileOut.close();
                    }
                    zipIn.closeEntry();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
