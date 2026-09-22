package it.trotta.ticketonbus.data

import androidx.annotation.StringRes
import it.trotta.ticketonbus.R

enum class Tenant(val basePath: String) {
    FIUMICINO("active/"),
    CAMPOBASSO("campobasso/active/");

    @get:StringRes
    val labelRes: Int
        get() = when (this) {
            FIUMICINO -> R.string.tenant_fiumicino
            CAMPOBASSO -> R.string.tenant_campobasso
        }

    companion object {
        fun fromName(name: String?): Tenant =
            entries.firstOrNull { it.name == name } ?: FIUMICINO
    }
}

data class Account(
    val firstName: String,
    val fullName: String,
)

enum class TicketStatus {
    NOT_ACTIVE,
    ACTIVE,
    EXPIRED,
    PENDING,
    UNKNOWN;

    @get:StringRes
    val labelRes: Int
        get() = when (this) {
            NOT_ACTIVE -> R.string.status_not_active
            ACTIVE -> R.string.status_active
            EXPIRED -> R.string.status_expired
            PENDING -> R.string.status_pending
            UNKNOWN -> R.string.status_unknown
        }

    companion object {
        fun from(raw: String?): TicketStatus {
            val s = raw.orEmpty().trim().uppercase()

            return when {
                s.contains("NON ATTIVO") -> NOT_ACTIVE
                s.contains("ATTESA") -> PENDING
                s.contains("SCADUTO") -> EXPIRED
                s.contains("ATTIVO") -> ACTIVE
                else -> UNKNOWN
            }
        }
    }
}

data class Ticket(
    val rowId: Long?,
    val guid: String?,
    val number: String,
    val issuedOn: String?,
    val bookedAt: String?,
    val purchasedAt: String?,
    val status: TicketStatus,
    val validFrom: String?,
    val validTo: String?,
    val validityMinutes: Int?,
    val price: String?,
    val transaction: String?,
    val paymentId: String?,
) {
    val canActivate: Boolean
        get() = rowId != null && guid != null && status == TicketStatus.NOT_ACTIVE
}

data class PaymentHandoff(
    val url: String,
    val bookingGuid: String?,
    val transactionNumber: String?,
    val amount: String?,
    val zone: String?,
    val description: String?,
)

data class CartItem(
    val bookingId: String?,
    val bookedAt: String?,
    val ticketCount: Int?,
    val totalPrice: String?,
    val payment: PaymentHandoff?,
)

class TrottaException(message: String) : Exception(message)
