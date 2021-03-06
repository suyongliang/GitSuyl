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



public class RouteEngine {
    private static int INFINITY = Integer.MAX_VALUE;
    private static Map<String, Vertex> baseVertexMap = new HashMap<String, Vertex>();
    private Map<String, Vertex> vertexMap = new HashMap<String, Vertex>();

    // 边距
     class Edge {
        private Vertex dest;
        private double cost;

        public Edge(Vertex d, double c) {
            this.dest = d;
            this.cost = c;
        }

        public double getCost() {
            return cost;
        }

        public void setCost(double cost) {
            this.cost = cost;
        }

        public Vertex getDest() {
            return dest;
        }

        public void setDest(Vertex dest) {
            this.dest = dest;
        }
    }

    // 静态类：Vertex
    class Vertex implements Comparable<Vertex> {
        private String name;
        private List<Edge> edges;
        private double dist;
        private Vertex prev;
        private int scratch;
        private boolean visited;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Edge> getEdges() {
            return edges;
        }

        public void setEdges(List<Edge> edges) {
            this.edges = edges;
        }

        public double getDist() {
            return dist;
        }

        public void setDist(double dist) {
            this.dist = dist;
        }

        public Vertex getPrev() {
            return prev;
        }

        public void setPrev(Vertex prev) {
            this.prev = prev;
        }

        public int getScratch() {
            return scratch;
        }

        public void setScratch(int scratch) {
            this.scratch = scratch;
        }

        public boolean isVisited() {
            return visited;
        }

        public void setVisited(boolean visited) {
            this.visited = visited;
        }

        public Vertex creatBaseVertex(String nm) {
            if(baseVertexMap.get(nm)!=null){
                return baseVertexMap.get(nm);
            }else{
                this.name = nm;
                baseVertexMap.put(nm,this);
                return this;
            }
        }

        public Vertex(String nm) {
            this.name = nm;
            edges = new ArrayList<Edge>();
            visited = false;
            dist = RouteEngine.INFINITY;
        }

        public Vertex() {
            edges = new ArrayList<Edge>();
            visited = false;
            dist = RouteEngine.INFINITY;
        }

        public Vertex addRouteVertex(String nm){
            if(vertexMap.get(nm)!=null){
                return vertexMap.get(nm);
            }else{
                this.name = nm;
                vertexMap.put(nm,this);
                return this;
            }
        };

        @Override
        public int compareTo(Vertex o) {
            double c = o.dist;
            return dist < c ? -1 : dist > c ? 1 : 0;
        }
    }

    public void resetRouteVertexMap() {
        vertexMap.clear();
        for(String key :baseVertexMap.keySet() ){
            vertexMap.put(key,baseVertexMap.get(key));
        }

    }

    // Dijkstra算法实现:找到从startName点出发，到其他所有点的最短路径:选取自己定义的终点
    public List<String> dijkstra(String startName, String endName) {
        //log.e("route.dijkstra ** start: "," vertexMap:"+ vertexMap.size() + " point:"+startName + " <--> " + endName);
        List<String> positionNames = new ArrayList<String>();
        PriorityQueue<Vertex> vertexPriorityQueue = new PriorityQueue<Vertex>();// 该队列以权值升序排列，因为Vertex实现Comparable接口
        Vertex start = vertexMap.get(startName);
        start.dist = 0;
        for (Vertex v : vertexMap.values()){
            vertexPriorityQueue.add(v);
        }
        int seenNum = 0;
        while (!vertexPriorityQueue.isEmpty() && seenNum < vertexMap.size()) {
            Vertex v = vertexPriorityQueue.remove();
            //log.e("route.while ** roop: ","seenNum: " + seenNum + " v.scratch： "+ v.scratch+" v.name: "+v.name);
            if (v.name.equals(endName)) { // 恰好是自己要找的那个点
                return  getPreNames(v);
            }
            seenNum++;

            for (Edge e : v.edges) {
                Vertex w = e.dest;
                double v2wCost = e.cost;
                //log.e("route.Edge ** roop: ",v.name + "" + v2wCost + " "+ w.name);
                if (w.dist > v.dist + v2wCost) {
                    w.dist = v.dist + v2wCost;
                    w.prev = v;
                    vertexPriorityQueue.remove(w);// 出队
                    vertexPriorityQueue.add(w);// 按优先级插在队头，先插入的在队头，依次往后
                }
            }
        }
        while (vertexPriorityQueue.peek() != null) {
            String positionName = vertexPriorityQueue.poll().toString();
            positionNames.add(positionName);
        }
        return positionNames;
    }

    /**
     * 得到最短路径所经历的路线 seven
     *
     * @param v
     * @return
     */
    public List<String> getPreNames(Vertex v) {
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
    public Position getProjectivePoint(Position p1, Position p2, Position p) {
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

}