const API='https://waslha-backend.vercel.app/api';
const $=id=>document.getElementById(id); let token=localStorage.getItem('waslha_token');
function user(){try{return JSON.parse(localStorage.getItem('waslha_user')||'null')}catch{return null}}
function saveAuth(data){const p=data?.data||data;token=p?.token||p?.accessToken||null;if(token)localStorage.setItem('waslha_token',token);if(p?.user)localStorage.setItem('waslha_user',JSON.stringify(p.user))}
function logout(){['waslha_token','waslha_user','waslha_ride','captain_ride'].forEach(k=>localStorage.removeItem(k));location.href='index.html'}
async function api(path,options={}){const h={...(options.body instanceof FormData?{}:{'Content-Type':'application/json'}),...(options.headers||{})};if(token)h.Authorization='Bearer '+token;let r;try{r=await fetch(API+path,{...options,headers:h})}catch(e){throw Error('تعذر الاتصال بالخادم. تحقق من الإنترنت أو CORS.')}const d=await r.json().catch(()=>({}));if(r.status===401){localStorage.removeItem('waslha_token');token=null}if(!r.ok)throw Error(d?.error?.message||d?.message||d?.error||`خطأ ${r.status}`);return d}
function toast(m){const e=document.createElement('div');e.className='toast';e.textContent=m;document.body.appendChild(e);setTimeout(()=>e.remove(),3500)}
function setBusy(b,x,t='جارٍ التنفيذ...'){if(!b)return;b.disabled=x;if(x){b.dataset.old=b.textContent;b.textContent=t}else if(b.dataset.old)b.textContent=b.dataset.old}
function requireRole(role){const u=user();if(!token||!u||u.role!==role){location.href='index.html';return false}return true}
function statusLabel(s){return({requested:'تم إنشاء الطلب',searching:'جاري البحث عن كابتن',captain_assigned:'تم تعيين الكابتن',captain_arriving:'الكابتن في الطريق',captain_arrived:'الكابتن وصل',trip_started:'الرحلة بدأت',trip_completed:'اكتملت الرحلة',cancelled:'ملغاة'})[s]||s||'غير معروف'}
function money(v){return v==null?'—':`${Number(v).toLocaleString('ar-IQ')} د.ع`}
function coordsFromForm(ids){const v=ids.map(id=>Number($(id)?.value));if(v.some(x=>!Number.isFinite(x)))throw Error('أدخل إحداثيات صحيحة');return v}
function fillLocation(a,b){if(!navigator.geolocation)return toast('المتصفح لا يدعم تحديد الموقع');navigator.geolocation.getCurrentPosition(p=>{$(a).value=p.coords.latitude.toFixed(6);$(b).value=p.coords.longitude.toFixed(6);toast('تم تحديد موقعك')},()=>toast('السماح بالموقع مطلوب'))}
