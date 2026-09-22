package it.trotta.ticketonbus.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed interface UiMessage {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiMessage
    data class Raw(val text: String) : UiMessage
}

@Composable
fun UiMessage.resolve(): String = when (this) {
    is UiMessage.Res -> stringResource(id, *args.toTypedArray())
    is UiMessage.Raw -> text
}

fun String?.asRawMessage(): UiMessage? = this?.takeIf { it.isNotBlank() }?.let(UiMessage::Raw)
