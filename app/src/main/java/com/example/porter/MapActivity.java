package com.example.porter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapMarkerItem;
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

    ListView listView;
    EditText editStart;
    EditText editEnd;
    ArrayAdapter<POI> mAdapter;
    String keyword;

    private boolean locationState = true;
    private boolean startBtnState = false;
    private boolean endBtnState = false;
    private TMapPoint tMapPointStart = null;
    private TMapPoint tMapPointEnd = null;
    //private TMapPoint tMapPointStart = new TMapPoint(35.17241886016579, 129.1263765979288);//영상물 등급위원회 : 35.17241886016579, 129.1263765979288 //혜화역 : 37.582191, 127.001915
    //private TMapPoint tMapPointEnd = new TMapPoint(35.17127425152002, 129.12722778443444);//영화의 전당: 35.17127425152002, 129.12722778443444 // 역삼역 : 37.500628, 127.036392

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        editStart = (EditText) findViewById(R.id.edit_start);
        editEnd = (EditText) findViewById(R.id.edit_end);
        listView = (ListView) findViewById(R.id.listView);
        mAdapter = new ArrayAdapter<POI>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(mAdapter);


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
        //drawCashPath(tMapPointStart, tMapPointEnd);

        //출발지 search 버튼 클릭 이벤트
        Button btnStart = (Button) findViewById(R.id.btn_start);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBtnState = true;
                keyword = editStart.getText().toString();
                searchPOI();
            }
        });
        //현재위치를 출발지로 설정 버튼
        Button btnSetLocStart = (Button) findViewById(R.id.btn_setLocStart);
        btnSetLocStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                locationState = true;
                //도착지가 null일 경우
                if (tMapPointStart != null && tMapPointEnd!= null) {
                    drawCashPath(tMapPointStart, tMapPointEnd);
                }else if(tMapPointEnd == null) {
                    Toast.makeText(getApplicationContext(), "도착지를 입력해주세요", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //도착지 search 버튼 클릭 이벤트
        Button btnEnd = (Button) findViewById(R.id.btn_end);
        btnEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                endBtnState = true;
                keyword = editEnd.getText().toString();
                searchPOI();
            }
        });

        //listview 클릭 이벤트
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                POI poi = (POI) listView.getItemAtPosition(position);
                moveMap(poi.item.getPOIPoint().getLatitude(), poi.item.getPOIPoint().getLongitude());
                Log.e("선택된 좌표", "lat : " + String.valueOf(poi.item.getPOIPoint().getLatitude()) + " lon : " + poi.item.getPOIPoint().getLongitude());
                
                if (startBtnState == true) { //출발지 선택
                    tMapPointStart = new TMapPoint(poi.item.getPOIPoint().getLatitude(), poi.item.getPOIPoint().getLongitude());
                    startBtnState = false;
                }else if(endBtnState == true) { //도착지 선택
                    tMapPointEnd = new TMapPoint(poi.item.getPOIPoint().getLatitude(), poi.item.getPOIPoint().getLongitude());
                    endBtnState = false;
                }
                
                //출발지 or 도착지가 null일 때
                if (tMapPointStart != null && tMapPointEnd!= null) {
                    drawCashPath(tMapPointStart, tMapPointEnd);
                }else if(tMapPointStart == null) {
                    Toast.makeText(getApplicationContext(), "출발지를 입력해주세요", Toast.LENGTH_SHORT).show();
                }else if(tMapPointEnd == null) {
                    Toast.makeText(getApplicationContext(), "도착지를 입력해주세요", Toast.LENGTH_SHORT).show();
                }
            }
        });
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
                //--경로 부분 (한번 더 호출)
                if (locationState == true) { //현재 위치를 출발지로 설정하고 싶으면 locationState가 true여야 함
                    tMapPointStart = tMapView.getCenterPoint();
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

    //주소 검색
    private void searchPOI() {
        TMapData data = new TMapData();

        if (!TextUtils.isEmpty(keyword)) {
            data.findAllPOI(keyword, new TMapData.FindAllPOIListenerCallback() {
                @Override
                public void onFindAllPOI(final ArrayList<TMapPOIItem> arrayList) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tMapView.removeAllMarkerItem();
                            mAdapter.clear();

                            for (TMapPOIItem poi : arrayList) {
                                addMarker(poi);
                                mAdapter.add(new POI(poi));
                            }

                            if (arrayList.size() > 0) {
                                TMapPOIItem poi = arrayList.get(0);
                                moveMap(poi.getPOIPoint().getLatitude(), poi.getPOIPoint().getLongitude());
                            }
                        }
                    });
                }
            });
        }
    }

    //마커 표시 함수
    public void addMarker(TMapPOIItem poi) {
        TMapMarkerItem item = new TMapMarkerItem();
        item.setTMapPoint(poi.getPOIPoint());
        Bitmap icon = ((BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.custom_poi_marker_selected)).getBitmap();
        item.setIcon(icon);
        item.setPosition(0.5f, 1);
        item.setCalloutTitle(poi.getPOIName());
        item.setCalloutSubTitle(poi.getPOIContent());
        item.setCanShowCallout(true);
        tMapView.addMarkerItem(poi.getPOIID(), item);
    }

    //해당 좌표로 지도 이동 함수
    private void moveMap(double lat, double lng) {
        tMapView.setCenterPoint(lng, lat);
    }
}
