package com.tangem.domain.tokens.actions

import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState.ActionState
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Factory for creating a set of unreachable token action states
 *
 * @param walletManagersFacade the facade for managing wallet operations
 * @param rampStateManager     the manager for handling ramp state operations
 *
[REDACTED_AUTHOR]
 */
internal class UnreachableActionsFactory(
    walletManagersFacade: WalletManagersFacade,
    rampStateManager: RampStateManager,
) : BaseActionsFactory(walletManagersFacade, rampStateManager) {

    suspend fun create(userWallet: UserWallet.Cold, cryptoCurrencyStatus: CryptoCurrencyStatus): Set<ActionState> =
        coroutineScope {
            val isAddressAvailable = isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)

            // region Deferred
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
            // endregion

            actionAvailabilityBuilder {
                // region Copy
                addCopyAction(isAddressAvailable = isAddressAvailable)
                // endregion

                // region Buy
                addBuyAction(reason = onrampUnavailabilityReasonDeferred.await())
                // endregion

                // region Receive
                addReceiveAction(isAddressAvailable = isAddressAvailable, requirementsDeferred = requirementsDeferred)
                // endregion

                // region Send, Swap, Sell, Stake
                listOf(
                    ActionState.Send(unavailabilityReason = ScenarioUnavailabilityReason.Unreachable),
                    ActionState.Swap(
                        unavailabilityReason = ScenarioUnavailabilityReason.Unreachable,
                        showBadge = false,
                    ),
                    ActionState.Sell(unavailabilityReason = ScenarioUnavailabilityReason.Unreachable),
                    ActionState.Stake(unavailabilityReason = ScenarioUnavailabilityReason.Unreachable, yield = null),
                ).disabled()
                // endregion

                // region HideToken
                addHideTokenAction()
                // endregion
            }
        }
}