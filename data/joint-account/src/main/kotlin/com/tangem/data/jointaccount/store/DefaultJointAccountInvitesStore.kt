package com.tangem.data.jointaccount.store

import com.tangem.common.services.secure.SecureStorage
import com.tangem.domain.jointaccount.store.JointAccountInvitesStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

internal class DefaultJointAccountInvitesStore(
    private val secureStorage: SecureStorage,
    private val json: Json,
    private val dispatchers: CoroutineDispatcherProvider,
) : JointAccountInvitesStore {

    private val logger = TangemLogger.withTag(tag = "JointAccountInvitesStore")
    private val mutex = Mutex()

    /** Secure storage cannot be observed, so every write bumps this to make [observeShared] read again */
    private val writes = MutableStateFlow(value = 0)

    override suspend fun store(userWalletId: UserWalletId, cryptoAccountId: String, invites: List<String>) {
        withContext(dispatchers.io) {
            mutex.withLock {
                val entries = read(userWalletId).orEmpty() + (cryptoAccountId to invites)

                secureStorage.store(createKey(userWalletId), json.encodeToString(SERIALIZER, entries))
            }
        }
    }

    override suspend fun get(userWalletId: UserWalletId, cryptoAccountId: String): List<String>? {
        return withContext(dispatchers.io) {
            mutex.withLock { read(userWalletId)?.get(cryptoAccountId) }
        }
    }

    override suspend fun markShared(userWalletId: UserWalletId, cryptoAccountId: String, inviteId: String) {
        withContext(dispatchers.io) {
            mutex.withLock {
                val key = createSharedKey(userWalletId)
                val entries = read(key).orEmpty()
                val shared = entries[cryptoAccountId].orEmpty()
                if (inviteId in shared) return@withLock

                val updated = entries + (cryptoAccountId to shared + inviteId)
                secureStorage.store(key, json.encodeToString(SERIALIZER, updated))
            }
            writes.update { it + 1 }
        }
    }

    override fun observeShared(userWalletId: UserWalletId, cryptoAccountId: String): Flow<Set<String>> {
        return writes
            .map {
                // Caught per read: a failure ending the flow would hide every later write from the screen
                runSuspendCatching {
                    mutex.withLock { read(createSharedKey(userWalletId))?.get(cryptoAccountId).orEmpty().toSet() }
                }.getOrElse { throwable ->
                    logger.e(messageString = "Failed to read the shared invites", throwable = throwable)
                    emptySet()
                }
            }
            .distinctUntilChanged()
            .flowOn(dispatchers.io)
    }

    override suspend fun clear(userWalletId: UserWalletId, cryptoAccountId: String) {
        withContext(dispatchers.io) {
            mutex.withLock {
                remove(key = createKey(userWalletId), cryptoAccountId = cryptoAccountId)
                remove(key = createSharedKey(userWalletId), cryptoAccountId = cryptoAccountId)
            }
            writes.update { it + 1 }
        }
    }

    override suspend fun clearAll(userWalletId: UserWalletId) {
        withContext(dispatchers.io) {
            mutex.withLock {
                secureStorage.delete(createKey(userWalletId))
                secureStorage.delete(createSharedKey(userWalletId))
            }
            writes.update { it + 1 }
        }
    }

    private fun remove(key: String, cryptoAccountId: String) {
        val entries = read(key).orEmpty() - cryptoAccountId

        if (entries.isEmpty()) {
            secureStorage.delete(key)
        } else {
            secureStorage.store(key, json.encodeToString(SERIALIZER, entries))
        }
    }

    private fun read(userWalletId: UserWalletId): Map<String, List<String>>? = read(createKey(userWalletId))

    private fun read(key: String): Map<String, List<String>>? {
        val payload = secureStorage.getAsString(key) ?: return null

        return try {
            json.decodeFromString(SERIALIZER, payload)
        } catch (e: SerializationException) {
            // The payload is a secret — log the fact, never the content or the parser message
            logger.e(messageString = "Failed to decode the stored invites; clearing them")
            secureStorage.delete(key)
            null
        }
    }

    private fun createKey(userWalletId: UserWalletId): String = KEY_PREFIX + userWalletId.stringValue

    private fun createSharedKey(userWalletId: UserWalletId): String = SHARED_KEY_PREFIX + userWalletId.stringValue

    private companion object {
        const val KEY_PREFIX = "joint_account_invites_"
        const val SHARED_KEY_PREFIX = "joint_account_shared_invites_"

        val SERIALIZER = MapSerializer(
            keySerializer = String.serializer(),
            valueSerializer = ListSerializer(String.serializer()),
        )
    }
}