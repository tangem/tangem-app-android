package com.tangem.data.wallets.store

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable

internal typealias WalletCardsRegistry = Map<String, RegisteredWalletCard>

/**
 * The last known state of every card this device has seen, keyed by card id.
 *
 * A report carries all cards of the wallet known here, not only the card that triggered it, so the backend learns
 * about a card even when it is reported from another card of the wallet. The registry is never cleared — a forgotten
 * wallet that is added again still reports the cards seen before.
 */
internal class WalletCardsRegistryStore(
    private val dataStore: DataStore<WalletCardsRegistry>,
) {

    /**
     * Stores [cards] of [walletId] and returns every card of the wallet afterwards, ordered by card id.
     *
     * A card reported without a role or an error keeps the ones stored for it within the same wallet: a card scanned
     * outside onboarding knows neither, and dropping them would erase what onboarding reported.
     */
    suspend fun register(walletId: String, cards: List<PendingWalletCardsBackup.Card>): Registration {
        var isChanged = false
        val registry = dataStore.updateData { current ->
            val updated = cards.fold(current) { registry, card ->
                registry + (card.cardId to merge(stored = registry[card.cardId], walletId = walletId, incoming = card))
            }
            isChanged = updated != current
            updated
        }

        return Registration(isChanged = isChanged, walletCards = registry.cardsOf(walletId))
    }

    suspend fun getWalletCards(walletId: String): List<PendingWalletCardsBackup.Card> {
        return dataStore.data.first().cardsOf(walletId)
    }

    private fun merge(
        stored: RegisteredWalletCard?,
        walletId: String,
        incoming: PendingWalletCardsBackup.Card,
    ): RegisteredWalletCard {
        val storedCard = stored?.takeIf { it.walletId == walletId }?.card
        val hasError = incoming.errorCode != null || incoming.errorMessage != null
        val keepsStoredError = !hasError && storedCard?.backupStatus == incoming.backupStatus

        return RegisteredWalletCard(
            walletId = walletId,
            card = incoming.copy(
                role = incoming.role ?: storedCard?.role,
                errorCode = if (keepsStoredError) storedCard?.errorCode else incoming.errorCode,
                errorMessage = if (keepsStoredError) storedCard?.errorMessage else incoming.errorMessage,
            ),
        )
    }

    private fun WalletCardsRegistry.cardsOf(walletId: String): List<PendingWalletCardsBackup.Card> {
        return values.filter { it.walletId == walletId }.map { it.card }.sortedBy { it.cardId }
    }

    /**
     * @property isChanged   `false` when the registry already held exactly these cards
     * @property walletCards every card of the wallet after the update, ordered by card id
     */
    data class Registration(
        val isChanged: Boolean,
        val walletCards: List<PendingWalletCardsBackup.Card>,
    )
}

@Serializable
internal data class RegisteredWalletCard(
    val walletId: String,
    val card: PendingWalletCardsBackup.Card,
)