package com.huaxia.hpn.route;

import android.text.TextUtils;

import com.mapbox.services.commons.models.Position;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Created by liulx on 2017/6/9.
 */

public class RoutePlanning {
    static List<LineSegment> lineSegments = null;
    static HashMap<String,Position> namePositionMap = new HashMap<String,Position>();
    static List<LineSegment> routeLineSegments = null;
    static RouteEngine routeEngine = new RouteEngine();
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

                        RouteEngine.Vertex v1 = routeEngine.new Vertex().creatBaseVertex(position1.toString());//建点
                        List<RouteEngine.Edge> v1Es = v1.getEdges();//建边

                        RouteEngine.Vertex v2 = routeEngine.new Vertex().creatBaseVertex(position2.toString());//建点
                        List<RouteEngine.Edge> v2Es = v2.getEdges();//建边

                        boolean exist = false;
                        for(RouteEngine.Edge edge :v1Es ){
                            if(StringUtils.equals(edge.getDest().getName(),v2.getName())){
                                exist = true;
                                break;
                            }
                        }
                        if(!exist){
                            RouteEngine.Edge edge =routeEngine.new Edge(v2, lineSegment.getLenth());//赋值边
                            v1Es.add(edge);
                        }
                        exist = false;
                        for(RouteEngine.Edge edge :v2Es ){
                            if(StringUtils.equals(edge.getDest().getName(),v2.getName())){
                                exist = true;
                                break;
                            }
                        }
                        if(!exist){
                            RouteEngine.Edge edge =routeEngine.new Edge(v1, lineSegment.getLenth());//赋值边
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

    public static LineSegment getNearestLine(Position position,List<LineSegment> lineSegments){
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
        LineSegment lineSegment = getNearestLine(position,lineSegments);
        Position p1 = lineSegment.getP1();
        Position p2 = lineSegment.getP2();
        Position pp = routeEngine.getProjectivePoint(p1,p2,position);

        //增加一个顶点，两条线段、并建图
        namePositionMap.put(pp.toString(),pp);
        LineSegment lineSegment1 = new LineSegment(p1,pp);
        LineSegment lineSegment2 = new LineSegment(p2,pp);
        RouteEngine.Vertex pv = routeEngine.new Vertex().addRouteVertex(pp.toString());//建点
        RouteEngine.Vertex pv1 = routeEngine.new Vertex().addRouteVertex(p1.toString());//获取p1顶点
        RouteEngine.Vertex pv2 = routeEngine.new Vertex().addRouteVertex(p2.toString());//获取p2顶点
        List<RouteEngine.Edge> pvEs = pv.getEdges();//建边
        boolean exist = false;
        for(RouteEngine.Edge edge :pvEs ){
            if(StringUtils.equals(edge.getDest().getName(),pv1.getName())){
                exist = true;
                break;
            }
        }
        if(!exist){
            RouteEngine.Edge edge =routeEngine.new Edge(pv1, lineSegment1.getLenth());//赋值边
            pvEs.add(edge);
        }
        exist = false;
        for(RouteEngine.Edge edge :pvEs ){
            if(StringUtils.equals(edge.getDest().getName(),pv2.getName())){
                exist = true;
                break;
            }
        }
        if(!exist){
            RouteEngine.Edge edge =routeEngine.new Edge(pv2, lineSegment2.getLenth());//赋值边
            pvEs.add(edge);
        }

        List<RouteEngine.Edge> pv1Es = pv1.getEdges();//建边
        exist = false;
        for(RouteEngine.Edge edge :pv1Es ){
            if(StringUtils.equals(edge.getDest().getName(),pv1.getName())){
                exist = true;
                break;
            }
        }
        if(!exist){
            RouteEngine.Edge pv1pv =routeEngine.new Edge(pv, lineSegment1.getLenth());//赋值边
            pv1Es.add(pv1pv);
        }

        List<RouteEngine.Edge> pv2Es = pv2.getEdges();//建边
        exist = false;
        for(RouteEngine.Edge edge :pv2Es ){
            if(StringUtils.equals(edge.getDest().getName(),pv2.getName())){
                exist = true;
                break;
            }
        }
        if(!exist){
            RouteEngine.Edge pv2pv =  routeEngine.new Edge(pv, lineSegment2.getLenth());//赋值边
            pv2Es.add(pv2pv);
        };
        return pp;
    }

    public List<Position> getRoutePlanning(Position startPosition, Position endPosition){
        routeEngine.resetRouteVertexMap();
        List<Position> positions = new ArrayList<Position>();
        routeLineSegments = new ArrayList<LineSegment>();
        positions.add(startPosition);
        Position snp = getNearestPosition(startPosition);
        Position sep = getNearestPosition(endPosition);
        List<String> positionNames = routeEngine.dijkstra(snp.toString(),sep.toString());
        Position firstPosition = null;
        for(String positionName : positionNames){
            Position lastPosition = namePositionMap.get(positionName);
            positions.add(lastPosition);
            if(firstPosition!=null){
                LineSegment lineSegment = new LineSegment(firstPosition,lastPosition);
                routeLineSegments.add(lineSegment);
            }
            firstPosition = lastPosition;
        }
        positions.add(endPosition);
        return positions;
    }

    public static Position getRouttingPosition(Position position){
        LineSegment lineSegment = getNearestLine(position,routeLineSegments);
        Position p1 = lineSegment.getP1();
        Position p2 = lineSegment.getP2();
        return routeEngine.getProjectivePoint(p1,p2,position);
    }

    public static LineSegment getNearestLine(Position position){
        LineSegment lineSegment = getNearestLine(position,routeLineSegments);
        return lineSegment;
    }
}
