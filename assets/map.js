/* Waslha Mapbox UI: tap-to-place points, real driver marker, Directions route and smooth movement. */
(function(){
 const token=window.WASLHA_MAPBOX_TOKEN||window.MAPBOX_ACCESS_TOKEN||window.mapboxToken||'';
 async function load(){
  if(window.mapboxgl)return;
  if(!token)throw Error('أضف MAPBOX_ACCESS_TOKEN في إعدادات Vercel لتفعيل الخريطة');
  mapboxgl.accessToken=token;
  await new Promise((resolve,reject)=>{
   if(document.querySelector('script[data-waslha-mapbox]'))return document.querySelector('script[data-waslha-mapbox]').addEventListener('load',resolve,{once:true});
   const s=document.createElement('script');s.dataset.waslhaMapbox='1';s.src='https://api.mapbox.com/mapbox-gl-js/v3.15.0/mapbox-gl.js';s.onload=resolve;s.onerror=reject;document.head.appendChild(s);
   const c=document.createElement('link');c.rel='stylesheet';c.href='https://api.mapbox.com/mapbox-gl-js/v3.15.0/mapbox-gl.css';document.head.appendChild(c);
  });
 }
 async function directions(a,b){
  if(!a||!b||!token)return null;
  const u=`https://api.mapbox.com/directions/v5/mapbox/driving/${a[0]},${a[1]};${b[0]},${b[1]}?alternatives=false&geometries=geojson&overview=full&steps=false&access_token=${encodeURIComponent(token)}`;
  const r=await fetch(u);if(!r.ok)throw Error('تعذر حساب مسار الطريق');
  const d=await r.json();const route=d.routes?.[0];return route?{coordinates:route.geometry.coordinates,distanceKm:route.distance/1000,durationMinutes:Math.max(1,Math.round(route.duration/60000))}:null;
 }
 async function create(el,o={}){
  if(!el)return null;
  if(!token){el.innerHTML='<div class="map-empty"><b>الخريطة جاهزة</b><span>أضف MAPBOX_ACCESS_TOKEN في إعدادات Vercel لتفعيل Mapbox.</span></div>';return null}
  try{
   await load();
   const map=new mapboxgl.Map({container:el,style:o.style||'mapbox://styles/mapbox/streets-v12',center:o.center||[44.3661,33.3152],zoom:o.zoom||12});
   map.addControl(new mapboxgl.NavigationControl({showCompass:false}),'top-left');
   let pm,dm,cm,animFrame=0,routeSeq=0;
   const sourceId='waslha-route';
   function marker(kind,p){const color=kind==='pickup'?'#0f9f6e':'#2563eb';const label=kind==='pickup'?'نقطة الانطلاق':'الوجهة';return new mapboxgl.Marker({color}).setLngLat(p).setPopup(new mapboxgl.Popup({offset:20}).setText(label)).addTo(map)}
   function points(p,d,fit=true){pm?.remove();dm?.remove();pm=p?marker('pickup',p):null;dm=d?marker('dropoff',d):null;if(p&&d&&fit){const b=new mapboxgl.LngLatBounds(p,p);b.extend(d);map.fitBounds(b,{padding:70,maxZoom:15})}else if(p&&fit)map.flyTo({center:p,zoom:15})}
   function setRoute(coords){if(!map.isStyleLoaded())return;const data={type:'Feature',geometry:{type:'LineString',coordinates:coords||[]}};if(map.getSource(sourceId))map.getSource(sourceId).setData(data);else{map.addSource(sourceId,{type:'geojson',data});map.addLayer({id:sourceId,type:'line',source:sourceId,paint:{'line-color':'#0f9f6e','line-width':5,'line-opacity':.82,'line-cap':'round','line-join':'round'}})}}
   async function route(a,b){const n=++routeSeq;try{const x=await directions(a,b);if(n!==routeSeq||!x)return null;setRoute(x.coordinates);o.onRoute?.(x);return x}catch(e){return null}}
   function animateDriver(to,duration=1000){
    if(!cm){cm=new mapboxgl.Marker({color:'#ef4444'}).setLngLat(to).setPopup(new mapboxgl.Popup({offset:20}).setText('الكابتن')).addTo(map);return}
    cancelAnimationFrame(animFrame);const from=cm.getLngLat();const start=performance.now();
    const step=now=>{const t=Math.min(1,(now-start)/duration);const ease=t<.5?2*t*t:1-Math.pow(-2*t+2,2)/2;cm.setLngLat([from.lng+(to[0]-from.lng)*ease,from.lat+(to[1]-from.lat)*ease]);if(t<1)animFrame=requestAnimationFrame(step)};animFrame=requestAnimationFrame(step);
   }
   map.on('load',()=>{
    const p=o.pickupLat&&o.pickupLng?[Number(o.pickupLng),Number(o.pickupLat)]:null,d=o.dropLat&&o.dropLng?[Number(o.dropLng),Number(o.dropLat)]:null;points(p,d);
    if(p&&d)route(p,d);
    if(o.interactive!==false)map.on('click',e=>o.onMapClick?.([e.lngLat.lng,e.lngLat.lat]));
   });
   return {map,setPoints(pointsA,pointsB){points(pointsA,pointsB);if(pointsA&&pointsB)route(pointsA,pointsB)},setDriver(lat,lng){const p=[Number(lng),Number(lat)];if(!Number.isFinite(p[0])||!Number.isFinite(p[1]))return null;animateDriver(p);if(o.followDriver)map.easeTo({center:p,duration:600});return cm},setRoute,route,clearRoute(){routeSeq++;if(map.getSource(sourceId))map.getSource(sourceId).setData({type:'Feature',geometry:{type:'LineString',coordinates:[]}})},destroy(){routeSeq++;cancelAnimationFrame(animFrame);map.remove()}};
  }catch(e){el.innerHTML='<div class="map-empty"><b>تعذر تحميل الخريطة</b><span>'+e.message+'</span></div>';return null}
 }
 async function init(o={}){const el=typeof o.container==='string'?document.getElementById(o.container):o.container;return create(el,o)}
 async function mount(el,o={}){return create(el,o)}
 window.WaslhaMap={init,mount,directions};
})();