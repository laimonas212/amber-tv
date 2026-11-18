package dev.amber.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface as TvSurface
import androidx.tv.material3.ClickableSurfaceDefaults
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AmberHome()
            }
        }
    }
}

private data class FeaturedApp(
    val name: String,
    val monogram: String,
    val tint: Color,
)

private val featuredApps = listOf(
    FeaturedApp("Apple TV", "tv+", Color(0xFF292B38)),
    FeaturedApp("YouTube", "▶", Color(0xFFB3262D)),
    FeaturedApp("Netflix", "N", Color(0xFF8D1720)),
    FeaturedApp("Disney+", "D+", Color(0xFF172C68)),
    FeaturedApp("Plex", "plex", Color(0xFFE18A25)),
    FeaturedApp("Spotify", "≋", Color(0xFF176B4A)),
)

@Composable
private fun AmberHome() {
    var selected by remember { mutableIntStateOf(0) }
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
    val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

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
                        text = date.replaceFirstChar { it.uppercase() },
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    featuredApps.forEachIndexed { index, app ->
                        AppTile(
                            app = app,
                            isSelected = selected == index,
                            onSelected = { selected = index },
                            modifier = Modifier.weight(1f)
                        )
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
    app: FeaturedApp,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvSurface(
        onClick = onSelected,
        modifier = modifier
            .height(116.dp)
            .onFocusChanged { if (it.isFocused) onSelected() },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(18.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = app.tint.copy(alpha = 0.92f),
            focusedContainerColor = Color(0xFF344D5D),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.monogram,
                color = Color.White,
                fontSize = if (app.monogram.length > 2) 25.sp else 34.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = app.name,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 4.dp, bottom = 2.dp),
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
