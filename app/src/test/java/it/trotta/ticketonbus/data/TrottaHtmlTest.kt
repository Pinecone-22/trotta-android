package it.trotta.ticketonbus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrottaHtmlTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/$name")) {
            "missing fixture: $name"
        }.bufferedReader().use { it.readText() }

    @Test
    fun `detects a logged in dashboard`() {
        assertTrue(TrottaHtml.isLoggedIn(fixture("area_clienti.html")))
    }

    @Test
    fun `does not treat the anonymous page as logged in`() {
        assertFalse(TrottaHtml.isLoggedIn(fixture("anonimo.html")))
        assertNull(TrottaHtml.account(fixture("anonimo.html")))
    }

    @Test
    fun `reads name and account holder from the header`() {
        val account = TrottaHtml.account(fixture("area_clienti.html"))
        assertEquals("ANDREA", account?.firstName)
        assertEquals("MARIO ROSSI", account?.fullName)
    }

    @Test
    fun `surfaces the engine login error text`() {
        assertEquals("Specificare mail e password!", TrottaHtml.loginError(fixture("login_errore.html")))
    }

    @Test
    fun `parses every ticket in the wallet`() {
        val tickets = TrottaHtml.tickets(fixture("booking_attivi.html"))

        assertEquals(3, tickets.size)

        val first = tickets.first()
        assertEquals(8920L, first.rowId)
        assertEquals("aaaaaaaa-0000-0000-0000-000000000001", first.guid)
        assertEquals("20260922_065927_83_1", first.number)
        assertEquals("22/09/2026", first.issuedOn)
        assertEquals(TicketStatus.NOT_ACTIVE, first.status)
        assertEquals(90, first.validityMinutes)
        assertEquals("1,00 €", first.price)
        assertEquals("CAPTURED", first.transaction)
        assertEquals("100000000000000001", first.paymentId)
        assertEquals("22/09/2026 06:58", first.bookedAt)
        assertEquals("22/09/2026 06:59", first.purchasedAt)
        assertNull(first.validFrom)
        assertTrue(first.canActivate)
    }

    @Test
    fun `keeps the ticket order and distinct guids`() {
        val tickets = TrottaHtml.tickets(fixture("booking_attivi.html"))
        assertEquals(listOf(8920L, 8921L, 8922L), tickets.map { it.rowId })
        assertEquals(3, tickets.mapNotNull { it.guid }.toSet().size)
    }

    @Test
    fun `parses expired tickets without an activation handle`() {
        val history = TrottaHtml.tickets(fixture("booking_storico.html"))

        assertEquals(4, history.size)
        assertTrue(history.all { it.status == TicketStatus.EXPIRED })

        val expired = history.first()
        assertNull(expired.rowId)
        assertNull(expired.guid)
        assertFalse(expired.canActivate)
        assertEquals("22/09/2026 07:18", expired.validFrom)
        assertEquals("22/09/2026 08:48", expired.validTo)

        assertTrue(history.none { it.canActivate })
    }

    @Test
    fun `parses an unpaid reservation and its nexi hand-off`() {
        val cart = TrottaHtml.cart(fixture("booking_carrello.html"))

        assertEquals(1, cart.size)
        val item = cart.first()
        assertEquals("bbbbbbbb-0000-0000-0000-000000000001", item.bookingId)
        assertEquals(1, item.ticketCount)
        assertEquals("1,00 €", item.totalPrice)
        assertEquals("22/09/2026 17:11", item.bookedAt)

        val payment = item.payment
        assertEquals("bbbbbbbb-0000-0000-0000-000000000001", payment?.bookingGuid)
        assertEquals("5083", payment?.transactionNumber)
        assertEquals("1", payment?.amount)
        assertEquals("TB00", payment?.zone)
        assertEquals("Acquisto Ticket On Bus", payment?.description)
        assertTrue(payment!!.url.startsWith("https://payment.trotta.it/nexi/response.aspx"))
    }

    @Test
    fun `parses the payment link returned after reserving`() {
        val payments = TrottaHtml.payments(fixture("acquista.html"))

        assertEquals(1, payments.size)
        assertEquals("bbbbbbbb-0000-0000-0000-000000000001", payments.first().bookingGuid)
        assertEquals("TB00", payments.first().zone)
    }

    @Test
    fun `an empty wallet yields no tickets`() {

        assertTrue(TrottaHtml.tickets(fixture("anonimo.html")).isEmpty())
        assertTrue(TrottaHtml.cart(fixture("anonimo.html")).isEmpty())
        assertTrue(TrottaHtml.payments(fixture("anonimo.html")).isEmpty())
    }

    @Test
    fun `surfaces the engine refusal reason for a validation`() {
        val refusal = fixture("attiva_rifiutata.html")
        assertEquals("Il codice vettura che hai inserito è errato.", TrottaHtml.activationFailure(refusal))
        assertFalse(TrottaHtml.activationConfirmed(refusal))
    }

    @Test
    fun `recognises a confirmed validation`() {
        val confirmed = fixture("attiva_confermata.html")
        assertTrue(TrottaHtml.activationConfirmed(confirmed))
        assertNull(TrottaHtml.activationFailure(confirmed))
    }

    @Test
    fun `a wallet page is neither a confirmation nor a refusal`() {
        val wallet = fixture("booking_attivi.html")
        assertFalse(TrottaHtml.activationConfirmed(wallet))
        assertNull(TrottaHtml.activationFailure(wallet))
    }

    @Test
    fun `maps engine status strings`() {
        assertEquals(TicketStatus.NOT_ACTIVE, TicketStatus.from("NON ATTIVO"))
        assertEquals(TicketStatus.ACTIVE, TicketStatus.from("ATTIVO"))
        assertEquals(TicketStatus.EXPIRED, TicketStatus.from("SCADUTO"))
        assertEquals(TicketStatus.UNKNOWN, TicketStatus.from(""))
        assertEquals(TicketStatus.UNKNOWN, TicketStatus.from(null))
    }
}
