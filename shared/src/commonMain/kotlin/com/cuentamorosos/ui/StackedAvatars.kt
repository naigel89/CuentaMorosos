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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuentamorosos.model.ProfileItem

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
            ProfileAvatar(
                name = profile.name,
                emoji = "",
                photoUrl = profile.photoUrl,
                size = size,
            )
        }
        if (overflow > 0) {
            OverflowCircle(
                text = "+$overflow",
                size = size,
            )
        }
    }
}

@Composable
private fun OverflowCircle(
    text: String,
    size: Dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.surface,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
