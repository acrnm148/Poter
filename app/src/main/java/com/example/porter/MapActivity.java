package com.example.porter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapMarkerItem2;
import com.skt.Tmap.TMapPOIItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity {
    private TMapGpsManager tMapGps = null;
    private TMapView tMapView;
    private double lat;
    private double lon;
    private double realdistance = 0; //거리값
    private static String AppKey = "l7xx2b1c5cd91b914c2c9c80aab1109ae5d3";
    private long backKeyPressedTime = 0; // 마지막으로 뒤로 가기 버튼을 눌렀던 시간 저장
    private Toast toast; // 첫 번째 뒤로 가기 버튼을 누를 때 표시

    ListView listView;
    EditText editStart;
    EditText editEnd;
    ArrayAdapter<POI> mAdapter;
    String keyword;
    LinearLayout layout;

    private double currentLatitude;
    private double currentlongitude;
    private boolean locationState = false;
    private boolean startBtnState = false;
    private boolean endBtnState = false;
    private TMapPoint tMapPointStart = null;
    private TMapPoint tMapPointEnd = null;
    //private TMapPoint tMapPointStart = new TMapPoint(35.17241886016579, 129.1263765979288);//영상물 등급위원회 : 35.17241886016579, 129.1263765979288
    //private TMapPoint tMapPointEnd = new TMapPoint(35.17127425152002, 129.12722778443444);//영화의 전당: 35.17127425152002, 129.12722778443444


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE); //키보드 내리기

        editStart = (EditText) findViewById(R.id.edit_start);
        editEnd = (EditText) findViewById(R.id.edit_end);
        listView = (ListView) findViewById(R.id.listView);
        layout = (LinearLayout) findViewById(R.id.tmap);
        mAdapter = new ArrayAdapter<POI>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(mAdapter);
        listView.setVisibility(View.GONE); //listview 안보이게

        editStart.selectAll();
        editEnd.selectAll();

        //상태바 투명 & 아이콘 회색
        setStateBar();

        //TMapAPI 활용(지도, 현재위치)
        setTMap();

        //버튼 클릭 이벤트
        /* 출발지 search 버튼 */
        Button btnStart = (Button) findViewById(R.id.btn_start);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //startBtnState = true;
                keyword = editStart.getText().toString();
                searchPOI();
                imm.hideSoftInputFromWindow(editStart.getWindowToken(), 0); //키보드 내리기
            }
        });
        /* 출발지 editText 클릭이벤트 */
        editStart.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        //터치했을 때의 이벤트
                        startBtnState = true;
                        break;
                    }
                }
                return false;
            }
        });
        /* 도착지 search 버튼 */
        Button btnEnd = (Button) findViewById(R.id.btn_end);
        btnEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //endBtnState = true;
                keyword = editEnd.getText().toString();
                searchPOI();
                imm.hideSoftInputFromWindow(editEnd.getWindowToken(), 0); //키보드 내리기
            }
        });
        /* 도착지 editText 클릭이벤트 */
        editEnd.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        //터치했을 때의 이벤트
                        endBtnState = true;
                        break;
                    }
                }
                return false;
            }
        });
        /* 현재위치를 출발지로 설정 버튼 */
//        Button btnSetLocStart = (Button) findViewById(R.id.btn_setLocStart);
//        btnSetLocStart.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                locationState = true;
//                LocationManager lm = (LocationManager)getSystemService(Context. LOCATION_SERVICE);
//                tMapView.removeAllMarkerItem(); //마커 안보이게
//            }
//        });
        TextView tvSetLocStart = (TextView)  findViewById(R.id.tv_setLocStart);
        tvSetLocStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                locationState = true;
                LocationManager lm = (LocationManager)getSystemService(Context. LOCATION_SERVICE);
                tMapView.removeAllMarkerItem(); //마커 안보이게
            }
        });
        /* 길찾기 버튼 */
        Button btnDraw = (Button) findViewById(R.id.btn_draw);
        btnDraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tMapView.removeAllMarkerItem(); //마커 안보이게
                //출발지 or 도착지가 null일 때
                if (tMapPointStart != null && tMapPointEnd != null) {
                    drawCashPath(tMapPointStart, tMapPointEnd);
                } else if (tMapPointStart == null && tMapPointEnd != null) {
                    Toast.makeText(getApplicationContext(), "출발지를 입력해주세요", Toast.LENGTH_SHORT).show();
                } else if (tMapPointStart != null && tMapPointEnd == null) {
                    Toast.makeText(getApplicationContext(), "도착지를 입력해주세요", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "출발지와 도착지를 입력해주세요", Toast.LENGTH_SHORT).show();
                }
            }
        });


        //listview 클릭 이벤트
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                POI poi = (POI) listView.getItemAtPosition(position);
                moveMap(poi.item.getPOIPoint().getLatitude(), poi.item.getPOIPoint().getLongitude());
                lat = poi.item.getPOIPoint().getLatitude();
                lon = poi.item.getPOIPoint().getLongitude();
                //Log.e("선택된 좌표", "lat : " + String.valueOf(lat) + " lon : " +lon);

                if (startBtnState == true) { //출발지 선택
                    tMapPointStart = new TMapPoint(poi.item.getPOIPoint().getLatitude(), poi.item.getPOIPoint().getLongitude());
                    startBtnState = false;
                    editStart.setText(poi.toString()); //장소명 setText
                    editStart.clearFocus(); //포커스 없앰
                } else if (endBtnState == true) { //도착지 선택
                    tMapPointEnd = new TMapPoint(poi.item.getPOIPoint().getLatitude(), poi.item.getPOIPoint().getLongitude());
                    endBtnState = false;
                    editEnd.setText(poi.toString()); //장소명 setText
                    editEnd.clearFocus(); //포커스 없앰
                }

                listView.setVisibility(View.GONE); //주소 선택 후 listview 안보이게
            }
        });

    } // -- onCreate()



    //상태바 투명 & 아이콘 회색 설정 함수
    private void setStateBar() {
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
    }

    //TMapAPI 활용(지도, 현재위치)
    private void setTMap() {
        /* 지도 부분 */
        LinearLayout linearLayoutTmap = (LinearLayout) findViewById(R.id.tmap);
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey(AppKey);
        linearLayoutTmap.addView(tMapView);
        /* 현재 위치 */
        tMapGps = new TMapGpsManager(MapActivity.this);
        setGps();
        /* 현위치 아이콘표시 */
        tMapView.setIconVisibility(true);
        /* 줌레벨 */
        tMapView.setZoomLevel(15);
        tMapView.setMapType(TMapView.MAPTYPE_STANDARD);
        tMapView.setLanguage(TMapView.LANGUAGE_KOREAN);
        /*현재 위치 마커 커스텀*/
        //Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.custom_poi_marker_end);
        //tMapView.setIcon(bitmap);
    }


    private View.OnClickListener buttonRefreshClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            // Update location to get.
            LocationManager lm = (LocationManager)getSystemService(Context. LOCATION_SERVICE);

            //lm.removeUpdates( mLocationListener );    // Stop the update if it is in progress.
        }

    };
    /**
     * 현재 위치로 표시될 좌표의 위도, 경도를 설정한다.
     */
    private final LocationListener mLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {

            if (location != null) {
                currentLatitude = location.getLatitude();
                currentlongitude = location.getLongitude();
                tMapView.setLocationPoint(currentlongitude, currentLatitude);
                tMapView.setCenterPoint(currentlongitude, currentLatitude);
                //--경로 부분 (한번 더 호출)
                if (locationState == true) { //현재 위치를 출발지로 설정하고 싶으면 locationState가 true여야 함
                    String address = getCurrentAddress(currentLatitude, currentlongitude); //현재 좌표->주소 반환
                    editStart.setText(address); //장소명 setText
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
    }

    // 경로 그리는 함수
    public void drawCashPath(TMapPoint tMapPointStart, TMapPoint tMapPointEnd) {
        TMapData tmapdata = new TMapData();
        tmapdata.findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH, tMapPointStart, tMapPointEnd, new TMapData.FindPathDataListenerCallback() {
            @Override
            public void onFindPathData(TMapPolyLine polyLine) {
                polyLine.setLineColor(Color.BLUE); // Color.rgb(85, 90, 181)
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
        listView.setVisibility(View.VISIBLE); //listview 보이게

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
        //item.setCalloutSubTitle(poi.getPOIContent());
        item.setCanShowCallout(true);
        tMapView.addMarkerItem(poi.getPOIID(), item);
    }

    //해당 좌표로 지도 이동 함수
    private void moveMap(double lat, double lng) {
        tMapView.setCenterPoint(lng, lat);
    }

    //좌표를 주소로 변환하는 함수
    public String getCurrentAddress(double latitude, double longitude) {
        //지오코더 - GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    7);
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";
        }

        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";
        }
        Address address = addresses.get(0);
        return address.getAddressLine(0).toString() + "\n";
    }
    
    //뒤로가기 - listview 지우기, 앱 종료
    public void onBackPressed() {
        //super.onBackPressed();
        // 기존 뒤로 가기 버튼의 기능을 막기 위해 주석 처리 또는 삭제

        // 마지막으로 뒤로 가기 버튼을 눌렀던 시간에 2.5초를 더해 현재 시간과 비교 후
        // 마지막으로 뒤로 가기 버튼을 눌렀던 시간이 2.5초가 지났으면 Toast 출력
        // 2500 milliseconds = 2.5 seconds
        if (System.currentTimeMillis() > backKeyPressedTime + 2500) {
            backKeyPressedTime = System.currentTimeMillis();
//            toast = Toast.makeText(this, "뒤로 가기 버튼을 한 번 더 누르시면 종료됩니다.", Toast.LENGTH_LONG);
//            toast.show();
            listView.setVisibility(View.GONE); //주소 선택 후 listview 안보이게
            return;
        }
        // 마지막으로 뒤로 가기 버튼을 눌렀던 시간에 2.5초를 더해 현재 시간과 비교 후
        // 마지막으로 뒤로 가기 버튼을 눌렀던 시간이 2.5초가 지나지 않았으면 종료
        if (System.currentTimeMillis() <= backKeyPressedTime + 2500) {
            finish();
//            toast.cancel();
//            toast = Toast.makeText(this,"이용해 주셔서 감사합니다.",Toast.LENGTH_LONG);
//            toast.show();
        }
    }
}
