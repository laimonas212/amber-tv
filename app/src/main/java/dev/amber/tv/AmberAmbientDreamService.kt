package dev.amber.tv

import android.service.dreams.DreamService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class AmberAmbientDreamService : DreamService(), LifecycleOwner {
    private val dreamLifecycle by lazy { LifecycleRegistry(this) }
    override val lifecycle: Lifecycle get() = dreamLifecycle

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        dreamLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        isInteractive = false
        isFullscreen = true
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AmberAmbientDreamService)
            setContent { AmberAmbientContent(this@AmberAmbientDreamService) }
        }
        setContentView(composeView)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        dreamLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        dreamLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDreamingStopped() {
        dreamLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        dreamLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        dreamLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDetachedFromWindow()
    }
}

private data class AmbientWallpaper(val url: String, val title: String)

@Composable
internal fun AmberAmbientContent(context: android.content.Context) {
    val meta = remember { context.getSharedPreferences("amber_tv_profiles", 0) }
    val profileId = remember { meta.getString("active_profile", "profile_1") ?: "profile_1" }
    val prefs = remember { context.getSharedPreferences("amber_tv_$profileId", 0) }
    val showClock = remember { prefs.getBoolean("ambient_show_clock", true) }
    val showDate = remember { prefs.getBoolean("ambient_show_date", true) }
    val showWeather = remember { prefs.getBoolean("ambient_show_weather", false) }
    val use24Hour = remember { prefs.getBoolean("ambient_24_hour", true) }
    val interval = remember { prefs.getInt("ambient_interval_minutes", 5).coerceIn(1, 10) }
    val position = remember { prefs.getInt("ambient_clock_position", 0).coerceIn(0, 4) }
    val dim = remember { prefs.getFloat("ambient_dim", .22f).coerceIn(0f, .65f) }
    val visualStyle = remember { prefs.getInt("ambient_visual_style", 0).coerceIn(0, 4) }
    val wallpapers = remember {
        runCatching {
            val raw = prefs.getString("remote_wallpaper_catalog", null) ?: return@runCatching emptyList()
            val array = JSONObject(raw).getJSONArray("wallpapers")
            (0 until array.length()).mapNotNull { i ->
                val item = array.getJSONObject(i)
                val url = item.optString("url")
                if (url.startsWith("https://")) AmbientWallpaper(url, item.optString("title", "Amber Ambient")) else null
            }
        }.getOrDefault(emptyList())
    }
    var wallpaperIndex by remember { mutableIntStateOf(0) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var drift by remember { mutableIntStateOf(0) }
    var temperature by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        while (true) { now = LocalDateTime.now(); delay(1000L) }
    }
    LaunchedEffect(showWeather) {
        if (showWeather) {
            while (true) {
                temperature = withContext(Dispatchers.IO) {
                    runCatching {
                        val url = URL("https://api.open-meteo.com/v1/forecast?latitude=54.75&longitude=25.25&current=temperature_2m&timezone=Europe%2FVilnius")
                        val json = JSONObject(url.openConnection().apply { connectTimeout = 5000; readTimeout = 5000 }.getInputStream().bufferedReader().use { it.readText() })
                        json.getJSONObject("current").getDouble("temperature_2m").toInt()
                    }.getOrNull()
                }
                delay(if (temperature == null) 120_000L else 1_800_000L)
            }
        } else temperature = null
    }
    LaunchedEffect(wallpapers.size, interval) {
        while (wallpapers.size > 1) { delay(interval * 60_000L); wallpaperIndex = (wallpaperIndex + 1) % wallpapers.size }
    }
    LaunchedEffect(Unit) {
        while (true) { delay(60_000L); drift = (drift + 1) % 5 }
    }
    val align = when (position) {
        1 -> Alignment.BottomEnd
        2 -> Alignment.TopStart
        3 -> Alignment.TopEnd
        4 -> Alignment.Center
        else -> Alignment.BottomStart
    }
    val time = now.format(DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a", Locale.getDefault()))
    val date = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
    val offsets = listOf(0, 8, -8, 14, -14)
    val offset = offsets[drift]
    Box(Modifier.fillMaxSize().background(Color(0xFF05080B))) {
        if (visualStyle == 4 && wallpapers.isNotEmpty()) {
            Crossfade(targetState = wallpaperIndex, label = "ambient-wallpaper") { index ->
                AsyncImage(model = wallpapers[index].url, contentDescription = wallpapers[index].title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        } else {
            AndroidView(factory = { AuroraAmbientView(it, visualStyle) }, modifier = Modifier.fillMaxSize(), update = { })
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
        Box(Modifier.fillMaxSize().padding(horizontal = 88.dp, vertical = 64.dp), contentAlignment = align) {
            Column(horizontalAlignment = when (position) { 1, 3 -> Alignment.End; 4 -> Alignment.CenterHorizontally; else -> Alignment.Start },
                modifier = Modifier.offset(x = offset.dp, y = (-offset).dp)) {
                if (showClock) Text(time, color = Color.White, fontSize = 76.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = (-2).sp)
                if (showDate) Text(date, color = Color.White.copy(alpha = .82f), fontSize = 20.sp, fontWeight = FontWeight.Light)
                if (showWeather && temperature != null) Text("VILNIUS  ·  $temperature°", color = Color.White.copy(alpha = .7f), fontSize = 12.sp, letterSpacing = 2.sp, modifier = Modifier.padding(top = 12.dp))
            }
        }
        if (visualStyle == 4 && wallpapers.isEmpty()) Text("ADD WALLPAPERS TO AMBER AMBIENT", color = Color.White.copy(alpha = .48f), fontSize = 12.sp, letterSpacing = 3.sp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(28.dp))
    }
}
