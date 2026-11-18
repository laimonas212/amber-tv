package dev.amber.tv

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme as Material3Theme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Material3Theme {
                TvMaterialTheme { AmberHome() }
            }
        }
    }
}

private data class InstalledTvApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable,
)

private fun loadInstalledTvApps(packageManager: PackageManager): List<InstalledTvApp> {
    val categories = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
    val resolved = categories.flatMap { category ->
        val query = Intent(Intent.ACTION_MAIN).addCategory(category)
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(query, 0)
    }
    val apps = resolved.mapNotNull { resolveInfo ->
        val activity = resolveInfo.activityInfo ?: return@mapNotNull null
        InstalledTvApp(
            packageName = activity.packageName,
            activityName = activity.name,
            label = resolveInfo.loadLabel(packageManager)?.toString()
                ?.takeIf { it.isNotBlank() } ?: activity.packageName,
            icon = resolveInfo.loadIcon(packageManager),
        )
    }.filter { it.packageName != "dev.amber.tv" }.distinctBy { it.packageName }

    val preferredOrder = listOf(
        "com.stremio.one", "com.netflix.ninja", "com.disney.disneyplus",
        "com.google.android.youtube.tv", "com.apple.atve.androidtv.appletv",
        "com.amazon.amazonvideo.livingroom", "lt.lrt.epika", "lt.go3.android.tv",
        "com.wbd.stream", "com.plexapp.android", "lt.telia.tv", "com.spotify.tv.android",
    )
    return apps.sortedWith(
        compareBy<InstalledTvApp> {
            val index = preferredOrder.indexOf(it.packageName)
            if (index == -1) Int.MAX_VALUE else index
        }.thenBy { it.label.lowercase() }
    )
}

@Composable
private fun AmberHome() {
    val context = LocalContext.current
    val apps = remember(context) { loadInstalledTvApps(context.packageManager) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = LocalDateTime.now()
        }
    }
    val time = now.format(DateTimeFormatter.ofPattern("H:mm"))

    Box(Modifier.fillMaxSize().background(Color(0xFF17232C))) {
        AsyncImage(
            model = "https://images.unsplash.com/photo-1500375592092-40eb2168fd21?auto=format&fit=crop&w=2400&q=85",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to Color(0x1807131D),
                    0.48f to Color(0x0807131D),
                    0.72f to Color(0x5507131D),
                    1.0f to Color(0xA807131D),
                )
            )
        ))

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 72.dp, end = 76.dp, top = 42.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(time, color = Color.White, fontSize = 36.sp,
                fontWeight = FontWeight.Light, letterSpacing = (-1).sp)
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                HeaderIcon(Icons.Default.Photo, "Wallpaper")
                HeaderIcon(Icons.Default.Apps, "Apps")
                HeaderIcon(Icons.Default.Settings, "Settings")
            }
        }

        if (apps.isEmpty()) {
            Text("No TV apps found", color = Color.White,
                modifier = Modifier.align(Alignment.Center), fontSize = 22.sp)
        } else {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 74.dp, end = 74.dp, bottom = 104.dp)
                    .height(174.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xB51A242B))
                    .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(32.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        AppDockItem(app) {
                            runCatching {
                                val launchIntent = Intent(Intent.ACTION_MAIN)
                                    .setClassName(app.packageName, app.activityName)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(launchIntent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
) {
    Icon(imageVector, contentDescription = description,
        tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(25.dp))
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppDockItem(app: InstalledTvApp, onClick: () -> Unit) {
    val iconBitmap = remember(app.packageName) {
        app.icon.toBitmap(width = 192, height = 192).asImageBitmap()
    }
    TvSurface(
        onClick = onClick,
        modifier = Modifier.width(174.dp).height(150.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(25.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color(0x45FFFFFF),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(iconBitmap, contentDescription = app.label,
                modifier = Modifier.size(58.dp), contentScale = ContentScale.Fit)
            Spacer(Modifier.height(13.dp))
            Text(app.label, color = Color.White.copy(alpha = 0.94f),
                fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}
