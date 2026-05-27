package com.tangem.tap.common.log

import android.app.Application
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.tangem.Log
import com.tangem.TangemSdkLogger
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import com.tangem.datasource.api.common.createNetworkLoggingInterceptor
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.datasource.utils.NetworkLogsSaveInterceptor
import com.tangem.datasource.utils.WireMockRedirectInterceptor
import com.tangem.domain.common.LogConfig
import com.tangem.operations.attestation.api.TangemApiServiceSettings
import com.tangem.utils.logging.TangemLogger
import com.tangem.wallet.BuildConfig

/**
 * Owns all app-startup wiring of the logging subsystem in a single place:
 *  - [initAppLogging] — registers [TangemLogger] writers (Logcat + file).
 *  - [initSdkLogging] — registers the Card SDK logger with [Log] and installs OkHttp
 *    interceptors for the Blockchain SDK and the Tangem API.
 *
 * @property appLogsStore     app logs store used by file-based writer and the network logs save
 *                            interceptor
 * @property tangemSdkLogger  Card SDK logger registered with [Log.addLogger]
 *
[REDACTED_AUTHOR]
 */
class TangemLoggingInitializer(
    private val appLogsStore: AppLogsStore,
    private val tangemSdkLogger: TangemSdkLogger,
) {

    fun initAppLogging() {
        TangemLogger.setLogWriters(
            buildList {
                if (BuildConfig.LOG_ENABLED) {
                    add(LogcatLogWriter())
                }
                add(FileLogWriter(appLogsStore))
            },
        )
    }

    /**
     * Configure logging for the underlying SDKs:
     *  - register [tangemSdkLogger] with the Card SDK static [Log] facade,
     *  - install OkHttp interceptors for the Blockchain SDK and Tangem API.
     *
     * Must be called from `TangemApplication.init()` AFTER `entryPoint.getWalletsRepository()`
     * has triggered Hilt singletons construction — in particular `DefaultCardSdkProvider`,
     * whose init block registers `AddHeadersInterceptor` in [TangemApiServiceSettings].
     * Calling this method earlier would invert the OkHttp interceptor chain order and
     * cause logging interceptors to see requests *without* auth headers.
     */
    fun initSdkLogging(application: Application) {
        Log.addLogger(logger = tangemSdkLogger)

        if (!LogConfig.network.isBlockchainSdkNetworkLogEnabled) return

        BlockchainSdkRetrofitBuilder.interceptors = buildList {
            if (BuildConfig.MOCK_DATA_SOURCE) {
                add(WireMockRedirectInterceptor())
            }
            add(createNetworkLoggingInterceptor())
            add(ChuckerInterceptor(application))
        }

        TangemApiServiceSettings.addInterceptors(
            *buildList {
                if (BuildConfig.MOCK_DATA_SOURCE) {
                    add(WireMockRedirectInterceptor())
                }
                add(createNetworkLoggingInterceptor())
                add(ChuckerInterceptor(application))
                add(NetworkLogsSaveInterceptor(appLogsStore))
            }.toTypedArray(),
        )
    }
}