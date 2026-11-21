package dev.amber.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface as TvSurface

@Composable
internal fun LauncherSwitcherScreen(onBack: () -> Unit, onAmberTv: () -> Unit, onGoogleTv: () -> Unit, onHomeButtonHelper: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFF101820)).padding(start = 76.dp, end = 76.dp, top = 30.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        TvSurface(onClick = onBack, modifier = Modifier.size(width = 110.dp, height = 48.dp),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
            colors = ClickableSurfaceDefaults.colors(containerColor = Color(0x33FFFFFF), focusedContainerColor = Color(0x664A86A8))) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("‹  Back", color = Color.White, fontSize = 16.sp) }
        }
        Text("Default launcher", color = Color.White, fontSize = 30.sp)
        Text("Choose which home screen opens when you press the Home button.", color = Color.White.copy(alpha = .65f), fontSize = 15.sp)
        Spacer(Modifier.height(6.dp))
        LauncherOption("Amber TV", "Make Amber TV your default Home app", "SET AMBER TV  ›", onAmberTv)
        LauncherOption("Google TV", "Switch back to the original Google TV launcher", "SET GOOGLE TV  ›", onGoogleTv)
        LauncherOption("Home button helper", "Enable Amber TV to handle the remote Home key", "OPEN ACCESSIBILITY  ›", onHomeButtonHelper)
        Text(
            "After choosing Amber TV, enable “Amber TV Home button” in Accessibility. On some Philips firmware the Home key is routed directly to Google TV, so the helper catches it and opens Amber TV. You can turn it off here in Accessibility any time.",
            color = Color.White.copy(alpha = .52f), fontSize = 12.sp, lineHeight = 18.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun LauncherOption(title: String, description: String, action: String, onClick: () -> Unit) {
    TvSurface(onClick = onClick, modifier = Modifier.fillMaxWidth().height(112.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0x33202B35), focusedContainerColor = Color(0x66517FA0))) {
        Row(Modifier.fillMaxSize().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, color = Color.White, fontSize = 19.sp)
                Text(description, color = Color.White.copy(alpha = .58f), fontSize = 13.sp)
            }
            Text(action, color = Color(0xFF9FD5FF), fontSize = 12.sp)
        }
    }
}
