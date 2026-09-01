/* Waslha Maps v7 — resilient keyless MapLibre map with CDN + OSM raster fallback. */
(function(){
'use strict';
const norm=p=>Array.isArray(p)&&p.length>=2&&Number.isFinite(+p[0])&&Number.isFinite(+p[1])?[+p[0],+p[1]]:null;
const diag=(stage,message,detail)=>{try{window.WaslhaMapDiagnostic?.({stage,message:String(message||''),detail:String(detail||'')})}catch{}};

const LIBERTY='https://tiles.openfreemap.org/styles/liberty';
const FALLBACK_STYLE={version:8,sources:{osm:{type:'raster',tiles:['https://tile.openstreetmap.org/{z}/{x}/{y}.png'],tileSize:256,attribution:'© OpenStreetMap contributors'}},layers:[{id:'osm',type:'raster',source:'osm'}]};

async function loadLib(){
  if(window.maplibregl)return window.maplibregl;
  const css='https://unpkg.com/maplibre-gl@6.6.0/dist/maplibre-gl.css';
  if(!document.querySelector('link[data-waslha-maplibre]')){
    const l=document.createElement('link');l.rel='stylesheet';l.href=css;l.dataset.waslhaMaplibre='1';document.head.appendChild(l);
  }
  for(const src of [
    'https://unpkg.com/maplibre-gl@6.6.0/dist/maplibre-gl.mjs',
    'https://cdn.jsdelivr.net/npm/maplibre-gl@6.6.0/dist/maplibre-gl.mjs'
  ]){
    try{
      const mod=await import(src);
      const lib=mod?.default||mod;
      if(lib?.Map){window.maplibregl=lib;return lib}
    }catch(e){diag('library','تعذر تحميل MapLibre',src)}
  }
  throw Error('تعذر تحميل مكتبة الخريطة');
}

async function reverseGeocode(lat,lon){
  try{
    const r=await fetch(`https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${encodeURIComponent(lat)}&lon=${encodeURIComponent(lon)}&accept-language=ar`,{headers:{Accept:'application/json'}});
    if(r.ok){const d=await r.json();return{formatted:d.display_name||'',address_line1:d.display_name||'',raw:d}}
  }catch(e){diag('geocoding','تعذر تحديد العنوان',e.message)}
  return null;
}

async function autocomplete(text,proximity){
  text=String(text||'').trim();if(text.length<2)return[];
  try{
    const q=new URLSearchParams({q:text,format:'jsonv2',limit:'6',accept-language:'ar',addressdetails:'1'});
    if(proximity){q.set('viewbox',`${proximity[0]-0.35},${proximity[1]+0.35},${proximity[0]+0.35},${proximity[1]-0.35}`);q.set('bounded','0')}
    const r=await fetch(`https://nominatim.openstreetmap.org/search?${q}`,{headers:{Accept:'application/json'}});
    if(r.ok)return(await r.json()).map(x=>({lat:+x.lat,lon:+x.lon,formatted:x.display_name||'',address_line1:x.display_name||'',raw:x}));
  }catch(e){diag('geocoding','تعذر البحث عن المكان',e.message)}
  return[];
}

async function directions(a,b,mode='drive'){
  a=norm(a);b=norm(b);if(!a||!b)return null;
  try{
    const profile=mode==='walk'?'foot':mode==='bicycle'?'bike':'car';
    const r=await fetch(`https://router.project-osrm.org/route/v1/${profile}/${a[0]},${a[1]};${b[0]},${b[1]}?overview=full&geometries=geojson&steps=false`,{headers:{Accept:'application/json'}});
    if(r.ok){const d=await r.json(),x=d.routes?.[0];if(x)return{coordinates:x.geometry?.coordinates||[],distanceKm:(x.distance||0)/1000,durationMinutes:Math.max(1,Math.round((x.duration||0)/60)),raw:x}}
  }catch(e){diag('routing','تعذر حساب الطريق',e.message)}
  return null;
}

async function create(el,o={}){
  if(!el)return null;
  try{
    const maplibregl=await loadLib();
    el.style.minHeight=el.style.minHeight||'320px';
    let usingFallback=false;
    const map=new maplibregl.Map({container:el,style:o.style||LIBERTY,center:o.center||[44.3661,33.3152],zoom:o.zoom||12,dragRotate:false,pitchWithRotate:false,attributionControl:true});
    map.addControl(new maplibregl.NavigationControl({showCompass:false}),'top-left');

    let styleReady=false, fallbackTimer=null;
    const activateFallback=()=>{
      if(styleReady||usingFallback)return;
      usingFallback=true;diag('style','الخريطة الأساسية لم تستجب، تم تشغيل خريطة OSM البديلة');
      try{map.setStyle(FALLBACK_STYLE)}catch(e){diag('style','تعذر تشغيل الخريطة البديلة',e.message)}
    };
    map.once('style.load',()=>{styleReady=true;if(fallbackTimer)clearTimeout(fallbackTimer)});
    fallbackTimer=setTimeout(activateFallback,7000);

    let pickup=norm(o.pickup),dropoff=norm(o.dropoff),mode=dropoff?'locked':pickup?'dropoff':(o.selectionMode||'pickup'),pm=null,dm=null,driver=null,routeSeq=0;
    const routeId='waslha-route';
    const pin=document.createElement('div');pin.className='waslha-center-pin';pin.innerHTML='<span></span>';Object.assign(pin.style,{position:'absolute',zIndex:8,left:'50%',top:'50%',transform:'translate(-50%,-100%)',pointerEvents:'none'});el.appendChild(pin);
    const centerPin=x=>pin.style.display=x?'block':'none';
    const marker=(type,p)=>{const x=document.createElement('div');x.className=`waslha-marker ${type}`;x.innerHTML=type==='pickup'?'●':'◆';return new maplibregl.Marker({element:x,anchor:'bottom'}).setLngLat(p).addTo(map)};
    function render(){pm?.remove();dm?.remove();pm=pickup?marker('pickup',pickup):null;dm=dropoff?marker('dropoff',dropoff):null}
    function draw(coords){
      if(!styleReady||!map.isStyleLoaded())return;
      const valid=Array.isArray(coords)&&coords.length>1?coords:[];
      const data={type:'FeatureCollection',features:valid.length?[{type:'Feature',geometry:{type:'LineString',coordinates:valid},properties:{}}]:[]};
      const s=map.getSource(routeId);
      if(s)s.setData(data);else{map.addSource(routeId,{type:'geojson',data});map.addLayer({id:routeId,type:'line',source:routeId,paint:{'line-color':'#0f9f6e','line-width':6,'line-opacity':.88,'line-cap':'round','line-join':'round'}})}
    }
    async function route(){
      const seq=++routeSeq;
      if(!pickup||!dropoff){draw([]);o.onRoute?.(null);return null}
      try{
        const r=await directions(pickup,dropoff,o.routingMode||'drive');if(seq!==routeSeq)return null;draw(r?.coordinates||[]);o.onRoute?.(r);
        if(r?.coordinates?.length>1)try{const bounds=r.coordinates.reduce((b,p)=>b.extend(p),new maplibregl.LngLatBounds(r.coordinates[0],r.coordinates[0]));map.fitBounds(bounds,{padding:{top:80,bottom:180,left:40,right:40},maxZoom:15,duration:500})}catch{}
        if(!r)o.onRouteError?.(Error('تعذر العثور على مسار بين النقطتين'));return r;
      }catch(e){o.onRouteError?.(e);diag('routing','تعذر حساب الطريق',e.message);return null}
    }
    async function choose(p){
      p=norm(p);if(!p||mode==='locked')return false;
      if(mode==='pickup'){pickup=p;dropoff=null;mode='dropoff';render();centerPin(true);o.onMapClick?.(p,'pickup',{pickup,dropoff,mode});o.onSelectionModeChange?.(mode);try{o.onAddress?.('pickup',await reverseGeocode(p[1],p[0]))}catch{}return true}
      if(mode==='dropoff'){dropoff=p;mode='locked';render();centerPin(false);o.onMapClick?.(p,'dropoff',{pickup,dropoff,mode});o.onSelectionModeChange?.(mode);route();try{o.onAddress?.('dropoff',await reverseGeocode(p[1],p[0]))}catch{}return true}
      return false;
    }
    function setMode(next){mode=next==='pickup'||next==='dropoff'||next==='locked'?next:'locked';if(mode==='pickup'){pickup=null;dropoff=null;routeSeq++;draw([])}if(mode==='dropoff')dropoff=null;centerPin(mode!=='locked');render();o.onSelectionModeChange?.(mode);return mode}
    function setPoints(a,b){const A=norm(a),B=norm(b);if(A&&B){pickup=A;dropoff=B;mode='locked';centerPin(false);render();route()}else if(A){pickup=A;dropoff=null;mode='dropoff';centerPin(true);render()}else{pickup=null;dropoff=null;mode='pickup';centerPin(true);render()}return{pickup,dropoff,mode}}

    map.once('load',()=>{document.getElementById('mapCover')?.classList.add('hidden');render();centerPin(mode!=='locked');if(pickup&&dropoff)route();o.onReady?.({pickup,dropoff,mode});setTimeout(()=>map.resize(),100);setTimeout(()=>map.resize(),600)});
    map.on('style.load',()=>{styleReady=true;render();if(pickup&&dropoff)route();map.resize()});
    map.on('click',e=>{if(o.interactive!==false)choose([e.lngLat.lng,e.lngLat.lat])});
    map.on('error',e=>{const msg=e?.error?.message||'';diag('map','MapLibre error',msg);if(!styleReady&&!usingFallback)setTimeout(activateFallback,300)});

    return{map,getPoints:()=>({pickup:pickup?[...pickup]:null,dropoff:dropoff?[...dropoff]:null,mode}),choose,setSelectionMode:setMode,setPoints,chooseCenter:()=>choose([map.getCenter().lng,map.getCenter().lat]),route,directions,autocomplete,reverseGeocode,setDriver(lat,lng){const p=[+lng,+lat];if(!Number.isFinite(p[0])||!Number.isFinite(p[1]))return;if(!driver){const x=document.createElement('div');x.className='waslha-driver-marker';x.textContent='🚗';driver=new maplibregl.Marker({element:x,anchor:'center'}).setLngLat(p).addTo(map)}else driver.setLngLat(p);if(o.followDriver)map.easeTo({center:p,duration:500})},clearRoute(){routeSeq++;draw([])},destroy(){routeSeq++;if(fallbackTimer)clearTimeout(fallbackTimer);pm?.remove();dm?.remove();driver?.remove();pin.remove();map.remove()}};
  }catch(e){diag('fatal','فشل تشغيل الخريطة',e.message);el.innerHTML=`<div class="map-empty"><b>تعذر تشغيل الخريطة</b><span>${String(e.message||e)}</span></div>`;return null}
}
window.WaslhaMap={init:o=>create(typeof o.container==='string'?document.getElementById(o.container):o.container,o),mount:(el,o={})=>create(el,o),autocomplete,reverseGeocode,directions};
})();
