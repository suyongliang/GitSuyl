package com.huaxia.hpn.convert;

import android.util.Log;

import ips.casm.com.radiomap.MPoint;

import static android.content.ContentValues.TAG;

/**
 * 高斯投影反算
 * Created by CUMT_BJX on 2017/4/3.
 */

public class  ConvertGauss2Geodetic {
//    private MPoint mPoint=null;
    private double y1;
    private double B1;
    private double Bf;//计算垂足纬度
    private double tf;
    private double eta;
    private double nf;
    private double mf;
    private double B1f;
    private double B;
    private double deltal;//经差
    private double L;

    public ConvertGauss2Geodetic(){

    }
    /**
     * 将高斯投影平面的x和y进行转换，转为B和L
     * @param x
     * @param y
     * @return
     */
    public MPoint getBL(double x , double y){
        MPoint mPoint=new MPoint();
        /**
         * 首先计算y差值
         */
        y1=y-500000;
        /**
         *计算垂足纬度的初始值
         */
        Bf=x/ CGCS2000EllipsoidParam.a0;
        do{
            B1=Bf;
            //子午线弧长公式
            Bf=(x+(1.0/2.0)* CGCS2000EllipsoidParam.a2*Math.sin(2*B1)-(1.0/4.0)* CGCS2000EllipsoidParam.a4*Math.sin(4*B1)+(1.0/6.0)* CGCS2000EllipsoidParam.a6*Math.sin(6*B1))/ CGCS2000EllipsoidParam.a0;
        }while (Math.abs(B1-Bf)>0.000000001);
        tf=Math.tan(Bf);
        eta= CGCS2000EllipsoidParam.e2_1*Math.cos(Bf);
        nf= CGCS2000EllipsoidParam.a/Math.sqrt(1- CGCS2000EllipsoidParam.e1_2*Math.sin(Bf)*Math.sin(Bf));
        mf=nf/(1+ CGCS2000EllipsoidParam.e2_2*Math.cos(Bf)*Math.cos(Bf));
        B1f=Bf-tf*Math.pow(y1,2)/(2*mf*nf)+tf*(5+3*Math.pow(tf,3)+Math.pow(eta,2)-9*Math.pow(eta,2)*Math.pow(tf,2))*Math.pow(y1,4)/(24*mf*Math.pow(nf,3))
                -tf*(61+90*Math.pow(tf,2)+45*Math.pow(tf,4))*Math.pow(y1,6)/(720*mf*Math.pow(nf,5));
        deltal=y1/(nf*Math.cos(Bf))-(1+2*Math.pow(tf,2)+Math.pow(eta,2))*Math.pow(y1,3)/(6*Math.pow(nf,3)*Math.cos(Bf))
                +(5+28*Math.pow(tf,2)+24*Math.pow(tf,4)+6*Math.pow(eta,2)+8*Math.pow(eta,2)*Math.pow(tf,2))*Math.pow(y1,5)/(120*Math.pow(nf,5)*Math.cos(Bf));

        L=117+deltal;//度数
        B=B1f;//度数
        mPoint.x=L;//the x coordinate (longitude)
        mPoint.y=B;
        return mPoint;
    }

    /**
     * 将高斯投影平面的x和y进行转换，转为B和L
     * @param x
     * @param y
     * @return
     */
    public MPoint getLatLon(double x , double y){
        MPoint mPoint=new MPoint();
        y1=y-500000;
        /**
         *计算垂足纬度的初始值
         */
        Bf=x/ CGCS2000EllipsoidParam.a0;
        int count=0;
        do{
            count++;
            B1=Bf;
            //子午线弧长公式
            Bf=(x+(1.0/2.0)* CGCS2000EllipsoidParam.a2*Math.sin(2*B1)-(1.0/4.0)* CGCS2000EllipsoidParam.a4*Math.sin(4*B1)+(1.0/6.0)* CGCS2000EllipsoidParam.a6*Math.sin(6*B1))/ CGCS2000EllipsoidParam.a0;
        }while (Math.abs(B1-Bf)>0.000000001);
        Log.i(TAG,"计算次数为"+count+";纬度为"+Bf);
        tf=Math.tan(Bf);
        eta= CGCS2000EllipsoidParam.e2_1*Math.cos(Bf);
        nf= CGCS2000EllipsoidParam.a/Math.sqrt(1- CGCS2000EllipsoidParam.e1_2*Math.sin(Bf)*Math.sin(Bf));
        mf=nf/(1+ CGCS2000EllipsoidParam.e2_2*Math.cos(Bf)*Math.cos(Bf));
        B1f=Bf-tf*Math.pow(y1,2)/(2*mf*nf)+tf*(5+3*Math.pow(tf,3)+Math.pow(eta,2)-9*Math.pow(eta,2)*Math.pow(tf,2))*Math.pow(y1,4)/(24*mf*Math.pow(nf,3))
                -tf*(61+90*Math.pow(tf,2)+45*Math.pow(tf,4))*Math.pow(y1,6)/(720*mf*Math.pow(nf,5));
        deltal=y1/(nf*Math.cos(Bf))-(1+2*Math.pow(tf,2)+Math.pow(eta,2))*Math.pow(y1,3)/(6*Math.pow(nf,3)*Math.cos(Bf))
                +(5+28*Math.pow(tf,2)+24*Math.pow(tf,4)+6*Math.pow(eta,2)+8*Math.pow(eta,2)*Math.pow(tf,2))*Math.pow(y1,5)/(120*Math.pow(nf,5)*Math.cos(Bf));
        //2017-04-04经测试，反算的数据是弧度，需转换为度数
        Log.i(TAG,"纬度（弧度）是："+B1f+";经度（弧度）是："+deltal);
        L=117+deltal*180/Math.PI;//转为度数
        B=B1f*180/Math.PI;//转为度数
        Log.i(TAG,"纬度是："+B+";经度是："+L);
        mPoint.x=L;//the x coordinate (longitude)
        mPoint.y=B;
        return mPoint;
    }
}
