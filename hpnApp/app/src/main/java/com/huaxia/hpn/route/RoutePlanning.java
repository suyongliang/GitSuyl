package com.huaxia.hpn.route;

import android.text.TextUtils;
import android.util.Log;

import com.mapbox.services.commons.models.Position;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by liulx on 2017/6/9.
 */

public class RoutePlanning {
    static List<LineSegment> lineSegments = null;
    static HashMap<String,Position> namePositionMap = new HashMap<String,Position>();
    public static void init(String geojosnString){
        try{
        if(lineSegments==null){
            lineSegments = new ArrayList<LineSegment>();
            // Parse JSON
            JSONObject json = new JSONObject(geojosnString);
            JSONArray features = json.getJSONArray("features");
            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject geometry = feature.getJSONObject("geometry");
                if (geometry != null) {
                    String type = geometry.getString("type");

                    // Our GeoJSON only has one feature: a line string
                    if (!TextUtils.isEmpty(type) && type.equalsIgnoreCase("LineString")) {
                        // Get the Coordinates
                        JSONArray coords = geometry.getJSONArray("coordinates");
                        JSONArray coord1 = coords.getJSONArray(0);
                        JSONArray coord2 = coords.getJSONArray(1);
                        Position position1 = Position.fromCoordinates(coord1.getDouble(0), coord1.getDouble(1));
                        Position position2 = Position.fromCoordinates(coord2.getDouble(0), coord2.getDouble(1));
                        LineSegment lineSegment = new LineSegment(position1,position2);
                        lineSegments.add(lineSegment);

                        // 构建有向图
                        namePositionMap.put(position1.toString(),position1);
                        namePositionMap.put(position2.toString(),position2);

                        DijkstraUtil.Vertex v1 = DijkstraUtil.Vertex.getInstance(position1.toString());//建点
                        List<DijkstraUtil.Edge> v1Es = v1.edges;//建边

                        DijkstraUtil.Vertex v2 = DijkstraUtil.Vertex.getInstance(position2.toString());//建点
                        List<DijkstraUtil.Edge> v2Es = v2.edges;//建边

                        boolean exist = false;
                        for(DijkstraUtil.Edge edge :v1Es ){
                            if(StringUtils.equals(edge.dest.name,v2.name)){
                                exist = true;
                                break;
                            }
                        }
                        if(!exist){
                            DijkstraUtil.Edge edge = new DijkstraUtil.Edge(v2, lineSegment.getLenth());//赋值边
                            v1Es.add(edge);
                        }
                        exist = false;
                        for(DijkstraUtil.Edge edge :v2Es ){
                            if(StringUtils.equals(edge.dest.name,v1.name)){
                                exist = true;
                                break;
                            }
                        }
                        if(!exist){
                            DijkstraUtil.Edge edge = new DijkstraUtil.Edge(v1, lineSegment.getLenth());//赋值边
                            v2Es.add(edge);
                        }
                    }
                }
            }
        }
        }catch (Exception e){
            e.printStackTrace();
        }

    };

    public static LineSegment getNearestLine(Position position){
        if(lineSegments==null){
            return null;
        }
        double nearestStep = Double.MAX_VALUE;
        HashMap<String, LineSegment> distanceLineMap = new HashMap<String, LineSegment>();
        for(LineSegment lineSegment:lineSegments){
            double dis = RouteUtil.getNearestDistance(position,lineSegment);
            distanceLineMap.put(String.valueOf(dis),lineSegment);
            if(dis<nearestStep){
                nearestStep = dis;
            }
        }
        return distanceLineMap.get(String.valueOf(nearestStep));
    }

    public static Position getNearestPosition(Position position){
        LineSegment lineSegment = getNearestLine(position);
        Position p1 = lineSegment.getP1();
        Position p2 = lineSegment.getP2();
        Position pp = DijkstraUtil.getProjectivePoint(p1,p2,position);

        //增加一个顶点，两条线段、并建图
        namePositionMap.put(pp.toString(),pp);
        LineSegment lineSegment1 = new LineSegment(p1,pp);
        LineSegment lineSegment2 = new LineSegment(p2,pp);
        DijkstraUtil.Vertex pv = DijkstraUtil.Vertex.getInstance(pp.toString());//建点
        DijkstraUtil.Vertex pv1 = DijkstraUtil.Vertex.getInstance(p1.toString());//获取p1顶点
        DijkstraUtil.Vertex pv2 = DijkstraUtil.Vertex.getInstance(p2.toString());//获取p2顶点
        List<DijkstraUtil.Edge> pvEs = pv.edges;//建边
        boolean exist = false;
        for(DijkstraUtil.Edge edge :pvEs ){
            if(StringUtils.equals(edge.dest.name,pv1.name)){
                exist = true;
                break;
            }
        }
        if(!exist){
            DijkstraUtil.Edge edge = new DijkstraUtil.Edge(pv1, lineSegment1.getLenth());//赋值边
            pvEs.add(edge);
        }
        exist = false;
        for(DijkstraUtil.Edge edge :pvEs ){
            if(StringUtils.equals(edge.dest.name,pv2.name)){
                exist = true;
                break;
            }
        }
        if(!exist){
            DijkstraUtil.Edge edge = new DijkstraUtil.Edge(pv2, lineSegment2.getLenth());//赋值边
            pvEs.add(edge);
        }

        List<DijkstraUtil.Edge> pv1Es = pv1.edges;//建边
        exist = false;
        for(DijkstraUtil.Edge edge :pv1Es ){
            if(StringUtils.equals(edge.dest.name,pv1.name)){
                exist = true;
                break;
            }
        }
        if(!exist){
            DijkstraUtil.Edge pv1pv = new DijkstraUtil.Edge(pv, lineSegment1.getLenth());//赋值边
            pv1Es.add(pv1pv);
        }

        List<DijkstraUtil.Edge> pv2Es = pv2.edges;//建边
        exist = false;
        for(DijkstraUtil.Edge edge :pv2Es ){
            if(StringUtils.equals(edge.dest.name,pv2.name)){
                exist = true;
                break;
            }
        }
        if(!exist){
            DijkstraUtil.Edge pv2pv = new DijkstraUtil.Edge(pv, lineSegment2.getLenth());//赋值边
            pv2Es.add(pv2pv);
        };
        return pp;
    }

    public static List<Position> getRoutePlanning(Position startPosition, Position endPosition){
        List<Position> positions = new ArrayList<Position>();
        //positions.add(startPosition);
        Position snp = getNearestPosition(startPosition);
        Position sep = getNearestPosition(endPosition);
         List<String> positionNames = DijkstraUtil.dijkstra(snp.toString(),sep.toString());
        for(String positionName : positionNames){
            positions.add(namePositionMap.get(positionName));
        }
        //positions.add(endPosition);
        return positions;
    }
}
