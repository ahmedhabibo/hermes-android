package com.hermes.android

import android.app.Application
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Cookie

class HermesApp : Application() {
    lateinit var httpClient: OkHttpClient
    companion object {
        lateinit var instance: HermesApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        val cookieJar = object : CookieJar {
            private val cookieStore = mutableMapOf<String, List<Cookie>>()

            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val encodedUrl = url.toString()
                cookieStore[encodedUrl] = cookies
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.toString()] ?: emptyList()
            }
        }
        httpClient = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .build()
    }
}