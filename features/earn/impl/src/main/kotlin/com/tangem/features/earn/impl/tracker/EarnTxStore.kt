package com.tangem.features.earn.impl.tracker

import androidx.datastore.preferences.core.stringPreferencesKey
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.features.earn.product.api.session.EarnTxRef
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** A JSON string: [EarnTxRef] is a kotlinx type, and the object helpers of [AppPreferencesStore] use Moshi. */
internal class EarnTxStore @Inject constructor(
    private val appPreferencesStore: AppPreferencesStore,
) {

    suspend fun add(ref: EarnTxRef) {
        appPreferencesStore.editData { preferences ->
            val refs = decode(preferences[sentTransactionsKey])
            preferences[sentTransactionsKey] = encode(refs.filterNot { it.id == ref.id } + ref)
        }
    }

    suspend fun remove(id: String) {
        appPreferencesStore.editData { preferences ->
            val refs = decode(preferences[sentTransactionsKey])
            preferences[sentTransactionsKey] = encode(refs.filterNot { it.id == id })
        }
    }

    suspend fun all(): List<EarnTxRef> = decode(appPreferencesStore.data.first()[sentTransactionsKey])

    private fun encode(refs: List<EarnTxRef>): String = json.encodeToString(serializer, refs)

    private fun decode(value: String?): List<EarnTxRef> {
        if (value == null) return emptyList()

        return try {
            json.decodeFromString(serializer, value)
        } catch (e: Exception) {
            TangemLogger.e("Earn journal is unreadable, dropped", e)
            emptyList()
        }
    }

    companion object {
        val sentTransactionsKey = stringPreferencesKey(name = "earnSentTransactions")

        private val serializer = ListSerializer(EarnTxRef.serializer())

        private val json = Json { ignoreUnknownKeys = true }
    }
}