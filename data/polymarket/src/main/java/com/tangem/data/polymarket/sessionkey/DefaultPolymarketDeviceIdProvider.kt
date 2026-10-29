package com.tangem.data.polymarket.sessionkey

import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toHexString
import com.tangem.common.services.secure.SecureStorage
import com.tangem.domain.polymarket.sessionkey.PolymarketDeviceIdProvider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.info.AppInfoProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

/** @param androidId reads `ANDROID_ID`, `null` when the platform has none */
internal class DefaultPolymarketDeviceIdProvider(
    private val androidId: () -> String?,
    appInfoProvider: AppInfoProvider,
    private val secureStorage: SecureStorage,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketDeviceIdProvider {

    private val mutex = Mutex()
    private var cached: String? = null

    override val deviceName: String = appInfoProvider.device.trim().take(MAX_DEVICE_NAME_LENGTH)

    override suspend fun deviceId(): String = withContext(dispatchers.io) {
        mutex.withLock {
            cached ?: resolve().also { cached = it }
        }
    }

    private suspend fun resolve(): String {
        val androidId = runSuspendCatching { androidId() }
            .onFailure { TangemLogger.e("Failed to read ANDROID_ID", it) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }

        return androidId?.let(::hash) ?: storedOrNewUuid()
    }

    private fun hash(androidId: String): String = (HASH_PREFIX + androidId)
        .toByteArray()
        .calculateSha256()
        .toHexString()
        .lowercase()

    private suspend fun storedOrNewUuid(): String = runSuspendCatching {
        secureStorage.getAsString(UUID_KEY)
            ?: UUID.randomUUID().toString().also { secureStorage.store(UUID_KEY, it) }
    }
        .onFailure { TangemLogger.e("Failed to persist the Polymarket device id", it) }
        .getOrElse { UUID.randomUUID().toString() }

    private companion object {
        const val UUID_KEY = "polymarket_device_id"
        const val HASH_PREFIX = "polymarket-device-id:"
        const val MAX_DEVICE_NAME_LENGTH = 64
    }
}