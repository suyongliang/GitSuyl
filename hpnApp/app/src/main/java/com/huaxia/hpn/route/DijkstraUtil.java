package com.huaxia.hpn.route;

/**
 * Created by liulx on 2017/6/9.
 */


import com.mapbox.services.commons.models.Position;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;


public class DijkstraUtil {
    public static int INFINITY = 99999999;
    public static Map<String, Vertex> vertexMap = new HashMap<String, Vertex>();

    // 边距
    static class Edge {
        public Vertex dest;
        public double cost;

        public Edge(Vertex d, double c) {
            this.dest = d;
            this.cost = c;
        }

    }

    // 静态类：Vertex
    static class Vertex implements Comparable<Vertex> {
        public String name;
        public List<Edge> edges;
        public double dist;
        public Vertex prev;
        public int scratch;
        public boolean visited;

        public Vertex(String nm) {
            this.name = nm;
            edges = new ArrayList<Edge>();
            vertexMap.put(nm,this);
            reset();
        }

        public static Vertex getInstance(String nm){
            if(vertexMap.get(nm)!=null){
                return vertexMap.get(nm);
            }else{
                new Vertex(nm);
                Vertex vertex = new Vertex(nm);
                return vertex;
            }
        };
        public void reset() {
            visited = false;
            dist = DijkstraUtil.INFINITY;
        }

        @Override
        public int compareTo(Vertex o) {
            double c = o.dist;

            return dist < c ? -1 : dist > c ? 1 : 0;
        }

    }

    // Dijkstra算法实现:找到从startName点出发，到其他所有点的最短路径:选取自己定义的终点
    public static List<String> dijkstra(String startName, String endName) {
        List<String> positionNames = new ArrayList<String>();
        PriorityQueue<Vertex> pq = new PriorityQueue<Vertex>();// 该队列以权值升序排列，因为Vertex实现Comparable接口
        Vertex start = vertexMap.get(startName);
        start.dist = 0;
        for (Vertex v : vertexMap.values())
            pq.add(v);
        int seenNum = 0;
        while (!pq.isEmpty() && seenNum < vertexMap.size()) {
            Vertex v = pq.remove();
            if (v.name.equals(endName)) { // 恰好是自己要找的那个点
                System.out.println(startName + "---->" + v.name + ":" + v.dist); // 最短距离
                System.out.println(getPreNames(v)); // 最短路线
                return  getPreNames(v);
            }
            if (v.scratch != 0)
                continue;
            v.scratch = 1;
            seenNum++;

            for (Edge e : v.edges) {
                Vertex w = e.dest;
                double v_to_w = e.cost;
                if (w.dist > v.dist + v_to_w) {
                    w.dist = v.dist + v_to_w;
                    w.prev = v;
                    pq.remove(w);// 出队
                    pq.add(w);// 按优先级插在队头，先插入的在队头，依次往后

                }
            }
        }
        while (pq.peek() != null) {
            System.out.println(pq.poll());
            positionNames.add(pq.poll().toString());
        }
        return positionNames;
    }

    /**
     * 得到最短路径所经历的路线 seven
     *
     * @param v
     * @return
     */
    public static List<String> getPreNames(Vertex v) {
        List<String> positionNames = new ArrayList<String>();
        String routeEndName = v.name;
        StringBuilder sb = new StringBuilder();
        while (v.prev != null) {
            sb.append(v.prev.name + ";");
            v = v.prev;
        }
        String reverseRoute = routeEndName + ";" + sb.toString();
        String[] reverseArray = reverseRoute.split(";");
        for (int i = 0; i < reverseArray.length; i++) {
            positionNames.add(reverseArray[reverseArray.length - 1 - i]);
        }
        return positionNames;
    }

    /**
     * 求直线外一点到直线上的投影点
     *
     * @param p1 线上一点
     * @param p2 线上另一点
     * @param p 线外一点
     * @return  投影点
     */
    public static Position getProjectivePoint(Position p1, Position p2, Position p) {
        double k=0;
        Position projectPosition = null;
        if(p1.getLatitude()==p2.getLatitude()&&p1.getLongitude() == p2.getLongitude()){
            projectPosition = p1;
        }else if(p1.getLatitude()==p2.getLatitude()){
            projectPosition = Position.fromCoordinates(p.getLongitude(),p1.getLatitude());
        }else if(p1.getLongitude()==p2.getLongitude()){
            projectPosition = Position.fromCoordinates(p1.getLongitude(),p.getLatitude());
        }else{
            k=(p2.getLatitude()-p1.getLatitude())/(p2.getLongitude()-p1.getLongitude());
            double pLongitude = (float) ((k * p.getLatitude() + p.getLongitude()- k*p1.getLatitude() +k*k* p1.getLongitude()) / (k*k+1));
            double pLatitude = (float) (k*pLongitude+p1.getLatitude()-k*p1.getLongitude());
            projectPosition=Position.fromCoordinates(pLongitude,pLatitude);
        }
        return projectPosition;
    }

    public static Position getProjectivePoint2(Position p1, Position p2, Position p) {
        double k=0;
        Position projectPosition = null;
        if(p1.getLatitude()==p2.getLatitude()&&p1.getLongitude() == p2.getLongitude()){
            projectPosition = p1;
        }else if(p1.getLatitude()==p2.getLatitude()){
            projectPosition = Position.fromCoordinates(p.getLongitude(),p1.getLatitude());
        }else if(p1.getLongitude()==p2.getLongitude()){
            projectPosition = Position.fromCoordinates(p1.getLongitude(),p.getLatitude());
        }else{
            k=(p2.getLatitude()-p1.getLatitude())/(p2.getLongitude()-p1.getLongitude());
            double pLongitude = (float) ((k * p1.getLongitude() + p.getLongitude() / k + p.getLatitude() - p1.getLatitude()) / (1 / k + k));
            double pLatitude = (float) (-1 / k * (pLongitude - p.getLongitude()) + p.getLatitude());
            projectPosition=Position.fromCoordinates(pLongitude,pLatitude);
        }
        return projectPosition;
    }
}