/* Waslha Maps v3 — Geoapify + MapLibre with resilient library loading and stable point selection. */
(function(){
  'use strict';
  let apiKey='';
  const norm=p=>Array.isArray(p)&&p.length>=2&&Number.isFinite(+p[0])&&Number.isFinite(+p[1])?[+p[0],+p[1]]:null;
  const diag=(stage,message,detail)=>{try{window.WaslhaMapDiagnostic?.({stage,message:String(message||''),detail:String(detail||'')})}catch{}};
  async function key(){
    if(apiKey)return apiKey;
    const r=await fetch('/api/geoapify-key',{cache:'no-store'});
    const d=await r.json().catch(()=>({}));
    if(!r.ok||!d.key)throw Error(d.error||'تعذر الحصول على مفتاح Geoapify');
    apiKey=d.key; return apiKey;
  }
  async function loadLib(){
    if(window.maplibregl)return;
    if(document.querySelector('link[data-waslha-maplibre-css]')===null){
      const css=document.createElement('link');css.rel='stylesheet';css.href='https://unpkg.com/maplibre-gl@4.7.1/dist/maplibre-gl.css';css.dataset.waslhaMaplibreCss='1';document.head.appendChild(css);
    }
    const urls=['https://unpkg.com/maplibre-gl@4.7.1/dist/maplibre-gl.js','https://cdn.jsdelivr.net/npm/maplibre-gl@4.7.1/dist/maplibre-gl.js'];
    for(const src of urls){
      if(window.maplibregl)return;
      try{await new Promise((resolve,reject)=>{const s=document.createElement('script');s.dataset.waslhaMaplibre='1';s.src=src;s.onload=resolve;s.onerror=()=>reject(Error('load failed'));document.head.appendChild(s)})}catch(e){diag('library','تعذر تحميل MapLibre من المصدر الأول',src)}
    }
    if(!window.maplibregl)throw Error('تعذر تحميل مكتبة الخريطة. تحقق من اتصال الإنترنت أو مانع الإعلانات.');
  }
  async function geoReverse(lat,lon){
    const k=await key();const u=`https://api.geoapify.com/v1/geocode/reverse?lat=${lat}&lon=${lon}&lang=ar&limit=1&apiKey=${encodeURIComponent(k)}`;
    const r=await fetch(u);if(!r.ok)throw Error('تعذر قراءة عنوان الموقع');const d=await r.json();return d.features?.[0]?.properties||null;
  }
  async function autocomplete(text,proximity){
    const k=await key();const q=new URLSearchParams({text,lang:'ar',limit:'6',format:'json',apiKey:k});
    if(proximity)q.set('bias',`proximity:${proximity[0]},${proximity[1]}`);
    const r=await fetch(`https://api.geoapify.com/v1/geocode/autocomplete?${q}`);if(!r.ok)throw Error('تعذر البحث عن المكان');const d=await r.json();return d.results||[];
  }
  async function directions(a,b,mode='drive'){
    a=norm(a);b=norm(b);if(!a||!b)return null;const k=await key();
    const q=new URLSearchParams({waypoints:`${a[1]},${a[0]}|${b[1]},${b[0]}`,mode,format:'geojson',apiKey:k});
    const r=await fetch(`https://api.geoapify.com/v1/routing?${q}`);if(!r.ok)throw Error('تعذر حساب الطريق');const d=await r.json();
    const f=d.features?.[0],p=f?.properties;return f?{coordinates:f.geometry.coordinates,distanceKm:(p.distance||0)/1000,durationMinutes:Math.max(1,Math.round((p.time||0)/60)),raw:p}:null;
  }
  async function create(el,o={}){
    if(!el)return null;
    try{
      await key();await loadLib();
      const map=new maplibregl.Map({container:el,style:`https://maps.geoapify.com/v1/styles/osm-bright/style.json?apiKey=${encodeURIComponent(apiKey)}`,center:o.center||[44.3661,33.3152],zoom:o.zoom||12,dragRotate:false,pitchWithRotate:false,attributionControl:true});
      map.addControl(new maplibregl.NavigationControl({showCompass:false}),'top-left');
      let pickup=norm(o.pickup),dropoff=norm(o.dropoff),mode=dropoff?'locked':pickup?'dropoff':(o.selectionMode||'pickup');
      let pm=null,dm=null,driver=null,routeSeq=0;const routeId='waslha-route';
      const pin=document.createElement('div');pin.className='waslha-center-pin';pin.innerHTML='<span></span>';el.appendChild(pin);
      const centerPin=show=>pin.style.display=show?'block':'none';
      const marker=(type,p)=>{const x=document.createElement('div');x.className=`waslha-marker ${type}`;x.innerHTML=type==='pickup'?'●':'◆';return new maplibregl.Marker({element:x,anchor:'bottom'}).setLngLat(p).addTo(map)};
      function render(){pm?.remove();dm?.remove();pm=pickup?marker('pickup',pickup):null;dm=dropoff?marker('dropoff',dropoff):null}
      function draw(coords){if(!map.isStyleLoaded())return;const data={type:'Feature',geometry:{type:'LineString',coordinates:coords||[]}};const s=map.getSource(routeId);if(s)s.setData(data);else{map.addSource(routeId,{type:'geojson',data});map.addLayer({id:routeId,type:'line',source:routeId,paint:{'line-color':'#0f9f6e','line-width':6,'line-opacity':.88,'line-cap':'round','line-join':'round'}})}}
      async function route(){const seq=++routeSeq;if(!pickup||!dropoff){draw([]);o.onRoute?.(null);return null}try{const r=await directions(pickup,dropoff,o.routingMode||'drive');if(seq!==routeSeq)return null;draw(r?.coordinates||[]);o.onRoute?.(r);return r}catch(e){o.onRouteError?.(e);diag('routing','تعذر حساب الطريق',e.message);return null}}
      async function choose(p){p=norm(p);if(!p||mode==='locked')return false;if(mode==='pickup'){pickup=p;dropoff=null;mode='dropoff';render();centerPin(true);o.onMapClick?.(p,'pickup',{pickup,dropoff,mode});o.onSelectionModeChange?.(mode);try{o.onAddress?.('pickup',await geoReverse(p[1],p[0]))}catch(e){diag('reverse','تعذر العنوان',e.message)}return true}if(mode==='dropoff'){dropoff=p;mode='locked';render();centerPin(false);o.onMapClick?.(p,'dropoff',{pickup,dropoff,mode});o.onSelectionModeChange?.(mode);try{o.onAddress?.('dropoff',await geoReverse(p[1],p[0]))}catch(e){diag('reverse','تعذر العنوان',e.message)}route();return true}return false}
      function setMode(next){mode=next==='pickup'||next==='dropoff'||next==='locked'?next:'locked';if(mode==='pickup'){pickup=null;dropoff=null;routeSeq++;draw([])}if(mode==='dropoff')dropoff=null;centerPin(mode!=='locked');render();o.onSelectionModeChange?.(mode);return mode}
      function setPoints(a,b){const A=norm(a),B=norm(b);if(A&&B){pickup=A;dropoff=B;mode='locked';centerPin(false);render();route()}else if(A){pickup=A;dropoff=null;mode='dropoff';centerPin(true);render()}else{pickup=null;dropoff=null;mode='pickup';centerPin(true);render()}return{pickup,dropoff,mode}}
      map.on('load',()=>{render();centerPin(mode!=='locked');if(pickup&&dropoff)route();o.onReady?.({pickup,dropoff,mode})});
      map.on('click',e=>{if(o.interactive!==false)choose([e.lngLat.lng,e.lngLat.lat])});
      map.on('error',e=>diag('map','Geoapify/MapLibre error',e?.error?.message));
      return {map,getPoints:()=>({pickup:pickup?[...pickup]:null,dropoff:dropoff?[...dropoff]:null,mode}),choose,setSelectionMode:setMode,setPoints,chooseCenter:()=>choose([map.getCenter().lng,map.getCenter().lat]),route,directions,autocomplete,reverseGeocode:geoReverse,setDriver(lat,lng){const p=[+lng,+lat];if(!driver){const x=document.createElement('div');x.className='waslha-driver-marker';x.textContent='🚗';driver=new maplibregl.Marker({element:x,anchor:'center'}).setLngLat(p).addTo(map)}else driver.setLngLat(p);if(o.followDriver)map.easeTo({center:p,duration:500})},clearRoute(){routeSeq++;draw([])},destroy(){routeSeq++;pm?.remove();dm?.remove();driver?.remove();pin.remove();map.remove()}};
    }catch(e){diag('fatal','فشل تشغيل الخريطة',e?.message);el.innerHTML=`<div class="map-empty"><b>تعذر تشغيل الخريطة</b><span>${String(e.message||e)}</span></div>`;return null}
  }
  window.WaslhaMap={init:o=>create(typeof o.container==='string'?document.getElementById(o.container):o.container,o),mount:(el,o={})=>create(el,o),autocomplete,reverseGeocode:geoReverse,directions};
})();