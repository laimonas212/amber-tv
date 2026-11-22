package dev.amber.tv

import android.content.Intent
import android.content.Context
import android.content.BroadcastReceiver
import android.content.IntentFilter
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme as Material3Theme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.core.content.ContextCompat
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
private const val PROFILE_META_PREFS = "amber_tv_profiles"
private data class LocalProfile(val id: String, val name: String)
private fun saveProfiles(context: Context, profiles: List<LocalProfile>, activeId: String) {
    val array = org.json.JSONArray()
    profiles.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name)) }
    context.getSharedPreferences(PROFILE_META_PREFS, 0).edit().putString("profiles", array.toString()).putString("active_profile", activeId).apply()
}
private fun readProfiles(context: Context): List<LocalProfile> {
    val meta = context.getSharedPreferences(PROFILE_META_PREFS, 0)
    meta.getString("profiles", null)?.let { raw ->
        runCatching { val a=org.json.JSONArray(raw); (0 until a.length()).map { i -> val o=a.getJSONObject(i); LocalProfile(o.getString("id"),o.getString("name")) } }.getOrNull()?.let { if(it.isNotEmpty()) return it }
    }
    val id="profile_1"
    val legacy=context.getSharedPreferences(PREFS,0)
    val target=context.getSharedPreferences("amber_tv_$id",0)
    if(target.all.isEmpty() && legacy.all.isNotEmpty()) {
        val e=target.edit()
        legacy.all.forEach { (k,v) -> when(v) {
            is String -> e.putString(k,v); is Boolean -> e.putBoolean(k,v); is Int -> e.putInt(k,v)
            is Long -> e.putLong(k,v); is Float -> e.putFloat(k,v)
            is Set<*> -> e.putStringSet(k,v.filterIsInstance<String>().toSet())
        } }
        e.apply()
    }
    val initial=listOf(LocalProfile(id,"Laimonas")); saveProfiles(context,initial,id); return initial
}
private const val KEY_DOCK = "dock_packages"
private const val KEY_MONO = "monochrome_icons"
private const val KEY_ICON_MODE = "icon_display_mode"
private const val KEY_ICON_NO_BG = "icon_no_adaptive_background"
private const val KEY_ICON_PACK = "icon_pack_package"
private const val KEY_FONT = "font_choice"
private const val KEY_WALLPAPER = "wallpaper_choice"
private const val KEY_WALLPAPER_ID = "wallpaper_choice_id"
private const val KEY_WEATHER = "show_weather"
private const val KEY_SHOW_APP_NAMES = "show_app_names"
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_ACCENT = "theme_accent"
private const val KEY_SURFACE = "theme_surface"
private const val KEY_WALLPAPER_CACHE = "remote_wallpaper_catalog"
private data class WallpaperItem(val id: String, val title: String, val category: String, val url: String, val thumbnail: String = url.replace("w=2400", "w=1200"), val credit: String = "Unsplash")

private const val WALLPAPER_CATALOG_URL = "https://amberweb.lt/amber-tv/wallpapers.json"

private val defaultWallpapers = emptyList<WallpaperItem>()

private fun remoteWallpaperId(prefs: android.content.SharedPreferences, id: String?) { if (!id.isNullOrBlank()) prefs.edit().putString(KEY_WALLPAPER_ID, id).apply() }

private fun resolveWallpaperAsset(rawPath: String, folder: String): String? {
    val value = rawPath.trim()
    if (value.isBlank()) return null
    return runCatching {
        if (value.startsWith("https://", ignoreCase = true) || value.startsWith("http://", ignoreCase = true)) {
            URL(value).toString()
        } else {
            val fileName = value.substringAfterLast("/")
            val path = when {
                value.startsWith("/amber-tv/") -> value
                value.startsWith("amber-tv/") -> "/$value"
                value.startsWith("/wallpapers/images/") -> "/amber-tv/wallpapers/$fileName"
                value.startsWith("/wallpapers/thumbnails/") -> "/amber-tv/thumbnails/$fileName"
                value.startsWith("/wallpapers/") -> "/amber-tv/wallpapers/$fileName"
                value.startsWith("/thumbnails/") -> "/amber-tv/thumbnails/$fileName"
                value.startsWith("wallpapers/") || value.startsWith("thumbnails/") -> "/amber-tv/$value"
                value.startsWith("/") -> value
                else -> "/amber-tv/$folder/$value"
            }
            URL("https://amberweb.lt$path").toString()
        }
    }.getOrNull()
}

private fun parseWallpaperCatalog(raw: String): List<WallpaperItem> {
    val items = JSONObject(raw).getJSONArray("wallpapers")
    return buildList {
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val resolvedUrl = resolveWallpaperAsset(item.optString("url"), "wallpapers")
            val resolvedThumbnail = resolveWallpaperAsset(item.optString("thumbnail", item.optString("url")), "thumbnails")
            if (resolvedUrl?.startsWith("https://") == true) add(WallpaperItem(
                id = item.optString("id", "remote-$i"),
                title = item.optString("title", "Wallpaper"),
                category = item.optString("category", "Featured"),
                url = resolvedUrl,
                thumbnail = resolvedThumbnail?.takeIf { it.startsWith("https://") } ?: resolvedUrl,
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


private fun openHomeSettings(context: Context) {
    val candidates = listOf(
        Intent("android.settings.HOME_SETTINGS"),
        Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS"),
        Intent(Settings.ACTION_SETTINGS)
    )
    for (intent in candidates) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: Exception) { }
    }
}

private data class InstalledTvApp(val packageName: String, val activityName: String, val label: String, val icon: Drawable)
private data class InstalledIconPack(val packageName: String, val label: String)

private fun loadInstalledIconPacks(pm: PackageManager): List<InstalledIconPack> {
    val actions = listOf("org.adw.launcher.THEMES", "com.novalauncher.THEME", "com.teslacoilsw.launcher.THEME")
    return actions.flatMap { action ->
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(Intent(action), PackageManager.GET_META_DATA)
    }.mapNotNull { info ->
        val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
        runCatching {
            val res = pm.getResourcesForApplication(pkg)
            res.assets.open("appfilter.xml").close()
            InstalledIconPack(pkg, info.loadLabel(pm)?.toString() ?: pkg)
        }.getOrNull()
    }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
}

private fun currentIconPack(context: Context): String {
    val active = context.getSharedPreferences(PROFILE_META_PREFS, 0).getString("active_profile", "profile_1") ?: "profile_1"
    return context.getSharedPreferences("amber_tv_$active", 0).getString(KEY_ICON_PACK, "") ?: ""
}

private fun themedAppIcon(context: Context, app: InstalledTvApp): Drawable? {
    val pack = currentIconPack(context).takeIf { it.isNotBlank() } ?: return null
    return runCatching {
        val res = context.packageManager.getResourcesForApplication(pack)
        val xml = res.assets.open("appfilter.xml").bufferedReader().use { it.readText() }
        val entries = Regex("""<item\s+component="ComponentInfo\{([^}]+)}"\s+drawable="([^"]+)"\s*/>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml).map { it.groupValues[1] to it.groupValues[2] }.toList()
        val exact = app.packageName + "/" + app.activityName
        val name = entries.firstOrNull { it.first == exact || it.first == "/" + app.activityName }?.second
            ?: entries.firstOrNull { it.first.startsWith(app.packageName + "/") }?.second
            ?: return@runCatching null
        val id = res.getIdentifier(name, "drawable", pack).takeIf { it != 0 } ?: res.getIdentifier(name, "mipmap", pack)
        if (id == 0) null else res.getDrawable(id, null)
    }.getOrNull()
}
private fun appIconBitmap(context: Context, app: InstalledTvApp, removeAdaptiveBackground: Boolean, size: Int = 160): Bitmap {
    val drawable = themedAppIcon(context, app) ?: app.icon
    val iconDrawable = if (removeAdaptiveBackground && drawable is AdaptiveIconDrawable) drawable.foreground else drawable
    val sourceSize = size * 2
    val source = Bitmap.createBitmap(sourceSize, sourceSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(source)
    iconDrawable.setBounds(0, 0, sourceSize, sourceSize)
    iconDrawable.draw(canvas)
    if (!removeAdaptiveBackground) return Bitmap.createScaledBitmap(source, size, size, true)
    val pixels = IntArray(sourceSize * sourceSize)
    source.getPixels(pixels, 0, sourceSize, 0, 0, sourceSize, sourceSize)
    var left=sourceSize; var top=sourceSize; var right=-1; var bottom=-1
    for (y in 0 until sourceSize) for (x in 0 until sourceSize) if ((pixels[y*sourceSize+x] ushr 24)>12) {
        if(x<left) left=x; if(x>right) right=x; if(y<top) top=y; if(y>bottom) bottom=y
    }
    if(right<left || bottom<top) return Bitmap.createScaledBitmap(source,size,size,true)
    val pad=(maxOf(right-left+1,bottom-top+1)*.07f).toInt()
    left=(left-pad).coerceAtLeast(0); top=(top-pad).coerceAtLeast(0)
    right=(right+pad).coerceAtMost(sourceSize-1); bottom=(bottom+pad).coerceAtMost(sourceSize-1)
    val cropped=Bitmap.createBitmap(source,left,top,right-left+1,bottom-top+1)
    val output=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888)
    val outCanvas=Canvas(output)
    val scale=(size*.82f)/maxOf(cropped.width,cropped.height)
    val width=(cropped.width*scale).toInt(); val height=(cropped.height*scale).toInt()
    val dest=android.graphics.Rect((size-width)/2,(size-height)/2,(size+width)/2,(size+height)/2)
    outCanvas.drawBitmap(cropped,null,dest,android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG))
    return output
}
private enum class Screen { HOME, SETTINGS, WALLPAPERS, AMBIENT, LAUNCHER, ALL_APPS, MANAGE_DOCK }
private data class AmberPalette(val surface: Color, val focus: Color, val accent: Color, val border: Color, val text: Color, val mutedText: Color)
private val LocalAmberPalette = androidx.compose.runtime.staticCompositionLocalOf { AmberPalette(Color(0xB31A242B), Color(0xB34C6578), Color(0xFF78B7FF), Color(0x55FFFFFF), Color.White, Color.White.copy(alpha = .58f)) }

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
    val activity = context as? ComponentActivity
    val meta = remember { context.getSharedPreferences(PROFILE_META_PREFS, 0) }
    var profiles by remember { mutableStateOf(readProfiles(context)) }
    var activeProfileId by remember { mutableStateOf(meta.getString("active_profile", profiles.first().id) ?: profiles.first().id) }
    var showProfiles by remember { mutableStateOf(false) }
    var renameProfile by remember { mutableStateOf<LocalProfile?>(null) }
    val activeProfile = profiles.firstOrNull { it.id == activeProfileId } ?: profiles.first()
    val prefs = remember(activeProfileId) { context.getSharedPreferences("amber_tv_$activeProfileId", 0) }
    var themeMode by remember { mutableIntStateOf(prefs.getInt(KEY_THEME_MODE, 0).coerceIn(0, 1)) }
    var accentChoice by remember { mutableIntStateOf(prefs.getInt(KEY_ACCENT, 0).coerceIn(0, 10)) }
    var surfaceChoice by remember { mutableIntStateOf(prefs.getInt(KEY_SURFACE, 0).coerceIn(0, 9)) }
    val accentColors = listOf(Color(0xFF78B7FF),Color(0xFFB69CFF),Color(0xFF72D6C1),Color(0xFFFFB86B),Color(0xFFFF82A8),Color(0xFFD0D5DA),Color(0xFFFFA6C9),Color(0xFFFFD166),Color(0xFFFF9B85),Color(0xFFC5A3FF),Color(0xFFA8D98B))
    val surfaceColors = listOf(Color(0xFF17232C),Color(0xFF101820),Color(0xFF25212F),Color(0xFF202A24),Color(0xFF282828),Color(0xFF352432),Color(0xFF382F1D),Color(0xFF3A2925),Color(0xFF29233A),Color(0xFF263329))
    val glassSurface = if (themeMode == 1) Color(0xDDEEF2F6) else surfaceColors[surfaceChoice].copy(alpha = .78f)
    val glassFocus = accentColors[accentChoice].copy(alpha = if (themeMode == 1) .34f else .30f)
    val apps = remember(context) { loadInstalledTvApps(context.packageManager) }
    var screen by remember { mutableStateOf(Screen.HOME) }
    var iconMode by remember { mutableIntStateOf(prefs.getInt(KEY_ICON_MODE, if (prefs.getBoolean(KEY_MONO, false)) 1 else 0).coerceIn(0, 2)) }
    val mono = iconMode != 0
    var removeIconBackground by remember { mutableStateOf(prefs.getBoolean(KEY_ICON_NO_BG, false)) }
    var iconPackPackage by remember { mutableStateOf(prefs.getString(KEY_ICON_PACK, "") ?: "") }
    var showIconPackPicker by remember { mutableStateOf(false) }
    val installedIconPacks = remember(context) { loadInstalledIconPacks(context.packageManager) }
    var fontChoice by remember { mutableIntStateOf(prefs.getInt(KEY_FONT, 0)) }
    var wallpaperItems by remember {
        mutableStateOf(runCatching { parseWallpaperCatalog(prefs.getString(KEY_WALLPAPER_CACHE, null).orEmpty()) }.getOrDefault(emptyList()))
    }
    var wallpaperChoice by remember {
        mutableIntStateOf(
            wallpaperItems.indexOfFirst { it.id == prefs.getString(KEY_WALLPAPER_ID, null) }
                .takeIf { it >= 0 } ?: prefs.getInt(KEY_WALLPAPER, 0).coerceAtLeast(0)
        )
    }
    var showWeather by remember { mutableStateOf(prefs.getBoolean(KEY_WEATHER, false)) }
    var showAppNames by remember { mutableStateOf(prefs.getBoolean(KEY_SHOW_APP_NAMES, true)) }
    var desktopKenBurns by remember { mutableStateOf(prefs.getBoolean("desktop_ken_burns_enabled", false)) }
    var desktopKenBurnsSpeed by remember { mutableFloatStateOf(prefs.getFloat("desktop_ken_burns_speed", 1f).coerceIn(.5f, 1.5f)) }
    var desktopKenBurnsZoom by remember { mutableFloatStateOf(prefs.getFloat("desktop_ken_burns_zoom", 1.06f).coerceIn(1.02f, 1.15f)) }
    var dockWiggle by remember { mutableStateOf(false) }
    var dockActionPackage by remember { mutableStateOf<String?>(null) }
    var movingDockPackage by remember { mutableStateOf<String?>(null) }
    var showAddDockDialog by remember { mutableStateOf(false) }
    val dockListState = rememberLazyListState()
    var lastDockFocusedIndex by remember { mutableIntStateOf(-1) }
    var dockFocusedIndex by remember { mutableIntStateOf(-1) }
    var dockScrollJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    var weather by remember { mutableStateOf<WeatherInfo?>(null) }
    var wakeRefreshTick by remember { mutableIntStateOf(0) }
    // Android TV's POWER button usually suspends the display without killing this process.
    // Listen for the screen waking so remote services refresh immediately instead of waiting for the next timer.
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: android.content.Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_ON) wakeRefreshTick++
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_SCREEN_ON),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
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
    BackHandler(showAddDockDialog || dockActionPackage != null || movingDockPackage != null || screen != Screen.HOME || dockWiggle) {
        when { showAddDockDialog -> showAddDockDialog = false; dockActionPackage != null -> dockActionPackage = null; movingDockPackage != null -> movingDockPackage = null; dockWiggle -> dockWiggle = false; else -> screen = if (screen == Screen.MANAGE_DOCK) Screen.SETTINGS else Screen.HOME }
    }
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
    LaunchedEffect(showWeather, wakeRefreshTick) {
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
    LaunchedEffect(wakeRefreshTick) {
        while (true) {
            val remote = fetchRemoteWallpapers()
            if (remote.isNotEmpty()) {
                wallpaperItems = remote
                wallpaperChoice = remote.indexOfFirst { it.id == prefs.getString(KEY_WALLPAPER_ID, null) }
                    .takeIf { it >= 0 } ?: prefs.getInt(KEY_WALLPAPER, 0).coerceIn(remote.indices)
                if (prefs.getString(KEY_WALLPAPER_ID, null).isNullOrBlank()) {
                    prefs.edit().putString(KEY_WALLPAPER_ID, remote[wallpaperChoice].id).apply()
                }
                prefs.edit().putString(KEY_WALLPAPER_CACHE, JSONObject().put("wallpapers", org.json.JSONArray().apply {
                    remote.forEach { item ->
                        put(JSONObject().put("id", item.id).put("title", item.title).put("category", item.category)
                            .put("url", item.url).put("thumbnail", item.thumbnail).put("credit", item.credit))
                    }
                }).toString()).apply()
            }
            delay(if (remote.isEmpty()) 2 * 60 * 1000L else 15 * 60 * 1000L)
        }
    }
    val font = when (fontChoice) { 1 -> FontFamily.Serif; 2 -> FontFamily.Monospace; else -> FontFamily.SansSerif }

    CompositionLocalProvider(LocalAmberPalette provides AmberPalette(glassSurface, glassFocus, accentColors[accentChoice], if (themeMode == 1) Color.White.copy(alpha = .9f) else Color.White.copy(alpha = .38f), if (themeMode == 1) Color(0xFF17232C) else Color.White, if (themeMode == 1) Color(0xFF52616A) else Color.White.copy(alpha = .58f))) {
    val desktopMotion = rememberInfiniteTransition(label = "desktop-wallpaper-motion")
    val desktopMotionProgress by desktopMotion.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(durationMillis = (45_000f / desktopKenBurnsSpeed).toInt().coerceIn(20_000, 120_000), easing = androidx.compose.animation.core.LinearEasing), repeatMode = RepeatMode.Reverse), label = "desktop-wallpaper-progress")
    Box(Modifier.fillMaxSize().background(surfaceColors[surfaceChoice])) {
        wallpaperItems.getOrNull(wallpaperChoice)?.let { wallpaper ->
            AsyncImage(wallpaper.url, null, Modifier.fillMaxSize().graphicsLayer {
                val scale = if (desktopKenBurns) 1f + (desktopKenBurnsZoom - 1f) * desktopMotionProgress else 1f
                scaleX = scale; scaleY = scale
                translationX = if (desktopKenBurns) size.width * .006f * desktopMotionProgress else 0f
                translationY = if (desktopKenBurns) -size.height * .004f * desktopMotionProgress else 0f
            }, contentScale = ContentScale.Crop)
        }
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                (fadeIn(tween(240, easing = FastOutSlowInEasing)) + slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { it / 22 }) togetherWith
                    (fadeOut(tween(150)) + slideOutHorizontally(tween(190)) { -it / 28 })
            },
            label = "amber-screen-transition"
        ) { activeScreen ->
        when (activeScreen) {
            Screen.HOME -> {
                Box(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(start = 72.dp, end = 76.dp, top = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(now.format(DateTimeFormatter.ofPattern("H:mm")), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Light, fontFamily = font)
                        if (showWeather) {
                            if (weather != null) WeatherChip(weather!!)
                            else Text("Weather unavailable · retrying", color = Color.White.copy(alpha = .65f), fontSize = 13.sp)
                        }
                    }
                    Row(Modifier.padding(horizontal = 4.dp, vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProfileHeaderButton { showProfiles = true }
                        HeaderButton(Icons.Default.Apps, "All apps") { screen = Screen.ALL_APPS }
                        HeaderButton(Icons.Default.Settings, "Launcher settings") { screen = Screen.SETTINGS }
                    }
                }
                val dockApps = dock.mapNotNull { p -> apps.find { it.packageName == p } }
                    val dockShape = RoundedCornerShape(36.dp)
                    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 74.dp, end = 74.dp, bottom = 40.dp).height(148.dp).clip(dockShape)) {
                        // Translucent frosted-glass Dock surface; no duplicate wallpaper layer.
                        Box(
                            Modifier.fillMaxSize()
                                .clip(dockShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            LocalAmberPalette.current.surface.copy(alpha = .58f),
                                            LocalAmberPalette.current.surface.copy(alpha = .62f),
                                            LocalAmberPalette.current.surface.copy(alpha = .84f)
                                        )
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = .18f), dockShape)
                        )
                        LazyRow(
                            state = dockListState,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 6.dp)
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(brush = Brush.horizontalGradient(0f to Color.Transparent, 0.10f to Color.White, 0.90f to Color.White, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                                },
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(dockApps, key = { it.packageName }) { app ->
                                val index = dock.indexOf(app.packageName)
                                AppDockItem(app, index, iconMode, removeIconBackground, font, dockWiggle, showAppNames,
                                    isMoving = movingDockPackage == app.packageName,
                                    onLongPress = { dockActionPackage = app.packageName },
                                    onRemove = { saveDock(dock - app.packageName); dockActionPackage = null },
                                    onClick = { if (movingDockPackage == app.packageName) movingDockPackage = null else if (dockWiggle) dockWiggle = false else launch(app) },
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
                            item(key = "dock_add_button") {
                                AddDockButton { showAddDockDialog = true }
                            }
                        }
                    }
                }
            }
            Screen.SETTINGS -> SettingsScreen(iconMode, removeIconBackground, fontChoice, wallpaperChoice, wallpaperItems.size, showAppNames,
                themeMode = themeMode, accentChoice = accentChoice, surfaceChoice = surfaceChoice,
                onTheme = { themeMode = it; prefs.edit().putInt(KEY_THEME_MODE, themeMode).apply() },
                onAccent = { accentChoice = it; prefs.edit().putInt(KEY_ACCENT, accentChoice).apply() },
                onSurface = { surfaceChoice = it; prefs.edit().putInt(KEY_SURFACE, surfaceChoice).apply() },
                onIconMode = { iconMode = it; prefs.edit().putInt(KEY_ICON_MODE, iconMode).putBoolean(KEY_MONO, iconMode != 0).apply() },
                onIconBackground = { removeIconBackground = it; prefs.edit().putBoolean(KEY_ICON_NO_BG, it).apply() },
                iconPackName = installedIconPacks.firstOrNull { it.packageName == iconPackPackage }?.label ?: if (iconPackPackage.isBlank()) "Default icons" else "Unavailable",
                onIconPack = { showIconPackPicker = true },
                onShowAppNames = { showAppNames = it; prefs.edit().putBoolean(KEY_SHOW_APP_NAMES, it).apply() },
                onFont = { fontChoice = it; prefs.edit().putInt(KEY_FONT, fontChoice).apply() },
                onWallpaper = { screen = Screen.WALLPAPERS },
                showWeather = showWeather,
                onWeather = { showWeather = it; prefs.edit().putBoolean(KEY_WEATHER, it).apply() },
                onAmbient = { screen = Screen.AMBIENT },
                onSystem = { runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                onDefaultLauncher = { screen = Screen.LAUNCHER },
                desktopKenBurns = desktopKenBurns,
                desktopKenBurnsSpeed = desktopKenBurnsSpeed,
                desktopKenBurnsZoom = desktopKenBurnsZoom,
                onDesktopKenBurns = { enabled -> desktopKenBurns = enabled; prefs.edit().putBoolean("desktop_ken_burns_enabled", enabled).apply() },
                onDesktopKenBurnsSpeed = { speed -> desktopKenBurnsSpeed = speed; prefs.edit().putFloat("desktop_ken_burns_speed", speed).apply() },
                onDesktopKenBurnsZoom = { zoom -> desktopKenBurnsZoom = zoom; prefs.edit().putFloat("desktop_ken_burns_zoom", zoom).apply() },
                onBack = { screen = Screen.HOME })
            Screen.AMBIENT -> AmbientSettingsScreen(prefs, onBack = { screen = Screen.SETTINGS }, onOpenSystemSettings = { runCatching { context.startActivity(Intent(Settings.ACTION_DREAM_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } } }, onStartNow = { runCatching { context.startActivity(Intent(context, AmberAmbientActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } })
            Screen.LAUNCHER -> LauncherSwitcherScreen(
                onBack = { screen = Screen.SETTINGS },
                onAmberTv = {
                    val manager = if (android.os.Build.VERSION.SDK_INT >= 29) context.getSystemService(android.app.role.RoleManager::class.java) else null
                    val request = manager?.takeIf { it.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME) }?.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME)
                    if (request != null) runCatching { context.startActivity(request) }.onFailure { openHomeSettings(context) } else openHomeSettings(context)
                },
                onGoogleTv = {
                    val googlePackage = "com.google.android.apps.tv.launcherx"
                    val disabled = runCatching { !context.packageManager.getApplicationInfo(googlePackage, PackageManager.MATCH_DISABLED_COMPONENTS).enabled }.getOrDefault(false)
                    if (disabled) {
                        runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$googlePackage"))) }.onFailure { openHomeSettings(context) }
                    } else openHomeSettings(context)
                },
                onHomeButtonHelper = {
                    runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
            )
            Screen.WALLPAPERS -> if (wallpaperItems.isNotEmpty()) WallpaperLibraryScreen(wallpaperItems, wallpaperChoice.coerceIn(wallpaperItems.indices), dock.mapNotNull { p -> apps.find { it.packageName == p } }, mono, font, onSelect = { index ->
                    wallpaperChoice = index
                    remoteWallpaperId(prefs, wallpaperItems.getOrNull(index)?.id)
                    prefs.edit().putInt(KEY_WALLPAPER, index).commit()
                }, onBack = { screen = Screen.SETTINGS }) else {
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
        }
        if (dockActionPackage != null) {
            val selectedApp = apps.firstOrNull { it.packageName == dockActionPackage }
            if (selectedApp != null) DockActionDialog(selectedApp, onDismiss = { dockActionPackage = null },
                onMove = { movingDockPackage = selectedApp.packageName; dockActionPackage = null },
                onRemove = { saveDock(dock - selectedApp.packageName); dockActionPackage = null })
        }
        if (showIconPackPicker) IconPackPickerDialog(installedIconPacks, iconPackPackage, onSelect = { pack -> iconPackPackage = pack; prefs.edit().putString(KEY_ICON_PACK, pack).apply(); showIconPackPicker = false }, onClose = { showIconPackPicker = false })
        if (showProfiles) ProfilePickerDialog(profiles, activeProfileId,
            onSelect = { p -> activeProfileId=p.id; saveProfiles(context,profiles,p.id); showProfiles=false; activity?.recreate() },
            onAdd = { val id="profile_${System.currentTimeMillis()}"; val name=if(profiles.none { it.name=="Dukra" }) "Dukra" else "Profilis ${profiles.size+1}"; profiles=profiles+LocalProfile(id,name); saveProfiles(context,profiles,activeProfileId) },
            onDelete = { p -> if(profiles.size>1 && p.id!=activeProfileId) { profiles=profiles.filterNot { it.id==p.id }; context.getSharedPreferences("amber_tv_${p.id}",0).edit().clear().apply(); saveProfiles(context,profiles,activeProfileId) } },
            onRename = { renameProfile = it },
            onClose = { showProfiles=false })
        renameProfile?.let { profile -> ProfileRenameDialog(profile.name, onSave = { newName -> profiles=profiles.map { if(it.id==profile.id) it.copy(name=newName) else it }; saveProfiles(context,profiles,activeProfileId); renameProfile=null }, onClose = { renameProfile=null }) }
        if (showAddDockDialog) AddDockDialog(apps, dock, iconMode, removeIconBackground, font,
            onApply = { selected -> saveDock(selected.map { it.packageName }) }, onClose = { showAddDockDialog = false })
        if (splashVisible) AmberBootSplash()
    }
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
private fun ProfileHeaderButton(onClick:()->Unit) {
    TvSurface(onClick,Modifier.size(48.dp),shape=ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
        colors=ClickableSurfaceDefaults.colors(containerColor=Color(0x18FFFFFF),focusedContainerColor=LocalAmberPalette.current.focus),
        scale=ClickableSurfaceDefaults.scale(focusedScale=1.05f)) {
        Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Icon(Icons.Default.AccountCircle, "Profiles", tint=Color.White, modifier=Modifier.size(25.dp)) }
    }
}
@Composable
private fun ProfilePickerDialog(profiles:List<LocalProfile>,activeId:String,onSelect:(LocalProfile)->Unit,onAdd:()->Unit,onDelete:(LocalProfile)->Unit,onRename:(LocalProfile)->Unit,onClose:()->Unit) {
    Dialog(onDismissRequest=onClose) {
        Column(Modifier.width(560.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xF21A242C))
            .border(1.dp,Color(0x55FFFFFF),RoundedCornerShape(28.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Who's watching?",color=Color.White,fontSize=25.sp,fontWeight=FontWeight.SemiBold)
            Text("Each profile keeps its own wallpaper, colours and Dock",color=Color.White.copy(alpha=.62f),fontSize=14.sp)
            profiles.forEach { p ->
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        TvSurface(onClick={onSelect(p)},modifier=Modifier.fillMaxWidth().height(64.dp),shape=ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
                            colors=ClickableSurfaceDefaults.colors(containerColor=if(p.id==activeId) Color(0x553D7890) else Color(0x22FFFFFF),focusedContainerColor=LocalAmberPalette.current.focus)) {
                            Row(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically) {
                                Text("●",color=LocalAmberPalette.current.accent,fontSize=20.sp)
                                Text(p.name,Modifier.padding(start=14.dp),color=Color.White,fontSize=18.sp)
                                Spacer(Modifier.weight(1f))
                                if(p.id==activeId) Text("ACTIVE",color=LocalAmberPalette.current.accent,fontSize=11.sp)
                            }
                        }
                    }
                    TvSurface(onClick={onRename(p)},modifier=Modifier.size(48.dp),shape=ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),colors=ClickableSurfaceDefaults.colors(containerColor=Color(0x22FFFFFF),focusedContainerColor=LocalAmberPalette.current.focus)) { Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Text("✎",color=Color.White,fontSize=20.sp) } }
                    if(profiles.size>1 && p.id!=activeId) TvSurface(onClick={onDelete(p)},modifier=Modifier.size(48.dp),
                        shape=ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),colors=ClickableSurfaceDefaults.colors(containerColor=Color(0x22FFFFFF),focusedContainerColor=Color(0x66B94B55))) {
                        Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Text("−",color=Color.White,fontSize=23.sp) }
                    }
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                TvSurface(onClick=onAdd,modifier=Modifier.weight(1f).height(54.dp),shape=ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                    colors=ClickableSurfaceDefaults.colors(containerColor=Color(0x223FFFFFF),focusedContainerColor=LocalAmberPalette.current.focus)) {
                    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Text("+  Add profile",color=Color.White,fontSize=16.sp) }
                }
                TvSurface(onClick=onClose,modifier=Modifier.width(120.dp).height(54.dp),shape=ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                    colors=ClickableSurfaceDefaults.colors(containerColor=Color(0x223FFFFFF),focusedContainerColor=LocalAmberPalette.current.focus)) {
                    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Text("Close",color=Color.White,fontSize=16.sp) }
                }
            }
        }
    }
}

@Composable
private fun ProfileRenameDialog(currentName:String,onSave:(String)->Unit,onClose:()->Unit) {
    var name by remember(currentName) { mutableStateOf(currentName) }
    Dialog(onDismissRequest=onClose) {
        Column(Modifier.width(460.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xF21A242C))
            .border(1.dp,Color(0x55FFFFFF),RoundedCornerShape(24.dp)).padding(24.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text("Rename profile",color=Color.White,fontSize=23.sp,fontWeight=FontWeight.SemiBold)
            OutlinedTextField(value=name,onValueChange={name=it},singleLine=true,label={Text("Profile name")})
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                TvSurface(onClick=onClose,modifier=Modifier.weight(1f).height(50.dp),shape=ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),colors=ClickableSurfaceDefaults.colors(containerColor=Color(0x22FFFFFF),focusedContainerColor=LocalAmberPalette.current.focus)) { Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Text("Cancel",color=Color.White) } }
                TvSurface(onClick={ if(name.isNotBlank()) onSave(name.trim()) },modifier=Modifier.weight(1f).height(50.dp),shape=ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),colors=ClickableSurfaceDefaults.colors(containerColor=Color(0x553D7890),focusedContainerColor=LocalAmberPalette.current.focus)) { Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Text("Save",color=Color.White) } }
            }
        }
    }
}

@Composable
private fun HeaderButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    TvSurface(onClick, Modifier.size(48.dp), shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0x18FFFFFF), focusedContainerColor = LocalAmberPalette.current.focus),
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
    mono: Boolean, font: FontFamily, onSelect: (Int) -> Unit, onBack: () -> Unit) {
    var category by remember { mutableStateOf("All") }
    var focusedIndex by remember { mutableIntStateOf(selectedIndex) }
    var previewIndex by remember { mutableIntStateOf(selectedIndex) }
    LaunchedEffect(focusedIndex) { delay(90); previewIndex = focusedIndex }
    LaunchedEffect(selectedIndex) { focusedIndex = selectedIndex; previewIndex = selectedIndex }
    val categories = listOf("All") + wallpapers.map { it.category }.distinct()
    val filtered = wallpapers.withIndex().filter { category == "All" || it.value.category == category }
    val wallpaperListState = rememberLazyListState()
    LaunchedEffect(selectedIndex, category, wallpapers.size) {
        val selectedPosition = filtered.indexOfFirst { it.index == selectedIndex }
        if (selectedPosition >= 0) wallpaperListState.animateScrollToItem(selectedPosition)
    }
    Row(Modifier.fillMaxSize().background(Color(0x99101820)).padding(start = 64.dp, end = 64.dp, top = 28.dp, bottom = 28.dp), horizontalArrangement = Arrangement.spacedBy(34.dp)) {
        Column(Modifier.weight(0.82f).fillMaxHeight()) {
            ScreenTitle("Background", onBack)
            Text("Choose a look for your Amber TV home screen", color = Color.White.copy(alpha = .62f), fontSize = 14.sp, modifier = Modifier.padding(start = 66.dp, top = 3.dp, bottom = 18.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                items(categories) { item ->
                    @OptIn(ExperimentalTvMaterial3Api::class)
                    TvSurface(onClick = { category = item }, shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                        colors = ClickableSurfaceDefaults.colors(containerColor = if (category == item) LocalAmberPalette.current.focus else LocalAmberPalette.current.surface, focusedContainerColor = LocalAmberPalette.current.focus)) {
                        Text(item, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp))
                    }
                }
            }
            LazyColumn(state = wallpaperListState, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(filtered, key = { it.value.id }) { indexed ->
                    val wallpaper = indexed.value
                    val selected = indexed.index == selectedIndex
                    @OptIn(ExperimentalTvMaterial3Api::class)
                    TvSurface(onClick = { focusedIndex = indexed.index; previewIndex = indexed.index; onSelect(indexed.index) }, modifier = Modifier.fillMaxWidth().height(58.dp).onFocusChanged { if (it.isFocused) focusedIndex = indexed.index },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                        colors = ClickableSurfaceDefaults.colors(containerColor = if (selected) LocalAmberPalette.current.focus else LocalAmberPalette.current.surface, focusedContainerColor = LocalAmberPalette.current.focus),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.015f)) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(wallpaper.thumbnail, wallpaper.title, Modifier.size(width = 58.dp, height = 38.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(wallpaper.title, color = LocalAmberPalette.current.text, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
                                Text(wallpaper.category, color = LocalAmberPalette.current.mutedText, fontSize = 11.sp)
                            }
                            if (selected) Text("✓", color = LocalAmberPalette.current.accent, fontSize = 18.sp, modifier = Modifier.padding(end = 4.dp))
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
                        .background(LocalAmberPalette.current.surface).border(1.dp, LocalAmberPalette.current.border, RoundedCornerShape(17.dp)).padding(horizontal = 8.dp, vertical = 8.dp),
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
private fun SettingsScreen(iconMode: Int, removeIconBackground: Boolean, fontChoice: Int, wallpaperChoice: Int, wallpaperCount: Int, showAppNames: Boolean, themeMode: Int, accentChoice: Int, surfaceChoice: Int, onTheme: (Int) -> Unit, onAccent: (Int) -> Unit, onSurface: (Int) -> Unit, onShowAppNames: (Boolean) -> Unit, showWeather: Boolean, onWeather: (Boolean) -> Unit, onIconMode: (Int) -> Unit, onIconBackground: (Boolean) -> Unit, iconPackName: String, onIconPack: () -> Unit, onFont: (Int) -> Unit, onWallpaper: () -> Unit, onAmbient: () -> Unit, onSystem: () -> Unit, onDefaultLauncher: () -> Unit, desktopKenBurns: Boolean, desktopKenBurnsSpeed: Float, desktopKenBurnsZoom: Float, onDesktopKenBurns: (Boolean) -> Unit, onDesktopKenBurnsSpeed: (Float) -> Unit, onDesktopKenBurnsZoom: (Float) -> Unit, onBack: () -> Unit) {
    var choiceDialog by remember { mutableStateOf<SettingsChoice?>(null) }
    Column(Modifier.fillMaxSize().padding(start = 76.dp, end = 76.dp, top = 30.dp, bottom = 26.dp)) {
        ScreenTitle("Settings", onBack)
        Text("Make Amber TV feel like yours", color = Color.White.copy(alpha = .68f), fontSize = 16.sp, modifier = Modifier.padding(start = 66.dp, top = 2.dp, bottom = 22.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { SettingsTile("Icon style", listOf("Full color", "Monochrome", "Mono · color on focus")[iconMode], "Choose how app icons look", "◈") { choiceDialog = SettingsChoice("Icon style", listOf("Full color", "Monochrome", "Mono · color on focus"), iconMode, onIconMode) } }
            item { SettingsTile("Icon background", if (removeIconBackground) "Adaptive background off" else "Original app icon", "Remove adaptive icon background where supported", "◉") { onIconBackground(!removeIconBackground) } }
            item { SettingsTile("Icon pack", iconPackName, "Use an independently installed icon pack", "◈", onIconPack) }
            item { SettingsTile("App names", if (showAppNames) "Visible" else "Hidden · larger icons", "Show or hide names below Dock icons", "Aa") { onShowAppNames(!showAppNames) } }
            item { SettingsTile("Desktop Ken Burns", if (desktopKenBurns) "On · slow wallpaper motion" else "Off", "Subtle zoom and pan on the Home background", "◫") { onDesktopKenBurns(!desktopKenBurns) } }
            if (desktopKenBurns) {
                item { SettingsTile("Desktop motion speed", listOf("0.5×","0.75×","1.0×","1.25×","1.5×")[listOf(.5f,.75f,1f,1.25f,1.5f).indexOf(desktopKenBurnsSpeed).coerceAtLeast(0)], "Choose how quickly the wallpaper moves", "↗") { choiceDialog = SettingsChoice("Desktop motion speed", listOf("0.5×","0.75×","1.0×","1.25×","1.5×"), listOf(.5f,.75f,1f,1.25f,1.5f).indexOf(desktopKenBurnsSpeed).coerceAtLeast(0)) { onDesktopKenBurnsSpeed(listOf(.5f,.75f,1f,1.25f,1.5f)[it]) } } }
                item { SettingsTile("Desktop zoom", "${((desktopKenBurnsZoom - 1f) * 100).toInt()}%", "Choose the amount of zoom", "⊕") { choiceDialog = SettingsChoice("Desktop zoom", listOf("2%","4%","6%","8%","10%","12%","15%"), listOf(1.02f,1.04f,1.06f,1.08f,1.10f,1.12f,1.15f).indexOf(desktopKenBurnsZoom).coerceAtLeast(0)) { onDesktopKenBurnsZoom(listOf(1.02f,1.04f,1.06f,1.08f,1.10f,1.12f,1.15f)[it]) } } }
            }
            item { SettingsTile("Typography", listOf("Sans Serif","Serif","Monospace")[fontChoice], "Change the system typeface", "Aa") { choiceDialog = SettingsChoice("Typography", listOf("Sans Serif","Serif","Monospace"), fontChoice, onFont) } }
            item { SettingsTile("Wallpapers", "${wallpaperChoice + 1} of $wallpaperCount backgrounds", "Online library · choose your look", "▧", onWallpaper) }
            item { SettingsTile("Amber Ambient", "Screensaver · customize", "Clock, date, slideshow, OLED dimming", "◷", onAmbient) }
            item { SettingsTile("Weather", if (showWeather) "On · Vilnius" else "Off", "Current conditions on Home", "☼") { onWeather(!showWeather) } }
            item { SettingsTile("Theme", if (themeMode == 0) "Dark" else "Light", "Switch light and dark appearance", "◐") { choiceDialog = SettingsChoice("Theme", listOf("Dark", "Light"), themeMode, onTheme) } }
            item { SettingsTile("Accent colour", listOf("Ice blue","Lavender","Mint","Amber","Rose","Neutral","Pink","Soft yellow","Peach","Lilac","Sage")[accentChoice], "Change focus and highlight colour", "✦") { choiceDialog = SettingsChoice("Accent colour", listOf("Ice blue","Lavender","Mint","Amber","Rose","Neutral","Pink","Soft yellow","Peach","Lilac","Sage"), accentChoice, onAccent) } }
            item { SettingsTile("Glass tint", listOf("Deep blue","Midnight","Plum","Forest","Graphite","Berry","Honey","Rosewood","Violet","Sage")[surfaceChoice], "Change translucent surface tone", "◧") { choiceDialog = SettingsChoice("Glass tint", listOf("Deep blue","Midnight","Plum","Forest","Graphite","Berry","Honey","Rosewood","Violet","Sage"), surfaceChoice, onSurface) } }
            item { SettingsTile("Default launcher", "Choose Amber TV or Google TV", "Change which Home app opens when you press Home", "⌂", onDefaultLauncher) }
            item { SettingsTile("Device Settings", "Network · Display · System", "Android preferences", "⚙", onSystem) }
        }
    }
    choiceDialog?.let { choice ->
        SettingsChoiceDialog(choice.title, choice.options, choice.selected, onSelect = { choice.onSelect(it); choiceDialog = null }, onClose = { choiceDialog = null })
    }
}

private data class SettingsChoice(val title: String, val options: List<String>, val selected: Int, val onSelect: (Int) -> Unit)

@Composable
private fun SettingsChoiceDialog(title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Column(Modifier.width(560.dp).heightIn(max = 620.dp).clip(RoundedCornerShape(26.dp))
            .background(Color(0xF21A242C)).border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(26.dp))
            .padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
            Text("Choose an option", color = Color.White.copy(alpha = .62f), fontSize = 14.sp)
            val accentSwatches = listOf(Color(0xFF78B7FF),Color(0xFFB69CFF),Color(0xFF72D6C1),Color(0xFFFFB86B),Color(0xFFFF82A8),Color(0xFFD0D5DA),Color(0xFFFFA6C9),Color(0xFFFFD166),Color(0xFFFF9B85),Color(0xFFC5A3FF),Color(0xFFA8D98B))
            val surfaceSwatches = listOf(Color(0xFF17232C),Color(0xFF101820),Color(0xFF25212F),Color(0xFF202A24),Color(0xFF282828),Color(0xFF352432),Color(0xFF382F1D),Color(0xFF3A2925),Color(0xFF29233A),Color(0xFF263329))
            val swatches = when (title) { "Accent colour" -> accentSwatches; "Glass tint" -> surfaceSwatches; else -> emptyList() }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(options.size) { index ->
                    val active = index == selected
                    TvSurface(onClick = { onSelect(index) }, modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(13.dp)),
                        colors = ClickableSurfaceDefaults.colors(containerColor = if (active) Color(0x553D7890) else Color(0x22FFFFFF), focusedContainerColor = LocalAmberPalette.current.focus)) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 17.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (index < swatches.size) Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(swatches[index]).border(1.dp, Color.White.copy(alpha = .28f), RoundedCornerShape(7.dp)))
                            Text(options[index], Modifier.padding(start = if (index < swatches.size) 13.dp else 0.dp), color = Color.White, fontSize = 16.sp)
                            Spacer(Modifier.weight(1f))
                            if (active) Text("✓  SELECTED", color = LocalAmberPalette.current.accent, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconPackPickerDialog(packs: List<InstalledIconPack>, selectedPackage: String, onSelect: (String) -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Column(Modifier.width(560.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xF21A242C))
            .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(28.dp)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Icon packs", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
            Text("Installed separately on this device", color = Color.White.copy(alpha = .62f), fontSize = 14.sp)
            TvSurface(onClick = { onSelect("") }, modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                colors = ClickableSurfaceDefaults.colors(containerColor = if (selectedPackage.isBlank()) Color(0x553D7890) else Color(0x22FFFFFF), focusedContainerColor = LocalAmberPalette.current.focus)) {
                Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Default app icons", color = Color.White, fontSize = 17.sp)
                    Spacer(Modifier.weight(1f))
                    if (selectedPackage.isBlank()) Text("ACTIVE", color = LocalAmberPalette.current.accent, fontSize = 11.sp)
                }
            }
            if (packs.isEmpty()) Text("No compatible icon packs found. Install an ADW / Blueprint-compatible pack first.", color = Color.White.copy(alpha = .72f), fontSize = 14.sp)
            packs.forEach { pack ->
                TvSurface(onClick = { onSelect(pack.packageName) }, modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                    colors = ClickableSurfaceDefaults.colors(containerColor = if (selectedPackage == pack.packageName) Color(0x553D7890) else Color(0x22FFFFFF), focusedContainerColor = LocalAmberPalette.current.focus)) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(pack.label, color = Color.White, fontSize = 17.sp)
                        Spacer(Modifier.weight(1f))
                        if (selectedPackage == pack.packageName) Text("ACTIVE", color = LocalAmberPalette.current.accent, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTile(title: String, value: String, hint: String, glyph: String, onClick: () -> Unit) {
    val palette = LocalAmberPalette.current
    @OptIn(ExperimentalTvMaterial3Api::class)
    TvSurface(onClick, Modifier.fillMaxWidth().height(142.dp), shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(22.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = palette.surface, focusedContainerColor = palette.focus),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.025f)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 15.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(glyph, color = palette.text.copy(alpha = .9f), fontSize = 25.sp, fontWeight = FontWeight.Light)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = palette.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(value, color = palette.text.copy(alpha = .88f), fontSize = 14.sp)
                Text(hint, color = palette.mutedText.copy(alpha = .9f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, value: String, hint: String, onClick: () -> Unit) {
    @OptIn(ExperimentalTvMaterial3Api::class)
    TvSurface(onClick, Modifier.fillMaxWidth().height(76.dp), shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = LocalAmberPalette.current.surface, focusedContainerColor = LocalAmberPalette.current.focus),
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
    val context = LocalContext.current
    val bitmap = remember(app.packageName, removeIconBackground, currentIconPack(context)) { appIconBitmap(context, app, removeIconBackground, 160).asImageBitmap() }
    var focused by remember { mutableStateOf(false) }
    TvSurface(onClick, Modifier.height(150.dp).fillMaxWidth().onFocusChanged { focused = it.isFocused }, shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(22.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = LocalAmberPalette.current.surface, focusedContainerColor = LocalAmberPalette.current.focus),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f)) {
        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(bitmap, app.label, Modifier.size(70.dp).clip(RoundedCornerShape(if (removeIconBackground) 17.dp else 17.dp)), contentScale = ContentScale.Fit,
                colorFilter = if (iconMode == 1 || (iconMode == 2 && !focused)) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null)
            Spacer(Modifier.height(10.dp))
            Text(app.label, color = LocalAmberPalette.current.text, fontSize = 15.sp, fontFamily = font, maxLines = 1)
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
    val context = LocalContext.current
    val bitmap = remember(app.packageName, removeIconBackground, currentIconPack(context)) { appIconBitmap(context, app, removeIconBackground, 96).asImageBitmap() }
    var focused by remember { mutableStateOf(false) }
    @OptIn(ExperimentalTvMaterial3Api::class)
    TvSurface(onClick, Modifier.fillMaxWidth().height(58.dp).onFocusChanged { focused = it.isFocused }, shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(13.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = LocalAmberPalette.current.surface, focusedContainerColor = LocalAmberPalette.current.focus)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(bitmap, app.label, Modifier.size(34.dp).clip(RoundedCornerShape(if (removeIconBackground) 8.dp else 0.dp)), colorFilter = if (iconMode == 1 || (iconMode == 2 && !focused)) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null)
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
private fun AddDockButton(onClick: () -> Unit) {
    TvSurface(onClick, Modifier.size(76.dp), shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(22.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = LocalAmberPalette.current.surface, focusedContainerColor = LocalAmberPalette.current.focus),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("+", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Light)
        }
    }
}

@Composable
private fun DockActionDialog(app: InstalledTvApp, onDismiss: () -> Unit, onMove: () -> Unit, onRemove: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Row(
            Modifier.width(330.dp).clip(RoundedCornerShape(28.dp))
                .background(LocalAmberPalette.current.surface)
                .border(1.dp, LocalAmberPalette.current.border, RoundedCornerShape(28.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockGlassAction("↔", "Move", onMove)
            DockGlassAction("−", "Remove", onRemove)
            DockGlassAction("×", "Cancel", onDismiss)
        }
    }
}

@Composable
private fun DockGlassAction(symbol: String, title: String, onClick: () -> Unit) {
    @OptIn(ExperimentalTvMaterial3Api::class)
    TvSurface(
        onClick,
        Modifier.width(96.dp).height(68.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(19.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0x33FFFFFF),
            focusedContainerColor = Color(0x55FFFFFF)
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f)
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(symbol, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Light)
            Text(title, color = Color.White.copy(alpha = .9f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }    }
}

@Composable
private fun AddDockDialog(
    apps: List<InstalledTvApp>,
    dockPackages: List<String>,
    iconMode: Int,
    removeIconBackground: Boolean,
    font: FontFamily,
    onApply: (List<InstalledTvApp>) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(dockPackages.toSet()) }
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.width(760.dp).height(500.dp).clip(RoundedCornerShape(30.dp))
                .background(LocalAmberPalette.current.surface)
                .border(1.dp, LocalAmberPalette.current.border, RoundedCornerShape(30.dp))
                .padding(24.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Dock apps", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                    Text("${selected.size} selected", color = Color.White.copy(alpha = .58f), fontSize = 13.sp)
                }
                DockGlassAction("✓", "Done", { onApply(apps.filter { it.packageName in selected }); onClose() })
            }
            Spacer(Modifier.height(18.dp))
            if (apps.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No apps available", color = Color.White.copy(alpha = .65f), fontSize = 17.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    gridItems(apps, key = { it.packageName }) { app ->
                        val isSelected = app.packageName in selected
                        val bitmap = remember(app.packageName, removeIconBackground, removeIconBackground) { appIconBitmap(context, app, removeIconBackground, 120).asImageBitmap() }
                        @OptIn(ExperimentalTvMaterial3Api::class)
                        TvSurface(
                            onClick = { selected = if (isSelected) selected - app.packageName else selected + app.packageName },
                            modifier = Modifier.height(116.dp),
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color.Transparent,
                                focusedContainerColor = Color(0x20FFFFFF)
                            ),
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.035f)
                        ) {
                            Box(Modifier.fillMaxSize().padding(7.dp)) {
                                Image(
                                    bitmap, app.label,
                                    Modifier.align(Alignment.Center).padding(bottom = 17.dp).size(62.dp),
                                    contentScale = ContentScale.Fit,
                                    colorFilter = if (iconMode == 1) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null
                                )
                                Text(
                                    app.label,
                                    Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                                    color = Color.White.copy(alpha = .88f),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    maxLines = 1
                                )
                                Box(
                                    Modifier.align(Alignment.TopEnd).size(19.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isSelected) Color.White else Color(0x33000000))
                                        .then(if (isSelected) Modifier else Modifier.border(1.dp, Color(0x99FFFFFF), RoundedCornerShape(50))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) Text("✓", color = Color(0xFF202329), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AppDockItem(app: InstalledTvApp, itemIndex: Int, iconMode: Int, removeIconBackground: Boolean, font: FontFamily, wiggle: Boolean, showAppNames: Boolean, isMoving: Boolean, onLongPress: () -> Unit, onRemove: () -> Unit, onClick: () -> Unit, onFocused: () -> Unit, onMove: (Int) -> Unit) {
    val context = LocalContext.current
    val bitmap = remember(app.packageName, removeIconBackground, removeIconBackground) { appIconBitmap(context, app, removeIconBackground, 160).asImageBitmap() }
    var focused by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(focused, isMoving, itemIndex) {
        if (focused) bringIntoViewRequester.bringIntoView()
    }
    val keyScope = rememberCoroutineScope()
    var longPressJob by remember { mutableStateOf<Job?>(null) }
    var longPressTriggered by remember { mutableStateOf(false) }
    var suppressNextOkKeyUp by remember { mutableStateOf(false) }
    Box(Modifier.width(132.dp).height(108.dp), contentAlignment = Alignment.TopEnd) {
        TvSurface({
            // OK is handled entirely in onPreviewKeyEvent to avoid a second click on key release.
        }, Modifier.fillMaxSize().bringIntoViewRequester(bringIntoViewRequester).onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocused() }
            .graphicsLayer { if (isMoving) { scaleX = 1.045f; scaleY = 1.045f } }
            .onPreviewKeyEvent { event ->
                val isOkKey = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
                if (isOkKey && event.type == KeyEventType.KeyUp && suppressNextOkKeyUp) {
                    // Move mode ends on KeyDown; consume its matching KeyUp so it cannot launch the app.
                    suppressNextOkKeyUp = false
                    true
                } else if (isMoving && event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                    onMove(-1); true
                } else if (isMoving && event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
                    onMove(1); true
                } else if (isMoving && event.type == KeyEventType.KeyDown && isOkKey) {
                    suppressNextOkKeyUp = true
                    onClick()
                    true
                } else if (!isMoving && !wiggle && isOkKey && event.type == KeyEventType.KeyDown) {
                    if (longPressJob == null && !longPressTriggered) {
                        longPressJob = keyScope.launch {
                            delay(650)
                            longPressTriggered = true
                            longPressJob = null
                        }
                    }
                    true
                } else if (!isMoving && !wiggle && isOkKey && event.type == KeyEventType.KeyUp) {
                    longPressJob?.cancel()
                    longPressJob = null
                    if (longPressTriggered) {
                        // Open the action bubble only after the hold is released, so this OK KeyUp
                        // cannot activate the first focused action inside the newly opened dialog.
                        longPressTriggered = false
                        onLongPress()
                    } else {
                        onClick()
                    }
                    true
                } else false
            },
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(27.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = if (isMoving) Color.Transparent else Color(0x32FFFFFF)
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.045f)) {
            Column(Modifier.fillMaxSize().padding(horizontal = 11.dp, vertical = if (showAppNames) 10.dp else 7.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Image(bitmap, app.label, Modifier.size(if (showAppNames) 52.dp else 76.dp).clip(RoundedCornerShape(if (removeIconBackground) 18.dp else if (showAppNames) 13.dp else 19.dp)), contentScale = ContentScale.Fit,
                    colorFilter = if (iconMode == 1 || (iconMode == 2 && !focused)) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null)
                if (showAppNames) {
                    Spacer(Modifier.height(6.dp))
                    Text(app.label, color = Color.White.copy(alpha = .96f), fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, fontFamily = font)
                }
            }
        }
        if (isMoving) {
            Row(Modifier.fillMaxWidth().align(Alignment.Center), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("‹", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Light)
                Text("›", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Light)
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