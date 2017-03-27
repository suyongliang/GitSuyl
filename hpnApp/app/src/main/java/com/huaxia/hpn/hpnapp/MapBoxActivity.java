package com.huaxia.hpn.hpnapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.widget.Toast;

import com.esri.android.map.GraphicsLayer;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.mapbox.mapboxsdk.MapboxAccountManager;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationServices;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.directions.v5.models.DirectionsRoute;

import java.io.File;

import ips.casm.com.radiomap.MPoint;
import ips.casm.com.util.iMessage;

public class MapBoxActivity extends Activity {
    private static final String TAG = "Activity";
    private MapView mapView;
    private MapboxMap map;
    private FloatingActionButton floatingActionButton;
    private LocationServices locationServices;
    private DirectionsRoute currentRoute;
    private Point mPoint;
    private Intent startIpsServiceIntent;
    private String rmFilePathStr=null;
    private BroadcastReceiver mIPSPointReceiver;
    private IntentFilter ipsPointRecerverIntentFilter;
    private PictureMarkerSymbol mPointMarkerSymbol;
    private GraphicsLayer graphicsLayer = null;
    private Drawable maker_img_locate,newmarker;//定位指向标志

    SpatialReference cgcs2000NoneZone=SpatialReference.create(4548);
    SpatialReference cgcs2000Geodetic=SpatialReference.create(4490);

    private String result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapboxAccountManager.start(this, getString(R.string.access_Token));
        //Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_mapbox);

        // Set up the MapView
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        // 定位图标
//        maker_img_locate= ContextCompat.getDrawable(MapBoxActivity.this, R.drawable.navi_map_gps_locked);
//        newmarker=zoomDrawable(maker_img_locate,60,60);
//        //生成当前点标记
//        mPointMarkerSymbol=new PictureMarkerSymbol(newmarker);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                mapboxMap.addMarker(new MarkerOptions()
                        .position(new LatLng(112.520855, -0.008069))
                        .title("Hello World!")
                        .snippet("Welcome to my marker."));
                mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(112.520855, -0.008069), 10));
            }
        });

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

            point.setX(112.42031428740871+(point.getX()-450460.3487085744)/100000);
            point.setY(39.94748200954813+(point.getY()-4423858.654904741)/100000);

            Log.i("IPSPointReceiver", point_Plane.toString());
//			//创建投影坐标系的空间参考
//			SpatialReference cgcs2000NoneZone=SpatialReference.create(4548);
//			//获取当前地图的空间参考
//			SpatialReference cgcs2000Geodetic=mMapView.getSpatialReference();
            //投影反算,获得点位信息
//            mPoint=(Point) GeometryEngine.project(point, cgcs2000NoneZone, cgcs2000Geodetic);
            Log.i("IPSPointReceiver", "投影后坐标为："+point.getX()+","+point.getY());
            //获得azimuth
            float azimuth=point_Plane.getAzimuth();

//            mapView.getMapAsync(new OnMapReadyCallback() {
//                @Override
//                public void onMapReady(MapboxMap mapboxMap) {
//                    mapboxMap.addMarker(new MarkerOptions()
//                            .position(new LatLng(112.520855, -0.008069))
//                            .title("Hello World!")
//                            .snippet("Welcome to my marker."));
//
//                }
//            });
//            mPointMarkerSymbol.setAngle(azimuth);
//            //生成graphic
//            Graphic mPointGraphic=new Graphic(mPoint, mPointMarkerSymbol);
//            graphicsLayer.removeAll();
//            graphicsLayer.addGraphic(mPointGraphic);
//            Envelope extent=new Envelope();
//            extent.merge(mPoint);
//            mapView.invalidate();

        }
    }

    private static Bitmap drawableToBitmap(Drawable drawable){
        int width=drawable.getIntrinsicWidth();
        int height=drawable.getIntrinsicHeight();
        Bitmap.Config config=drawable.getOpacity()!= PixelFormat.OPAQUE?Bitmap.Config.ARGB_8888:Bitmap.Config.RGB_565;
        Bitmap bitmap=Bitmap.createBitmap(width, height, config);
        Canvas canvas=new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }
    private  Drawable zoomDrawable(Drawable drawable, int w,int h){
        int width=drawable.getIntrinsicWidth();
        int height=drawable.getIntrinsicHeight();
        Bitmap oldbmp=drawableToBitmap(drawable);
        Matrix matrix=new Matrix();
        float scaleWidth=((float)w/width);
        float scaleHeight=((float)h/height);
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap newbmp=Bitmap.createBitmap(oldbmp, 0, 0, width, height,matrix,true);
        Drawable newDrawable=new BitmapDrawable(MapBoxActivity.this.getResources(), newbmp);
        return newDrawable;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
