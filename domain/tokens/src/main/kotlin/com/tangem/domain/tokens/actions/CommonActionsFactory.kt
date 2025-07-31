package com.tangem.domain.tokens.actions

import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState.ActionState
import com.tangem.domain.transaction.models.AssetRequirementsCondition
import com.tangem.domain.walletmanager.WalletManagersFacade
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Factory class for creating common token actions
 *
 * @param walletManagersFacade the facade for managing wallet operations
 * @param rampStateManager     the manager for handling ramp state operations
 *
[REDACTED_AUTHOR]
 */
internal class CommonActionsFactory(
    walletManagersFacade: WalletManagersFacade,
    private val rampStateManager: RampStateManager,
) : BaseActionsFactory(walletManagersFacade, rampStateManager) {

    /**
     * Creates a set of token actions based on the provided parameters
     *
     * @param userWallet            the user's cold wallet
     * @param cryptoCurrencyStatus  the status of the cryptocurrency
     * @param stakingAvailability   the staking availability for the cryptocurrency
     * @param shouldShowSwapStories a flag indicating whether to show swap stories
     */
    suspend fun create(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        stakingAvailability: StakingAvailability,
        shouldShowSwapStories: Boolean,
    ): Set<ActionState> = coroutineScope {
        val isAddressAvailable = isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)

        val requirementsDeferred = if (isAddressAvailable) {
            async {
                getAssetRequirements(userWalletId = userWallet.walletId, currency = cryptoCurrencyStatus.currency)
            }
        } else {
            null
        }

        val onrampUnavailabilityReasonDeferred = async {
            getOnrampUnavailabilityReason(userWallet = userWallet, currency = cryptoCurrencyStatus.currency)
        }

        val sendUnavailabilityReasonDeferred = async {
            getSendUnavailabilityReason(userWalletId = userWallet.walletId, cryptoCurrencyStatus = cryptoCurrencyStatus)
        }

        val swapUnavailabilityReason = if (!cryptoCurrencyStatus.currency.isCustom &&
            cryptoCurrencyStatus.value !is CryptoCurrencyStatus.NoQuote
        ) {
            async {
                getSwapUnavailabilityReason(
                    userWalletId = userWallet.walletId,
                    currency = cryptoCurrencyStatus.currency,
                    requirementsDeferred = requirementsDeferred,
                )
            }
        } else {
            null
        }

        actionAvailabilityBuilder {
            // region Analytics
            if (cryptoCurrencyStatus.currency.id.rawCurrencyId != null) {
                ActionState.Analytics(unavailabilityReason = ScenarioUnavailabilityReason.None).active()
            }
            // endregion

            // region Copy
            addCopyAction(isAddressAvailable = isAddressAvailable)
            // endregion

            // region Receive
            addReceiveAction(isAddressAvailable = isAddressAvailable, requirementsDeferred = requirementsDeferred)
            // endregion

            // region Stake
            createStakingAction(currency = cryptoCurrencyStatus.currency, stakingAvailability = stakingAvailability)
                .addByReason()
            // endregion

            val sendUnavailabilityReason = sendUnavailabilityReasonDeferred.await()

            // region Send
            ActionState.Send(unavailabilityReason = sendUnavailabilityReason).addByReason()
            // endregion

            // region Swap
            createSwapAction(
                userWallet = userWallet,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                swapUnavailableReasonDeferred = swapUnavailabilityReason,
                shouldShowSwapStories = shouldShowSwapStories,
            ).addByReason()
            // endregion

            // region Buy
            addBuyAction(reason = onrampUnavailabilityReasonDeferred.await())
            // endregion

            // region Sell
            val sellUnavailabilityReason = getSellUnavailabilityReason(
                userWalletId = userWallet.walletId,
                status = cryptoCurrencyStatus,
                sendUnavailabilityReason = sendUnavailabilityReason,
            )

            ActionState.Sell(unavailabilityReason = sellUnavailabilityReason).addByReason()
            // endregion

            // region HideToken
            addHideTokenAction()
            // endregion
        }
    }

    private suspend fun createSwapAction(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        swapUnavailableReasonDeferred: Deferred<ScenarioUnavailabilityReason>?,
        shouldShowSwapStories: Boolean,
    ): ActionState {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val isMultiCurrency = userWallet is UserWallet.Cold && userWallet.isMultiCurrency ||
            userWallet is UserWallet.Hot

        if (!isMultiCurrency) {
            return ActionState.Swap(
                unavailabilityReason = ScenarioUnavailabilityReason.SingleWallet,
                showBadge = false,
            )
        }

        return when {
            cryptoCurrency.isCustom -> {
                ActionState.Swap(
                    unavailabilityReason = ScenarioUnavailabilityReason.CustomToken(cryptoCurrency.name),
                    showBadge = false,
                )
            }
            cryptoCurrencyStatus.value is CryptoCurrencyStatus.NoQuote -> {
                ActionState.Swap(
                    unavailabilityReason = ScenarioUnavailabilityReason.TokenNoQuotes(cryptoCurrency.name),
                    showBadge = false,
                )
            }
            else -> {
                val reason = swapUnavailableReasonDeferred!!.await()

                return ActionState.Swap(
                    unavailabilityReason = reason,
                    showBadge = reason == ScenarioUnavailabilityReason.None && shouldShowSwapStories,
                )
            }
        }
    }

    private suspend fun getSwapUnavailabilityReason(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        requirementsDeferred: Deferred<AssetRequirementsCondition?>?,
    ): ScenarioUnavailabilityReason {
        val swapUnavailabilityReason = rampStateManager
            .availableForSwap(userWalletId = userWalletId, cryptoCurrency = currency)
        val shouldCheckAssetRequirements =
            swapUnavailabilityReason == ScenarioUnavailabilityReason.None && requirementsDeferred != null
        return if (shouldCheckAssetRequirements) {
            getReceiveScenario(requirementsDeferred.await())
        } else {
            swapUnavailabilityReason
        }
    }
}