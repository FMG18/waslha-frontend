/* Waslha Map — production location picker: stable points, explicit modes, no accidental recentering. */
(function(){
  'use strict';
  let accessToken=window.WASLHA_MAPBOX_TOKEN||window.MAPBOX_ACCESS_TOKEN||window.mapboxToken||'';
  const diag=(stage,msg,extra)=>{try{window.WaslhaMapDiagnostic?.({stage,message:String(msg||''),detail:extra?String(extra):''})}catch{}};
  const validPoint=p=>Array.isArray(p)&&p.length>=2&&Number.isFinite(Number(p[0]))&&Number.isFinite(Number(p[1]))&&Math.abs(Number(p[0]))<=180&&Math.abs(Number(p[1]))<=90;
  const norm=p=>validPoint(p)?[Number(p[0]),Number(p[1])]:null;

  async function getToken(){
    if(accessToken)return accessToken;
    try{const r=await fetch('/api/mapbox-token',{cache:'no-store'});if(r.ok){const d=await r.json();accessToken=d?.data?.token||d?.token||''}else diag('token',`HTTP ${r.status}`)}catch(e){diag('token','تعذر الاتصال بالخادم',e?.message)}
    return accessToken;
  }
  async function load(){
    const token=await getToken();
    if(!token)throw Error('تعذر الحصول على Mapbox token من الخادم');
    if(window.mapboxgl){mapboxgl.accessToken=token;return token}
    await new Promise((resolve,reject)=>{
      const old=document.querySelector('script[data-waslha-mapbox]');
      if(old){old.addEventListener('load',resolve,{once:true});old.addEventListener('error',()=>reject(Error('فشل تحميل Mapbox')),{once:true});return}
      const s=document.createElement('script');s.dataset.waslhaMapbox='1';s.src='https://api.mapbox.com/mapbox-gl-js/v3.15.0/mapbox-gl.js';s.onload=resolve;s.onerror=()=>reject(Error('فشل تحميل مكتبة Mapbox'));document.head.appendChild(s);
      const c=document.createElement('link');c.rel='stylesheet';c.href='https://api.mapbox.com/mapbox-gl-js/v3.15.0/mapbox-gl.css';document.head.appendChild(c);
    });
    mapboxgl.accessToken=token;return token;
  }
  async function directions(a,b){
    a=norm(a);b=norm(b);const token=await getToken();if(!a||!b||!token)return null;
    const u=`https://api.mapbox.com/directions/v5/mapbox/driving/${a[0]},${a[1]};${b[0]},${b[1]}?alternatives=false&geometries=geojson&overview=full&steps=false&access_token=${encodeURIComponent(token)}`;
    const r=await fetch(u);if(!r.ok)throw Error('تعذر حساب مسار الطريق');
    const d=await r.json(),route=d.routes?.[0];
    return route?{coordinates:route.geometry.coordinates,distanceKm:route.distance/1000,durationMinutes:Math.max(1,Math.round(route.duration/60000))}:null;
  }

  async function create(el,o={}){
    if(!el)return null;
    try{
      await load();
      const map=new mapboxgl.Map({container:el,style:o.style||'mapbox://styles/mapbox/streets-v12',center:o.center||[44.3661,33.3152],zoom:o.zoom||13,dragRotate:false,touchPitch:false,attributionControl:true});
      map.addControl(new mapboxgl.NavigationControl({showCompass:false}),'top-left');

      let pickup=norm(o.pickup)||((Number.isFinite(Number(o.pickupLng))&&Number.isFinite(Number(o.pickupLat)))?[+o.pickupLng,+o.pickupLat]:null);
      let dropoff=norm(o.dropoff)||((Number.isFinite(Number(o.dropLng))&&Number.isFinite(Number(o.dropLat)))?[+o.dropLng,+o.dropLat]:null);
      let mode=dropoff?'locked':pickup?'dropoff':(o.selectionMode||'pickup');
      let pm=null,dm=null,cm=null,routeSeq=0,animFrame=0;
      const sourceId='waslha-route';

      // A center pin is visual only. It NEVER owns or mutates the selected coordinates.
      const pin=document.createElement('div');
      pin.className='waslha-map-center-pin';
      pin.innerHTML='<div class="waslha-map-pin-dot"></div><div class="waslha-map-pin-shadow"></div>';
      Object.assign(pin.style,{position:'absolute',left:'50%',top:'50%',transform:'translate(-50%,-100%)',zIndex:'20',pointerEvents:'none',display:'none'});
      el.style.position=el.style.position||'relative';el.appendChild(pin);

      function showCenterPin(show){pin.style.display=show?'block':'none'}
      function marker(kind,p){return new mapboxgl.Marker({color:kind==='pickup'?'#0f9f6e':'#2563eb'}).setLngLat(p).setPopup(new mapboxgl.Popup({offset:20}).setText(kind==='pickup'?'نقطة الانطلاق':'الوجهة')).addTo(map)}
      function renderMarkers(){pm?.remove();dm?.remove();pm=pickup?marker('pickup',pickup):null;dm=dropoff?marker('dropoff',dropoff):null}
      function setRoute(coords){
        if(!map.isStyleLoaded())return;
        const data={type:'Feature',geometry:{type:'LineString',coordinates:coords||[]}};
        if(map.getSource(sourceId))map.getSource(sourceId).setData(data);
        else{map.addSource(sourceId,{type:'geojson',data});map.addLayer({id:sourceId,type:'line',source:sourceId,paint:{'line-color':'#0f9f6e','line-width':5,'line-opacity':.82,'line-cap':'round','line-join':'round'}})}
      }
      async function route(a,b){const n=++routeSeq;try{const x=await directions(a,b);if(n!==routeSeq||!x)return null;setRoute(x.coordinates);o.onRoute?.(x);return x}catch(e){o.onRouteError?.(e);return null}}
      function emit(kind,p){o.onMapClick?.(p,kind,{pickup,dropoff,mode})}
      function choose(point){
        // Selection is a finite-state machine. Once locked, map clicks cannot mutate anything.
        const p=norm(point);if(!p||mode==='locked')return false;
        if(mode==='pickup'){pickup=p;mode='dropoff';renderMarkers();showCenterPin(true);emit('pickup',p);diag('selection','تم تثبيت نقطة الانطلاق');return true}
        if(mode==='dropoff'){dropoff=p;mode='locked';renderMarkers();showCenterPin(false);emit('dropoff',p);diag('selection','تم تثبيت نقطة الوصول');route(pickup,dropoff);return true}
        return false;
      }
      function chooseCenter(){return choose([map.getCenter().lng,map.getCenter().lat])}
      function setMode(next){
        next=next==='pickup'||next==='dropoff'||next==='locked'?next:'locked';
        mode=next;
        if(next==='pickup'){pickup=null;dropoff=null;routeSeq++;setRoute([])}
        if(next==='dropoff'){dropoff=null;routeSeq++;if(pickup)map.setCenter(pickup)}
        if(next==='locked')showCenterPin(false);else showCenterPin(true);
        renderMarkers();o.onSelectionModeChange?.(mode);return mode;
      }
      function setPoints(a,b,fit=false){
        const A=norm(a),B=norm(b);
        // Programmatic updates are atomic. Partial updates never overwrite an existing point.
        if(A&&B){pickup=A;dropoff=B;mode='locked';renderMarkers();showCenterPin(false);route(A,B);return{pickup,dropoff}}
        if(A&&!B){pickup=A;mode='dropoff';showCenterPin(true);renderMarkers();return{pickup,dropoff}}
        if(!A&&!B){pickup=null;dropoff=null;mode='pickup';showCenterPin(true);renderMarkers();return{pickup,dropoff}}
        if(fit&&A&&B){const bounds=new mapboxgl.LngLatBounds(A,A);bounds.extend(B);map.fitBounds(bounds,{padding:70,maxZoom:15})}
        return{pickup,dropoff};
      }
      function animateDriver(to,duration=900){
        to=norm(to);if(!to)return;
        if(!cm){cm=new mapboxgl.Marker({color:'#ef4444'}).setLngLat(to).setPopup(new mapboxgl.Popup({offset:20}).setText('الكابتن')).addTo(map);return}
        cancelAnimationFrame(animFrame);const from=cm.getLngLat(),start=performance.now();
        const step=now=>{const t=Math.min(1,(now-start)/duration),e=t<.5?2*t*t:1-Math.pow(-2*t+2,2)/2;cm.setLngLat([from.lng+(to[0]-from.lng)*e,from.lat+(to[1]-from.lat)*e]);if(t<1)animFrame=requestAnimationFrame(step)};animFrame=requestAnimationFrame(step);
      }

      map.on('load',()=>{
        renderMarkers();
        if(pickup&&dropoff){mode='locked';showCenterPin(false);route(pickup,dropoff)}
        else {showCenterPin(true);if(pickup)mode='dropoff'}
        o.onReady?.({pickup,dropoff,mode});
      });
      map.on('click',e=>{if(o.interactive===false)return;choose([e.lngLat.lng,e.lngLat.lat])});
      map.on('error',e=>diag('map-error','Mapbox error',e?.error?.message||e?.error?.status||'unknown'));

      return {
        map,
        getPoints:()=>({pickup:pickup?[...pickup]:null,dropoff:dropoff?[...dropoff]:null,mode}),
        choose,
        chooseCenter,
        setSelectionMode:setMode,
        lockSelection:()=>{mode='locked';showCenterPin(false);o.onSelectionModeChange?.(mode)},
        setPoints,
        setDriver(lat,lng){animateDriver([+lng,+lat]);if(o.followDriver)map.easeTo({center:[+lng,+lat],duration:600});return cm},
        setRoute,route,
        clearRoute(){routeSeq++;setRoute([])},
        destroy(){routeSeq++;cancelAnimationFrame(animFrame);pm?.remove();dm?.remove();cm?.remove();pin.remove();map.remove()}
      };
    }catch(e){
      diag('fatal','فشل تشغيل الخريطة',e?.message||e);el.innerHTML='<div class="map-empty"><b>تعذر تحميل الخريطة</b><span>'+String(e.message||e)+'</span></div>';return null;
    }
  }
  window.WaslhaMap={init:o=>create(typeof o.container==='string'?document.getElementById(o.container):o.container,o),mount:(el,o={})=>create(el,o),directions};
})();