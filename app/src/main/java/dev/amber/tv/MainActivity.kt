package dev.amber.tv

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme as Material3Theme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.Surface as TvSurface
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val PREFS = "amber_tv_prefs"
private const val KEY_DOCK = "dock_packages"
private const val KEY_MONO = "monochrome_icons"
private const val KEY_FONT = "font_choice"
private const val KEY_WALLPAPER = "wallpaper_choice"
private const val KEY_WEATHER = "show_weather"
private val wallpapers = listOf(
    "https://images.unsplash.com/photo-1500375592092-40eb2168fd21?auto=format&fit=crop&w=2400&q=85",
    "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=2400&q=85",
    "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?auto=format&fit=crop&w=2400&q=85",
    "https://images.unsplash.com/photo-1518837695005-2083093ee35b?auto=format&fit=crop&w=2400&q=85",
    "https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?auto=format&fit=crop&w=2400&q=85",
    "https://images.unsplash.com/photo-1501785888041-af3ef285b470?auto=format&fit=crop&w=2400&q=85",
    "https://images.unsplash.com/photo-1472396961693-142e6e269027?auto=format&fit=crop&w=2400&q=85",
    "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=2400&q=85",
    "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?auto=format&fit=crop&w=2400&q=85",
    "https://images.unsplash.com/photo-1500534623283-312aade485b7?auto=format&fit=crop&w=2400&q=85",
    "https://images.unsplash.com/photo-1439066615861-d1af74d74000?auto=format&fit=crop&w=2400&q=85",
    "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=2400&q=85",
    "https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=2400&q=85",
    "https://images.unsplash.com/photo-1497250681960-ef046c08a56e?auto=format&fit=crop&w=2400&q=85",
    "https://images.unsplash.com/photo-1511497584788-876760111969?auto=format&fit=crop&w=2400&q=85",
    "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=2400&q=85",
    "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?auto=format&fit=crop&w=2400&q=85",
    "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=2400&q=85",
    "https://images.unsplash.com/photo-1490750967868-88aa4486c946?auto=format&fit=crop&w=2400&q=85",
    "https://images.unsplash.com/photo-1472214103451-9374bd1c798e?auto=format&fit=crop&w=2400&q=85"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Material3Theme { TvMaterialTheme { AmberHome() } } }
    }
}

private data class InstalledTvApp(val packageName: String, val activityName: String, val label: String, val icon: Drawable)
private enum class Screen { HOME, SETTINGS, ALL_APPS, MANAGE_DOCK }

private fun loadInstalledTvApps(pm: PackageManager): List<InstalledTvApp> {
    val resolved = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER).flatMap { category ->
        val query = Intent(Intent.ACTION_MAIN).addCategory(category)
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(query, 0)
    }
    val apps = resolved.mapNotNull { info ->
        val a = info.activityInfo ?: return@mapNotNull null
        InstalledTvApp(a.packageName, a.name, info.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: a.packageName, info.loadIcon(pm))
    }.filter { it.packageName != "dev.amber.tv" }.distinctBy { it.packageName }
    val preferred = listOf("com.stremio.one","com.netflix.ninja","com.disney.disneyplus","com.google.android.youtube.tv","com.apple.atve.androidtv.appletv","com.amazon.amazonvideo.livingroom","lt.lrt.epika","lt.go3.android.tv","com.wbd.stream","com.plexapp.android","lt.telia.tv","com.spotify.tv.android")
    return apps.sortedWith(compareBy<InstalledTvApp> { val i = preferred.indexOf(it.packageName); if (i < 0) Int.MAX_VALUE else i }.thenBy { it.label.lowercase() })
}

private data class WeatherInfo(val temperature: Int, val code: Int)

private suspend fun fetchWeather(): WeatherInfo? = withContext(Dispatchers.IO) {
    runCatching {
        val connection = URL("https://api.open-meteo.com/v1/forecast?latitude=54.75&longitude=25.25&current=temperature_2m,weather_code&timezone=Europe%2FVilnius").openConnection().apply { connectTimeout = 5000; readTimeout = 5000 }
        val json = JSONObject(connection.getInputStream().bufferedReader().use { it.readText() })
        val current = json.getJSONObject("current")
        WeatherInfo(current.getDouble("temperature_2m").toInt(), current.getInt("weather_code"))
    }.getOrNull()
}

@Composable
private fun WeatherChip(info: WeatherInfo) {
    val icon = when (info.code) { 0 -> "☀"; 1, 2 -> "◐"; 3 -> "☁"; 45, 48 -> "≋"; in 51..67 -> "☂"; in 71..86 -> "❄"; in 95..99 -> "⚡"; else -> "☁" }
    Text(icon + "  " + info.temperature + "°  Vilnius", color = Color.White.copy(alpha = .9f), fontSize = 16.sp, fontWeight = FontWeight.Light)
}

@Composable
private fun AmberHome() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, 0) }
    val apps = remember(context) { loadInstalledTvApps(context.packageManager) }
    var screen by remember { mutableStateOf(Screen.HOME) }
    var mono by remember { mutableStateOf(prefs.getBoolean(KEY_MONO, false)) }
    var fontChoice by remember { mutableIntStateOf(prefs.getInt(KEY_FONT, 0)) }
    var wallpaperChoice by remember { mutableIntStateOf(prefs.getInt(KEY_WALLPAPER, 0).coerceIn(wallpapers.indices)) }
    var showWeather by remember { mutableStateOf(prefs.getBoolean(KEY_WEATHER, false)) }
    var dockWiggle by remember { mutableStateOf(false) }
    val dockListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var weather by remember { mutableStateOf<WeatherInfo?>(null) }
    var dock by remember {
        mutableStateOf(prefs.getString(KEY_DOCK, null)?.split("|")?.filter(String::isNotBlank) ?: apps.map { it.packageName })
    }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    fun saveDock(value: List<String>) { dock = value; prefs.edit().putString(KEY_DOCK, value.joinToString("|")).apply() }
    fun moveDockItem(index: Int, delta: Int) { val target = index + delta; if (target in dock.indices) { val next = dock.toMutableList(); val item = next.removeAt(index); next.add(target, item); saveDock(next) } }
    fun launch(app: InstalledTvApp) {
        runCatching { context.startActivity(Intent(Intent.ACTION_MAIN).setClassName(app.packageName, app.activityName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
    BackHandler(screen != Screen.HOME || dockWiggle) { if (dockWiggle) dockWiggle = false else screen = if (screen == Screen.MANAGE_DOCK) Screen.SETTINGS else Screen.HOME }
    LaunchedEffect(showWeather) { while (true) { now = LocalDateTime.now(); if (showWeather) weather = fetchWeather(); delay(30 * 60 * 1000L) } }
    val font = when (fontChoice) { 1 -> FontFamily.Serif; 2 -> FontFamily.Monospace; else -> FontFamily.SansSerif }

    Box(Modifier.fillMaxSize().background(Color(0xFF17232C))) {
        AsyncImage(wallpapers[wallpaperChoice], null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colorStops = arrayOf(
            0f to Color(0x1807131D), .48f to Color(0x0807131D), .72f to Color(0x5507131D), 1f to Color(0xA807131D)
        ))))
        when (screen) {
            Screen.HOME -> {
                Row(Modifier.fillMaxWidth().padding(start = 72.dp, end = 76.dp, top = 34.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(now.format(DateTimeFormatter.ofPattern("H:mm")), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Light, fontFamily = font)
                        if (showWeather && weather != null) WeatherChip(weather!!)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        HeaderButton(Icons.Default.Photo, "Wallpaper") { wallpaperChoice = (wallpaperChoice + 1) % wallpapers.size; prefs.edit().putInt(KEY_WALLPAPER, wallpaperChoice).apply() }
                        HeaderButton(Icons.Default.Apps, "All apps") { screen = Screen.ALL_APPS }
                        HeaderButton(Icons.Default.Settings, "Launcher settings") { screen = Screen.SETTINGS }
                    }
                }
                val dockApps = dock.mapNotNull { p -> apps.find { it.packageName == p } }
                if (dockApps.isEmpty()) Text("Your dock is empty. Open Launcher Settings to add apps.", color = Color.White, modifier = Modifier.align(Alignment.Center), fontSize = 20.sp)
                else {
                    val dockShape = RoundedCornerShape(36.dp)
                    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 74.dp, end = 74.dp, bottom = 40.dp).height(148.dp).clip(dockShape)) {
                        AsyncImage(wallpapers[wallpaperChoice], null, Modifier.fillMaxSize().blur(8.dp).clip(dockShape), contentScale = ContentScale.Crop)
                        Box(Modifier.fillMaxSize().clip(dockShape).background(Brush.verticalGradient(listOf(Color(0x680A1118), Color(0x780A1118), Color(0x680A1118)))).border(1.dp, Color(0x45FFFFFF), dockShape))
                        LazyRow(
                            state = dockListState,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(brush = Brush.horizontalGradient(0f to Color.Transparent, 0.10f to Color.White, 0.90f to Color.White, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                                },
                            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(dockApps, key = { it.packageName }) { app ->
                                val index = dock.indexOf(app.packageName)
                                AppDockItem(app, mono, font, dockWiggle,
                                    onLongPress = { dockWiggle = true },
                                    onRemove = { saveDock(dock - app.packageName); if (dock.size <= 1) dockWiggle = false },
                                    onClick = { if (dockWiggle) dockWiggle = false else launch(app) },
                                    onFocused = {
                                        val visible = dockListState.layoutInfo.visibleItemsInfo
                                        val first = visible.firstOrNull()?.index ?: 0
                                        val last = visible.lastOrNull()?.index ?: dockApps.lastIndex
                                        if (index >= last - 1 && last < dockApps.lastIndex) scope.launch { dockListState.animateScrollToItem((index - visible.size + 2).coerceAtLeast(0)) }
                                        else if (index <= first + 1 && first > 0) scope.launch { dockListState.animateScrollToItem((index - 1).coerceAtLeast(0)) }
                                    },
                                    onMove = { delta -> moveDockItem(index, delta) }
                                )
                            }
                        }
                    }
                }
            }
            Screen.SETTINGS -> SettingsScreen(mono, fontChoice, wallpaperChoice,
                onMono = { mono = it; prefs.edit().putBoolean(KEY_MONO, it).apply() },
                onFont = { fontChoice = (fontChoice + 1) % 3; prefs.edit().putInt(KEY_FONT, fontChoice).apply() },
                onWallpaper = { wallpaperChoice = (wallpaperChoice + 1) % wallpapers.size; prefs.edit().putInt(KEY_WALLPAPER, wallpaperChoice).apply() },
                showWeather = showWeather,
                onWeather = { showWeather = it; prefs.edit().putBoolean(KEY_WEATHER, it).apply() },
                onManage = { screen = Screen.MANAGE_DOCK }, onAllApps = { screen = Screen.ALL_APPS },
                onSystem = { runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                onBack = { screen = Screen.HOME })
            Screen.ALL_APPS -> AppListScreen("All apps", apps, mono, font, { screen = Screen.HOME }, ::launch)
            Screen.MANAGE_DOCK -> ManageDockScreen(apps, dock, mono, font, { screen = Screen.SETTINGS },
                { app -> saveDock(if (app.packageName in dock) dock - app.packageName else dock + app.packageName) },
                { index, delta -> val target = index + delta; if (target in dock.indices) { val next = dock.toMutableList(); val item = next.removeAt(index); next.add(target, item); saveDock(next) } })
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeaderButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    TvSurface(onClick, Modifier.size(48.dp), shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color(0x35FFFFFF)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(icon, label, tint = Color.White, modifier = Modifier.size(25.dp)) }
    }
}

@Composable
private fun ScreenTitle(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        HeaderButton(Icons.Default.ArrowBack, "Back", onBack)
        Text(title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsScreen(mono: Boolean, fontChoice: Int, wallpaperChoice: Int, showWeather: Boolean, onWeather: (Boolean) -> Unit, onMono: (Boolean) -> Unit, onFont: () -> Unit, onWallpaper: () -> Unit, onManage: () -> Unit, onAllApps: () -> Unit, onSystem: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(start = 76.dp, end = 76.dp, top = 30.dp, bottom = 26.dp)) {
        ScreenTitle("Settings", onBack)
        Text("Make Amber TV feel like yours", color = Color.White.copy(alpha = .68f), fontSize = 16.sp, modifier = Modifier.padding(start = 66.dp, top = 2.dp, bottom = 22.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { SettingsTile("Appearance", if (mono) "Monochrome icons" else "Full color icons", "Personalize the interface", "◈") { onMono(!mono) } }
            item { SettingsTile("Typography", listOf("Sans Serif","Serif","Monospace")[fontChoice], "Change the system typeface", "Aa", onFont) }
            item { SettingsTile("Wallpapers", "${wallpaperChoice + 1} of ${wallpapers.size} backgrounds", "Explore scenic backgrounds", "▧", onWallpaper) }
            item { SettingsTile("Weather", if (showWeather) "On · Vilnius" else "Off", "Current conditions on Home", "☼") { onWeather(!showWeather) } }
            item { SettingsTile("Customize Dock", "Add, remove and rearrange", "Choose your favorite apps", "▤", onManage) }
            item { SettingsTile("App Library", "Browse installed apps", "Open all applications", "▦", onAllApps) }
            item { SettingsTile("Device Settings", "Network · Display · System", "Android preferences", "⚙", onSystem) }
        }
    }
}

@Composable
private fun SettingsTile(title: String, value: String, hint: String, glyph: String, onClick: () -> Unit) {
    @OptIn(ExperimentalTvMaterial3Api::class)
    TvSurface(onClick, Modifier.fillMaxWidth().height(142.dp), shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(22.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0x76202E3A), focusedContainerColor = Color(0xB34C6578)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.025f)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 15.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(glyph, color = Color.White.copy(alpha = .9f), fontSize = 25.sp, fontWeight = FontWeight.Light)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(value, color = Color.White.copy(alpha = .88f), fontSize = 14.sp)
                Text(hint, color = Color.White.copy(alpha = .52f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, value: String, hint: String, onClick: () -> Unit) {
    @OptIn(ExperimentalTvMaterial3Api::class)
    TvSurface(onClick, Modifier.fillMaxWidth().height(76.dp), shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xB51A242B), focusedContainerColor = Color(0xDD435563)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.015f)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold); Text(hint, color = Color(0xFFB7C2C9), fontSize = 12.sp) }
            Text(value, color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
private fun AppListScreen(title: String, apps: List<InstalledTvApp>, mono: Boolean, font: FontFamily, onBack: () -> Unit, onLaunch: (InstalledTvApp) -> Unit) {
    Column(Modifier.fillMaxSize().padding(start = 76.dp, end = 76.dp, top = 34.dp, bottom = 26.dp)) {
        ScreenTitle(title, onBack); Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 150.dp), modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            gridItems(apps, key = { it.packageName }) { app -> AppTile(app, mono, font) { onLaunch(app) } }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppTile(app: InstalledTvApp, mono: Boolean, font: FontFamily, onClick: () -> Unit) {
    val bitmap = remember(app.packageName) { app.icon.toBitmap(160, 160).asImageBitmap() }
    TvSurface(onClick, Modifier.height(150.dp).fillMaxWidth(), shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(22.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0x70202D38), focusedContainerColor = Color(0xAA526779)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f)) {
        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(bitmap, app.label, Modifier.size(70.dp).clip(RoundedCornerShape(17.dp)), contentScale = ContentScale.Fit,
                colorFilter = if (mono) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null)
            Spacer(Modifier.height(10.dp))
            Text(app.label, color = Color.White, fontSize = 15.sp, fontFamily = font, maxLines = 1)
        }
    }
}

@Composable
private fun ManageDockScreen(apps: List<InstalledTvApp>, dock: List<String>, mono: Boolean, font: FontFamily, onBack: () -> Unit, onToggle: (InstalledTvApp) -> Unit, onMove: (Int, Int) -> Unit) {
    val selected = dock.mapNotNull { p -> apps.find { it.packageName == p } }
    val remaining = apps.filter { it.packageName !in dock }
    Column(Modifier.fillMaxSize().padding(start = 76.dp, end = 76.dp, top = 26.dp, bottom = 20.dp)) {
        ScreenTitle("Manage Dock", onBack)
        Text("DOCK ORDER  ·  Use arrows to move apps", color = Color(0xFFD0D9DE), fontSize = 12.sp, letterSpacing = 1.5.sp)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            items(selected, key = { it.packageName }) { app ->
                val index = dock.indexOf(app.packageName)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(Modifier.weight(1f)) { AppListRow(app, mono, font) {} }
                    SmallAction("↑", index > 0) { onMove(index, -1) }
                    SmallAction("↓", index < dock.lastIndex) { onMove(index, 1) }
                    SmallAction("−", true) { onToggle(app) }
                }
            }
            item { Text("ADD APPS", color = Color(0xFFD0D9DE), fontSize = 12.sp, letterSpacing = 1.5.sp) }
            items(remaining, key = { "add_" + it.packageName }) { app ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { AppListRow(app, mono, font) {} }
                    SmallAction("+", true) { onToggle(app) }
                }
            }
        }
    }
}

@Composable
private fun AppListRow(app: InstalledTvApp, mono: Boolean, font: FontFamily, onClick: () -> Unit) {
    val bitmap = remember(app.packageName) { app.icon.toBitmap(96, 96).asImageBitmap() }
    @OptIn(ExperimentalTvMaterial3Api::class)
    TvSurface(onClick, Modifier.fillMaxWidth().height(58.dp), shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(13.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xAA18232C), focusedContainerColor = Color(0xDD435563))) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(bitmap, app.label, Modifier.size(34.dp), colorFilter = if (mono) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null)
            Spacer(Modifier.width(16.dp)); Text(app.label, color = Color.White, fontSize = 16.sp, fontFamily = font)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SmallAction(label: String, enabled: Boolean, onClick: () -> Unit) {
    TvSurface(onClick, Modifier.size(48.dp), enabled = enabled, shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xAA18232C), focusedContainerColor = Color(0xDD435563))) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(label, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppDockItem(app: InstalledTvApp, mono: Boolean, font: FontFamily, wiggle: Boolean, onLongPress: () -> Unit, onRemove: () -> Unit, onClick: () -> Unit, onFocused: () -> Unit, onMove: (Int) -> Unit) {
    val bitmap = remember(app.packageName) { app.icon.toBitmap(160, 160).asImageBitmap() }
    Box(Modifier.width(160.dp).height(124.dp), contentAlignment = Alignment.TopEnd) {
        TvSurface(onClick, Modifier.fillMaxSize().onFocusChanged { if (it.isFocused) onFocused() }
            .onKeyEvent { event ->
                if (wiggle && event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) { onMove(-1); true }
                else if (wiggle && event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) { onMove(1); true }
                else false
            }
            .pointerInput(app.packageName, wiggle) { detectTapGestures(onLongPress = { onLongPress() }) },
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(27.dp)),
            colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color(0x32FFFFFF)),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.045f)) {
            Column(Modifier.fillMaxSize().padding(horizontal = 11.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Image(bitmap, app.label, Modifier.size(58.dp).clip(RoundedCornerShape(13.dp)), contentScale = ContentScale.Fit,
                    colorFilter = if (mono) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null)
                Spacer(Modifier.height(8.dp))
                Text(app.label, color = Color.White.copy(alpha = .96f), fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, fontFamily = font)
            }
        }
        if (wiggle) {
            TvSurface(onRemove, Modifier.padding(top = 2.dp, end = 2.dp).size(27.dp), shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
                colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xFFE34D4D), focusedContainerColor = Color(0xFFFF7777))) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("−", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
