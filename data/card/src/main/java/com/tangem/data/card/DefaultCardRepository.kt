package com.tangem.data.card

import androidx.datastore.preferences.core.MutablePreferences
import com.tangem.datasource.local.card.UsedCardInfo
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectList
import com.tangem.datasource.local.preferences.utils.getObjectListSync
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.card.repository.CardRepository
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DefaultCardRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : CardRepository {

    override fun wasCardScanned(cardId: String): Flow<Boolean> {
        return appPreferencesStore.getObjectList<UsedCardInfo>(key = PreferencesKeys.USED_CARDS_INFO_KEY)
            .map { savedCards ->
                savedCards?.any { it.cardId == cardId } ?: false
            }
    }

    override suspend fun setCardWasScanned(cardId: String) {
        appPreferencesStore.editUsedCards(cardId) { it.copy(isScanned = true) }
    }

    override suspend fun startCardActivation(cardId: String) {
        appPreferencesStore.editUsedCards(cardId) { it.copy(isActivationStarted = true) }
    }

    override suspend fun finishCardActivation(cardId: String) {
        appPreferencesStore.editUsedCards(cardId) {
            it.copy(isActivationStarted = true, isActivationFinished = true)
        }
    }

    override suspend fun finishCardsActivation(cardIds: List<String>) {
        appPreferencesStore.editData { mutablePreferences ->
            val usedCards = mutablePreferences.getUsedCards()

            val newCards = cardIds.mapNotNull { newCardId ->
                if (usedCards.none { it.cardId == newCardId }) {
                    createDefaultUsedCardInfo(cardId = newCardId)
                } else {
                    null
                }
            }

            val updatedUsedCards = (usedCards + newCards).map { card ->
                if (cardIds.contains(card.cardId)) {
                    card.copy(isActivationStarted = true, isActivationFinished = true)
                } else {
                    card
                }
            }

            mutablePreferences.setObjectList(key = PreferencesKeys.USED_CARDS_INFO_KEY, value = updatedUsedCards)
        }
    }

    override suspend fun isActivationStarted(cardId: String): Boolean {
        return getUsedCardSync(cardId)?.isActivationStarted ?: false
    }

    override suspend fun isActivationFinished(cardId: String): Boolean {
        return getUsedCardSync(cardId)?.isActivationFinished ?: false
    }

    override suspend fun isActivationInProgress(cardId: String): Boolean {
        val card = getUsedCardSync(cardId) ?: return false

        return card.isActivationStarted && !card.isActivationFinished
    }

    override suspend fun isTangemTOSAccepted(): Boolean {
        return appPreferencesStore.getSyncOrDefault(key = PreferencesKeys.IS_TANGEM_TOS_ACCEPTED_KEY, default = false)
    }

    override suspend fun acceptTangemTOS() {
        return appPreferencesStore.store(key = PreferencesKeys.IS_TANGEM_TOS_ACCEPTED_KEY, true)
    }

    private suspend fun AppPreferencesStore.editUsedCards(cardId: String, update: (UsedCardInfo) -> UsedCardInfo) {
        editData { mutablePreferences ->
            val usedCards = mutablePreferences.getUsedCards()

            val updatedUsedCards = usedCards.updateCard(cardId = cardId, update = update)

            mutablePreferences.setObjectList(key = PreferencesKeys.USED_CARDS_INFO_KEY, value = updatedUsedCards)
        }
    }

    private fun MutablePreferences.getUsedCards(): List<UsedCardInfo> {
        return with(appPreferencesStore) {
            getObjectListOrDefault(key = PreferencesKeys.USED_CARDS_INFO_KEY, default = mutableListOf())
        }
    }

    private fun List<UsedCardInfo>.updateCard(
        cardId: String,
        update: (UsedCardInfo) -> UsedCardInfo,
    ): List<UsedCardInfo> {
        val card = find { it.cardId == cardId } ?: createDefaultUsedCardInfo(cardId = cardId)
        return addOrReplace(item = update(card), predicate = { it.cardId == cardId })
    }

    private suspend fun getUsedCardSync(cardId: String): UsedCardInfo? {
        return appPreferencesStore.getObjectListSync<UsedCardInfo>(PreferencesKeys.USED_CARDS_INFO_KEY)
            .firstOrNull { it.cardId == cardId }
    }

    private fun createDefaultUsedCardInfo(cardId: String) = UsedCardInfo(cardId = cardId)
}