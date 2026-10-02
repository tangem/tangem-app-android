package com.tangem.domain.polymarket.support

import com.tangem.domain.feedback.models.SupportChatInfoProvider
import com.tangem.domain.feedback.models.SupportChatTopic
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.flow.PredictionAccountStatusSupplier
import com.tangem.domain.polymarket.flow.PredictionPositionsValueSupplier
import com.tangem.domain.polymarket.model.PredictionPositionsValue
import com.tangem.domain.polymarket.model.PredictionTotalBalance
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

class PredictionSupportChatInfoProvider(
    private val predictionAccountStatusSupplier: PredictionAccountStatusSupplier,
    private val predictionPositionsValueSupplier: PredictionPositionsValueSupplier,
    private val derivePolymarketAddressesUseCase: DerivePolymarketAddressesUseCase,
) : SupportChatInfoProvider {

    override val topic: SupportChatTopic = SupportChatTopic.PREDICTIONS

    override suspend fun getInfo(userWalletId: UserWalletId): String {
        val status = withTimeoutOrNull(READ_TIMEOUT_MILLIS) {
            predictionAccountStatusSupplier(userWalletId = userWalletId).firstOrNull()
        }
        val positions = withTimeoutOrNull(READ_TIMEOUT_MILLIS) {
            predictionPositionsValueSupplier(userWalletId = userWalletId).firstOrNull()
        }
        val depositWalletAddress = derivePolymarketAddressesUseCase.stored(userWalletId = userWalletId)
            ?.depositWalletAddress
            ?: NOT_DERIVED

        return buildList {
            add("Polymarket account state" to describeState(status))
            add("Polymarket deposit wallet address" to depositWalletAddress)
            add("Polymarket available balance" to describeAvailableBalance(status))
            add("Polymarket total balance" to describeTotalBalance(status, positions))
            if (status is PredictionAccountStatusValue.Active) {
                add("Polymarket bridge deposit address" to (status.depositAddress ?: NOT_LOADED))
            }
        }.joinToString(separator = "\n") { (key, value) -> "$key: $value" }
    }

    private fun describeState(status: PredictionAccountStatusValue?): String {
        val state = when (status) {
            null -> return NOT_LOADED
            is PredictionAccountStatusValue.Loading -> "loading"
            is PredictionAccountStatusValue.NotOnboarded -> "not onboarded"
            is PredictionAccountStatusValue.Onboarding -> "onboarding (${status.stage})"
            is PredictionAccountStatusValue.Onboarded -> "onboarded, no credentials on this device"
            is PredictionAccountStatusValue.Active -> "active"
            is PredictionAccountStatusValue.Error.OnboardingFailed -> "error (onboarding failed)"
            is PredictionAccountStatusValue.Error.Unavailable -> "error (unavailable)"
        }

        return "$state, source ${status.source}"
    }

    private fun describeAvailableBalance(status: PredictionAccountStatusValue?): String {
        val active = status as? PredictionAccountStatusValue.Active ?: return NOT_LOADED

        return active.balance.toPlainString()
    }

    private fun describeTotalBalance(
        status: PredictionAccountStatusValue?,
        positions: PredictionPositionsValue?,
    ): String {
        val active = status as? PredictionAccountStatusValue.Active ?: return NOT_LOADED
        val total = positions?.let { PredictionTotalBalance.of(status = active, positions = it) }
            ?: return POSITIONS_UNKNOWN

        return "${total.amount.toPlainString()}, source ${total.source}"
    }

    private companion object {
        const val READ_TIMEOUT_MILLIS = 2_000L
        const val NOT_LOADED = "[not loaded]"
        const val NOT_DERIVED = "[not derived on this device]"
        const val POSITIONS_UNKNOWN = "[positions value unknown]"
    }
}