package com.cuentamorosos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.ExpenseCategory
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.formatEuros

@Composable
fun ExpenseItemCard(
    expense: EventExpenseItem,
    paidByProfile: ProfileItem?,
    isCurrentUser: Boolean,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    enabledEdit: Boolean = true,
    enabledDelete: Boolean = true,
    isReadOnly: Boolean = false,
) {
    val category = ExpenseCategory.fromId(expense.category)
    val colors = LocalNeoFintechColors.current
    val themeColors = MaterialTheme.colorScheme

    val splitBadge = when {
        expense.assignedProfileIds.isEmpty() -> "Sin asignar"
        expense.assignedProfileIds.size == 1 -> "Individual"
        else -> "Repartido (${expense.assignedProfileIds.size})"
    }

    val paidByText = when {
        isCurrentUser -> "Pagado por ti"
        paidByProfile != null -> "Pagado por ${paidByProfile.name}"
        else -> "Sin asignar"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .cardShadow(),
        colors = CardDefaults.cardColors(containerColor = themeColors.surfaceContainerLowest),
        shape = NeoFintechShapes.lg,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Category icon circle (48dp per design)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(NeoFintechShapes.full)
                    .background(category.iconBgColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = category.iconEmoji,
                    fontSize = 22.sp,
                )
            }

            // Expense details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = expense.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = themeColors.onSurface,
                )
                Text(
                    text = paidByText,
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.onSurfaceVariant,
                )
                // Split badge (pill)
                Surface(
                    color = themeColors.surfaceContainer.copy(alpha = 0.5f),
                    shape = NeoFintechShapes.full,
                ) {
                    Text(
                        text = splitBadge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = themeColors.onSurfaceVariant,
                        fontFamily = JetBrainsMonoFontFamily(),
                    )
                }
                // Read-only badge for READER role
                if (isReadOnly) {
                    Surface(
                        color = themeColors.surfaceContainerHigh.copy(alpha = 0.6f),
                        shape = NeoFintechShapes.full,
                    ) {
                        Text(
                            text = "Solo lectura",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = themeColors.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            // Amount + actions
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = formatEuros(expense.amountEuros),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = JetBrainsMonoFontFamily(),
                    color = themeColors.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(onClick = onEdit, enabled = enabledEdit) {
                        Text("Editar", fontSize = 11.sp, color = colors.onSurfaceVariant)
                    }
                    TextButton(onClick = onDelete, enabled = enabledDelete) {
                        Text("Eliminar", fontSize = 11.sp, color = colors.error)
                    }
                }
            }
        }
    }
}
