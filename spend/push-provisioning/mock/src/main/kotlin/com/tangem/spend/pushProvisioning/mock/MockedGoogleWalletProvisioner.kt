package com.tangem.spend.pushProvisioning.mock

import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.AddResult
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.Card
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.CardStatus
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.Environment
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.FailureReason
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MockedGoogleWalletProvisioner @Inject constructor() : GoogleWalletProvisioner {

    private val addedCards = mutableSetOf<String>()

    override val environment: Environment = Environment.TEST

    override val walletChanges: Flow<Unit> = flow { awaitCancellation() }

    override suspend fun getCardStatus(lastFourDigits: String): CardStatus {
        if (lastFourDigits in addedCards) return CardStatus.Added

        return when (System.getProperty(UITEST_CARD_STATUS_KEY)) {
            STATUS_UNSUPPORTED -> CardStatus.Unsupported
            STATUS_UNKNOWN -> CardStatus.Unknown
            STATUS_INCOMPLETE -> CardStatus.Incomplete
            else -> CardStatus.NotAdded
        }
    }

    override suspend fun addCard(card: Card): AddResult = add(card.lastFourDigits)

    override suspend fun resumeAddingCard(lastFourDigits: String, displayName: String): AddResult = add(lastFourDigits)

    private fun add(lastFourDigits: String): AddResult {
        val result = when (System.getProperty(UITEST_ADD_RESULT_KEY)) {
            RESULT_ALREADY_ADDED -> AddResult.AlreadyAdded
            RESULT_CANCELLED -> AddResult.Cancelled
            RESULT_NETWORK_ERROR -> AddResult.Failed(
                reason = FailureReason.NETWORK_ERROR,
                description = "Mocked network error",
            )
            RESULT_FAILED -> AddResult.Failed(reason = FailureReason.MEAWALLET_ERROR, description = "Mocked failure")
            else -> AddResult.Added
        }

        if (result == AddResult.Added || result == AddResult.AlreadyAdded) {
            addedCards += lastFourDigits
        }
        return result
    }

    private companion object {
        const val UITEST_ADD_RESULT_KEY = "uitest.tangempay.google_wallet_add_result"
        const val UITEST_CARD_STATUS_KEY = "uitest.tangempay.google_wallet_card_status"
        const val RESULT_ALREADY_ADDED = "already_added"
        const val RESULT_CANCELLED = "cancelled"
        const val RESULT_NETWORK_ERROR = "network_error"
        const val RESULT_FAILED = "failed"
        const val STATUS_UNSUPPORTED = "unsupported"
        const val STATUS_UNKNOWN = "unknown"
        const val STATUS_INCOMPLETE = "incomplete"
    }
}