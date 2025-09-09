// Leaflet 초기화
var map = L.map("map").setView({ lon: 127.766, lat: 36.355 }, 13);

// 최대 범위 지정
map.setMaxBounds([
  [32, 123],
  [44, 132.5],
]);

// '오픈스트리트맵 한국'에서 서비스하는 '군사 시설 없는 오픈스트리트맵 지도 타일'을 삽입
L.tileLayer("https://tiles.osm.kr/hot/{z}/{x}/{y}.png", {
  maxZoom: 19,
  attribution: '&copy; <a href="https://openstreetmap.org/copyright">OpenStreetMap 기여자</a>',
}).addTo(map);

// 축척 막대를 지도 왼쪽 하단에 노출
L.control.scale({ imperial: true, metric: true }).addTo(map);

// 마커를 지도에 추가
L.marker({ lon: 127.766, lat: 36.355 }).bindPopup("대한민국의 중심지, 장연리마을").addTo(map);
