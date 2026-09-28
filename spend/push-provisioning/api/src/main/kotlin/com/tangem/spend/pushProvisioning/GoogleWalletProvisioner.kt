package com.tangem.spend.pushProvisioning

import kotlinx.coroutines.flow.Flow

interface GoogleWalletProvisioner {

    val environment: Environment?

    val isAdditionInProgress: Boolean

    val walletChanges: Flow<Unit>

    suspend fun getCardStatus(lastFourDigits: String): CardStatus

    suspend fun addCard(card: Card): AddResult

    suspend fun resumeAddingCard(lastFourDigits: String, displayName: String): AddResult

    enum class Environment { TEST, PROD }

    enum class FailureReason {
        UNSUPPORTED,
        NO_ACTIVITY,
        ADDITION_IN_PROGRESS,
        ENVIRONMENT_MISMATCH,
        TOKEN_LOOKUP_FAILED,
        NO_TOKEN_AWAITING_VERIFICATION,
        NETWORK_ERROR,
        TOKENIZATION_DECLINED,
        GOOGLE_WALLET_ERROR,
        MEAWALLET_ERROR,
        UNEXPECTED_ERROR,
    }

    data class Card(
        val processorCardId: String,
        val cardSecret: String,
        val lastFourDigits: String,
        val displayName: String,
        val environment: Environment,
    ) {
        override fun toString(): String = "Card(processorCardId=$processorCardId, cardSecret=***, " +
            "lastFourDigits=$lastFourDigits, displayName=$displayName, environment=$environment)"
    }

    sealed interface CardStatus {
        data object NotAdded : CardStatus
        data object Incomplete : CardStatus
        data object Added : CardStatus
        data object Unsupported : CardStatus
        data object Unknown : CardStatus
    }

    sealed interface AddResult {
        data object Added : AddResult
        data object AlreadyAdded : AddResult
        data object Cancelled : AddResult
        data class Failed(
            val reason: FailureReason,
            val description: String,
            val sdkErrorCode: Int? = null,
            val walletErrorCode: Int? = null,
        ) : AddResult
    }
}