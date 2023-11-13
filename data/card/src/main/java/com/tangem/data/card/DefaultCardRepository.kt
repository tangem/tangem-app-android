package com.tangem.data.card

import com.tangem.datasource.local.card.UsedCardInfo
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObject
import com.tangem.domain.card.repository.CardRepository
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DefaultCardRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : CardRepository {

    override fun wasCardScanned(cardId: String): Flow<Boolean> {
        return appPreferencesStore.getObject<List<UsedCardInfo>>(key = PreferencesKeys.USED_CARDS_INFO_KEY)
            .map { savedCards ->
                savedCards?.any { it.cardId == cardId } ?: false
            }
    }

    override suspend fun setCardWasScanned(cardId: String) {
        appPreferencesStore.editData { mutablePreferences ->
            val usedCards: List<UsedCardInfo>? = mutablePreferences.getObject(
                key = PreferencesKeys.USED_CARDS_INFO_KEY,
            )

            val updatedUsedCards = usedCards?.updateCard(cardId)
                ?: listOf(UsedCardInfo(cardId = cardId, isScanned = true))

            mutablePreferences.setObject(
                key = PreferencesKeys.USED_CARDS_INFO_KEY,
                value = updatedUsedCards,
            )
        }
    }

    private fun List<UsedCardInfo>.updateCard(cardId: String): List<UsedCardInfo> {
        val card = find { it.cardId == cardId } ?: UsedCardInfo(cardId = cardId, isScanned = true)
        return addOrReplace(item = card.copy(isScanned = true), predicate = { it.cardId == cardId })
    }
}
