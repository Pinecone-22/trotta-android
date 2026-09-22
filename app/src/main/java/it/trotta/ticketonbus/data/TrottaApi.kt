package it.trotta.ticketonbus.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class TrottaApi(private val session: SessionStore) {

    private val client = OkHttpClient.Builder()
        .cookieJar(session)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    fun pageUrl(tenant: Tenant, page: String? = null): String = buildString {
        append(ORIGIN).append(tenant.basePath)
        if (!page.isNullOrEmpty()) append("?page=").append(page)
    }

    fun printUrl(tenant: Tenant, guid: String): String =
        ORIGIN + tenant.basePath + "stampa.aspx?id=" + guid

    fun checkUrl(tenant: Tenant, guid: String): String =
        ORIGIN + tenant.basePath + "check.aspx?id=" + guid

    private fun engineUrl(tenant: Tenant): String = ORIGIN + tenant.basePath + "engine.aspx"

    suspend fun login(tenant: Tenant, email: String, password: String): Account =
        withContext(Dispatchers.IO) {

            get(engineUrl(tenant))

            val fields = formFields("login", menu = "1") +
                listOf("email" to email, "password" to password)
            val html = post(engineUrl(tenant), pageUrl(tenant), fields)

            val account = TrottaHtml.account(html)
                ?: throw TrottaException(TrottaHtml.loginError(html) ?: "Accesso non riuscito.")
            session.lastEmail = email
            account
        }

    suspend fun account(tenant: Tenant): Account? = withContext(Dispatchers.IO) {
        val html = get(pageUrl(tenant, "area_clienti"))
        if (!TrottaHtml.isLoggedIn(html)) null else TrottaHtml.account(html)
    }

    suspend fun activeTickets(tenant: Tenant): List<Ticket> = withContext(Dispatchers.IO) {
        TrottaHtml.tickets(get(pageUrl(tenant, "booking_attivi")))
    }

    suspend fun history(tenant: Tenant): List<Ticket> = withContext(Dispatchers.IO) {
        TrottaHtml.tickets(get(pageUrl(tenant, "booking")))
    }

    suspend fun cart(tenant: Tenant): List<CartItem> = withContext(Dispatchers.IO) {
        TrottaHtml.cart(get(pageUrl(tenant, "booking_carrello")))
    }

    suspend fun reserve(tenant: Tenant, count: Int): PaymentHandoff = withContext(Dispatchers.IO) {
        require(count in 1..100) { "Numero ticket non valido" }
        val fields = formFields("acquista") + listOf("num_ticket" to count.toString())
        val html = post(engineUrl(tenant), pageUrl(tenant, "area_ticket"), fields)
        TrottaHtml.payments(html).firstOrNull()
            ?: throw TrottaException("Prenotazione non riuscita.")
    }

    suspend fun activate(tenant: Tenant, ticket: Ticket, busNumber: String): Ticket? =
        withContext(Dispatchers.IO) {
            val rowId = ticket.rowId ?: throw TrottaException("Ticket non attivabile.")
            val guid = ticket.guid ?: throw TrottaException("Ticket non attivabile.")
            val bus = busNumber.trim()
            if (!bus.matches(Regex("""\d{1,4}"""))) {
                throw TrottaException("Inserisci il numero del bus: solo cifre, da 1 a 4.")
            }

            val fields = formFields("attiva") + listOf(
                "ID" to rowId.toString(),
                "GUID" to guid,
                "minuti" to (ticket.validityMinutes ?: 90).toString(),
                "bus_attivazione" to bus,
            )
            val html = post(engineUrl(tenant), pageUrl(tenant, "booking_attivi"), fields)

            TrottaHtml.activationFailure(html)?.let { throw TrottaException(it) }

            val refreshed = activeTickets(tenant)
            refreshed.firstOrNull { it.guid == guid }
                ?: history(tenant).firstOrNull { it.guid == guid }
                ?: if (TrottaHtml.activationConfirmed(html)) null
                else throw TrottaException("Attivazione non confermata dal server. Riprova.")
        }

    suspend fun logout(tenant: Tenant) = withContext(Dispatchers.IO) {
        runCatching {
            get(ORIGIN + tenant.basePath + "logout.aspx", referer = pageUrl(tenant, "area_clienti"))
        }
        session.clear()
    }

    private fun formFields(page: String, menu: String = "0") = mutableListOf(
        "IDScheda" to "2",
        "IDMenu" to menu,
        "page" to page,
        "usermode" to "",
        "pos" to "0",
    )

    private fun get(url: String, referer: String? = null): String {
        val builder = Request.Builder()
            .url(url)
            .header("Accept-Language", ACCEPT_LANGUAGE)
        referer?.let { builder.header("Referer", it) }
        return execute(builder.build())
    }

    private fun post(url: String, referer: String, fields: List<Pair<String, String>>): String {
        val form = FormBody.Builder()
        fields.forEach { (name, value) -> form.add(name, value) }
        val request = Request.Builder()
            .url(url)
            .header("Accept-Language", ACCEPT_LANGUAGE)
            .header("Referer", referer)
            .post(form.build())
            .build()
        return execute(request)
    }

    private fun execute(request: Request): String = client.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw TrottaException("Errore HTTP ${response.code} da ${request.url.encodedPath}")
        }
        body
    }

    companion object {
        const val ORIGIN = "https://ticketonbus.trotta.it/"
        private const val ACCEPT_LANGUAGE = "it-IT,it;q=0.9,en;q=0.8"
    }
}
