package com.huaxia.hpn.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @ClassName: ToastUtils
 * @author hx-suyl
 * @createddate 2017/3/9
 * @Description: 跟App相关的辅助类
 */
public class AppUtils
{

    private AppUtils()
    {
        /* cannot be instantiated */
        throw new UnsupportedOperationException("cannot be instantiated");

    }

    /**
     * 获取应用程序名称
     */
    public static String getAppName(Context context)
    {
        try
        {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getPackageName(), 0);
            int labelRes = packageInfo.applicationInfo.labelRes;
            return context.getResources().getString(labelRes);
        } catch (NameNotFoundException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * [获取应用程序版本名称信息]
     *
     * @param context
     * @return 当前应用的版本名称
     */
    public static String getVersionName(Context context)
    {
        try
        {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getPackageName(), 0);
            return packageInfo.versionName;

        } catch (NameNotFoundException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private static Properties urlProps;
    public static Properties getProperties(Context c){
        Properties props = new Properties();
        try {
            //方法一：通过activity中的context攻取setting.properties的FileInputStream
            InputStream in = c.getAssets().open("appConfig.properties");
            //方法二：通过class获取setting.properties的FileInputStream
            //InputStream in = PropertiesUtill.class.getResourceAsStream("/assets/  setting.properties "));
            props.load(in);
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        urlProps = props;

        System.out.println(urlProps.getProperty("serverUrl"));
        return urlProps;
    }

    public static boolean InputStreamToByte(InputStream is) {
        ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
        String SystemDirPath = Environment.getExternalStorageDirectory().toString();
        String IPSFolderPath = SystemDirPath + File.separator + "IndoorPositionSystem";
        String RadioMapPath = IPSFolderPath + File.separator + "RadioMap";
        String rmfilePath = RadioMapPath + File.separator + "Dice_Radio_Map.txt";
        try {
            FileOutputStream fos = new FileOutputStream(new File(rmfilePath));
            int len = 0;
            byte[] buffer = new byte[1024];
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
                fos.flush();
            }
            return true;
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return false;
        }
    }

}