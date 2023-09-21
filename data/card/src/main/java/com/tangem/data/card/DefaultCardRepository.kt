package com.tangem.data.card

import com.tangem.datasource.local.card.UsedCardInfo
import com.tangem.datasource.local.card.UsedCardsStore
import com.tangem.domain.card.repository.CardRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class DefaultCardRepository(
    private val usedCardsStore: UsedCardsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : CardRepository {

    override fun wasCardScanned(cardId: String): Flow<Boolean> {
        return channelFlow {
            launch(dispatchers.io) {
                usedCardsStore.get()
                    .collect { savedCards ->
                        send(element = savedCards.any { it.cardId == cardId })
                    }
            }

            launch(dispatchers.io) {
                if (usedCardsStore.getSyncOrNull() == null) {
                    send(element = false)
                }
            }
        }
    }

    override suspend fun setCardWasScanned(cardId: String) {
        withContext(dispatchers.io) {
            usedCardsStore.store(
                item = usedCardsStore.getSyncOrNull()
                    ?.updateCard(cardId)
                    ?: listOf(UsedCardInfo(cardId = cardId, isScanned = true)),
            )
        }
    }

    private fun List<UsedCardInfo>.updateCard(cardId: String): List<UsedCardInfo> {
        return map { cardInfo ->
            if (cardInfo.cardId == cardId) {
                cardInfo.copy(isScanned = true)
            } else {
                cardInfo
            }
        }
    }
}
