package com.example.porter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.android.gms.common.internal.service.Common;
import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapPOIItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;

import java.util.ArrayList;

public class MapActivity extends AppCompatActivity {
    private TMapGpsManager tMapGps = null;
    private TMapView tMapView;
    private double lat;
    private double lon;
    private double realdistance = 0; //거리값
    private static String AppKey = "l7xx2b1c5cd91b914c2c9c80aab1109ae5d3";
    MapPoint mapPoint = new MapPoint();

    private boolean locationState = true;
    private TMapPoint tMapPointStart;
    //private TMapPoint tMapPointStart = new TMapPoint(35.17241886016579, 129.1263765979288);//영상물 등급위원회 : 35.17241886016579, 129.1263765979288 //혜화역 : 37.582191, 127.001915
    private TMapPoint tMapPointEnd = new TMapPoint(35.17127425152002, 129.12722778443444);//영화의 전당: 35.17127425152002, 129.12722778443444 // 역삼역 : 37.500628, 127.036392

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        //상태바 투명 & 아이콘 회색
        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        View view = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (view != null) {
                // 23 버전 이상일 때 상태바 하얀 색상에 회색 아이콘 색상을 설정
                view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                getWindow().setStatusBarColor(Color.parseColor("#f2f2f2"));
            }
        } else if (Build.VERSION.SDK_INT >= 21) {
            // 21 버전 이상일 때
            getWindow().setStatusBarColor(Color.BLACK);
        }

        //tmap
        //--지도 부분
        LinearLayout linearLayoutTmap = (LinearLayout) findViewById(R.id.tmap);
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey(AppKey);
        linearLayoutTmap.addView(tMapView);
        //tMapView.setCenterPoint(127.001691,37.540263,  true);

        //--현재 위치
        tMapGps = new TMapGpsManager(MapActivity.this);
        setGps();
         /* 현위치 아이콘표시 */
        tMapView.setIconVisibility(true);
         /* 줌레벨 */
        tMapView.setZoomLevel(15);
        tMapView.setMapType(TMapView.MAPTYPE_STANDARD);
        tMapView.setLanguage(TMapView.LANGUAGE_KOREAN);
        //tMapView.setCompassMode(true); //현재 보는 방향
         /*현재 위치 마커 커스텀*/
        //Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.custom_poi_marker_end);
        //tMapView.setIcon(bitmap);

        //--경로 부분
        drawCashPath(tMapPointStart, tMapPointEnd);
    }


    /**
     * 현재 위치로 표시될 좌표의 위도, 경도를 설정한다.
     */
    private final LocationListener mLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {

            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                tMapView.setLocationPoint(longitude, latitude);
                tMapView.setCenterPoint(longitude, latitude);
                tMapPointStart = tMapView.getCenterPoint();
                //--경로 부분 (한번 더 호출)
                if (locationState == true) { //현재 위치를 출발지로 설정하고 싶으면 locationState가 true여야 함
                    drawCashPath(tMapPointStart, tMapPointEnd);
                    locationState = false;
                }
            }
        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    public void setGps() {
        final LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, // 등록할 위치제공자(실내에선 NETWORK_PROVIDER 권장)
                1000, // 통지사이의 최소 시간간격 (miliSecond)
                1, // 통지사이의 최소 변경거리 (m)
                mLocationListener);
        tMapPointStart = tMapGps.getLocation();
    }

    // 경로 그리는 함수
    public void drawCashPath(TMapPoint tMapPointStart, TMapPoint tMapPointEnd) {
        TMapData tmapdata = new TMapData();
        tmapdata.findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH, tMapPointStart, tMapPointEnd, new TMapData.FindPathDataListenerCallback() {
            @Override
            public void onFindPathData(TMapPolyLine polyLine) {
                polyLine.setLineColor(Color.BLUE);
                tMapView.addTMapPath(polyLine);
                realdistance = polyLine.getDistance(); //거리
                Log.e("(경로그리는함수)", "거리 : " + String.valueOf(realdistance) + "m  " + "출발 : " + tMapPointStart + "  도착 : " + tMapPointEnd);
                setGps();
            }
        });
    }

}
