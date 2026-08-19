package com.tangem.data.jointaccount.store

import com.tangem.common.services.secure.SecureStorage
import com.tangem.domain.jointaccount.store.JointAccountInvitesStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
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

    override suspend fun clear(userWalletId: UserWalletId, cryptoAccountId: String) {
        withContext(dispatchers.io) {
            mutex.withLock {
                val entries = read(userWalletId).orEmpty() - cryptoAccountId

                if (entries.isEmpty()) {
                    secureStorage.delete(createKey(userWalletId))
                } else {
                    secureStorage.store(createKey(userWalletId), json.encodeToString(SERIALIZER, entries))
                }
            }
        }
    }

    override suspend fun clearAll(userWalletId: UserWalletId) {
        withContext(dispatchers.io) {
            mutex.withLock { secureStorage.delete(createKey(userWalletId)) }
        }
    }

    private fun read(userWalletId: UserWalletId): Map<String, List<String>>? {
        val key = createKey(userWalletId)
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

    private companion object {
        const val KEY_PREFIX = "joint_account_invites_"

        val SERIALIZER = MapSerializer(
            keySerializer = String.serializer(),
            valueSerializer = ListSerializer(String.serializer()),
        )
    }
}