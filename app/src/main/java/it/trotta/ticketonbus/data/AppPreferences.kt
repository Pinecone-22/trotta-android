package it.trotta.ticketonbus.data

import android.content.Context

class AppPreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var favourites: Set<String>
        get() = prefs.getStringSet(KEY_FAVOURITES, emptySet()).orEmpty()
        set(value) = prefs.edit().putStringSet(KEY_FAVOURITES, value).apply()

    fun isFavourite(stopId: String): Boolean = stopId in favourites

    fun toggleFavourite(stopId: String): Boolean {
        val current = favourites
        val next = if (stopId in current) current - stopId else current + stopId
        favourites = next
        return stopId in next
    }

    companion object {
        private const val FILE = "app_settings"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_FAVOURITES = "favourites"

        fun languageOf(context: Context): String =
            context.applicationContext
                .getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE, "")
                .orEmpty()
    }
}
