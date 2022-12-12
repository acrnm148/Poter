# 🚙 Poter 🚙
차량 경로 제공 시스템 (차량용 길 찾기)

<br>

## 1️⃣ 기획배경

무료 경로, 최단 거리, 최소 시간 등 원하는 경로를 선택하여 
출발지부터 도착지까지의 맞춤형 경로를 확인할 수 있는 서비스를 구현하고자 한다.

<br>

## 2️⃣ 개발 환경 & 기술 스택

  - AndroidStudio, Java, GSON
  - TMap API

<br>

## 3️⃣ 개발 내용 / 성과

- TMapAPI를 활용한 주소 검색 가능, 해당 주소를 출발지, 도착지로 설정 가능
- GPS를 이용하여 현재 위치를 출발지로 설정 가능
- 지도 위 현재 위치, 출발지에서 도착지까지의 자동차 경로 표시
- 선택 경로 종류
```
     - 0: 교통최적+추천(기본값)
     - 1: 교통최적+무료우선
     - 2: 교통최적+최소시간
     - 3: 교통최적+초보
     - 4: 교통최적+고속도로우선
     - 10: 최단거리+유/무료
     - 12: 이륜차도로우선 (일반도로가 없는 경우 자동차 전용도로로 안내 할 수 있습니다.)
     - 19: 교통최적+어린이보호구역 회피
```

<br>

## 4️⃣ 시행착오
- Kakao Maps API -> Google Maps API -> TMapAPI 로 세 차례 지도 API를 변경한 시행착오가 있었다.
  - Google Maps API는 보안 상의 이슈로 한국의 차량 경로를 제공하지 않는다.
  
<br>

## 5️⃣ 주요 화면

<div>
<img width="400" src = "https://user-images.githubusercontent.com/67724306/106705757-e537c480-6631-11eb-99b7-ab1c89a86fc2.png">
<img width="400" src = "https://user-images.githubusercontent.com/67724306/106705771-eff25980-6631-11eb-9ee7-5cb9aa73061c.png">
</div>
<div>
![image](https://user-images.githubusercontent.com/67724306/206969588-4c0eb0a4-be0b-42fc-b0de-fa89a5673153.png)
![image](https://user-images.githubusercontent.com/67724306/206969603-1dab0c94-41f7-4e22-9f52-a0ccb92753d2.png)
</div>
