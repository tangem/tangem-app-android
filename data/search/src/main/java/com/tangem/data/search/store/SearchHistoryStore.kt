package com.tangem.data.search.store

import com.tangem.data.search.model.RecentTokenDTO
import com.tangem.data.search.model.TextHintDTO
import kotlinx.coroutines.flow.Flow

internal interface SearchHistoryStore {
    fun getTextHints(): Flow<List<TextHintDTO>>
    fun getRecentTokens(): Flow<List<RecentTokenDTO>>
    suspend fun saveTextHint(hint: TextHintDTO)
    suspend fun saveRecentToken(token: RecentTokenDTO)
    suspend fun clearAll()
}