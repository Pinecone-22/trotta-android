package it.trotta.ticketonbus

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.trotta.ticketonbus.data.AppPreferences
import it.trotta.ticketonbus.ui.AppViewModel
import it.trotta.ticketonbus.ui.Locales
import it.trotta.ticketonbus.ui.TicketOnBusApp
import it.trotta.ticketonbus.ui.TransitViewModel
import it.trotta.ticketonbus.ui.theme.TicketOnBusTheme

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()
    private val transitVm: TransitViewModel by viewModels()

    private var awaitingPayment = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Locales.wrap(newBase, AppPreferences.languageOf(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TicketOnBusTheme {
                val state by vm.state.collectAsStateWithLifecycle()
                val transitState by transitVm.state.collectAsStateWithLifecycle()
                TicketOnBusApp(
                    state = state,
                    vm = vm,
                    transitState = transitState,
                    transitVm = transitVm,
                    onOpenUrl = { url ->
                        awaitingPayment = true
                        openExternally(url)
                    },
                    onChangeLanguage = { language ->
                        vm.setLanguage(language)

                        recreate()
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (awaitingPayment) {
            awaitingPayment = false
            vm.refresh(showSpinner = false)
        }
    }

    private fun openExternally(url: String) {
        val uri = Uri.parse(url)
        val opened = runCatching {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(true)
                .build()
                .launchUrl(this, uri)
        }
        if (opened.isFailure) {
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        }
    }
}
