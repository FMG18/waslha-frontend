package com.waslha.admin

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

class AdminLiveMapActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminApiProvider.init(this)
        setContent {
            var trips by remember { mutableStateOf<List<AdminTripDto>>(emptyList()) }
            var drivers by remember { mutableStateOf<List<AdminDriverDto>>(emptyList()) }
            LaunchedEffect(Unit) {
                while (true) {
                    runCatching { AdminApiProvider.api.trips() }.onSuccess { if (it.success) trips = it.data.orEmpty() }
                    runCatching { AdminApiProvider.api.drivers() }.onSuccess { if (it.success) drivers = it.data.orEmpty() }
                    delay(8000)
                }
            }
            Scaffold(topBar = { Text("الخريطة الحية — وصلها") }) { _ ->
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            webViewClient = WebViewClient()
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            loadDataWithBaseURL("https://waslha.local/", html(), "text/html", "UTF-8", null)
                        }
                    },
                    update = { webView ->
                        val payload = JSONObject()
                            .put("trips", JSONArray(tripsJson(trips)))
                            .put("drivers", JSONArray(driversJson(drivers)))
                            .toString()
                        webView.evaluateJavascript("window.updateMap(${JSONObject.quote(payload)});", null)
                    }
                )
            }
        }
    }

    private fun tripsJson(trips: List<AdminTripDto>): List<JSONObject> = trips
        .filter { it.pickup != null && it.destination != null && it.status !in listOf("completed", "cancelled") }
        .map { t -> JSONObject().apply {
            put("id", t.id); put("status", t.status); put("fare", t.estimatedFare)
            put("pickupLat", t.pickup!!.lat); put("pickupLng", t.pickup!!.lng)
            put("destLat", t.destination!!.lat); put("destLng", t.destination!!.lng)
            put("driver", t.driver?.name ?: "غير معين")
        }}

    private fun driversJson(drivers: List<AdminDriverDto>): List<JSONObject> = drivers.mapNotNull { d ->
        val lat = d.lat ?: return@mapNotNull null
        val lng = d.lng ?: return@mapNotNull null
        JSONObject().apply {
            put("id", d.id); put("name", d.name.ifBlank { d.id }); put("lat", lat); put("lng", lng)
            put("available", d.available); put("lastLocationAt", d.lastLocationAt ?: 0L)
        }
    }

    private fun html(): String = """
<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'><link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'><style>
html,body,#map{margin:0;width:100%;height:100%;font-family:sans-serif}#badge{position:fixed;z-index:5000;top:12px;right:12px;background:#fff;padding:8px 12px;border-radius:18px;box-shadow:0 2px 12px #0002;font-size:12px}#legend{position:fixed;z-index:5000;bottom:18px;right:12px;background:#fff;padding:9px 12px;border-radius:16px;box-shadow:0 2px 12px #0002;font-size:11px;line-height:1.8}
</style></head><body><div id='badge'>تحديث حي</div><div id='map'></div><div id='legend'>🚕 كابتن &nbsp; • &nbsp; 📍 رحلة</div><script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>
const map=L.map('map').setView([33.5138,36.2913],12);L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap contributors'}).addTo(map);const tripLayers={},driverLayers={};
function clearLayerBag(bag){Object.values(bag).forEach(x=>{if(x.marker)map.removeLayer(x.marker);if(x.line)map.removeLayer(x.line);});Object.keys(bag).forEach(k=>delete bag[k]);}
window.updateMap=function(raw){try{const obj=JSON.parse(raw);clearLayerBag(tripLayers);clearLayerBag(driverLayers);const trips=obj.trips||[],drivers=obj.drivers||[];trips.forEach(t=>{const pickup=[t.pickupLat,t.pickupLng],dest=[t.destLat,t.destLng];const marker=L.marker(pickup).addTo(map).bindPopup('<b>رحلة #'+t.id.slice(-6)+'</b><br>الحالة: '+t.status+'<br>الكابتن: '+t.driver+'<br>الأجرة: '+t.fare+' ل.س');const line=L.polyline([pickup,dest],{weight:4}).addTo(map);tripLayers[t.id]={marker,line};});drivers.forEach(d=>{const marker=L.circleMarker([d.lat,d.lng],{radius:8,weight:3,fillOpacity:.9}).addTo(map).bindPopup('<b>🚕 '+d.name+'</b><br>'+(d.available?'متاح':'غير متاح')+(d.lastLocationAt?'<br>آخر موقع: '+new Date(d.lastLocationAt).toLocaleTimeString('ar-SY'):''));driverLayers[d.id]={marker};});document.getElementById('badge').textContent='رحلات: '+trips.length+' • كباتن على الخريطة: '+drivers.length;}catch(e){document.getElementById('badge').textContent='تعذر تحديث الخريطة';}};
</script></body></html>
""".trimIndent()
}
