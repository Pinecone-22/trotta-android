package it.trotta.ticketonbus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoogleAccountTest {

    @Test
    fun `keeps the values Google returns`() {
        val account = googleAccountOf(
            id = "1234567890",
            email = "andrea@example.com",
            displayName = "Andrea Tamburini",
            givenName = "Andrea",
        )

        assertEquals("1234567890", account?.id)
        assertEquals("andrea@example.com", account?.email)
        assertEquals("Andrea Tamburini", account?.displayName)
        assertNull(account?.pictureUrl)
        assertEquals("Andrea Tamburini", account?.label)
    }

    @Test
    fun `falls back to the given name when there is no display name`() {
        val account = googleAccountOf(
            id = "42",
            email = "a@b.it",
            displayName = "  ",
            givenName = "Andrea",
        )
        assertEquals("Andrea", account?.displayName)
        assertEquals("Andrea", account?.label)
    }

    @Test
    fun `uses the email as label when no name is available`() {
        val account = googleAccountOf(id = "42", email = "a@b.it", displayName = null, givenName = null)
        assertEquals("a@b.it", account?.label)
    }

    @Test
    fun `falls back to the email when the account id is missing`() {
        val account = googleAccountOf(id = "", email = "a@b.it", displayName = null, givenName = null)
        assertEquals("a@b.it", account?.id)
    }

    @Test
    fun `rejects a credential with nothing to identify the account`() {
        assertNull(googleAccountOf(id = " ", email = null, displayName = "Andrea", givenName = null))
    }
}
