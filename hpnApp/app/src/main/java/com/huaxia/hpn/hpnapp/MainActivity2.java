package com.huaxia.hpn.hpnapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.huaxia.hpn.utils.IpMacUtils;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.MyLocationTracking;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationSource;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.Constants;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.permissions.PermissionsManager;
import com.mapbox.services.api.ServicesException;
import com.mapbox.services.api.directions.v5.DirectionsCriteria;
import com.mapbox.services.api.directions.v5.MapboxDirections;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.geocoding.v5.GeocodingCriteria;
import com.mapbox.services.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.android.ui.geocoder.GeocoderAutoCompleteView;

import net.sf.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ips.casm.com.radiomap.MPoint;
import ips.casm.com.util.iMessage;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity2 extends AppCompatActivity {
    private static final String TAG = "AppCompatActivity";
    private static final int PERMISSIONS_LOCATION = 0;

    private MapView mapView;
    private MapboxMap map;
    private FloatingActionButton floatingActionButton;
    private LocationEngine locationEngine;
    private LocationEngineListener locationEngineListener;
    private DirectionsRoute currentRoute;

    private String result;

    private Point mPoint;
    private Intent startIpsServiceIntent;
    private String rmFilePathStr=null;
    private BroadcastReceiver mIPSPointReceiver;
    private IntentFilter ipsPointRecerverIntentFilter;
    private Map pointMap = new HashMap();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        MapboxAccountManager.start(this, getString(R.string.access_Token));
        Mapbox.getInstance(this, getString(R.string.access_Token));
//        Mapbox.getInstance(this, getString(R.string.access_Token));
        setContentView(R.layout.activity_main2);
//        locationServices = LocationServices.getLocationServices(MainActivity2.this);

        // Get the location engine object for later use.
        locationEngine = LocationSource.getLocationEngine(this);
//        locationEngine.activate();
//        locationEngine.requestLocationUpdates();

        try{
            // 开启一个子线程，进行网络操作，等待有返回结果，使用handler通知UI
            new Thread(getThread).start();
        }catch (Exception e){
            e.printStackTrace();
        }
        // 取得经纬度和方位角
        getPointAndAzimuth();

        // Set up the MapView
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;
                //toggleGps(!map.isMyLocationEnabled());
                longclick(map);

                toggleGps(true);
                // Enable user tracking to show the padding affect.
                map.getTrackingSettings().setMyLocationTrackingMode(MyLocationTracking.TRACKING_FOLLOW);
//                map.getTrackingSettings().setDismissAllTrackingOnGesture(false);
//
                // Customize the user location icon using the getMyLocationViewSettings object.
                map.getMyLocationViewSettings().setPadding(0, 500, 0, 0);
                map.getMyLocationViewSettings().setForegroundTintColor(Color.parseColor("#56B881"));
                map.getMyLocationViewSettings().setAccuracyTintColor(Color.parseColor("#FBB03B"));
            }
        });
        String mac = IpMacUtils.getMacFromWifi();
        Log.i(TAG, "123");
        floatingActionButton = (FloatingActionButton) findViewById(R.id.location_toggle_fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (map != null) {
                    toggleGps(!map.isMyLocationEnabled());
                }
            }
        });

        // Set up autocomplete widget
        GeocoderAutoCompleteView autocomplete = (GeocoderAutoCompleteView) findViewById(R.id.query);
        autocomplete.setAccessToken(Mapbox.getAccessToken());
        autocomplete.setType(GeocodingCriteria.TYPE_POI);
        autocomplete.setOnFeatureListener(new GeocoderAutoCompleteView.OnFeatureListener() {
            @Override
            public void onFeatureClick(CarmenFeature feature) {
                Position position = feature.asPosition();
                updateMap(position.getLatitude(), position.getLongitude());
            }
        });

    }

    private void updateMap(double latitude, double longitude) {
        // Build marker
        map.addMarker(new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .title("Geocoder result"));
        //跳转目的地界面
        // Animate camera to geocoder result location
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude))
                .zoom(15)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 5000, null);
    }

    private void longclick(final MapboxMap mapboxMap) {
        mapboxMap.setOnMapLongClickListener(new MapboxMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng point) {

                //删除所有之前的标记
                mapboxMap.removeAnnotations();

                // Set the origin waypoint to the devices location设置初始位置
                Position origin = Position.fromCoordinates(mapboxMap.getMyLocation().getLongitude(), mapboxMap.getMyLocation().getLatitude());

                // 设置目的地路径--点击的位置点
                Position destination = Position.fromCoordinates(point.getLongitude(), point.getLatitude());

                // Add marker to the destination waypoint
                mapboxMap.addMarker(new MarkerOptions()
                        .position(new LatLng(point))
                        .title("目的地")
                        .snippet("My destination"));

                // Get route from API
                try {
                    getRoute(origin, destination);
                } catch (ServicesException servicesException) {
                    servicesException.printStackTrace();
                }
            }
        });
    }

    private void getRoute(Position origin, Position destination) throws ServicesException{
        ArrayList<Position> positions = new ArrayList<>();
        positions.add(origin);
        positions.add(destination);

        MapboxDirections directions = new MapboxDirections.Builder()
                .setAccessToken(getString(R.string.access_Token))
                .setOrigin(origin)
                .setDestination(destination)
                .setProfile(DirectionsCriteria.PROFILE_WALKING)
                .build();

        directions.enqueueCall(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                // You can get the generic HTTP info about the response
                Log.d(TAG, "Response code: " + response.code());
                if (response.body() == null) {
                    Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                    return;
                }
                // Print some info about the route
                currentRoute = response.body().getRoutes().get(0);
                Log.d(TAG, "Distance: " + currentRoute.getDistance());
                showToastMessage("You are %d meters from your destination");

                // Draw the route on the map
                drawRoute(currentRoute);
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                Log.e(TAG, "Error: " + throwable.getMessage());
                showToastMessage("Error: " + throwable.getMessage());
            }
        });
    }

    private void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void drawRoute(DirectionsRoute route) {
        // Convert LineString coordinates into LatLng[]
        LineString lineString = LineString.fromPolyline(route.getGeometry(), Constants.OSRM_PRECISION_V5);
        List<Position> coordinates = lineString.getCoordinates();
//        LatLng[] points = new LatLng[coordinates.size()];
        ArrayList<LatLng> points = new ArrayList<LatLng>();
        for (int i = 0; i < coordinates.size(); i++) {
            points.add(new LatLng(
                    coordinates.get(i).getLatitude(),
                    coordinates.get(i).getLongitude()));
        }

        // Draw Points on MapView
        map.addPolyline(new PolylineOptions()
                .addAll(points)
                .color(Color.parseColor("#3bb2d0"))
                .width(10));
    }

    private void toggleGps(boolean enableGps) {
        if (enableGps) {
            // Check if user has granted location permission
            if (!PermissionsManager.areLocationPermissionsGranted(this)) {
                ActivityCompat.requestPermissions(this, new String[] {
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_LOCATION);
            } else {
                enableLocation(true);
            }
        } else {
            enableLocation(false);
        }
    }

    private void enableLocation(boolean enabled) {
        if (enabled) {
            // If we have the last location of the user, we can move the camera to that position.
            Location lastLocation = locationEngine.getLastLocation();
            if (lastLocation != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation), 16));
            }

            locationEngineListener = new LocationEngineListener() {
                @Override
                public void onConnected() {
                    locationEngine.requestLocationUpdates();
                }

                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        // Move the map camera to where the user location is and then remove the
                        // listener so the camera isn't constantly updating when the user location
                        // changes. When the user disables and then enables the location again, this
                        // listener is registered again and will adjust the camera once again.
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location), 16));
                        locationEngine.removeLocationEngineListener(this);
                    }
                }
            };
            locationEngine.addLocationEngineListener(locationEngineListener);
            floatingActionButton.setImageResource(R.drawable.ic_location_disabled_24dp);
        } else {
            floatingActionButton.setImageResource(R.drawable.ic_my_location_24dp);
        }
        // Enable or disable the location layer on the map
        map.setMyLocationEnabled(enabled);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private Thread getThread = new Thread(){
        public void run() {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("http://10.0.2.2:8081/hpn-new/app/hpn/spotData!obtainCollectionses.do");
                connection = (HttpURLConnection) url.openConnection();
                // 设置请求方法，默认是GET
                connection.setRequestMethod("POST");
                // 设置字符集
                connection.setRequestProperty("Charset", "UTF-8");
                // 设置文件类型
                connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
                // 设置请求参数，可通过Servlet的getHeader()获取
                connection.setRequestProperty("Cookie", "AppName=" + URLEncoder.encode("你好", "UTF-8"));
                // 设置自定义参数
                connection.setRequestProperty("MyProperty", "this is me!");
                OutputStream outputStream = connection.getOutputStream();
                JSONObject maps = new JSONObject();
                maps.put("MACCode", "08:00:20:0A:8C:6D");
                maps.put("latitude", "-0.008065");
                maps.put("longitude", "112.520855");
                maps.put("azimuth", "0");
                maps.put("operater", "admin");
                String params = "data.MACCode=08:00:20:0A:8C:6D&data.latitude=-0.008065&data.longitude=112.520855&data.azimuth=0&data.operater=admin";
                JSONObject paramsJ = new JSONObject();
                paramsJ.put("data",maps);
                // 注意编码格式
                outputStream.write(maps.toString().getBytes());
                outputStream.close();

                if(connection.getResponseCode() == 200){
                    InputStream is = connection.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(
                            is, "utf-8");
                    BufferedReader bufferedReader = new BufferedReader(
                            inputStreamReader);
                    String str = null;
                    StringBuffer buffer = new StringBuffer();
                    while ((str = bufferedReader.readLine()) != null) {
                        buffer.append(str);
                    }

                    // 释放资源
                    bufferedReader.close();
                    inputStreamReader.close();
                    is.close();
                    result = buffer.toString();

                    Message msg = Message.obtain();
                    msg.what = 0;
                    getHandler.sendMessage(msg);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
            }
        };
    };

    private Handler getHandler = new Handler(){
        public void handleMessage(Message msg) {
            if(msg.what == 0 && result!=null){
                Log.i(TAG, "return: " + result);;
            }
        };
    };
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocation(true);
            }
        }
    }

// 调用毕博士的service
    private void getPointAndAzimuth(){
        //取得指纹数据
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String SystemDirPath=Environment.getExternalStorageDirectory().toString();
            String IPSFolderPath=SystemDirPath+ File.separator+"IndoorPositionSystem";
            String RadioMapPath=IPSFolderPath+File.separator+"RadioMap";
            String rmfilePath=RadioMapPath+File.separator+"Dice_Radio_Map.txt";
            //魅族手机获取的是相对路径。
            File rmFile=new File(rmfilePath);
            if (!rmFile.exists()) {
                Toast.makeText(MainActivity2.this, "没有找到该指纹库文件", Toast.LENGTH_SHORT).show();
                finish();
            }else {
                rmFilePathStr=rmfilePath;
                Log.i("IPS_DCActivity","获得的文件路径为："+rmfilePath);
            }
        }
        startIpsServiceIntent=new Intent(MainActivity2.this,ips.casm.com.service.MIPSService.class);
        Bundle ipsServiceBundle=new Bundle();
        ipsServiceBundle.putString(iMessage.IN_FILE_PATH, rmFilePathStr);
        startIpsServiceIntent.putExtras(ipsServiceBundle);
//        startIpsServiceIntent.putExtra(iMessage.IN_FILE_PATH, rmFilePathStr);
        bindService(startIpsServiceIntent, this.ipsServiceConnection, Context.BIND_AUTO_CREATE);

        mIPSPointReceiver=new MainActivity2.IPSPointReceiver();
        ipsPointRecerverIntentFilter=new IntentFilter(iMessage.sendIPSPoint2ActivityBroadcastActionIntent);
        registerReceiver(mIPSPointReceiver,ipsPointRecerverIntentFilter);
    }


    private ServiceConnection ipsServiceConnection=new ServiceConnection(){
        /**
         * 获取service调用
         */
        public void onServiceConnected(ComponentName name, IBinder service) {
            // TODO Auto-generated method stub
//            mIPSService=((MIPSService.MBinder)(service)).getService();
        }

        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub
        }
    };

    private class IPSPointReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("IPSPointReceiver", "调用onReceiver()方法");
            // TODO Auto-generated method stub
            //获得service回传的point
            MPoint point_Plane = (MPoint) intent.getExtras().get(iMessage.sendIPSPoint2ActivityBroadcastActionKey);

            //创建geometry
            final Point point=new Point(point_Plane.x, point_Plane.y);

//            point.setX(112.42031428740871+(point.getX()-450460.3487085744)/100000);
//            point.setY(39.94748200954813+(point.getY()-4423858.654904741)/100000);

            Log.i("IPSPointReceiver", point_Plane.toString());
//			//创建投影坐标系的空间参考
//			SpatialReference cgcs2000NoneZone=SpatialReference.create(4548);
//			//获取当前地图的空间参考
//			SpatialReference cgcs2000Geodetic=mMapView.getSpatialReference();
            //投影反算,获得点位信息
//            mPoint=(Point) GeometryEngine.project(point, cgcs2000NoneZone, cgcs2000Geodetic);
            Log.i("IPSPointReceiver", "投影后坐标为："+point_Plane.x+","+point_Plane.y);
            //获得azimuth
            float azimuth=point_Plane.getAzimuth();
            pointMap.put("Point", point);
            pointMap.put("Azimuth", azimuth);
            if (point != null && map != null){
                //删除所有之前的标记
                map.removeAnnotations();
                map.addMarker(new MarkerOptions()
                        .position(new LatLng(point.getY(), point.getX()))
                        .title("我的位置!")
                        .snippet("Welcome to my marker."));
            }

            Log.i("pointMap", pointMap.toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ensure no memory leak occurs if we register the location listener but the call hasn't
        // been made yet.
        if (locationEngineListener != null) {
            locationEngine.removeLocationEngineListener(locationEngineListener);
        }
        if (mapView!=null) {
            mapView.onDestroy();
        }
        if (startIpsServiceIntent!=null) {
            stopService(startIpsServiceIntent);
        }
        if (mIPSPointReceiver!=null) {
            unregisterReceiver(mIPSPointReceiver);
        }
        System.gc();
    }
}

