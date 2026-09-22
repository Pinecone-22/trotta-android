package it.trotta.ticketonbus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.trotta.ticketonbus.R
import it.trotta.ticketonbus.data.Ticket
import it.trotta.ticketonbus.data.TicketStatus
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ITALIAN_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

fun parseItalianDateTime(raw: String?): LocalDateTime? =
    raw?.trim()?.takeIf { it.isNotEmpty() }
        ?.let { runCatching { LocalDateTime.parse(it, ITALIAN_DATE) }.getOrNull() }

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

@Composable
fun StatusChip(status: TicketStatus) {
    val (background, foreground) = when (status) {
        TicketStatus.ACTIVE -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        TicketStatus.NOT_ACTIVE -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        TicketStatus.PENDING -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        TicketStatus.EXPIRED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        TicketStatus.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = background, contentColor = foreground, shape = RoundedCornerShape(999.dp)) {
        Text(
            text = stringResource(status.labelRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun rememberRemaining(validTo: String?): String? {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    val end = remember(validTo) { parseItalianDateTime(validTo) }
    LaunchedEffect(end) {
        if (end == null) return@LaunchedEffect
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    if (end == null) return null
    val remaining = end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - now
    if (remaining <= 0) return stringResource(R.string.expired_label)
    val totalSeconds = remaining / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%dh %02dm".format(hours, minutes)
    } else {
        "%dm %02ds".format(minutes, seconds)
    }
}

@Composable
fun TicketCard(
    ticket: Ticket,
    printUrl: String?,
    onActivate: (() -> Unit)?,
    onOpenUrl: (String) -> Unit,
) {
    val remaining = if (ticket.status == TicketStatus.ACTIVE) rememberRemaining(ticket.validTo) else null

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ticket.number,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (ticket.issuedOn != null) {
                        Text(
                            text = ticket.issuedOn,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                StatusChip(ticket.status)
            }

            HorizontalDivider()

            ticket.bookedAt?.let { InfoRow(stringResource(R.string.label_booking), it) }
            ticket.purchasedAt?.let { InfoRow(stringResource(R.string.label_purchase), it) }
            ticket.validityMinutes?.let {
                InfoRow(stringResource(R.string.label_validity), stringResource(R.string.validity_minutes, it))
            }
            ticket.validFrom?.let { InfoRow(stringResource(R.string.label_valid_from), it) }
            ticket.validTo?.let { InfoRow(stringResource(R.string.label_expires), it) }
            remaining?.let { InfoRow(stringResource(R.string.label_remaining), it) }
            ticket.price?.let { InfoRow(stringResource(R.string.label_price), it) }
            ticket.transaction?.let { InfoRow(stringResource(R.string.label_transaction), it) }
            ticket.paymentId?.let { InfoRow(stringResource(R.string.label_payment_id), it) }

            if (onActivate != null) {
                Spacer(Modifier.height(2.dp))
                Button(onClick = onActivate, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_validate_on_bus))
                }
            }

            if (printUrl != null) {
                TextButton(onClick = { onOpenUrl(printUrl) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_qr))
                }
            }
        }
    }
}

@Composable
fun EmptyState(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun WarningBanner(text: String, tint: Color = MaterialTheme.colorScheme.secondaryContainer) {
    Surface(color = tint, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(12.dp),
        )
    }
}
