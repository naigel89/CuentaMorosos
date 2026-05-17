package com.cuentamorosos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.ExpenseCategory
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.formatEuros
import com.cuentamorosos.model.formattedDate

@Composable
fun EventCard(
    event: EventItem,
    participantCount: Int,
    pendingTotal: Double,
    totalExpense: Double = 0.0,
    yourShare: Double = 0.0,
    youAreOwed: Double = 0.0,
    profiles: List<ProfileItem>,
    category: ExpenseCategory = ExpenseCategory.SHARED,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    canEdit: Boolean = true,
    canDelete: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val neoColors = LocalNeoFintechColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val stateBadgeColor = event.state.stateBadgeColor(neoColors)
    val stateLabelText = event.state.stateBadgeLabel()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isHovered == true) NeoFintechElevation.cardShadowHoverElevation
                else NeoFintechElevation.cardShadowElevation,
                shape = NeoFintechElevation.cardShadowShape,
                clip = false,
            )
            .border(1.dp, colors.outlineVariant, NeoFintechShapes.lg)
            .clip(NeoFintechShapes.lg)
            .hoverable(interactionSource)
            .clickable(onClick = onTap),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = NeoFintechShapes.lg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NeoFintechSpacing.md),
            verticalArrangement = Arrangement.spacedBy(NeoFintechSpacing.sm),
        ) {
            // Header: icon + status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                // Icon square with category background
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(NeoFintechShapes.lg)
                        .background(category.iconBgColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = category.iconEmoji,
                        fontSize = 24.sp,
                    )
                }
                // State badge (pill shape)
                Surface(
                    color = stateBadgeColor,
                    shape = NeoFintechShapes.full,
                ) {
                    Text(
                        text = stateLabelText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = colors.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Event name + date
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(NeoFintechSpacing.xs),
            ) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 20.sp,
                        fontWeight = if (isHovered == true) FontWeight.Bold else FontWeight.SemiBold,
                    ),
                    color = if (isHovered == true) neoColors.primaryContainer else colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = colors.onSurfaceVariant,
                    )
                    Text(
                        text = event.formattedDate(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            // Divider + Your Share row
            Divider(color = colors.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Total gastado",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        text = formatEuros(totalExpense),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 18.sp),
                        color = colors.onSurface,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Tu parte",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        text = formatEuros(yourShare),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 18.sp),
                        color = if (yourShare > 0.0) neoColors.primaryContainer else neoColors.error,
                    )
                }
            }

            // Stacked avatars + "You are owed" text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StackedAvatars(profiles = profiles, maxVisible = 3)
                Text(
                    text = "Te deben ${formatEuros(youAreOwed)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                    color = neoColors.primaryContainer,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Action buttons (OutlinedButton)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NeoFintechSpacing.sm),
            ) {
                if (canEdit) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        shape = NeoFintechShapes.md,
                    ) {
                        Text("Editar", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (canDelete) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        shape = NeoFintechShapes.md,
                    ) {
                        Text("Eliminar", style = MaterialTheme.typography.labelSmall, color = colors.error)
                    }
                }
            }
        }
    }
}
