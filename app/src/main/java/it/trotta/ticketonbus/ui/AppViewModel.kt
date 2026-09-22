package it.trotta.ticketonbus.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trotta.ticketonbus.R
import it.trotta.ticketonbus.data.Account
import it.trotta.ticketonbus.data.CartItem
import it.trotta.ticketonbus.data.GoogleAccount
import it.trotta.ticketonbus.data.PaymentHandoff
import it.trotta.ticketonbus.data.SessionStore
import it.trotta.ticketonbus.data.Tenant
import it.trotta.ticketonbus.data.Ticket
import it.trotta.ticketonbus.data.TicketStatus
import it.trotta.ticketonbus.data.TrottaApi
import it.trotta.ticketonbus.data.TrottaException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Screen { LOGIN, HOME, BUY, WALLET, HISTORY, CART, TRANSIT, INFO }

data class UiState(
    val tenant: Tenant = Tenant.FIUMICINO,
    val screen: Screen = Screen.LOGIN,
    val account: Account? = null,
    val tickets: List<Ticket> = emptyList(),
    val history: List<Ticket> = emptyList(),
    val cart: List<CartItem> = emptyList(),
    val busy: Boolean = false,
    val refreshing: Boolean = false,
    val message: UiMessage? = null,
    val error: UiMessage? = null,
    val lastEmail: String? = null,
    val loaded: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,

    val googleAccount: GoogleAccount? = null,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionStore(app)
    private val prefs = it.trotta.ticketonbus.data.AppPreferences(app)
    private val api = TrottaApi(session)

    private val _state = MutableStateFlow(
        UiState(
            tenant = session.tenant,
            lastEmail = session.lastEmail,
            language = AppLanguage.fromTag(prefs.language),
            googleAccount = session.googleAccount,
        ),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        restoreSession()
    }

    private fun restoreSession() = viewModelScope.launch {
        val tenant = session.tenant
        _state.update { it.copy(busy = true) }
        val account = runCatching { api.account(tenant) }.getOrNull()
        if (account == null) {
            _state.update { it.copy(account = null, screen = Screen.LOGIN, busy = false, loaded = true) }
        } else {
            _state.update { it.copy(account = account, screen = Screen.HOME, busy = false, loaded = true) }
            refresh(showSpinner = false)
        }
    }

    fun login(email: String, password: String) = viewModelScope.launch {
        if (email.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = UiMessage.Res(R.string.msg_login_missing)) }
            return@launch
        }
        val tenant = _state.value.tenant
        _state.update { it.copy(busy = true, error = null) }
        runCatching { api.login(tenant, email.trim(), password) }
            .onSuccess { account ->
                _state.update {
                    it.copy(account = account, screen = Screen.HOME, busy = false, lastEmail = email.trim())
                }
                refresh(showSpinner = false)
            }
            .onFailure { e -> _state.update { it.copy(busy = false, error = describe(e)) } }
    }

    fun logout() = viewModelScope.launch {
        val tenant = _state.value.tenant
        _state.update { it.copy(busy = true) }
        runCatching { api.logout(tenant) }
        _state.update {
            it.copy(
                account = null, screen = Screen.LOGIN, busy = false,
                tickets = emptyList(), history = emptyList(), cart = emptyList(),
                message = UiMessage.Res(R.string.msg_logged_out),
            )
        }
    }

    fun switchTenant(tenant: Tenant) {
        if (tenant == _state.value.tenant) return
        session.tenant = tenant
        _state.update {
            it.copy(
                tenant = tenant, account = null, screen = Screen.LOGIN,
                tickets = emptyList(), history = emptyList(), cart = emptyList(), error = null,
            )
        }
    }

    fun refresh(showSpinner: Boolean = true) = viewModelScope.launch {
        val tenant = _state.value.tenant
        if (_state.value.account == null) return@launch
        if (showSpinner) _state.update { it.copy(refreshing = true) }
        runCatching {
            coroutineScope {
                val tickets = async { api.activeTickets(tenant) }
                val history = async { api.history(tenant) }
                val cart = async { api.cart(tenant) }
                Triple(tickets.await(), history.await(), cart.await())
            }
        }
            .onSuccess { (tickets, history, cart) ->
                _state.update {
                    it.copy(tickets = tickets, history = history, cart = cart, refreshing = false)
                }
            }
            .onFailure { e -> _state.update { it.copy(refreshing = false, error = describe(e)) } }
    }

    fun reserve(count: Int, onReady: (PaymentHandoff) -> Unit) = viewModelScope.launch {
        val tenant = _state.value.tenant
        _state.update { it.copy(busy = true, error = null) }
        runCatching { api.reserve(tenant, count) }
            .onSuccess { handoff ->
                _state.update {
                    it.copy(
                        busy = false,
                        screen = Screen.CART,
                        message = UiMessage.Res(R.string.msg_reservation_created),
                    )
                }
                refresh(showSpinner = false)
                onReady(handoff)
            }
            .onFailure { e -> _state.update { it.copy(busy = false, error = describe(e)) } }
    }

    fun activate(ticket: Ticket, busNumber: String) = viewModelScope.launch {
        val tenant = _state.value.tenant
        _state.update { it.copy(busy = true, error = null) }
        runCatching { api.activate(tenant, ticket, busNumber.trim()) }
            .onSuccess { updated ->
                val message = if (updated == null) {

                    UiMessage.Res(R.string.msg_ticket_validated)
                } else if (updated.status == TicketStatus.ACTIVE) {
                    UiMessage.Res(R.string.msg_ticket_validated)
                } else {
                    UiMessage.Res(updated.status.labelRes)
                }
                _state.update { current ->
                    current.copy(
                        busy = false,
                        tickets = if (updated == null) {
                            current.tickets
                        } else {
                            current.tickets.map { if (it.guid == updated.guid) updated else it }
                        },
                        message = message,
                    )
                }
                refresh(showSpinner = false)
            }
            .onFailure { e -> _state.update { it.copy(busy = false, error = describe(e)) } }
    }

    fun googleSignIn(context: android.content.Context) = viewModelScope.launch {
        _state.update { it.copy(busy = true, error = null) }
        runCatching { GoogleSignIn.signIn(context) }
            .onSuccess { account ->
                session.googleAccount = account
                _state.update {
                    it.copy(
                        busy = false,
                        googleAccount = account,
                        message = UiMessage.Res(R.string.msg_google_signed_in, listOf(account.label)),
                    )
                }
            }
            .onFailure { e -> _state.update { it.copy(busy = false, error = googleFailure(e)) } }
    }

    fun googleSignOut() {
        session.googleAccount = null
        _state.update {
            it.copy(googleAccount = null, message = UiMessage.Res(R.string.msg_google_signed_out))
        }
    }

    private fun googleFailure(e: Throwable): UiMessage? = when ((e as? GoogleSignInException)?.kind) {
        GoogleSignInException.Kind.CANCELLED -> null
        GoogleSignInException.Kind.NOT_CONFIGURED -> UiMessage.Res(R.string.google_not_configured)
        GoogleSignInException.Kind.NO_ACCOUNT -> UiMessage.Res(R.string.google_no_account)
        else -> UiMessage.Res(R.string.google_sign_in_failed)
    }

    fun setLanguage(language: AppLanguage) {
        prefs.language = language.tag
        _state.update { it.copy(language = language) }
    }

    fun go(screen: Screen) {
        _state.update { it.copy(screen = screen) }
        if (screen == Screen.WALLET || screen == Screen.HISTORY || screen == Screen.CART) {
            refresh(showSpinner = false)
        }
    }

    fun printUrl(guid: String): String = api.printUrl(_state.value.tenant, guid)

    fun consumeAlert() = _state.update { it.copy(message = null, error = null) }

    private fun describe(e: Throwable): UiMessage = when (e) {
        is TrottaException, is IllegalArgumentException ->
            e.message?.takeIf { it.isNotBlank() }?.let(UiMessage::Raw) ?: networkFailure()
        else -> networkFailure()
    }

    private fun networkFailure() = UiMessage.Res(R.string.msg_network_error)
}
