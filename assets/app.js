const API='https://waslha-backend.vercel.app/api';
const $=id=>document.getElementById(id);
let token=localStorage.getItem('waslha_token');
function user(){try{return JSON.parse(localStorage.getItem('waslha_user')||'null')}catch{return null}}
function saveAuth(data){token=data.token;localStorage.setItem('waslha_token',token);localStorage.setItem('waslha_user',JSON.stringify(data.user||{}))}
function logout(){localStorage.removeItem('waslha_token');localStorage.removeItem('waslha_user');localStorage.removeItem('waslha_ride');location.href='index.html'}
async function api(path,options={}){const headers={'Content-Type':'application/json',...(options.headers||{})};if(token)headers.Authorization='Bearer '+token;let response;try{response=await fetch(API+path,{...options,headers})}catch(e){throw new Error('تعذر الاتصال بالخادم. تحقق من الإنترنت أو إعدادات CORS في الـ Backend.')};const data=await response.json().catch(()=>({}));if(response.status===401){localStorage.removeItem('waslha_token');token=null}if(!response.ok)throw new Error(data?.error?.message||data?.message||`خطأ ${response.status}`);return data}
function toast(message){const el=document.createElement('div');el.className='toast';el.textContent=message;document.body.appendChild(el);setTimeout(()=>el.remove(),3500)}
function setBusy(button,busy,text='جارٍ التنفيذ...'){if(!button)return;button.disabled=busy;if(busy){button.dataset.old=text==='جارٍ التنفيذ...'?(button.textContent||''):button.dataset.old||button.textContent;button.textContent=text}else if(button.dataset.old){button.textContent=button.dataset.old}}
function requireRole(role){const u=user();if(!token||!u||u.role!==role){location.href='index.html';return false}return true}
function statusLabel(s){return ({requested:'تم إنشاء الطلب',searching:'جاري البحث عن كابتن',captain_assigned:'تم تعيين الكابتن',captain_arriving:'الكابتن في الطريق',captain_arrived:'الكابتن وصل',trip_started:'الرحلة بدأت',trip_completed:'اكتملت الرحلة',cancelled:'ملغاة'})[s]||s||'غير معروف')}
function money(v){return v==null?'—':`${Number(v).toLocaleString('ar-IQ')} د.ع`}
function coordsFromForm(ids){const values=ids.map(id=>Number($(id)?.value));if(values.some(v=>!Number.isFinite(v)))throw new Error('أدخل إحداثيات صحيحة');return values}
function fillLocation(latId,lngId){if(!navigator.geolocation){toast('المتصفح لا يدعم تحديد الموقع');return}navigator.geolocation.getCurrentPosition(p=>{$(latId).value=p.coords.latitude.toFixed(6);$(lngId).value=p.coords.longitude.toFixed(6);toast('تم تحديد موقعك')},()=>toast('لم نتمكن من الوصول إلى موقعك'))}
