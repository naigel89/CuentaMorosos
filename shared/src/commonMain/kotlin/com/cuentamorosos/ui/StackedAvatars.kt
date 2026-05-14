package com.cuentamorosos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuentamorosos.model.ProfileItem
import kotlin.math.absoluteValue

@Composable
fun StackedAvatars(
    profiles: List<ProfileItem>,
    maxVisible: Int = 3,
    size: Dp = 28.dp,
    overlap: Dp = 8.dp,
) {
    val visible = profiles.take(maxVisible)
    val overflow = profiles.size - maxVisible

    Row(horizontalArrangement = Arrangement.spacedBy(-overlap)) {
        visible.forEach { profile ->
            AvatarCircle(
                text = profile.name.take(1).uppercase(),
                color = avatarColorForProfile(profile),
                size = size,
            )
        }
        if (overflow > 0) {
            AvatarCircle(
                text = "+$overflow",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                size = size,
                textColor = MaterialTheme.colorScheme.surface,
            )
        }
    }
}

@Composable
private fun AvatarCircle(
    text: String,
    color: Color,
    size: Dp,
    textColor: Color = Color.White,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = if (text.startsWith("+")) 10.sp else 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private val avatarColors = listOf(
    Color(0xFF39FF14), // neon green
    Color(0xFF4F83FF), // blue
    Color(0xFFD16BA5), // rose
    Color(0xFFE0A106), // amber
    Color(0xFF2EAF7D), // green
    Color(0xFFFF6B6B), // red
    Color(0xFFA78BFA), // purple
    Color(0xFF60A5FA), // light blue
)

@Composable
private fun avatarColorForProfile(profile: ProfileItem): Color {
    val index = profile.id.hashCode().absoluteValue % avatarColors.size
    return avatarColors[index]
}
