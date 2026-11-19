package dev.amber.tv

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.Bitmap
import android.graphics.Canvas
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
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
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
import kotlinx.coroutines.Job
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
private const val KEY_ICON_MODE = "icon_display_mode"
private const val KEY_ICON_NO_BG = "icon_no_adaptive_background"
private const val KEY_FONT = "font_choice"
private const val KEY_WALLPAPER = "wallpaper_choice"
private const val KEY_WEATHER = "show_weather"
private const val KEY_SHOW_APP_NAMES = "show_app_names"
private const val KEY_WALLPAPER_CACHE = "remote_wallpaper_catalog"
private data class WallpaperItem(val id: String, val title: String, val category: String, val url: String, val thumbnail: String = url.replace("w=2400", "w=1200"), val credit: String = "Unsplash")

private const val WALLPAPER_CATALOG_URL = "https://amberweb.lt/amber-tv/wallpapers.json"

private val defaultWallpapers = emptyList<WallpaperItem>()

private fun parseWallpaperCatalog(raw: String): List<WallpaperItem> {
    val items = JSONObject(raw).getJSONArray("wallpapers")
    return buildList {
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val url = item.optString("url")
            if (url.startsWith("https://")) add(WallpaperItem(
                id = item.optString("id", "remote-$i"),
                title = item.optString("title", "Wallpaper"),
                category = item.optString("category", "Featured"),
                url = url,
                thumbnail = item.optString("thumbnail", url).takeIf { it.startsWith("https://") } ?: url,
                credit = item.optString("credit", "")
            ))
        }
    }
}

private suspend fun fetchRemoteWallpapers(): List<WallpaperItem> = withContext(Dispatchers.IO) {
    runCatching {
        val connection = URL(WALLPAPER_CATALOG_URL).openConnection().apply {
            connectTimeout = 10000
            readTimeout = 12000
            useCaches = false
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("Pragma", "no-cache")
            setRequestProperty("User-Agent", "AmberTV/1.0")
        }
        val raw = connection.getInputStream().bufferedReader().use { it.readText() }
        parseWallpaperCatalog(raw).also { require(it.isNotEmpty()) { "Wallpaper catalog is empty" } }
    }.getOrDefault(emptyList())
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Material3Theme { TvMaterialTheme { AmberHome() } } }
    }
}

private data class InstalledTvApp(val packageName: String, val activityName: String, val label: String, val icon: Drawable)
private fun appIconBitmap(app: InstalledTvApp, removeAdaptiveBackground: Boolean, size: Int = 160): Bitmap {
    val drawable = if (removeAdaptiveBackground && app.icon is AdaptiveIconDrawable) app.icon.foreground else app.icon
    val sourceSize = size * 2
    val source = Bitmap.createBitmap(sourceSize, sourceSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(source)
    drawable.setBounds(0, 0, sourceSize, sourceSize)
    drawable.draw(canvas)
    if (!removeAdaptiveBackground) return Bitmap.createScaledBitmap(source, size, size, true)

    // Trim transparent padding, then fit every foreground into the same visual box.
    val pixels = IntArray(sourceSize * sourceSize)
    source.getPixels(pixels, 0, sourceSize, 0, 0, sourceSize, sourceSize)
    var left = sourceSize
    var top = sourceSize
    var right = -1
    var bottom = -1
    for (y in 0 until sourceSize) {
        for (x in 0 until sourceSize) {
            if ((pixels[y * sourceSize + x] ushr 24) > 12) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
    }
    if (right < left || bottom < top) return Bitmap.createScaledBitmap(source, size, size, true)
    val padding = (maxOf(right - left + 1, bottom - top + 1) * 0.07f).toInt()
    left = (left - padding).coerceAtLeast(0)
    top = (top - padding).coerceAtLeast(0)
    right = (right + padding).coerceAtMost(sourceSize - 1)
    bottom = (bottom + padding).coerceAtMost(sourceSize - 1)
    val cropped = Bitmap.createBitmap(source, left, top, right - left + 1, bottom - top + 1)
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val outCanvas = Canvas(output)
    val scale = (size * 0.82f) / maxOf(cropped.width, cropped.height)
    val width = (cropped.width * scale).toInt()
    val height = (cropped.height * scale).toInt()
    val dest = android.graphics.Rect((size - width) / 2, (size - height) / 2, (size + width) / 2, (size + height) / 2)
    outCanvas.drawBitmap(cropped, null, dest, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG))
    return output
}
private enum class Screen { HOME, SETTINGS, WALLPAPERS, ALL_APPS, MANAGE_DOCK }

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
    var iconMode by remember { mutableIntStateOf(prefs.getInt(KEY_ICON_MODE, if (prefs.getBoolean(KEY_MONO, false)) 1 else 0).coerceIn(0, 2)) }
    val mono = iconMode != 0
    var removeIconBackground by remember { mutableStateOf(prefs.getBoolean(KEY_ICON_NO_BG, false)) }
    var fontChoice by remember { mutableIntStateOf(prefs.getInt(KEY_FONT, 0)) }
    var wallpaperItems by remember {
        mutableStateOf(runCatching { parseWallpaperCatalog(prefs.getString(KEY_WALLPAPER_CACHE, null).orEmpty()) }.getOrDefault(emptyList()))
    }
    var wallpaperChoice by remember { mutableIntStateOf(prefs.getInt(KEY_WALLPAPER, 0).coerceAtLeast(0)) }
    var showWeather by remember { mutableStateOf(prefs.getBoolean(KEY_WEATHER, false)) }
    var showAppNames by remember { mutableStateOf(prefs.getBoolean(KEY_SHOW_APP_NAMES, true)) }
    var dockWiggle by remember { mutableStateOf(false) }
    val dockListState = rememberLazyListState()
    var lastDockFocusedIndex by remember { mutableIntStateOf(-1) }
    var dockFocusedIndex by remember { mutableIntStateOf(-1) }
    var dockScrollJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    var weather by remember { mutableStateOf<WeatherInfo?>(null) }
    var dock by remember {
        mutableStateOf(prefs.getString(KEY_DOCK, null)?.split("|")?.filter(String::isNotBlank) ?: apps.map { it.packageName })
    }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var splashVisible by remember { mutableStateOf(true) }
    fun saveDock(value: List<String>) { dock = value; prefs.edit().putString(KEY_DOCK, value.joinToString("|")).apply() }
    fun moveDockItem(index: Int, delta: Int) { val target = index + delta; if (target in dock.indices) { val next = dock.toMutableList(); val item = next.removeAt(index); next.add(target, item); saveDock(next) } }
    fun launch(app: InstalledTvApp) {
        runCatching { context.startActivity(Intent(Intent.ACTION_MAIN).setClassName(app.packageName, app.activityName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
    BackHandler(screen != Screen.HOME || dockWiggle) { if (dockWiggle) dockWiggle = false else screen = if (screen == Screen.MANAGE_DOCK) Screen.SETTINGS else Screen.HOME }
    // Keep the Home clock live; switching back from Settings updates it immediately.
    LaunchedEffect(screen) {
        if (screen == Screen.HOME) {
            while (true) {
                now = LocalDateTime.now()
                delay(1000L)
            }
        }
    }
    // Retry weather more frequently after failures; use a longer interval once it works.
    LaunchedEffect(showWeather) {
        if (showWeather) {
            while (true) {
                val result = fetchWeather()
                weather = result
                delay(if (result == null) 2 * 60 * 1000L else 30 * 60 * 1000L)
            }
        } else {
            weather = null
        }
    }
    LaunchedEffect(Unit) { delay(1800); splashVisible = false }
    // The web catalog is the only wallpaper source. Keep its last successful copy as an offline cache.
    LaunchedEffect(Unit) {
        while (true) {
            val remote = fetchRemoteWallpapers()
            if (remote.isNotEmpty()) {
                wallpaperItems = remote
                wallpaperChoice = prefs.getInt(KEY_WALLPAPER, 0).coerceIn(remote.indices)
                prefs.edit().putString(KEY_WALLPAPER_CACHE, URL(WALLPAPER_CATALOG_URL).openConnection().apply {
                    connectTimeout = 10000; readTimeout = 12000; useCaches = false
                }.getInputStream().bufferedReader().use { it.readText() }).apply()
            }
            delay(if (remote.isEmpty()) 2 * 60 * 1000L else 6 * 60 * 60 * 1000L)
        }
    }
    val font = when (fontChoice) { 1 -> FontFamily.Serif; 2 -> FontFamily.Monospace; else -> FontFamily.SansSerif }

    Box(Modifier.fillMaxSize().background(Color(0xFF17232C))) {
        wallpaperItems.getOrNull(wallpaperChoice)?.let { wallpaper ->
            AsyncImage(wallpaper.url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
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
                        HeaderButton(Icons.Default.Apps, "All apps") { screen = Screen.ALL_APPS }
                        HeaderButton(Icons.Default.Settings, "Launcher settings") { screen = Screen.SETTINGS }
                    }
                }
                val dockApps = dock.mapNotNull { p -> apps.find { it.packageName == p } }
                if (dockApps.isEmpty()) Text("Your dock is empty. Open Launcher Settings to add apps.", color = Color.White, modifier = Modifier.align(Alignment.Center), fontSize = 20.sp)
                else {
                    val dockShape = RoundedCornerShape(36.dp)
                    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 74.dp, end = 74.dp, bottom = 40.dp).height(148.dp).clip(dockShape)) {
                        wallpaperItems.getOrNull(wallpaperChoice)?.let { wallpaper ->
                            AsyncImage(wallpaper.url, null, Modifier.fillMaxSize().blur(8.dp).clip(dockShape), contentScale = ContentScale.Crop)
                        }
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
                                AppDockItem(app, iconMode, removeIconBackground, font, dockWiggle, showAppNames,
                                    onLongPress = { dockWiggle = true },
                                    onRemove = { saveDock(dock - app.packageName); if (dock.size <= 1) dockWiggle = false },
                                    onClick = { if (dockWiggle) dockWiggle = false else launch(app) },
                                    onFocused = {
                                        val movingRight = lastDockFocusedIndex < 0 || index > lastDockFocusedIndex
                                        lastDockFocusedIndex = index
                                        dockScrollJob?.cancel()
                                        dockScrollJob = scope.launch {
                                            // Move the row by one item only when the focused item reaches an edge.
                                            delay(16)
                                            val item = dockListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                            if (item != null) {
                                                val density = context.resources.displayMetrics.density
                                                val margin = (18f * density).toInt()
                                                val rightEdge = dockListState.layoutInfo.viewportSize.width - margin
                                                val leftEdge = margin
                                                val step = (168f * density)
                                                if (movingRight && item.offset + item.size > rightEdge) {
                                                    dockListState.animateScrollBy(step)
                                                } else if (!movingRight && item.offset < leftEdge) {
                                                    dockListState.animateScrollBy(-step)
                                                }
                                            }
                                        }
                                    },
                                    onMove = { delta -> moveDockItem(index, delta) }
                                )
                            }
                        }
                    }
                }
            }
            Screen.SETTINGS -> SettingsScreen(iconMode, removeIconBackground, fontChoice, wallpaperChoice, wallpaperItems.size, showAppNames,
                onIconMode = { iconMode = (iconMode + 1) % 3; prefs.edit().putInt(KEY_ICON_MODE, iconMode).putBoolean(KEY_MONO, iconMode != 0).apply() },
                onIconBackground = { removeIconBackground = it; prefs.edit().putBoolean(KEY_ICON_NO_BG, it).apply() },
                onShowAppNames = { showAppNames = it; prefs.edit().putBoolean(KEY_SHOW_APP_NAMES, it).apply() },
                onFont = { fontChoice = (fontChoice + 1) % 3; prefs.edit().putInt(KEY_FONT, fontChoice).apply() },
                onWallpaper = { screen = Screen.WALLPAPERS },
                showWeather = showWeather,
                onWeather = { showWeather = it; prefs.edit().putBoolean(KEY_WEATHER, it).apply() },
                onManage = { screen = Screen.MANAGE_DOCK }, onAllApps = { screen = Screen.ALL_APPS },
                onSystem = { runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                onBack = { screen = Screen.HOME })
            Screen.WALLPAPERS -> if (wallpaperItems.isNotEmpty()) WallpaperLibraryScreen(wallpaperItems, wallpaperChoice.coerceIn(wallpaperItems.indices), dock.mapNotNull { p -> apps.find { it.packageName == p } }, mono, font, onSelect = { index -> wallpaperChoice = index; prefs.edit().putInt(KEY_WALLPAPER, index).commit() }, onBack = { screen = Screen.SETTINGS }) else {
                Box(Modifier.fillMaxSize().background(Color(0xE9121B22)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Wallpaper library is temporarily unavailable", color = Color.White, fontSize = 20.sp)
                        Text("Amber TV will retry automatically when the server is reachable.", color = Color.White.copy(alpha = .65f), fontSize = 14.sp)
                        HeaderButton(Icons.Default.ArrowBack, "Back") { screen = Screen.SETTINGS }
                    }
                }
            }
            Screen.ALL_APPS -> AppListScreen("All apps", apps, iconMode, removeIconBackground, font, { screen = Screen.HOME }, ::launch)
            Screen.MANAGE_DOCK -> ManageDockScreen(apps, dock, iconMode, removeIconBackground, font, { screen = Screen.SETTINGS },
                { app -> saveDock(if (app.packageName in dock) dock - app.packageName else dock + app.packageName) },
                { index, delta -> val target = index + delta; if (target in dock.indices) { val next = dock.toMutableList(); val item = next.removeAt(index); next.add(target, item); saveDock(next) } })
        }
        if (splashVisible) AmberBootSplash()
    }
}

@Composable
private fun AmberBootSplash() {
    val context = LocalContext.current
    val icon = remember { context.packageManager.getApplicationIcon(context.packageName).toBitmap(320, 320).asImageBitmap() }
    val transition = rememberInfiniteTransition(label = "amber-glow")
    val glowAlpha = transition.animateFloat(0.07f, 0.13f, infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow-alpha")
    val scale = transition.animateFloat(0.985f, 1.015f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scale")
    Box(Modifier.fillMaxSize().background(Color(0xFF050607)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(contentAlignment = Alignment.Center) {
                // Glow follows the crystal silhouette and stays clear of the wordmark.
                Image(
                    icon,
                    null,
                    Modifier.size(62.dp).graphicsLayer { alpha = glowAlpha.value }.blur(7.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(Color(0xFFFFA52B))
                )
                Image(
                    icon,
                    "Amber crystal",
                    Modifier.size(58.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value },
                    contentScale = ContentScale.Fit
                )
            }
            Text("AMBER TV", color = Color.White.copy(alpha = 0.88f), fontSize = 7.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
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
private fun WallpaperLibraryScreen(
    wallpapers: List<WallpaperItem>, selectedIndex: Int, dockApps: List<InstalledTvApp>,
    mono: Boolean, font: FontFamily, onSelect: (Int) -> Unit, onBack: () -> Unit
) {
    var category by remember { mutableStateOf("All") }
    var focusedIndex by remember { mutableIntStateOf(selectedIndex) }
    var previewIndex by remember { mutableIntStateOf(selectedIndex) }
    LaunchedEffect(focusedIndex) { delay(90); previewIndex = focusedIndex }
    LaunchedEffect(selectedIndex) { focusedIndex = selectedIndex; previewIndex = selectedIndex }
    val categories = listOf("All") + wallpapers.map { it.category }.distinct()
    val filtered = wallpapers.withIndex().filter { category == "All" || it.value.category == category }
    Row(Modifier.fillMaxSize().background(Color(0xE9121B22)).padding(start = 64.dp, end = 64.dp, top = 28.dp, bottom = 28.dp), horizontalArrangement = Arrangement.spacedBy(34.dp)) {
        Column(Modifier.weight(0.82f).fillMaxHeight()) {
            ScreenTitle("Background", onBack)
            Text("Choose a look for your Amber TV home screen", color = Color.White.copy(alpha = .62f), fontSize = 14.sp, modifier = Modifier.padding(start = 66.dp, top = 3.dp, bottom = 18.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                items(categories) { item ->
                    @OptIn(ExperimentalTvMaterial3Api::class)
                    TvSurface(onClick = { category = item }, shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                        colors = ClickableSurfaceDefaults.colors(containerColor = if (category == item) Color(0xCCB66B24) else Color(0x66202E3A), focusedContainerColor = Color(0xDDAA692F))) {
                        Text(item, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp))
                    }
                }
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(filtered, key = { it.value.id }) { indexed ->
                    val wallpaper = indexed.value
                    val selected = indexed.index == selectedIndex
                    @OptIn(ExperimentalTvMaterial3Api::class)
                    TvSurface(onClick = { focusedIndex = indexed.index; previewIndex = indexed.index; onSelect(indexed.index) }, modifier = Modifier.fillMaxWidth().height(58.dp).onFocusChanged { if (it.isFocused) focusedIndex = indexed.index },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                        colors = ClickableSurfaceDefaults.colors(containerColor = if (selected) Color(0xE6F1F4F4) else Color(0x77202B32), focusedContainerColor = if (selected) Color.White else Color(0xAA526779)),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.015f)) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(wallpaper.thumbnail, wallpaper.title, Modifier.size(width = 58.dp, height = 38.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(wallpaper.title, color = if (selected) Color(0xFF17232C) else Color.White, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
                                Text(wallpaper.category, color = if (selected) Color(0xFF52616A) else Color.White.copy(alpha = .58f), fontSize = 11.sp)
                            }
                            if (selected) Text("✓", color = Color(0xFF17232C), fontSize = 18.sp, modifier = Modifier.padding(end = 4.dp))
                        }
                    }
                }
            }
        }
        Column(Modifier.weight(1.18f).fillMaxHeight().padding(top = 68.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val wallpaper = wallpapers[previewIndex.coerceIn(wallpapers.indices)]
            Crossfade(targetState = wallpaper.id, animationSpec = tween(380), label = "wallpaper-preview") {
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(22.dp)).background(Color(0xFF202A31))) {
                    AsyncImage(wallpaper.thumbnail, wallpaper.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x22000000), Color.Transparent, Color(0x99000000)))))
                    Row(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("9:41", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Light, fontFamily = font)
                        Text("ALL APPS     ⚙ SETTINGS", color = Color.White.copy(alpha = .9f), fontSize = 9.sp)
                    }
                    Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth(.92f).padding(bottom = 18.dp).clip(RoundedCornerShape(17.dp))
                        .background(Color(0xB51A2934)).border(1.dp, Color(0x50FFFFFF), RoundedCornerShape(17.dp)).padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        val previewApps = dockApps.take(6)
                        if (previewApps.isEmpty()) Text("Your dock is empty", color = Color.White.copy(alpha = .7f), modifier = Modifier.padding(12.dp), fontSize = 11.sp)
                        else previewApps.forEach { app ->
                            Column(Modifier.weight(1f).padding(vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                val icon = remember(app.packageName) { app.icon.toBitmap(96, 96).asImageBitmap() }
                                Image(icon, app.label, Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)), contentScale = ContentScale.Fit,
                                    colorFilter = if (mono) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null)
                                Text(app.label, color = Color.White, fontSize = 8.sp, maxLines = 1, fontFamily = font)
                            }
                        }
                    }
                }
            }
            Text(wallpaper.title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
            Text("LIVE HOME SCREEN PREVIEW  ·  " + wallpaper.category.uppercase(), color = Color.White.copy(alpha = .56f), fontSize = 10.sp, letterSpacing = 1.2.sp)
            Text("Select a background on the left to apply it instantly", color = Color.White.copy(alpha = .78f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun SettingsScreen(iconMode: Int, removeIconBackground: Boolean, fontChoice: Int, wallpaperChoice: Int, wallpaperCount: Int, showAppNames: Boolean, onShowAppNames: (Boolean) -> Unit, showWeather: Boolean, onWeather: (Boolean) -> Unit, onIconMode: () -> Unit, onIconBackground: (Boolean) -> Unit, onFont: () -> Unit, onWallpaper: () -> Unit, onManage: () -> Unit, onAllApps: () -> Unit, onSystem: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(start = 76.dp, end = 76.dp, top = 30.dp, bottom = 26.dp)) {
        ScreenTitle("Settings", onBack)
        Text("Make Amber TV feel like yours", color = Color.White.copy(alpha = .68f), fontSize = 16.sp, modifier = Modifier.padding(start = 66.dp, top = 2.dp, bottom = 22.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { SettingsTile("Icon style", listOf("Full color", "Monochrome", "Mono · color on focus")[iconMode], "Choose how app icons look", "◈", onIconMode) }
            item { SettingsTile("Icon background", if (removeIconBackground) "Adaptive background off" else "Original app icon", "Remove adaptive icon background where supported", "◉") { onIconBackground(!removeIconBackground) } }
            item { SettingsTile("App names", if (showAppNames) "Visible" else "Hidden · larger icons", "Show or hide names below Dock icons", "Aa") { onShowAppNames(!showAppNames) } }
            item { SettingsTile("Typography", listOf("Sans Serif","Serif","Monospace")[fontChoice], "Change the system typeface", "Aa", onFont) }
            item { SettingsTile("Wallpapers", "${wallpaperChoice + 1} of $wallpaperCount backgrounds", "Online library · choose your look", "▧", onWallpaper) }
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
private fun AppListScreen(title: String, apps: List<InstalledTvApp>, iconMode: Int, removeIconBackground: Boolean, font: FontFamily, onBack: () -> Unit, onLaunch: (InstalledTvApp) -> Unit) {
    Column(Modifier.fillMaxSize().padding(start = 76.dp, end = 76.dp, top = 34.dp, bottom = 26.dp)) {
        ScreenTitle(title, onBack); Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 150.dp), modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            gridItems(apps, key = { it.packageName }) { app -> AppTile(app, iconMode, removeIconBackground, font) { onLaunch(app) } }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppTile(app: InstalledTvApp, iconMode: Int, removeIconBackground: Boolean, font: FontFamily, onClick: () -> Unit) {
    val bitmap = remember(app.packageName, removeIconBackground) { appIconBitmap(app, removeIconBackground, 160).asImageBitmap() }
    var focused by remember { mutableStateOf(false) }
    TvSurface(onClick, Modifier.height(150.dp).fillMaxWidth().onFocusChanged { focused = it.isFocused }, shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(22.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0x70202D38), focusedContainerColor = Color(0xAA526779)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f)) {
        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(bitmap, app.label, Modifier.size(70.dp).clip(RoundedCornerShape(17.dp)), contentScale = ContentScale.Fit,
                colorFilter = if (iconMode == 1 || (iconMode == 2 && !focused)) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null)
            Spacer(Modifier.height(10.dp))
            Text(app.label, color = Color.White, fontSize = 15.sp, fontFamily = font, maxLines = 1)
        }
    }
}

@Composable
private fun ManageDockScreen(apps: List<InstalledTvApp>, dock: List<String>, iconMode: Int, removeIconBackground: Boolean, font: FontFamily, onBack: () -> Unit, onToggle: (InstalledTvApp) -> Unit, onMove: (Int, Int) -> Unit) {
    val selected = dock.mapNotNull { p -> apps.find { it.packageName == p } }
    val remaining = apps.filter { it.packageName !in dock }
    Column(Modifier.fillMaxSize().padding(start = 76.dp, end = 76.dp, top = 26.dp, bottom = 20.dp)) {
        ScreenTitle("Manage Dock", onBack)
        Text("DOCK ORDER  ·  Use arrows to move apps", color = Color(0xFFD0D9DE), fontSize = 12.sp, letterSpacing = 1.5.sp)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            items(selected, key = { it.packageName }) { app ->
                val index = dock.indexOf(app.packageName)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(Modifier.weight(1f)) { AppListRow(app, iconMode, removeIconBackground, font) {} }
                    SmallAction("↑", index > 0) { onMove(index, -1) }
                    SmallAction("↓", index < dock.lastIndex) { onMove(index, 1) }
                    SmallAction("−", true) { onToggle(app) }
                }
            }
            item { Text("ADD APPS", color = Color(0xFFD0D9DE), fontSize = 12.sp, letterSpacing = 1.5.sp) }
            items(remaining, key = { "add_" + it.packageName }) { app ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { AppListRow(app, iconMode, removeIconBackground, font) {} }
                    SmallAction("+", true) { onToggle(app) }
                }
            }
        }
    }
}

@Composable
private fun AppListRow(app: InstalledTvApp, iconMode: Int, removeIconBackground: Boolean, font: FontFamily, onClick: () -> Unit) {
    val bitmap = remember(app.packageName, removeIconBackground) { appIconBitmap(app, removeIconBackground, 96).asImageBitmap() }
    var focused by remember { mutableStateOf(false) }
    @OptIn(ExperimentalTvMaterial3Api::class)
    TvSurface(onClick, Modifier.fillMaxWidth().height(58.dp).onFocusChanged { focused = it.isFocused }, shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(13.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xAA18232C), focusedContainerColor = Color(0xDD435563))) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(bitmap, app.label, Modifier.size(34.dp), colorFilter = if (iconMode == 1 || (iconMode == 2 && !focused)) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null)
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
private fun AppDockItem(app: InstalledTvApp, iconMode: Int, removeIconBackground: Boolean, font: FontFamily, wiggle: Boolean, showAppNames: Boolean, onLongPress: () -> Unit, onRemove: () -> Unit, onClick: () -> Unit, onFocused: () -> Unit, onMove: (Int) -> Unit) {
    val bitmap = remember(app.packageName, removeIconBackground) { appIconBitmap(app, removeIconBackground, 160).asImageBitmap() }
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.width(160.dp).height(124.dp), contentAlignment = Alignment.TopEnd) {
        TvSurface(onClick, Modifier.fillMaxSize().onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocused() }
            .onKeyEvent { event ->
                if (wiggle && event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) { onMove(-1); true }
                else if (wiggle && event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) { onMove(1); true }
                else false
            }
            .pointerInput(app.packageName, wiggle) { detectTapGestures(onLongPress = { onLongPress() }) },
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(27.dp)),
            colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color(0x32FFFFFF)),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.045f)) {
            Column(Modifier.fillMaxSize().padding(horizontal = 11.dp, vertical = if (showAppNames) 10.dp else 7.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Image(bitmap, app.label, Modifier.size(if (showAppNames) 58.dp else 88.dp).clip(RoundedCornerShape(if (showAppNames) 13.dp else 19.dp)), contentScale = ContentScale.Fit,
                    colorFilter = if (iconMode == 1 || (iconMode == 2 && !focused)) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null)
                if (showAppNames) {
                    Spacer(Modifier.height(8.dp))
                    Text(app.label, color = Color.White.copy(alpha = .96f), fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, fontFamily = font)
                }
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
