/* Waslha customer UX enhancements: safe, additive, no map replacement. */
(function(){
  'use strict';
  const isCustomer=/\/customer\.html(?:$|\?)/.test(location.pathname);
  if(!isCustomer)return;
  const $=id=>document.getElementById(id);
  function addClass(el,c,on){if(el)el.classList.toggle(c,on)}
  function setup(){
    const request=$('requestBtn'), locate=$('locateBtn'), reset=$('resetMapBtn');
    if(!request)return;
    request.setAttribute('aria-live','polite');
    locate?.addEventListener('click',()=>{ addClass(locate,'is-busy',true); setTimeout(()=>addClass(locate,'is-busy',false),1200); });
    reset?.addEventListener('click',()=>{ request.disabled=true; request.textContent='حدد الوجهة أولاً'; });
    document.querySelectorAll('.service-option').forEach(btn=>btn.addEventListener('keydown',e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();btn.click()}}));
    const search=$('placeSearch');
    search?.addEventListener('keydown',e=>{if(e.key==='Escape'){$('clearSearch')?.click();search.blur()}});
    window.addEventListener('pageshow',()=>{ if($('rideMap')?.querySelector('.maplibregl-map')) window.dispatchEvent(new Event('resize')); });
    // Restore a pending ride indicator after refresh without creating a second ride controller.
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
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',setup,{once:true});else setup();
})();
