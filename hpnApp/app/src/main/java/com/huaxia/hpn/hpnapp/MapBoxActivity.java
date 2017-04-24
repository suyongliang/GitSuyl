package com.huaxia.hpn.hpnapp;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationSource;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.Constants;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.permissions.PermissionsManager;
import com.mapbox.services.android.ui.geocoder.GeocoderAutoCompleteView;
import com.mapbox.services.api.ServicesException;
import com.mapbox.services.api.directions.v5.DirectionsCriteria;
import com.mapbox.services.api.directions.v5.MapboxDirections;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.geocoding.v5.GeocodingCriteria;
import com.mapbox.services.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.models.Position;
import com.mapzen.android.lost.api.LocationServices;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ips.casm.com.radiomap.MPoint;
import ips.casm.com.util.iMessage;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapBoxActivity extends Activity {
    private static final String TAG = "Activity";
    private static final int PERMISSIONS_LOCATION = 0;
    private MapView mapView;
    private MapboxMap map;
    private FloatingActionButton floatingActionButton;
    private LocationEngine locationEngine;
    private LocationEngineListener locationEngineListener;
    private DirectionsRoute currentRoute;
    private Point mPoint;
    private Intent startIpsServiceIntent;
    private String rmFilePathStr=null;
    private BroadcastReceiver mIPSPointReceiver;
    private IntentFilter ipsPointRecerverIntentFilter;
//    private PictureMarkerSymbol mPointMarkerSymbol;
//    private GraphicsLayer graphicsLayer = null;
//    private Drawable maker_img_locate,newmarker;//定位指向标志
    private Map pointMap = new HashMap();

    SpatialReference cgcs2000NoneZone=SpatialReference.create(4548);
    SpatialReference cgcs2000Geodetic=SpatialReference.create(4490);

    private String result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        MapboxAccountManager.start(this, getString(R.string.access_Token));
        Mapbox.getInstance(this, getString(R.string.access_Token));
        setContentView(R.layout.activity_mapbox);
//        locationServices = LocationServices.getLocationServices(MapBoxActivity.this);

        // Get the location engine object for later use.
        locationEngine = LocationSource.getLocationEngine(this);

        // Set up the MapView
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;
                longclick(map);
//                mapboxMap.addMarker(new MarkerOptions()
//                        .position(new LatLng(112.520855, -0.008069))
//                        .title("Hello World!")
//                        .snippet("Welcome to my marker."));
                for(Marker maker : map.getMarkers()){
                    map.removeMarker(maker);
                }
                map.addMarker(new MarkerOptions().position(new LatLng(-0.008069, 112.520855)));
            }
        });

        // 取得经纬度和方位角
        getPointAndAzimuth();

        //定位
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
                } else if (response.body().getRoutes().size() < 1) {
                    Log.e(TAG, "No routes found");
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
        LatLng[] points = new LatLng[coordinates.size()];
        for (int i = 0; i < coordinates.size(); i++) {
            points[i] = new LatLng(
                    coordinates.get(i).getLatitude(),
                    coordinates.get(i).getLongitude());
        }

        // Draw Points on MapView
        map.addPolyline(new PolylineOptions()
                .add(points)
                .color(Color.parseColor("#009688"))
                .width(5));
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
                Toast.makeText(MapBoxActivity.this, "没有找到该指纹库文件", Toast.LENGTH_SHORT).show();
                finish();
            }else {
                rmFilePathStr=rmfilePath;
                Log.i("IPS_DCActivity","获得的文件路径为："+rmfilePath);
            }
        }
        startIpsServiceIntent=new Intent(MapBoxActivity.this,ips.casm.com.service.MIPSService.class);
        Bundle ipsServiceBundle=new Bundle();
        ipsServiceBundle.putString(iMessage.IN_FILE_PATH, rmFilePathStr);
        startIpsServiceIntent.putExtras(ipsServiceBundle);
//        startIpsServiceIntent.putExtra(iMessage.IN_FILE_PATH, rmFilePathStr);
        bindService(startIpsServiceIntent, this.ipsServiceConnection, Context.BIND_AUTO_CREATE);

        mIPSPointReceiver=new IPSPointReceiver();
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

            Log.i("pointMap", pointMap.toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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


    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
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

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        mapView.onDestroy();
//    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
