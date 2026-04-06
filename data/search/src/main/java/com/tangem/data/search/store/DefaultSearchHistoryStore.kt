package com.tangem.data.search.store

import androidx.datastore.core.DataStore
import com.tangem.data.search.model.RecentTokenDTO
import com.tangem.data.search.model.SearchHistoryDTO
import com.tangem.data.search.model.TextHintDTO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DefaultSearchHistoryStore(
    private val dataStore: DataStore<SearchHistoryDTO>,
) : SearchHistoryStore {

    override fun getTextHints(): Flow<List<TextHintDTO>> {
        return dataStore.data.map { it.textHints.sortedByDescending(TextHintDTO::timestamp) }
    }

    override fun getRecentTokens(): Flow<List<RecentTokenDTO>> {
        return dataStore.data.map { it.recentTokens.sortedByDescending(RecentTokenDTO::timestamp) }
    }

    override suspend fun saveTextHint(hint: TextHintDTO) {
        dataStore.updateData { current ->
            val updated = current.textHints
                .filter { it.text != hint.text }
                .toMutableList()
                .apply { add(0, hint) }
                .take(MAX_HISTORY_SIZE)
            current.copy(textHints = updated)
        }
    }

    override suspend fun saveRecentToken(token: RecentTokenDTO) {
        dataStore.updateData { current ->
            val updated = current.recentTokens
                .filter { it.id != token.id }
                .toMutableList()
                .apply { add(0, token) }
                .take(MAX_HISTORY_SIZE)
            current.copy(recentTokens = updated)
        }
    }

    override suspend fun clearAll() {
        dataStore.updateData {
            SearchHistoryDTO()
        }
    }

    private companion object {
        const val MAX_HISTORY_SIZE = 3
    }
}