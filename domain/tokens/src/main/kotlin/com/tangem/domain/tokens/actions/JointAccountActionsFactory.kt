package com.tangem.domain.tokens.actions

import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.yield.supply.models.YieldSupplyAvailability
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState.ActionState
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.walletmanager.WalletManagersFacade

/**
 * Actions of a token of a joint account. The token belongs to a Safe contract: the wallet's own keys spend nothing
 * from it, so everything that moves funds is disabled with [ScenarioUnavailabilityReason.JointAccount]; receiving to
 * the Safe address, copying it, analytics and hiding the token stay.
 */
internal class JointAccountActionsFactory(
    walletManagersFacade: WalletManagersFacade,
    rampStateManager: RampStateManager,
) : BaseActionsFactory(walletManagersFacade, rampStateManager) {

    suspend fun create(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        yieldSupplyAvailability: YieldSupplyAvailability,
    ): Set<ActionState> {
        val isAddressAvailable = isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)
        val hideTokenUnavailabilityReason = getTokenHideUnavailabilityReason(userWallet)

        return actionAvailabilityBuilder {
            if (cryptoCurrencyStatus.currency.id.rawCurrencyId != null) {
                ActionState.Analytics(unavailabilityReason = ScenarioUnavailabilityReason.None).active()
            }

            addCopyAction(isAddressAvailable = isAddressAvailable)

            if (isAddressAvailable) {
                ActionState.Receive(unavailabilityReason = ScenarioUnavailabilityReason.None).active()
            }

            ActionState.HideToken(unavailabilityReason = hideTokenUnavailabilityReason).addByReason()

            listOf(
                ActionState.Send(unavailabilityReason = ScenarioUnavailabilityReason.JointAccount),
                ActionState.Swap(
                    unavailabilityReason = ScenarioUnavailabilityReason.JointAccount,
                    shouldShowBadge = false,
                ),
                ActionState.Buy(unavailabilityReason = ScenarioUnavailabilityReason.JointAccount),
                ActionState.Sell(unavailabilityReason = ScenarioUnavailabilityReason.JointAccount),
                ActionState.Stake(unavailabilityReason = ScenarioUnavailabilityReason.JointAccount, option = null),
            ).disabled()

            if (yieldSupplyAvailability is YieldSupplyAvailability.Available) {
                ActionState.YieldMode(
                    unavailabilityReason = ScenarioUnavailabilityReason.JointAccount,
                    apy = yieldSupplyAvailability.apy,
                ).disabled()
            }
        }
    }
}