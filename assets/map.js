/* Waslha Mapbox adapter. Set MAPBOX_ACCESS_TOKEN in Vercel or define window.WASLHA_MAPBOX_TOKEN before this script. */
(function(){
  const token=window.WASLHA_MAPBOX_TOKEN||window.MAPBOX_ACCESS_TOKEN||'';
  window.WaslhaMap={
    async mount(el,opts={}){
      if(!el)return null;
      if(!token){el.innerHTML='<div class="map-empty"><b>الخريطة جاهزة</b><span>أضف MAPBOX_ACCESS_TOKEN في Vercel لتفعيل Mapbox.</span></div>';return null}
      if(!window.mapboxgl){await new Promise((resolve,reject)=>{const s=document.createElement('script');s.src='https://api.mapbox.com/mapbox-gl-js/v3.15.0/mapbox-gl.js';s.onload=resolve;s.onerror=reject;document.head.appendChild(s);const c=document.createElement('link');c.rel='stylesheet';c.href='https://api.mapbox.com/mapbox-gl-js/v3.15.0/mapbox-gl.css';document.head.appendChild(c)})}
      mapboxgl.accessToken=token;
      const center=opts.center||[44.3661,33.3152];
      const map=new mapboxgl.Map({container:el,style:opts.style||'mapbox://styles/mapbox/streets-v12',center,zoom:opts.zoom||12,attributionControl:true});
      map.addControl(new mapboxgl.NavigationControl({showCompass:false}),'top-left');
      if(opts.locate!==false)map.addControl(new mapboxgl.GeolocateControl({positionOptions:{enableHighAccuracy:true},trackUserLocation:true,showUserHeading:true}),'top-left');
      const markers=[];
      const addMarker=(lngLat,type='pickup',popup)=>{const m=new mapboxgl.Marker({color:type==='pickup'?'#111827':'#0f9f6e'}).setLngLat(lngLat);if(popup)m.setPopup(new mapboxgl.Popup({offset:25}).setText(popup));m.addTo(map);markers.push(m);return m};
      const setPoints=(pickup,dropoff)=>{markers.forEach(m=>m.remove());markers.length=0;if(pickup)addMarker(pickup,'pickup','نقطة الانطلاق');if(dropoff)addMarker(dropoff,'dropoff','الوجهة');if(pickup&&dropoff){const b=new mapboxgl.LngLatBounds(pickup,pickup);b.extend(dropoff);map.fitBounds(b,{padding:70,maxZoom:15,duration:600})}};
      const setCaptain=(lngLat)=>{if(map._captainMarker)map._captainMarker.setLngLat(lngLat);else map._captainMarker=addMarker(lngLat,'dropoff','موقع الكابتن')};
      map.on('load',()=>{if(opts.onReady)opts.onReady({map,setPoints,setCaptain,addMarker})});
      return {map,setPoints,setCaptain,addMarker,destroy:()=>map.remove()};
    }
  };
})();