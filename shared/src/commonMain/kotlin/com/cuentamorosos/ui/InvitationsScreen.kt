package com.cuentamorosos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentamorosos.model.EventInvitation

@Composable
fun InvitationsScreen(
    modifier: Modifier = Modifier,
    invitations: List<EventInvitation>,
    onAccept: (EventInvitation) -> Unit,
    onReject: (EventInvitation) -> Unit,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Invitaciones",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Invitaciones pendientes para unirte a eventos de otros usuarios.",
            style = MaterialTheme.typography.bodyMedium
        )

        if (invitations.isEmpty()) {
            EmptyState(
                modifier = Modifier.weight(1f),
                title = "Sin invitaciones pendientes",
                message = "Cuando alguien te invite a un evento aparecerá aquí para que puedas aceptarla o rechazarla."
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(invitations, key = { it.id }) { invitation ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProfileAvatar(
                                    name = invitation.invitedByName,
                                    photoUrl = invitation.invitedByPhotoUrl,
                                    size = 40.dp,
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = invitation.eventName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Invitado por: ${invitation.invitedByName}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onAccept(invitation) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Aceptar")
                                }
                                OutlinedButton(
                                    onClick = { onReject(invitation) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Rechazar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
