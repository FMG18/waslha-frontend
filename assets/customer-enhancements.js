/* Waslha customer UX enhancements: safe, additive, no map replacement. */
(function(){
  'use strict';
  const isCustomer=/\/customer\.html(?:$|\?)/.test(location.pathname);
  if(!isCustomer)return;
  const $=id=>document.getElementById(id);
  function addClass(el,c,on){if(el)el.classList.toggle(c,on)}
  function loadAsset(type,src){return new Promise((resolve,reject)=>{const el=document.createElement(type==='css'?'link':'script');if(type==='css'){el.rel='stylesheet';el.href=src}else{el.src=src;el.defer=true}el.onload=resolve;el.onerror=reject;document.head.appendChild(el)})}
  async function setupRating(){try{await loadAsset('css','assets/rating-sheet.css?v=2026.09.01');await loadAsset('js','assets/rating-sheet.js?v=2026.09.01')}catch(e){console.warn('Waslha rating UI unavailable',e)}}
  function watchCompletedRide(){const rideId=localStorage.getItem('waslha_ride');if(!rideId||!$('status')||typeof api!=='function')return;let busy=false;const check=async()=>{if(busy)return;busy=true;try{const d=await api('/rides/'+encodeURIComponent(rideId));const r=d?.data?.ride||d?.data;if(!r)return;$('status').innerHTML='<b>'+statusLabel(r.status)+'</b>'+(r.captain?.user?.name?'<br><small>الكابتن: '+r.captain.user.name+'</small>':'');if(r.status==='trip_completed'){const key='waslha_rating_pending_'+rideId;if(!localStorage.getItem(key)){localStorage.setItem(key,'1');if(window.WaslhaRating?.show)window.WaslhaRating.show(r)}}else if(r.status==='cancelled'){localStorage.removeItem('waslha_ride')}}catch{}finally{busy=false}};check();setInterval(check,5000)}
  async function fallbackRideRequest(request){
    const state=window.__waslhaCustomerState;
    if(!state||typeof api!=='function')return;
    const mapApi=state.mapApi;
    const points=mapApi?.getPoints?.()||{};
    const pickup=state.pickup||points.pickup;
    const dropoff=state.dropoff||points.dropoff;
    if(!pickup||!dropoff){toast('حدد الانطلاق والوجهة أولاً');return}
    try{
      setBusy(request,true,'جارٍ إرسال الطلب...');
      const d=await api('/rides',{method:'POST',body:JSON.stringify({serviceType:state.service||'taxi',pickup:{lat:+pickup[1],lng:+pickup[0]},dropoff:{lat:+dropoff[1],lng:+dropoff[0]},pickupAddress:$('pickupAddress')?.value||'',dropoffAddress:$('dropoffAddress')?.value||'',couponCode:localStorage.getItem('waslha_coupon')||undefined})});
      const ride=d?.data?.ride||d?.data||d;
      if(!ride?._id)throw Error('لم يصل رقم الرحلة من الخادم');
      localStorage.setItem('waslha_ride',ride._id);
      if($('status'))$('status').innerHTML='<b>'+statusLabel(ride.status)+'</b>';
      $('cancelBtn')?.classList.remove('hidden');
      toast('تم إرسال طلب الرحلة');
    }catch(e){toast(e?.message||'تعذر إرسال طلب الرحلة')}finally{setBusy(request,false)}
  }
  function installRideFallback(request){
    if(!request||request.dataset.fallbackBound==='1')return;
    let tries=0;
    const check=()=>{
      if(request.onclick)return;
      if(tries++<30){setTimeout(check,100);return}
      request.dataset.fallbackBound='1';
      request.addEventListener('click',()=>fallbackRideRequest(request));
    };
    check();
  }
  function setup(){
    const request=$('requestBtn'), locate=$('locateBtn'), reset=$('resetMapBtn');
    if(!request)return;
    request.setAttribute('aria-live','polite');
    installRideFallback(request);
    locate?.addEventListener('click',()=>{ addClass(locate,'is-busy',true); setTimeout(()=>addClass(locate,'is-busy',false),1200); });
    reset?.addEventListener('click',()=>{ request.disabled=true; request.textContent='حدد الوجهة أولاً'; });
    document.querySelectorAll('.service-option').forEach(btn=>btn.addEventListener('keydown',e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();btn.click()}}));
    const search=$('placeSearch');
    search?.addEventListener('keydown',e=>{if(e.key==='Escape'){$('clearSearch')?.click();search.blur()}});
    window.addEventListener('pageshow',()=>{ if($('rideMap')?.querySelector('.maplibregl-map')) window.dispatchEvent(new Event('resize')); });
    const rideId=localStorage.getItem('waslha_ride');
    if(rideId && $('status') && !$('status').dataset.restored){
      $('status').dataset.restored='1';
      api('/rides/'+encodeURIComponent(rideId)).then(d=>{
        const r=d?.data?.ride||d?.data;
        if(!r)return;
        $('status').innerHTML='<b>'+statusLabel(r.status)+'</b>'+(r.captain?.user?.name?'<br><small>الكابتن: '+r.captain.user.name+'</small>':'');
        if(!['trip_completed','completed','cancelled'].includes(r.status)) addClass($('cancelBtn'),'hidden',false);
      }).catch(()=>{});
    }
    setupRating();
    watchCompletedRide();
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',setup,{once:true});else setup();
})();