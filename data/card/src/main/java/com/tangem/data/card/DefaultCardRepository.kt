package com.tangem.data.card

import com.tangem.data.source.preferences.PreferencesDataSource
import com.tangem.domain.card.repository.CardRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class DefaultCardRepository(
    private val preferencesDataSource: PreferencesDataSource,
    private val dispatchers: CoroutineDispatcherProvider,
) : CardRepository {

    override suspend fun wasCardScanned(cardId: String): Boolean {
        return withContext(dispatchers.io) { preferencesDataSource.usedCardsPrefStorage.wasScanned(cardId) }
    }
}
