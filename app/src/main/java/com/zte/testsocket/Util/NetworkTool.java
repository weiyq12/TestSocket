package com.zte.testsocket.Util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Created by Torick on 16/11/8.
 */

public class NetworkTool
{
    /**
     * 获取手机当前IP地址
     * @param context 上下文
     * @return IP地址
     */
    public static String getCurrIpAddress(Context context)
    {
        int networkType = getNetworkType(context);

        if (0 == networkType)
        {
            return "";
        }
        else if (1 == networkType)
        {
            return getWifiIpAdress(context);
        }
        else
        {
            return getMobileIpAddress(context);
        }
    }

    /** 获取当前网络类型
     * @return 0：没有网络 1：WIFI网络 2：WAP网络 3：NET网络
     * @author 10130567 weiyiqing
     * @since 2015.7.14
     */
    public static int getNetworkType(Context context)
    {
        int netType = 0;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null)
        {
            return netType;
        }
        int nType = networkInfo.getType();
        if (nType == ConnectivityManager.TYPE_MOBILE)
        {
            String extraInfo = networkInfo.getExtraInfo();
            if (!TextUtils.isEmpty(extraInfo))
            {
                if (extraInfo.toLowerCase().endsWith("cmnet"))
                {
                    netType = 3;
                }
                else
                {
                    netType = 2;
                }
            }
        }
        else if (nType == ConnectivityManager.TYPE_WIFI)
        {
            netType = 1;
        }

        return netType;
    }


    /**
     * 获取mobile的IP地址
     * @param context
     * @return IP地址字符串
     * @author 10130567 weiyiqing
     * @since 2014.8.30
     */
    public static String getMobileIpAddress(Context context)
    {
        try
        {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
            {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
                {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress())
                    {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * 获取wifi的IP地址
     * @param context
     * @return IP地址
     * @author 10130567 weiying
     * @since 2014.8.30
     */
    public static String getWifiIpAdress(Context context)
    {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (null == wifiManager || !wifiManager.isWifiEnabled())
        {
            return null;
        }
        else
        {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            String ip = intToIp(ipAddress);

            return ip;
        }
    }

    /**
     * wifi的IP转换成字符串地址
     * @param i wifi中获取的原始IP字符串
     * @return 格式化后IP地址
     * @author 10130567 weiyiqing
     * @since 2014.8.30
     */
    private static String intToIp(int i)
    {

        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + (i >> 24 & 0xFF);
    }
}
