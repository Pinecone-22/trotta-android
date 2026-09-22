package it.trotta.ticketonbus.data

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

internal object TrottaHtml {

    private const val LOOP_START = "INIZIO LOOP"
    private const val LOOP_END = "FINE LOOP"

    private val ACTIVATE = Regex("""Attiva\(\s*'(\d+)'\s*,\s*'([^']+)'\s*,\s*(\d+)""")
    private val LOGIN_ERROR = Regex("""Area Clienti\s*(.*?)\s*Riprova""", RegexOption.DOT_MATCHES_ALL)
    private val HELLO = Regex("""Benvenuto,(?:&nbsp;|\s)*([^<.]+)""")
    private val RESERVED_FOR = Regex("""Area riservata di:\s*([^<&]+)""")
    private val FIRST_INT = Regex("""(\d+)""")
    private val WHITESPACE = Regex("""\s+""")

    private val ACTIVATION_FAILED = Regex(
        """NON EFFETTUATA\.?(.*?)(?:Clicca|BORSELLINO TICKET|</h5>)""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    )
    private val ACTIVATION_OK = Regex("""Validazione ticket EFFETTUATA""", RegexOption.IGNORE_CASE)

    fun isLoggedIn(html: String): Boolean = HELLO.containsMatchIn(html)

    fun account(html: String): Account? {
        val firstName = HELLO.find(html)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
            ?: return null
        val fullName = RESERVED_FOR.find(html)?.groupValues?.get(1)
            ?.replace(WHITESPACE, " ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        return Account(firstName = firstName, fullName = fullName ?: firstName)
    }

    fun loginError(html: String): String? {
        val raw = LOGIN_ERROR.find(html)?.groupValues?.get(1) ?: return null
        return Jsoup.parseBodyFragment(raw).text().replace(WHITESPACE, " ").trim().takeIf { it.isNotEmpty() }
    }

    fun activationFailure(html: String): String? {
        if (ACTIVATION_OK.containsMatchIn(html)) return null
        val raw = ACTIVATION_FAILED.find(html)?.groupValues?.get(1)?.trim() ?: return null
        val text = Jsoup.parseBodyFragment(raw).text().replace(WHITESPACE, " ").trim()
        return text.ifEmpty { "Validazione del ticket non effettuata." }
    }

    fun activationConfirmed(html: String): Boolean = ACTIVATION_OK.containsMatchIn(html)

    fun tickets(html: String): List<Ticket> = fragments(html).mapNotNull(::parseTicket)

    fun cart(html: String): List<CartItem> = fragments(html).mapNotNull(::parseCart)

    fun payments(html: String): List<PaymentHandoff> = document(html)
        .select("a[href*=payment.trotta.it]")
        .mapNotNull { paymentFrom(it.attr("href")) }

    fun document(html: String): Document = Jsoup.parse(html)

    private fun fragments(html: String): List<String> {
        val out = ArrayList<String>()
        var cursor = 0
        while (true) {
            val start = html.indexOf(LOOP_START, cursor)
            if (start < 0) break
            val end = html.indexOf(LOOP_END, start)
            if (end < 0) break
            out += html.substring(start + LOOP_START.length, end)
            cursor = end + LOOP_END.length
        }
        return out
    }

    private fun labelValues(fragment: String): Map<String, String> {
        val doc = Jsoup.parseBodyFragment(fragment)
        val fields = LinkedHashMap<String, String>()
        doc.select("h5").forEach { h5 ->
            val text = h5.text().replace(WHITESPACE, " ").trim()
            val colon = text.indexOf(':')
            if (colon > 0) {
                fields[text.substring(0, colon).trim().lowercase()] = text.substring(colon + 1).trim()
            }
        }
        return fields
    }

    private fun parseTicket(fragment: String): Ticket? {
        val fields = labelValues(fragment)
        val rawNumber = fields["n.ticket"] ?: return null

        val delAt = rawNumber.lastIndexOf(" del ")
        val number = if (delAt > 0) rawNumber.substring(0, delAt).trim() else rawNumber
        val issuedOn = if (delAt > 0) rawNumber.substring(delAt + 5).trim() else null

        val onclick = Jsoup.parseBodyFragment(fragment)
            .select("a[onclick]")
            .firstOrNull { it.attr("onclick").contains("Attiva") }
            ?.attr("onclick")
        val activate = onclick?.let { ACTIVATE.find(it) }

        val minutes = activate?.groupValues?.get(3)?.toIntOrNull()
            ?: fields["validità"]?.let { FIRST_INT.find(it)?.groupValues?.get(1)?.toIntOrNull() }

        return Ticket(
            rowId = activate?.groupValues?.get(1)?.toLongOrNull(),
            guid = activate?.groupValues?.get(2),
            number = number,
            issuedOn = issuedOn,
            bookedAt = fields["data prenotazione"].orNullIfBlank(),
            purchasedAt = fields["data acquisto"].orNullIfBlank(),
            status = TicketStatus.from(fields["stato"]),
            validFrom = fields["data inizio validità"].orNullIfBlank(),
            validTo = fields["data fine validità"].orNullIfBlank(),
            validityMinutes = minutes,
            price = fields["prezzo"].orNullIfBlank(),
            transaction = fields["transazione"].orNullIfBlank(),
            paymentId = fields["id pagamento"].orNullIfBlank(),
        )
    }

    private fun parseCart(fragment: String): CartItem? {
        val doc = Jsoup.parseBodyFragment(fragment)
        val fields = labelValues(fragment)
        val payment = doc.select("a[href*=payment.trotta.it]")
            .firstOrNull()
            ?.let { paymentFrom(it.attr("href")) }
        if (payment == null && fields.isEmpty()) return null

        val count = fields["totale ticket"]?.let { FIRST_INT.find(it)?.groupValues?.get(1)?.toIntOrNull() }
            ?: fields["totale biglietti"]?.let { FIRST_INT.find(it)?.groupValues?.get(1)?.toIntOrNull() }
        val total = fields["prezzo totale"]
            ?: fields["totale biglietti"]?.substringAfter("Totale importo:", "")?.trim()?.takeIf { it.isNotEmpty() }
        val bookingId = payment?.bookingGuid
            ?: doc.select("div[data-target^=#collapsebox_]").attr("data-target")
                .removePrefix("#collapsebox_").takeIf { it.isNotEmpty() }

        return CartItem(
            bookingId = bookingId,
            bookedAt = fields["data prenotazione"].orNullIfBlank(),
            ticketCount = count,
            totalPrice = total,
            payment = payment,
        )
    }

    private fun paymentFrom(href: String): PaymentHandoff? {
        val url = href.toHttpUrlOrNull() ?: return null
        if (!url.host.contains("payment.trotta.it")) return null
        return PaymentHandoff(
            url = href,
            bookingGuid = url.queryParameter("id"),
            transactionNumber = url.queryParameter("num_trans"),
            amount = url.queryParameter("prezzo"),
            zone = url.queryParameter("zona"),
            description = url.queryParameter("desc"),
        )
    }

    private fun String?.orNullIfBlank(): String? = this?.takeIf { it.isNotBlank() }
}
