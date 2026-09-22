package it.trotta.ticketonbus.data

import android.net.Uri

data class GoogleAccount(
    val id: String,
    val email: String?,
    val displayName: String?,
    val pictureUrl: String?,
) {

    val label: String
        get() = displayName?.takeIf { it.isNotBlank() }
            ?: email?.takeIf { it.isNotBlank() }
            ?: id
}

fun googleAccountOf(
    id: String?,
    email: String?,
    displayName: String?,
    givenName: String?,
    picture: Uri? = null,
): GoogleAccount? {
    val stableId = id?.takeIf { it.isNotBlank() } ?: email?.takeIf { it.isNotBlank() } ?: return null
    val name = displayName?.takeIf { it.isNotBlank() } ?: givenName?.takeIf { it.isNotBlank() }
    return GoogleAccount(
        id = stableId,
        email = email?.takeIf { it.isNotBlank() },
        displayName = name,
        pictureUrl = picture?.toString()?.takeIf { it.isNotBlank() },
    )
}
