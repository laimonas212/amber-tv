package dev.amber.tv

import android.content.SharedPreferences
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.window.Dialog
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
private val ambientVisualStyles = listOf("Gradient Dots", "Apple Image Playground", "Photo slideshow")

@Composable
internal fun AmbientSettingsScreen(prefs: SharedPreferences, onBack: () -> Unit, onOpenSystemSettings: () -> Unit, onStartNow: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showClock by remember { mutableStateOf(prefs.getBoolean("ambient_show_clock", true)) }
    var showDate by remember { mutableStateOf(prefs.getBoolean("ambient_show_date", true)) }
    var showWeather by remember { mutableStateOf(prefs.getBoolean("ambient_show_weather", false)) }
    var use24Hour by remember { mutableStateOf(prefs.getBoolean("ambient_24_hour", true)) }
    var interval by remember { mutableIntStateOf(prefs.getInt("ambient_interval_minutes", 5).coerceIn(1, 10)) }
    var position by remember { mutableIntStateOf(prefs.getInt("ambient_clock_position", 0).coerceIn(0, 4)) }
    var dim by remember { mutableFloatStateOf(prefs.getFloat("ambient_dim", .22f).coerceIn(0f, .65f)) }
    var visualStyle by remember { mutableIntStateOf(prefs.getInt("ambient_visual_style", 2).coerceIn(0, 2)) }
    var selectedPhotos by remember { mutableStateOf(prefs.getStringSet("ambient_photo_uris", emptySet())?.toList().orEmpty()) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            val retained = uris.mapNotNull { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    uri.toString()
                }.getOrNull()
            }
            if (retained.isNotEmpty()) {
                selectedPhotos = retained
                prefs.edit().putStringSet("ambient_photo_uris", retained.toSet()).apply()
            }
        }
    }
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
                colors = ClickableSurfaceDefaults.colors(containerColor = Color(0x33FFFFFF), focusedContainerColor = Color(0x664A86A8))) {
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
            item { Text("VISUAL EXPERIENCE", color = Color(0xFF91BCE0), fontSize = 11.sp, letterSpacing = 2.sp) }
            item {
                AmbientChoice(
                    "Ambient style",
                    when (visualStyle) {
                        0 -> "Animated gradient dots · supplied Lottie"
                        1 -> "Soft animated image · supplied Lottie"
                        else -> "Use your selected wallpaper collection"
                    },
                    ambientVisualStyles,
                    visualStyle
                ) { visualStyle = it; save("ambient_visual_style", it) }
            }
            item { Text("DISPLAY", color = Color(0xFF91BCE0), fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(top = 8.dp)) }
            item { AmbientToggle("Clock", "Show a large cinematic clock", showClock) { showClock = it; save("ambient_show_clock", it) } }
            item { AmbientToggle("Date", "Show the current date below the clock", showDate) { showDate = it; save("ambient_show_date", it) } }
            item { AmbientToggle("Weather", "Show current temperature in Vilnius", showWeather) { showWeather = it; save("ambient_show_weather", it) } }
            item { AmbientToggle("24-hour clock", "Use 16:45 instead of 4:45 PM", use24Hour) { use24Hour = it; save("ambient_24_hour", it) } }
            item { AmbientChoice("Photo rotation interval", "Change photo every $interval minute(s)", ambientIntervals.map { "$it min" }, ambientIntervals.indexOf(interval).coerceAtLeast(0)) { interval = ambientIntervals[it]; save("ambient_interval_minutes", interval) } }
            item { AmbientChoice("Clock position", ambientPositions[position], ambientPositions, position) { position = it; save("ambient_clock_position", it) } }
            if (visualStyle == 2) {
                item {
                    AmbientAction("Choose photos from gallery", "${selectedPhotos.size} selected · only these photos will be shown", "SELECT  ›") {
                        photoPicker.launch(arrayOf("image/*"))
                    }
                }
                if (selectedPhotos.isNotEmpty()) {
                    item { AmbientAction("Clear selected photos", "Return to the online wallpaper collection", "CLEAR  ×") {
                        selectedPhotos = emptyList()
                        prefs.edit().remove("ambient_photo_uris").apply()
                    } }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0x33202B35)).padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("OLED dimming", color = Color.White, fontSize = 17.sp)
                            Text("Darkens photos and both Lottie animations in real time", color = Color.White.copy(alpha = .58f), fontSize = 12.sp)
                        }
                        Text("${(dim * 100).toInt()}%", color = Color(0xFFB9D9F3), fontSize = 16.sp)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AmbientStepButton("−") { dim = (dim - .05f).coerceAtLeast(0f); save("ambient_dim", dim) }
                        Slider(modifier = Modifier.weight(1f), value = dim, onValueChange = { dim = it; save("ambient_dim", it) }, valueRange = 0f..0.80f)
                        AmbientStepButton("+") { dim = (dim + .05f).coerceAtMost(.80f); save("ambient_dim", dim) }
                    }
                }
            }
            item { AmbientAction("Start Amber Ambient now", "Launch the full-screen Ambient display immediately", "START NOW  ›", onStartNow) }
            item { Text("START DELAY", color = Color(0xFF91BCE0), fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(top = 8.dp)) }
            item { AmbientAction("When should screensaver start?", "Set the idle timeout in Android TV system settings", "SET DELAY  ›", onOpenSystemSettings) }
            item { Text("Android TV controls the actual idle delay. This opens the system page where you can choose after how long Amber Ambient starts.", color = Color.White.copy(alpha = .55f), fontSize = 12.sp) }
        }
    }
}

@Composable
private fun AmbientStepButton(label: String, onClick: () -> Unit) {
    TvSurface(onClick = onClick, modifier = Modifier.size(48.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0x33202B35), focusedContainerColor = Color(0x66517FA0))) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(label, color = Color.White, fontSize = 25.sp) }
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
    var expanded by remember { mutableStateOf(false) }
    TvSurface(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().height(68.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0x33202B35), focusedContainerColor = Color(0x66517FA0))) {
        Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = Color.White, fontSize = 16.sp)
                Text(value, color = Color.White.copy(alpha = .58f), fontSize = 12.sp)
            }
            Text("CHOOSE  ▾", color = Color(0xFF9FD5FF), fontSize = 11.sp)
        }
    }
    AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
        Dialog(onDismissRequest = { expanded = false }) {
            Column(Modifier.width(500.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xF21A242C))
                .padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, color = Color.White, fontSize = 23.sp)
                Text("Choose an option", color = Color.White.copy(alpha = .62f), fontSize = 13.sp)
                options.forEachIndexed { index, option ->
                    TvSurface(onClick = { onSelect(index); expanded = false }, modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(13.dp)),
                        colors = ClickableSurfaceDefaults.colors(containerColor = if (index == selected) Color(0x553D7890) else Color(0x22FFFFFF), focusedContainerColor = Color(0x66517FA0))) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(option, color = Color.White, fontSize = 16.sp)
                            Spacer(Modifier.weight(1f))
                            if (index == selected) Text("✓  SELECTED", color = Color(0xFF9FD5FF), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AmbientAction(title: String, value: String, hint: String, onClick: () -> Unit) {
    TvSurface(onClick = onClick, modifier = Modifier.fillMaxWidth().height(68.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0x33202B35), focusedContainerColor = Color(0x66517FA0))) {
        Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = Color.White, fontSize = 16.sp)
                Text(value, color = Color.White.copy(alpha = .58f), fontSize = 12.sp)
            }
            Text(hint, color = Color(0xFF9FD5FF), fontSize = 11.sp)
        }
    }
}
