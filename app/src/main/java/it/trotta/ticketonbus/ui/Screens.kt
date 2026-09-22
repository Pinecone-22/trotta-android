package it.trotta.ticketonbus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import it.trotta.ticketonbus.BuildConfig
import it.trotta.ticketonbus.R
import it.trotta.ticketonbus.data.CartItem
import it.trotta.ticketonbus.data.Tenant
import it.trotta.ticketonbus.data.Ticket
import it.trotta.ticketonbus.data.TicketStatus

@Composable
fun TicketOnBusApp(
    state: UiState,
    vm: AppViewModel,
    transitState: TransitUiState,
    transitVm: TransitViewModel,
    onOpenUrl: (String) -> Unit,
    onChangeLanguage: (AppLanguage) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val alert = state.error ?: state.message
    val alertText = alert?.resolve()
    LaunchedEffect(alertText) {
        if (alertText != null) {
            snackbarHostState.showSnackbar(alertText)
            vm.consumeAlert()
        }
    }

    val showNav = state.screen in setOf(Screen.HOME, Screen.WALLET, Screen.TRANSIT, Screen.INFO)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showNav) {
                BottomNav(
                    current = state.screen,
                    loggedIn = state.account != null,
                    onSelect = { target ->

                        val anonymous = state.account == null
                        vm.go(
                            if (anonymous && (target == Screen.WALLET || target == Screen.HOME)) {
                                Screen.LOGIN
                            } else {
                                target
                            },
                        )
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (state.screen) {
                Screen.LOGIN -> LoginScreen(state, vm, onOpenUrl)
                Screen.HOME -> HomeScreen(state, vm, onOpenUrl)
                Screen.BUY -> BuyScreen(state, vm, onOpenUrl)
                Screen.WALLET -> WalletScreen(state, vm, onOpenUrl)
                Screen.HISTORY -> HistoryScreen(state, vm, onOpenUrl)
                Screen.CART -> CartScreen(state, vm, onOpenUrl)
                Screen.INFO -> InfoScreen(state, vm, onChangeLanguage)
                Screen.TRANSIT -> TransitApp(
                    state = transitState,
                    vm = transitVm,
                    onOpenUrl = onOpenUrl,
                    onExit = { vm.go(if (state.account != null) Screen.HOME else Screen.LOGIN) },
                )
            }
            if (state.busy) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun BottomNav(current: Screen, loggedIn: Boolean, onSelect: (Screen) -> Unit) {
    data class Item(val screen: Screen, val icon: ImageVector, val label: String)

    val items = buildList {
        add(Item(Screen.HOME, Icons.Filled.Home, stringResource(R.string.nav_home)))
        if (loggedIn) add(Item(Screen.WALLET, Icons.Filled.ShoppingCart, stringResource(R.string.nav_tickets)))
        add(Item(Screen.TRANSIT, Icons.Filled.Place, stringResource(R.string.nav_transit)))
        add(Item(Screen.INFO, Icons.Filled.Info, stringResource(R.string.nav_info)))
    }
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = current == item.screen,
                onClick = { onSelect(item.screen) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, maxLines = 1) },
            )
        }
    }
}

@Composable
private fun LoginScreen(state: UiState, vm: AppViewModel, onOpenUrl: (String) -> Unit) {
    var email by rememberSaveable { mutableStateOf(state.lastEmail.orEmpty()) }
    var password by rememberSaveable { mutableStateOf("") }
    var revealPassword by rememberSaveable { mutableStateOf(false) }
    var tenant by rememberSaveable { mutableStateOf(state.tenant.name) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        ScreenHeader(title = stringResource(R.string.app_name), subtitle = stringResource(R.string.app_operator))

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            WarningBanner(stringResource(R.string.disclaimer_short))

            Text(
                text = stringResource(R.string.login_intro),
                style = MaterialTheme.typography.bodyMedium,
            )

            GoogleBlock(state, vm)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.login_service), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tenant.entries.forEach { candidate ->
                        SelectableChip(
                            label = stringResource(candidate.labelRes),
                            selected = tenant == candidate.name,
                            onClick = {
                                tenant = candidate.name
                                vm.switchTenant(candidate)
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.field_email)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.field_password)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                visualTransformation = if (revealPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    TextButton(onClick = { revealPassword = !revealPassword }) {
                        Text(
                            stringResource(
                                if (revealPassword) R.string.action_hide else R.string.action_show,
                            ),
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { vm.login(email, password) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_sign_in))
            }

            WarningBanner(stringResource(R.string.login_privacy))

            OutlinedButton(onClick = { vm.go(Screen.TRANSIT) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.transit_guest))
            }
            TextButton(onClick = { vm.go(Screen.INFO) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.about_title))
            }
        }
    }
}

@Composable
private fun HomeScreen(state: UiState, vm: AppViewModel, onOpenUrl: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = state.account?.fullName ?: stringResource(R.string.home_title),
            subtitle = stringResource(state.tenant.labelRes),
            actions = {
                IconButton(onClick = { vm.refresh() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh))
                }
                IconButton(onClick = { vm.logout() }) {
                    Icon(Icons.Filled.ExitToApp, contentDescription = stringResource(R.string.action_sign_out))
                }
            },
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_greeting, state.account?.firstName.orEmpty()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        StatBlock(stringResource(R.string.stat_active), state.tickets.count { it.status == TicketStatus.ACTIVE }.toString())
                        StatBlock(stringResource(R.string.stat_to_validate), state.tickets.count { it.status == TicketStatus.NOT_ACTIVE }.toString())
                        StatBlock(stringResource(R.string.stat_to_pay), state.cart.size.toString())
                    }
                }
            }

            GoogleBlock(state, vm, compact = true)

            ActionTile(
                title = stringResource(R.string.tile_buy),
                subtitle = stringResource(R.string.tile_buy_sub),
                onClick = { vm.go(Screen.BUY) },
            )
            ActionTile(
                title = stringResource(R.string.tile_wallet),
                subtitle = pluralStringResource(R.plurals.n_tickets, state.tickets.size, state.tickets.size),
                onClick = { vm.go(Screen.WALLET) },
            )
            ActionTile(
                title = stringResource(R.string.tile_cart),
                subtitle = if (state.cart.isEmpty()) {
                    stringResource(R.string.tile_cart_sub_empty)
                } else {
                    pluralStringResource(R.plurals.n_tickets, state.cart.size, state.cart.size)
                },
                onClick = { vm.go(Screen.CART) },
            )
            ActionTile(
                title = stringResource(R.string.tile_history),
                subtitle = pluralStringResource(R.plurals.n_tickets, state.history.size, state.history.size),
                onClick = { vm.go(Screen.HISTORY) },
            )
            ActionTile(
                title = stringResource(R.string.tile_transit),
                subtitle = stringResource(R.string.tile_transit_sub),
                onClick = { vm.go(Screen.TRANSIT) },
            )

            val active = state.tickets.firstOrNull { it.status == TicketStatus.ACTIVE }
            if (active != null) {
                Text(
                    stringResource(R.string.section_running_ticket),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                TicketCard(
                    ticket = active,
                    printUrl = active.guid?.let { vm.printUrl(it) },
                    onActivate = null,
                    onOpenUrl = onOpenUrl,
                )
            }

            Text(
                stringResource(R.string.disclaimer_short),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActionTile(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("›", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun BuyScreen(state: UiState, vm: AppViewModel, onOpenUrl: (String) -> Unit) {
    var count by rememberSaveable { mutableStateOf(1) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(R.string.buy_title),
            subtitle = stringResource(state.tenant.labelRes),
            onBack = { vm.go(Screen.HOME) },
        )

        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.buy_intro),
                style = MaterialTheme.typography.bodyMedium,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(
                        onClick = { if (count > 1) count-- },
                        enabled = count > 1,
                    ) {
                        Text(
                            text = "\u2212",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = pluralStringResource(R.plurals.n_tickets, count, count),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = { if (count < 100) count++ },
                        enabled = count < 100,
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Button(
                onClick = { vm.reserve(count) { handoff -> onOpenUrl(handoff.url) } },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_book_pay))
            }

            WarningBanner(stringResource(R.string.buy_note))
        }
    }
}

@Composable
private fun WalletScreen(state: UiState, vm: AppViewModel, onOpenUrl: (String) -> Unit) {
    var pendingActivation by remember { mutableStateOf<Ticket?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(R.string.wallet_title),
            subtitle = pluralStringResource(R.plurals.n_tickets, state.tickets.size, state.tickets.size),
            onBack = { vm.go(Screen.HOME) },
            actions = {
                IconButton(onClick = { vm.refresh() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh))
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.tickets.isEmpty()) {
                EmptyState(stringResource(R.string.wallet_empty))
            }
            state.tickets.forEach { ticket ->
                TicketCard(
                    ticket = ticket,
                    printUrl = ticket.guid?.let { vm.printUrl(it) },
                    onActivate = if (ticket.canActivate) {
                        { pendingActivation = ticket }
                    } else {
                        null
                    },
                    onOpenUrl = onOpenUrl,
                )
            }
        }
    }

    pendingActivation?.let { ticket ->
        VehicleCodeDialog(
            ticket = ticket,
            onDismiss = { pendingActivation = null },
            onConfirm = { bus ->
                vm.activate(ticket, bus)
                pendingActivation = null
            },
        )
    }
}

@Composable
private fun VehicleCodeDialog(
    ticket: Ticket,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var bus by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_validate_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.dialog_validate_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    ticket.number,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = bus,
                    onValueChange = { input -> if (input.length <= 4 && input.all(Char::isDigit)) bus = input },
                    label = { Text(stringResource(R.string.field_vehicle_code)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.dialog_validate_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(bus) }, enabled = bus.isNotBlank()) {
                Text(stringResource(R.string.action_validate))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun HistoryScreen(state: UiState, vm: AppViewModel, onOpenUrl: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(R.string.history_title),
            subtitle = pluralStringResource(R.plurals.n_tickets, state.history.size, state.history.size),
            onBack = { vm.go(Screen.HOME) },
            actions = {
                IconButton(onClick = { vm.refresh() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh))
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.history.isEmpty()) {
                EmptyState(stringResource(R.string.history_empty))
            }
            state.history.forEach { ticket ->
                TicketCard(
                    ticket = ticket,
                    printUrl = ticket.guid?.let { vm.printUrl(it) },
                    onActivate = null,
                    onOpenUrl = onOpenUrl,
                )
            }
        }
    }
}

@Composable
private fun CartScreen(state: UiState, vm: AppViewModel, onOpenUrl: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(R.string.cart_title),
            subtitle = if (state.cart.isEmpty()) {
                stringResource(R.string.tile_cart_sub_empty)
            } else {
                pluralStringResource(R.plurals.n_tickets, state.cart.size, state.cart.size)
            },
            onBack = { vm.go(Screen.HOME) },
            actions = {
                IconButton(onClick = { vm.refresh() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh))
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.cart.isEmpty()) {
                EmptyState(stringResource(R.string.cart_empty))
            }
            state.cart.forEach { item -> CartCard(item, onOpenUrl) }
            if (state.cart.isNotEmpty()) {
                WarningBanner(stringResource(R.string.cart_note))
            }
        }
    }
}

@Composable
private fun CartCard(item: CartItem, onOpenUrl: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = item.ticketCount
                    ?.let { pluralStringResource(R.plurals.n_tickets, it, it) }
                    ?: stringResource(R.string.cart_reservation),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            item.bookedAt?.let {
                Text(
                    stringResource(R.string.cart_reserved_on, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item.totalPrice?.let {
                Text(
                    stringResource(R.string.cart_total, it),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (item.payment != null) {
                Button(onClick = { onOpenUrl(item.payment.url) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_pay_nexi))
                }
            } else {
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cart_pay_unavailable))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoScreen(state: UiState, vm: AppViewModel, onChangeLanguage: (AppLanguage) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(R.string.about_title),
            subtitle = stringResource(R.string.app_name),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(stringResource(R.string.app_operator), style = MaterialTheme.typography.bodySmall)
                    Text(
                        stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.about_disclaimer_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(stringResource(R.string.disclaimer_short), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.about_trademark), style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.about_google_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(stringResource(R.string.about_google_body), style = MaterialTheme.typography.bodySmall)
                    GoogleBlock(state, vm, compact = true)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.language_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { language ->
                            val label = language.autonym ?: stringResource(R.string.language_system)
                            SelectableChip(
                                label = label,
                                selected = state.language == language,
                                onClick = { onChangeLanguage(language) },
                            )
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.about_sources_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(stringResource(R.string.about_sources_body), style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.about_estimates_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(stringResource(R.string.about_estimates_body), style = MaterialTheme.typography.bodySmall)
                    Text(
                        stringResource(R.string.transit_works_offline),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun GoogleBlock(state: UiState, vm: AppViewModel, compact: Boolean = false) {
    val context = LocalContext.current
    if (!GoogleSignIn.isConfigured) {
        if (!compact) WarningBanner(stringResource(R.string.google_not_configured))
        return
    }
    val account = state.googleAccount
    if (account == null) {
        OutlinedButton(
            onClick = { vm.googleSignIn(context) },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.google_sign_in))
        }
    } else {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.google_signed_in_as, account.label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    account.email?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = { vm.googleSignOut() }) {
                    Text(stringResource(R.string.action_sign_out))
                }
            }
        }
    }
}

@Composable
internal fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
