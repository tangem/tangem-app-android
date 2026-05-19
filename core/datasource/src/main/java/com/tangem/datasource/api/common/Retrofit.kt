package com.tangem.datasource.api.common

import android.util.Log
import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import okhttp3.Interceptor

fun createNetworkLoggingInterceptor(): Interceptor {
    return LoggingInterceptor.Builder()
        .setLevel(Level.BODY)
        .log(Log.VERBOSE)
        .tag(NETWORK_LOGS_TAG)
        .build()
}

private const val NETWORK_LOGS_TAG = "NetworkLogs"