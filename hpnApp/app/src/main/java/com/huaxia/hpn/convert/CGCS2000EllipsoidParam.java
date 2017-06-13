package com.huaxia.hpn.convert;

/**
 * Created by CUMT_BJX on 2017/4/3.
 * 为了存储CGCS2000的椭球参数，参考程鹏飞的论文《2000国家大地坐标系椭球参数与GRS80和WGS84的比较》
 *
 */

public class CGCS2000EllipsoidParam {
    public static final double a=6378137;
    public static  final double b=6356752.3141403558;
    public static  final double c=6399593.6258640232;
    /**
     * 扁率f
     */
    public static  final double f=0.0033528106811823;
    /**
     * 第一偏心率e
     */
    public static  final double e1_1=0.0818191910428158;
    /**
     * 第一偏心率e^2
     */
    public static  final double e1_2=0.0066943800229008;
    /**
     * 第二偏心率e'
     */
    public static final  double e2_1=0.0820944381519172;
    /**
     * 第二偏心率e'^2
     */
    public static  final double e2_2=0.006739496775479;

    //以下是计算
    /**
     * m0=a*(1-e^2)
     */
    public static  final double m0=6335439.3270838755601904;
    /**
     * m2=(3/2)e^2*m0
     */
    public static  final double m2=63617.757701295575721464741071212;
    /**
     * m4=(5/4)e^2*m2
     */
    public static  final double m4=532.35180782162077720511106726803;
    /**
     * m6=(7/6)e^2*m4
     */
    public static  final double m6=4.157726192008881308912345274636;
    /**
     * m8=(9/8)e^2*m6
     */
    public static  final double m8=0.0313125740555351296890528169186;

    //计算a值
    /**
     * a0=m0+(1/2)*m2+(3/8)*m4+(5/16)*m6+(35/128)*m8
     */

    public static  final double a0=6367449.1457139234269283803466441;

    /**
     * a2=(1/2)*m2+(1/2)*m4+(15/32)*m6+(7/16)*m8
     */

    public static  final double a2=32077.017387962251709067717691695;

    /**
     * a4=(1/8)*m4+(3/16)*m6+(7/32)*m8
     */

    public static  final double a4=67.330399264278910705679428451199;


    /**
     * a6=(1/32)*m6+(1/16)*m8
     */

    public static  final double a6=0.13188597937874848650907659088979;

    /**
     * a8=(1/128)*m8
     */

    public static  final double a8=2.4462948480886820069572513217656e-4;


}
