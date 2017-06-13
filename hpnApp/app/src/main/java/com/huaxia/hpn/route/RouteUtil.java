package com.huaxia.hpn.route;

import com.esri.core.geometry.Point;
import com.mapbox.services.commons.models.Position;

/**
 * Created by liulx on 2017/6/9.
 */

public class RouteUtil {

    public static double getPointDistance(Position p1, Position p2)
    {
        return Math.sqrt((p1.getLatitude()-p2.getLatitude())*(p1.getLatitude()-p2.getLatitude())+(p1.getLongitude()-p2.getLongitude())*(p1.getLongitude()-p2.getLongitude()))*10000000;
    }

    public static double getNearestDistance(Position point, LineSegment line)
    {

//----------图2--------------------
        double a,b,c;
        a=getPointDistance(line.getP1(),point);
        if(a<=0.00001)
            return 0.0f;
        b=getPointDistance(line.getP2(),point);
        if(b<=0.00001)
            return 0.0f;
        c=getPointDistance(line.getP1(),line.getP2());
        if(c<=0.00001)
            return a;//如果PA和PB坐标相同，则退出函数，并返回距离
//------------------------------


        if(a*a>=b*b+c*c)//--------图3--------
            return b;
        if(b*b>=a*a+c*c)//--------图4-------
            return a;

//图1
        double l=(a+b+c)/2;     //周长的一半
        double s=Math.sqrt(l*(l-a)*(l-b)*(l-c));  //海伦公式求面积
        return 2*s/c;
    }
}
