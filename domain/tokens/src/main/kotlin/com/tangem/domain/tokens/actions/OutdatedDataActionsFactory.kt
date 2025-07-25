package com.tangem.domain.tokens.actions

import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.StatusSource
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState.ActionState
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.models.wallet.UserWallet
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Factory for creating a set of token action states when data is outdated
 *
 * @param walletManagersFacade the facade for managing wallet operations
 * @param rampStateManager     the manager for handling ramp state operations
 *
[REDACTED_AUTHOR]
 */
internal class OutdatedDataActionsFactory(
    walletManagersFacade: WalletManagersFacade,
    rampStateManager: RampStateManager,
) : BaseActionsFactory(walletManagersFacade, rampStateManager) {

    /**
     * Creates a set of token actions based on the provided parameters
     *
     * @param userWallet            the user's cold wallet
     * @param cryptoCurrencyStatus  the status of the cryptocurrency
     * @param stakingAvailability   the staking availability for the cryptocurrency
     */
    suspend fun create(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        stakingAvailability: StakingAvailability,
    ): Set<ActionState> = coroutineScope {
        val sources = cryptoCurrencyStatus.value.sources

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

        val sendUnavailabilityReasonDeferred = if (sources.networkSource == StatusSource.ACTUAL) {
            async {
                getSendUnavailabilityReason(
                    userWalletId = userWallet.walletId,
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                )
            }
        } else {
            null
        }

        actionAvailabilityBuilder {
            // region Copy
            addCopyAction(isAddressAvailable = isAddressAvailable)
            // endregion

            // region Receive
            addReceiveAction(isAddressAvailable = isAddressAvailable, requirementsDeferred = requirementsDeferred)
            // endregion

            // region Swap
            createSwapAction(sources = sources).addByReason()
            // endregion

            // region Buy
            addBuyAction(reason = onrampUnavailabilityReasonDeferred.await())
            // endregion

            // region Stake
            if (sources.networkSource.isActual()) {
                val stakingAction = createStakingAction(
                    currency = cryptoCurrencyStatus.currency,
                    stakingAvailability = stakingAvailability,
                )

                stakingAction.addByReason()
            } else {
                val stakingAction = ActionState.Stake(
                    unavailabilityReason = ScenarioUnavailabilityReason.UsedOutdatedData,
                    yield = null,
                )

                stakingAction.disabled()
            }
            // endregion

            val sendUnavailabilityReason = getSendUnavailabilityReason(
                sources = sources,
                reasonDeferred = sendUnavailabilityReasonDeferred,
            )

            // region Send
            ActionState.Send(sendUnavailabilityReason).addByReason()
            // endregion

            // region Sell
            if (sendUnavailabilityReason == ScenarioUnavailabilityReason.None) {
                val sellUnavailabilityReason = getSellUnavailabilityReason(
                    userWalletId = userWallet.walletId,
                    status = cryptoCurrencyStatus,
                    sendUnavailabilityReason = sendUnavailabilityReason,
                )

                ActionState.Sell(sellUnavailabilityReason).addByReason()
            } else {
                ActionState.Sell(sendUnavailabilityReason).disabled()
            }
            // endregion

            // region HideToken
            addHideTokenAction()
            // endregion
        }
    }

    private fun createSwapAction(sources: CryptoCurrencyStatus.Sources): ActionState {
        val isSwapAvailable = with(sources) { quoteSource.isActual() && networkSource.isActual() }

        return ActionState.Swap(
            unavailabilityReason = when {
                isSwapAvailable -> ScenarioUnavailabilityReason.None
                sources.networkSource == StatusSource.ONLY_CACHE -> ScenarioUnavailabilityReason.UsedOutdatedData
                else -> ScenarioUnavailabilityReason.DataLoading // CACHE source always when loading
            },
            showBadge = false,
        )
    }

    private suspend fun getSendUnavailabilityReason(
        sources: CryptoCurrencyStatus.Sources,
        reasonDeferred: Deferred<ScenarioUnavailabilityReason>?,
    ): ScenarioUnavailabilityReason {
        if (sources.networkSource != StatusSource.ACTUAL || reasonDeferred == null) {
            return ScenarioUnavailabilityReason.UsedOutdatedData
        }

        return reasonDeferred.await()
    }
}