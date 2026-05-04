package com.tangem.data.search.repository

import com.tangem.data.search.converter.RecentSearchTokenToRecentTokenDTOConverter
import com.tangem.data.search.converter.RecentTokenDTOToRecentSearchTokenConverter
import com.tangem.data.search.converter.TextHintDTOToSearchTextHintConverter
import com.tangem.data.search.model.TextHintDTO
import com.tangem.data.search.store.SearchHistoryStore
import com.tangem.domain.search.model.RecentSearchToken
import com.tangem.domain.search.model.SearchTextHint
import com.tangem.domain.search.repository.SearchRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class DefaultSearchRepository(
    private val store: SearchHistoryStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : SearchRepository {

    private val textHintConverter by lazy {
        TextHintDTOToSearchTextHintConverter()
    }
    private val recentTokenConverter by lazy {
        RecentTokenDTOToRecentSearchTokenConverter()
    }
    private val recentTokenDMConverter by lazy {
        RecentSearchTokenToRecentTokenDTOConverter()
    }

    override fun getTextHints(): Flow<List<SearchTextHint>> {
        return store.getTextHints()
            .map { textHintConverter.convertList(it) }
            .flowOn(dispatchers.io)
    }

    override fun getRecentTokens(): Flow<List<RecentSearchToken>> {
        return store.getRecentTokens()
            .map { recentTokenConverter.convertList(it) }
            .flowOn(dispatchers.io)
    }

    override suspend fun saveTextHint(text: String) = withContext(dispatchers.io) {
        store.saveTextHint(
            TextHintDTO(
                text = text,
                timestamp = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun saveRecentToken(token: RecentSearchToken) = withContext(dispatchers.io) {
        store.saveRecentToken(recentTokenDMConverter.convert(token))
    }

    override suspend fun clearHistory() = withContext(dispatchers.io) {
        store.clearAll()
    }
}