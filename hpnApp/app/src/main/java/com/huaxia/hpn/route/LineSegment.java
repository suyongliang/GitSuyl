package com.huaxia.hpn.route;

import com.esri.core.geometry.Point;
import com.mapbox.services.commons.models.Position;

/**
 * Created by liulx on 2017/6/9.
 */

public class LineSegment {
    private Position p1;
    private Position p2;
    private double lenth;

    public LineSegment(Position p1,Position p2){
        this.p1=p1;
        this.p2=p2;
        this.lenth =RouteUtil.getPointDistance(p1,p2);
    }
    public Position getP1() {
        return p1;
    }

    public void setP1(Position p1) {
        this.p1 = p1;
    }

    public Position getP2() {
        return p2;
    }

    public void setP2(Position p2) {
        this.p2 = p2;
    }

    public double getLenth() {
        return lenth;
    }

    public void setLenth(double lenth) {
        this.lenth = lenth;
    }
}
