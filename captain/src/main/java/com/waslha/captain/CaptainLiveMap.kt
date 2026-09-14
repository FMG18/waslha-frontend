package com.waslha.captain

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CaptainLiveMap(
    modifier: Modifier = Modifier,
    driver: Coordinates?,
    pickup: Coordinates? = null,
    destination: Coordinates? = null,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                setBackgroundColor(0xFFE8EFEB.toInt())
                loadDataWithBaseURL("https://localhost/", HTML, "text/html", "UTF-8", null)
            }
        },
        update = { web ->
            val d = driver ?: return@AndroidView
            val p = pickup
            val dest = destination
            val js = buildString {
                append("window.updateDriver(${d.lat},${d.lng});")
                if (p != null) append("window.setPickup(${p.lat},${p.lng});") else append("window.clearPickup();")
                if (dest != null) append("window.setDestination(${dest.lat},${dest.lng});") else append("window.clearDestination();")
                if (p != null && dest != null) append("window.fitTrip();") else append("window.centerDriver();")
            }
            web.evaluateJavascript(js, null)
        }
    )
}

private val HTML = """
<!doctype html>
<html dir='rtl'>
<head>
<meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>
<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>
<style>
html,body,#map{height:100%;margin:0;background:#e8efeb;font-family:Arial,sans-serif}
.leaflet-control-attribution{font-size:8px}
.pin{width:36px;height:36px;border-radius:50%;display:flex;align-items:center;justify-content:center;color:#fff;font-weight:900;border:3px solid #fff;box-shadow:0 3px 12px rgba(0,0,0,.25);font-size:15px}
.driver{background:#0b805e}.pickup{background:#075b43}.dest{background:#b42318}
</style>
</head>
<body>
<div id='map'></div>
<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>
<script>
const map=L.map('map',{zoomControl:false,attributionControl:true}).setView([33.5138,36.2765],13);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19}).addTo(map);
const icon=(cls,text)=>L.divIcon({className:'',html:`<div class="pin ${cls}">${text}</div>`,iconSize:[36,36],iconAnchor:[18,18]});
let driverMarker=null,pickupMarker=null,destMarker=null;
function updateDriver(lat,lng){if(!driverMarker){driverMarker=L.marker([lat,lng],{icon:icon('driver','و')}).addTo(map)}else driverMarker.setLatLng([lat,lng]);}
function setPickup(lat,lng){if(!pickupMarker) pickupMarker=L.marker([lat,lng],{icon:icon('pickup','↑')}).addTo(map);else pickupMarker.setLatLng([lat,lng]);}
function clearPickup(){if(pickupMarker){map.removeLayer(pickupMarker);pickupMarker=null}}
function setDestination(lat,lng){if(!destMarker) destMarker=L.marker([lat,lng],{icon:icon('dest','●')}).addTo(map);else destMarker.setLatLng([lat,lng]);}
function clearDestination(){if(destMarker){map.removeLayer(destMarker);destMarker=null}}
function centerDriver(){if(driverMarker)map.setView(driverMarker.getLatLng(),15,{animate:true})}
function fitTrip(){const pts=[];if(driverMarker)pts.push(driverMarker.getLatLng());if(pickupMarker)pts.push(pickupMarker.getLatLng());if(destMarker)pts.push(destMarker.getLatLng());if(pts.length>1)map.fitBounds(L.latLngBounds(pts),{padding:[35,35]});}
</script>
</body>
</html>
""".trimIndent()
