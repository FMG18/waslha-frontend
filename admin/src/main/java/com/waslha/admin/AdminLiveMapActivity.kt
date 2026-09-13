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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class AdminLiveMapActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminApiProvider.init(this)
        setContent {
            var drivers by remember { mutableStateOf<List<AdminDriverDto>>(emptyList()) }
            var error by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(Unit) {
                while (true) {
                    runCatching { AdminApiProvider.api.drivers() }
                        .onSuccess { if (it.success) { drivers = it.data.orEmpty(); error = null } else error = it.message }
                        .onFailure { error = it.message }
                    delay(8000)
                }
            }
            Scaffold(topBar = { Text("الخريطة الحية — كباتن وصلها") }) { padding ->
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
                        val payload = driversJson(drivers)
                        val message = JSONObject().put("drivers", JSONArray(payload)).toString()
                        webView.evaluateJavascript("window.updateDrivers(${JSONObject.quote(message)});", null)
                    }
                )
            }
        }
    }

    private fun driversJson(drivers: List<AdminDriverDto>): List<JSONObject> = drivers.mapNotNull { d ->
        val lat = d.lat ?: return@mapNotNull null
        val lng = d.lng ?: return@mapNotNull null
        JSONObject().apply {
            put("id", d.id)
            put("name", d.name.ifBlank { d.id })
            put("lat", lat)
            put("lng", lng)
            put("available", d.available)
        }
    }

    private fun html(): String = """
<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'><link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'><style>html,body,#map{margin:0;width:100%;height:100%;font-family:sans-serif}#badge{position:fixed;z-index:5000;top:12px;right:12px;background:#fff;padding:8px 12px;border-radius:18px;box-shadow:0 2px 12px #0002;font-size:12px}</style></head><body><div id='badge'>تحديث حي</div><div id='map'></div><script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>
const map=L.map('map').setView([33.5138,36.2913],12);L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap contributors'}).addTo(map);const markers={};
window.updateDrivers=function(raw){try{const obj=JSON.parse(raw);const list=obj.drivers||[];list.forEach(d=>{if(markers[d.id]){markers[d.id].setLatLng([d.lat,d.lng]);markers[d.id].setPopupContent('<b>'+d.name+'</b><br>'+((d.available)?'متاح':'غير متاح'));}else{markers[d.id]=L.marker([d.lat,d.lng]).addTo(map).bindPopup('<b>'+d.name+'</b><br>'+((d.available)?'متاح':'غير متاح'));}});document.getElementById('badge').textContent='كباتن على الخريطة: '+list.length;}catch(e){document.getElementById('badge').textContent='تعذر تحديث الخريطة';}};
</script></body></html>
""".trimIndent()
}
