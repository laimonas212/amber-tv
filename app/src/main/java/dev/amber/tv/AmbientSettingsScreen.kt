package dev.amber.tv

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface as TvSurface

private val ambientIntervals = listOf(1, 2, 5, 10)
private val ambientPositions = listOf("Bottom left", "Bottom right", "Top left", "Top right", "Center")

@Composable
internal fun AmbientSettingsScreen(prefs: SharedPreferences, onBack: () -> Unit) {
    var showClock by remember { mutableStateOf(prefs.getBoolean("ambient_show_clock", true)) }
    var showDate by remember { mutableStateOf(prefs.getBoolean("ambient_show_date", true)) }
    var showWeather by remember { mutableStateOf(prefs.getBoolean("ambient_show_weather", false)) }
    var use24Hour by remember { mutableStateOf(prefs.getBoolean("ambient_24_hour", true)) }
    var interval by remember { mutableIntStateOf(prefs.getInt("ambient_interval_minutes", 5).coerceIn(1, 10)) }
    var position by remember { mutableIntStateOf(prefs.getInt("ambient_clock_position", 0).coerceIn(0, 4)) }
    var dim by remember { mutableFloatStateOf(prefs.getFloat("ambient_dim", .22f).coerceIn(0f, .65f)) }
    fun save(key: String, value: Any) {
        prefs.edit().apply {
            when (value) {
                is Boolean -> putBoolean(key, value)
                is Int -> putInt(key, value)
                is Float -> putFloat(key, value)
            }
        }.apply()
    }
    Column(Modifier.fillMaxSize().padding(start = 76.dp, end = 76.dp, top = 28.dp, bottom = 24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TvSurface(onClick = onBack, modifier = Modifier.size(width = 110.dp, height = 48.dp),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                colors = ClickableSurfaceDefaults.colors(containerColor = Color(0x332FFFFFF), focusedContainerColor = Color(0x664A86A8))) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("‹  Back", color = Color.White, fontSize = 16.sp) }
            }
            Spacer(Modifier.width(22.dp))
            Column {
                Text("Amber Ambient", color = Color.White, fontSize = 28.sp)
                Text("Customize your screensaver · settings are saved per profile", color = Color.White.copy(alpha = .62f), fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(22.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            item { Text("DISPLAY", color = Color(0xFF91BCE0), fontSize = 11.sp, letterSpacing = 2.sp) }
            item { AmbientToggle("Clock", "Show a large cinematic clock", showClock) { showClock = it; save("ambient_show_clock", it) } }
            item { AmbientToggle("Date", "Show the current date below the clock", showDate) { showDate = it; save("ambient_show_date", it) } }
            item { AmbientToggle("Weather", "Show current temperature in Vilnius", showWeather) { showWeather = it; save("ambient_show_weather", it) } }
            item { AmbientToggle("24-hour clock", "Use 16:45 instead of 4:45 PM", use24Hour) { use24Hour = it; save("ambient_24_hour", it) } }
            item { AmbientChoice("Background rotation", "Change to another image every $interval minute(s)", ambientIntervals.map { "$it min" }, ambientIntervals.indexOf(interval).coerceAtLeast(0)) { interval = ambientIntervals[it]; save("ambient_interval_minutes", interval) } }
            item { AmbientChoice("Clock position", ambientPositions[position], ambientPositions, position) { position = it; save("ambient_clock_position", it) } }
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0x33202B35)).padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("OLED dimming", color = Color.White, fontSize = 17.sp)
                            Text("Darken the image to reduce static brightness", color = Color.White.copy(alpha = .58f), fontSize = 12.sp)
                        }
                        Text("${(dim * 100).toInt()}%", color = Color(0xFFB9D9F3), fontSize = 16.sp)
                    }
                    Slider(value = dim, onValueChange = { dim = it }, onValueChangeFinished = { save("ambient_dim", dim) }, valueRange = 0f..0.65f)
                }
            }
            item { Text("SYSTEM", color = Color(0xFF91BCE0), fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(top = 8.dp)) }
            item { Text("Choose Amber Ambient as the active screensaver in Android Settings. The idle timeout is controlled by the TV system, not by the launcher.", color = Color.White.copy(alpha = .55f), fontSize = 12.sp) }
        }
    }
}

@Composable
private fun AmbientToggle(title: String, hint: String, checked: Boolean, onClick: (Boolean) -> Unit) {
    TvSurface(onClick = { onClick(!checked) }, modifier = Modifier.fillMaxWidth().height(68.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0x33202B35), focusedContainerColor = Color(0x66517FA0))) {
        Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = Color.White, fontSize = 16.sp)
                Text(hint, color = Color.White.copy(alpha = .55f), fontSize = 12.sp)
            }
            Text(if (checked) "ON" else "OFF", color = if (checked) Color(0xFF9FD5FF) else Color.White.copy(alpha = .48f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun AmbientChoice(title: String, value: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    TvSurface(onClick = { onSelect((selected + 1) % options.size) }, modifier = Modifier.fillMaxWidth().height(68.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0x33202B35), focusedContainerColor = Color(0x66517FA0))) {
        Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = Color.White, fontSize = 16.sp)
                Text(value, color = Color.White.copy(alpha = .58f), fontSize = 12.sp)
            }
            Text("CHANGE  ›", color = Color(0xFF9FD5FF), fontSize = 11.sp)
        }
    }
}
