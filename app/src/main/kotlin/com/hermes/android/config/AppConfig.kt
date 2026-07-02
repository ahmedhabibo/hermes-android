package com.hermes.android.config

object AppConfig {
    const val APP_VERSION = "0.1.0"
    const val APP_NAME = "Hermex"

    // hermes-webui server defaults
    const val DEFAULT_PORT = 8787
    const val DEFAULT_HOST = "127.0.0.1"

    // SSE heartbeat interval (server sends every 30s)
    const val SSE_HEARTBEAT_INTERVAL_SEC = 30

    // Offline cache
    const val CACHE_TTL_DAYS = 7
    const val CACHE_MAX_MESSAGES = 5000

    // Request timeouts
    const val CONNECT_TIMEOUT_SEC = 30L
    const val READ_TIMEOUT_SEC = 120L
    const val WRITE_TIMEOUT_SEC = 30L
}
