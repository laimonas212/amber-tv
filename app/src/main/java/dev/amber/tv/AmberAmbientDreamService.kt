package dev.amber.tv

import android.service.dreams.DreamService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
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
import kotlin.math.cos
import kotlin.math.sin

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
    val mode = remember { prefs.getString("ambient_mode", "slideshow") ?: "slideshow" }
    val showClock = remember { prefs.getBoolean("ambient_show_clock", true) }
    val showDate = remember { prefs.getBoolean("ambient_show_date", true) }
    val showWeather = remember { prefs.getBoolean("ambient_show_weather", false) }
    val use24Hour = remember { prefs.getBoolean("ambient_24_hour", true) }
    val interval = remember { prefs.getInt("ambient_interval_minutes", 5).coerceIn(1, 30) }
    val position = remember { prefs.getInt("ambient_clock_position", 0).coerceIn(0, 4) }
    val dim = remember { prefs.getFloat("ambient_dim", .22f).coerceIn(0f, .65f) }
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
    LaunchedEffect(showWeather, mode) {
        if (showWeather || mode == "weather") {
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
    LaunchedEffect(wallpapers.size, interval, mode) {
        if (mode == "slideshow") while (wallpapers.size > 1) {
            delay(interval * 60_000L)
            wallpaperIndex = (wallpaperIndex + 1) % wallpapers.size
        }
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
    val horizontal = when (position) {
        1, 3 -> Alignment.End
        4 -> Alignment.CenterHorizontally
        else -> Alignment.Start
    }
    val time = now.format(DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a", Locale.getDefault()))
    val date = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
    val offset = listOf(0, 8, -8, 14, -14)[drift]

    Box(Modifier.fillMaxSize().background(Color(0xFF05080B))) {
        when (mode) {
            "slideshow" -> {
                if (wallpapers.isNotEmpty()) {
                    Crossfade(targetState = wallpaperIndex, animationSpec = tween(1800, easing = LinearEasing), label = "ambient-wallpaper") { index ->
                        AsyncImage(model = wallpapers[index].url, contentDescription = wallpapers[index].title,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                } else AmbientArt("generative")
            }
            "digital_clock" -> {
                AmbientArt("digital")
                AmbientClockOverlay(time, date, temperature, showClock, showDate, showWeather, position, offset)
            }
            "analog_clock" -> {
                AmbientArt("digital")
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    AnalogClock(now.hour, now.minute, now.second)
                    if (showDate) Text(date, color = Color.White.copy(alpha = .8f), fontSize = 20.sp, modifier = Modifier.padding(top = 28.dp))
                    if (showWeather && temperature != null) Text("VILNIUS  ·  $temperature°", color = Color.White.copy(alpha = .65f), fontSize = 13.sp, letterSpacing = 2.sp, modifier = Modifier.padding(top = 12.dp))
                }
            }
            "generative" -> {
                AmbientArt("generative")
                AmbientClockOverlay(time, date, temperature, showClock, showDate, showWeather, position, offset)
            }
            "aurora" -> {
                AmbientArt("aurora")
                AmbientClockOverlay(time, date, temperature, showClock, showDate, showWeather, position, offset)
            }
            "weather" -> {
                AmbientArt("digital")
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("VILNIUS", color = Color.White.copy(alpha = .62f), fontSize = 16.sp, letterSpacing = 5.sp)
                    Text(temperature?.let { "$it°" } ?: "—", color = Color.White, fontSize = 112.sp, fontWeight = FontWeight.ExtraLight)
                    Text("CURRENT WEATHER", color = Color.White.copy(alpha = .62f), fontSize = 13.sp, letterSpacing = 3.sp)
                    Text(time, color = Color.White.copy(alpha = .9f), fontSize = 32.sp, modifier = Modifier.padding(top = 30.dp))
                    if (showDate) Text(date, color = Color.White.copy(alpha = .7f), fontSize = 18.sp)
                }
            }
            else -> AmbientArt("generative")
        }

        if (mode == "slideshow") {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
            Box(Modifier.fillMaxSize().padding(horizontal = 88.dp, vertical = 64.dp), contentAlignment = align) {
                Column(horizontalAlignment = horizontal, modifier = Modifier.offset(x = offset.dp, y = (-offset).dp)) {
                    if (showClock) Text(time, color = Color.White, fontSize = 76.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = (-2).sp)
                    if (showDate) Text(date, color = Color.White.copy(alpha = .82f), fontSize = 20.sp, fontWeight = FontWeight.Light)
                    if (showWeather && temperature != null) Text("VILNIUS  ·  $temperature°", color = Color.White.copy(alpha = .7f), fontSize = 12.sp, letterSpacing = 2.sp, modifier = Modifier.padding(top = 12.dp))
                }
            }
            if (wallpapers.isEmpty()) Text("AMBER AMBIENT", color = Color.White.copy(alpha = .35f), fontSize = 12.sp, letterSpacing = 4.sp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(28.dp))
        } else if (mode !in listOf("analog_clock", "weather")) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim * .55f)))
        }
    }
}

@Composable
private fun AmbientClockOverlay(time: String, date: String, temperature: Int?, showClock: Boolean, showDate: Boolean, showWeather: Boolean, position: Int, offset: Int) {
    val align = when (position) {
        1 -> Alignment.BottomEnd
        2 -> Alignment.TopStart
        3 -> Alignment.TopEnd
        4 -> Alignment.Center
        else -> Alignment.BottomStart
    }
    val horizontal = when (position) {
        1, 3 -> Alignment.End
        4 -> Alignment.CenterHorizontally
        else -> Alignment.Start
    }
    Box(Modifier.fillMaxSize().padding(horizontal = 88.dp, vertical = 64.dp), contentAlignment = align) {
        Column(horizontalAlignment = horizontal, modifier = Modifier.offset(x = offset.dp, y = (-offset).dp)) {
            if (showClock) Text(time, color = Color.White, fontSize = 76.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = (-2).sp)
            if (showDate) Text(date, color = Color.White.copy(alpha = .82f), fontSize = 20.sp, fontWeight = FontWeight.Light)
            if (showWeather && temperature != null) Text("VILNIUS  ·  $temperature°", color = Color.White.copy(alpha = .7f), fontSize = 12.sp, letterSpacing = 2.sp, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun AnalogClock(hour: Int, minute: Int, second: Int) {
    Canvas(Modifier.size(310.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * .46f
        drawCircle(Color.White.copy(alpha = .10f), radius, center)
        drawCircle(Color.White.copy(alpha = .82f), radius, center, style = Stroke(width = 2.dp.toPx()))
        repeat(60) { tick ->
            val angle = tick * Math.PI / 30.0 - Math.PI / 2.0
            val inner = radius * if (tick % 5 == 0) .82f else .88f
            val outer = radius * .92f
            val start = Offset(center.x + cos(angle).toFloat() * inner, center.y + sin(angle).toFloat() * inner)
            val end = Offset(center.x + cos(angle).toFloat() * outer, center.y + sin(angle).toFloat() * outer)
            drawLine(Color.White.copy(alpha = if (tick % 5 == 0) .9f else .35f), start, end,
                strokeWidth = if (tick % 5 == 0) 3.dp.toPx() else 1.dp.toPx(), cap = StrokeCap.Round)
        }
        val hourAngle = ((hour % 12) + minute / 60f) * (Math.PI / 6.0) - Math.PI / 2.0
        val minuteAngle = (minute + second / 60f) * (Math.PI / 30.0) - Math.PI / 2.0
        val secondAngle = second * (Math.PI / 30.0) - Math.PI / 2.0
        drawLine(Color.White, center, Offset(center.x + cos(hourAngle).toFloat() * radius * .48f, center.y + sin(hourAngle).toFloat() * radius * .48f), 5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(Color.White.copy(alpha = .9f), center, Offset(center.x + cos(minuteAngle).toFloat() * radius * .72f, center.y + sin(minuteAngle).toFloat() * radius * .72f), 3.dp.toPx(), cap = StrokeCap.Round)
        drawLine(Color(0xFF83C7FF), Offset(center.x - cos(secondAngle).toFloat() * radius * .12f, center.y - sin(secondAngle).toFloat() * radius * .12f),
            Offset(center.x + cos(secondAngle).toFloat() * radius * .78f, center.y + sin(secondAngle).toFloat() * radius * .78f), 1.5.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(Color(0xFF83C7FF), 5.dp.toPx(), center)
    }
}

@Composable
private fun AmbientArt(mode: String) {
    val motion = rememberInfiniteTransition(label = "ambient-motion")
    val phase by motion.animateFloat(initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart), label = "ambient-phase")
    val colors = if (mode == "aurora") listOf(Color(0xFF061B2A), Color(0xFF123D46), Color(0xFF17213F), Color(0xFF05080B))
        else listOf(Color(0xFF101A2B), Color(0xFF202D43), Color(0xFF111827), Color(0xFF05080B))
    Canvas(Modifier.fillMaxSize()) {
        drawRect(brush = Brush.linearGradient(colors, start = Offset(size.width * phase, 0f), end = Offset(size.width * (1f - phase), size.height)))
        val count = if (mode == "aurora") 7 else 5
        repeat(count) { index ->
            val t = phase * (Math.PI * 2.0) + index * 1.37
            val x = size.width * (.12f + .76f * ((index + 1f) / (count + 1f))) + cos(t).toFloat() * size.width * .10f
            val y = size.height * (.22f + .56f * ((index % 3) / 2f)) + sin(t * .7).toFloat() * size.height * .12f
            val radius = size.minDimension * if (mode == "aurora") .20f else .15f
            val color = if (mode == "aurora") listOf(Color(0xFF48D6B5), Color(0xFF6C9DFF), Color(0xFF9A76E8))[index % 3]
                else listOf(Color(0xFF3C7FB8), Color(0xFF775DB8), Color(0xFF397C88))[index % 3]
            drawCircle(brush = Brush.radialGradient(listOf(color.copy(alpha = .22f), color.copy(alpha = .06f), Color.Transparent), center = Offset(x, y), radius = radius),
                radius = radius, center = Offset(x, y))
        }
        if (mode == "aurora") repeat(3) { band ->
            val y = size.height * (.30f + band * .15f)
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, y)
                for (step in 0..40) {
                    val x = size.width * step / 40f
                    val wave = sin((step / 40.0 * Math.PI * 3.0) + phase * Math.PI * 2.0 + band).toFloat() * size.height * .075f
                    lineTo(x, y + wave)
                }
            }
            drawPath(path, Color(0xFF62E4C5).copy(alpha = .10f - band * .02f), style = Stroke(width = 34.dp.toPx()))
        }
    }
}
