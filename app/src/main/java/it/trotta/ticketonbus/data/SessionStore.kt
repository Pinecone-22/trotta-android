package it.trotta.ticketonbus.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class SessionStore(context: Context) : CookieJar {

    private val prefs: SharedPreferences = openPrefs(context)

    private val lock = Any()
    private val jar = LinkedHashMap<String, Cookie>()

    init {
        restore()
    }

    var tenant: Tenant
        get() = Tenant.fromName(prefs.getString(KEY_TENANT, null))
        set(value) = prefs.edit().putString(KEY_TENANT, value.name).apply()

    var lastEmail: String?
        get() = prefs.getString(KEY_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_EMAIL, value).apply()

    var googleAccount: GoogleAccount?
        get() {
            val id = prefs.getString(KEY_GOOGLE_ID, null) ?: return null
            return GoogleAccount(
                id = id,
                email = prefs.getString(KEY_GOOGLE_EMAIL, null),
                displayName = prefs.getString(KEY_GOOGLE_NAME, null),
                pictureUrl = prefs.getString(KEY_GOOGLE_PICTURE, null),
            )
        }
        set(value) = prefs.edit()
            .putString(KEY_GOOGLE_ID, value?.id)
            .putString(KEY_GOOGLE_EMAIL, value?.email)
            .putString(KEY_GOOGLE_NAME, value?.displayName)
            .putString(KEY_GOOGLE_PICTURE, value?.pictureUrl)
            .apply()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            cookies.forEach { cookie ->
                // a cookie that is already expired is the server asking us to delete it
                if (cookie.expiresAt <= now) jar.remove(keyOf(cookie)) else jar[keyOf(cookie)] = cookie
            }
            persist()
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            return jar.values.filter { it.expiresAt > now && it.matches(url) }
        }
    }

    fun clear() {
        synchronized(lock) {
            jar.clear()
            persist()
        }
    }

    private fun keyOf(c: Cookie) = "${c.name}|${c.domain}|${c.path}"

    private fun persist() {
        val blob = jar.values.joinToString("\n") { encode(it) }
        prefs.edit().putString(KEY_COOKIES, blob).apply()
    }

    private fun restore() {
        val blob = prefs.getString(KEY_COOKIES, null) ?: return
        blob.lineSequence().forEach { line ->
            decode(line)?.let { jar[keyOf(it)] = it }
        }
    }

    private fun encode(c: Cookie) = listOf(
        c.name,
        c.value,
        c.domain,
        c.path,
        c.expiresAt.toString(),
        if (c.secure) "1" else "0",
        if (c.httpOnly) "1" else "0",
        if (c.hostOnly) "1" else "0",
    ).joinToString(SEP)

    private fun decode(line: String): Cookie? {
        val parts = line.split(SEP)
        if (parts.size != 8) return null
        return runCatching {
            val builder = Cookie.Builder()
                .name(parts[0])
                .value(parts[1])
                .path(parts[3])
                .expiresAt(parts[4].toLong())
                .apply {
                    if (parts[7] == "1") hostOnlyDomain(parts[2]) else domain(parts[2])
                    if (parts[5] == "1") secure()
                    if (parts[6] == "1") httpOnly()
                }
                .build()
            builder
        }.getOrNull()
    }

    private companion object {
        const val FILE = "trotta_session"

        /**
         * The Keystore master key can become unusable (restored device, security-patch resets, ...),
         * in which case opening the encrypted file throws on every launch. The session is only a
         * cache of cookies, so start over with a fresh file instead of crashing.
         */
        fun openPrefs(context: Context): SharedPreferences = try {
            createPrefs(context)
        } catch (e: Exception) {
            context.deleteSharedPreferences(FILE)
            createPrefs(context)
        }

        private fun createPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }

        const val SEP = "\u0001"
        const val KEY_COOKIES = "cookies"
        const val KEY_TENANT = "tenant"
        const val KEY_EMAIL = "email"
        const val KEY_GOOGLE_ID = "google_id"
        const val KEY_GOOGLE_EMAIL = "google_email"
        const val KEY_GOOGLE_NAME = "google_name"
        const val KEY_GOOGLE_PICTURE = "google_picture"
    }
}
