package dev.amber.tv

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
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
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Material3Theme {
                TvMaterialTheme {
                    AmberHome()
                }
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
    val categories = listOf(
        Intent.CATEGORY_LEANBACK_LAUNCHER,
        Intent.CATEGORY_LAUNCHER,
    )

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
    }.filter { it.packageName != "dev.amber.tv" }
        .distinctBy { it.packageName }

    // Keep the most-used streaming apps near the front when they are installed.
    val preferredOrder = listOf(
        "com.apple.atve.androidtv.appletv",
        "lt.lrt.epika",
        "com.wbd.stream",
        "lt.go3.android.tv",
        "com.stremio.one",
        "com.disney.disneyplus",
        "com.plexapp.android",
        "lt.telia.tv",
        "com.spotify.tv.android",
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
    val apps = remember(context) {
        loadInstalledTvApps(context.packageManager)
    }

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = LocalDateTime.now()
        }
    }

    val time = now.format(DateTimeFormatter.ofPattern("HH:mm"))
    val date = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
        .replaceFirstChar { it.uppercase() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF17212D),
                        Color(0xFF101820),
                        Color(0xFF090D13),
                    )
                )
            )
    ) {
        // Atmospheric glow, kept subtle so foreground content remains readable.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 54.dp, end = 90.dp)
                .size(420.dp)
                .clip(CircleShape)
                .background(Color(0x203B7B8F))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 76.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7ED6C2))
                        )
                        Text(
                            text = "  AMBER",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Your screen. Your choice.",
                        color = Color(0xFF9AAAB7),
                        fontSize = 14.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = time,
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Light
                    )
                    Text(
                        text = date,
                        color = Color(0xFF9AAAB7),
                        fontSize = 13.sp
                    )
                }
            }

            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(
                    text = "Good evening.",
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Everything you love, in one place.",
                    color = Color(0xFFB2C0CA),
                    fontSize = 19.sp
                )
            }

            Column {
                Text(
                    text = "YOUR APPS",
                    color = Color(0xFF9AAAB7),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(18.dp))

                if (apps.isEmpty()) {
                    Text(
                        text = "No TV apps found yet.",
                        color = Color(0xFFB2C0CA),
                        fontSize = 16.sp
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(apps, key = { it.packageName }) { app ->
                            AppTile(
                                app = app,
                                onClick = {
                                    runCatching {
                                        val launchIntent = Intent(Intent.ACTION_MAIN)
                                            .setClassName(app.packageName, app.activityName)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(launchIntent)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AMBER TV  ·  PREVIEW",
                    color = Color(0xFF687A88),
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = Color(0xFF9AAAB7),
                        modifier = Modifier.size(17.dp)
                    )
                    Text("  Apps", color = Color(0xFF9AAAB7), fontSize = 13.sp)
                    Spacer(Modifier.size(28.dp))
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color(0xFF9AAAB7),
                        modifier = Modifier.size(17.dp)
                    )
                    Text("  Settings", color = Color(0xFF9AAAB7), fontSize = 13.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppTile(
    app: InstalledTvApp,
    onClick: () -> Unit,
) {
    val iconBitmap = remember(app.packageName) {
        app.icon.toBitmap(width = 144, height = 144).asImageBitmap()
    }

    TvSurface(
        onClick = onClick,
        modifier = Modifier.size(width = 168.dp, height = 122.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(18.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xE6293542),
            focusedContainerColor = Color(0xFF344D5D),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start,
        ) {
            Image(
                bitmap = iconBitmap,
                contentDescription = app.label,
                modifier = Modifier.size(48.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = app.label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}
