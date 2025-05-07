package edu.pmdm.movaapp.repository

import android.content.Context

object TokenManager {

    private const val PREFS_NAME = "amadeus_prefs"
    private const val KEY_TOKEN = "ty9uT4C74Z758BY7zSUCzWunCgSb"
    private const val KEY_EXPIRY = "1799"

    fun saveToken(context: Context, token: String, expiresIn: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val expiryTime = System.currentTimeMillis() + (expiresIn * 1000)
        editor.putString(KEY_TOKEN, token)
        editor.putLong(KEY_EXPIRY, expiryTime)
        editor.apply()
    }

    fun getValidToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_TOKEN, null)
        val expiry = prefs.getLong(KEY_EXPIRY, 0)
        val now = System.currentTimeMillis()
        return if (token != null && now < expiry) token else null
    }
}
