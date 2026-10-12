package com.tangem.domain.tokens.actions

import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState.ActionState
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.yield.supply.models.YieldSupplyAvailability

/**
 * Actions of a token of a joint account. The set mirrors what a personal account would offer in the same context —
 * an action a personal account would hide (no staking for the currency) stays hidden, one it would disable keeps its
 * natural reason — but swap, sell, stake and yield mode are held back with [ScenarioUnavailabilityReason.JointAccount],
 * because the token belongs to a Safe contract and the wallet's own keys spend nothing from it. Send targets the
 * Safe contract and follows the regular availability rules, and so does buy: a purchase is a deposit to the Safe
 * address the wallet manager reports, the same as receiving. Receiving to the Safe address, copying that address,
 * analytics and hiding the token stay available.
 */
internal class JointAccountActionsFactory(
    walletManagersFacade: WalletManagersFacade,
    rampStateManager: RampStateManager,
) : BaseActionsFactory(walletManagersFacade, rampStateManager) {

    suspend fun create(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        stakingAvailability: StakingAvailability,
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

            createStakingAction(
                currency = cryptoCurrencyStatus.currency,
                stakingAvailability = stakingAvailability,
            )?.let { natural ->
                addHeldBack(natural = natural.unavailabilityReason) {
                    ActionState.Stake(unavailabilityReason = it, option = null)
                }
            }

            val naturalSend = getSendUnavailabilityReason(
                userWalletId = userWallet.walletId,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
            )
            ActionState.Send(unavailabilityReason = naturalSend).addByReason()

            val naturalSwap = createSwapAction(
                userWallet = userWallet,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                shouldShowSwapStories = false,
            )
            addHeldBack(natural = naturalSwap.unavailabilityReason) {
                ActionState.Swap(unavailabilityReason = it, shouldShowBadge = false)
            }

            val naturalBuy = getBuyUnavailabilityReason(
                userWallet = userWallet,
                currency = cryptoCurrencyStatus.currency,
            )
            ActionState.Buy(unavailabilityReason = naturalBuy).addByReason()

            val naturalSell = getSellUnavailabilityReason(
                userWallet = userWallet,
                status = cryptoCurrencyStatus,
                sendUnavailabilityReason = naturalSend,
            )
            addHeldBack(natural = naturalSell) { ActionState.Sell(unavailabilityReason = it) }

            ActionState.HideToken(unavailabilityReason = hideTokenUnavailabilityReason).addByReason()

            if (yieldSupplyAvailability is YieldSupplyAvailability.Available) {
                ActionState.YieldMode(
                    unavailabilityReason = ScenarioUnavailabilityReason.JointAccount,
                    apy = yieldSupplyAvailability.apy,
                ).active()
            }
        }
    }

    /**
     * Adds the action the way a personal account would render it: an action the personal account could perform
     * ([natural] is None) is held back with [ScenarioUnavailabilityReason.JointAccount] but keeps its place among
     * the active ones; an action the personal account would disable keeps its [natural] reason and its place in
     * the disabled tail.
     */
    private fun ActionAvailabilityBuilder.addHeldBack(
        natural: ScenarioUnavailabilityReason,
        action: (ScenarioUnavailabilityReason) -> ActionState,
    ) {
        if (natural == ScenarioUnavailabilityReason.None) {
            action(ScenarioUnavailabilityReason.JointAccount).active()
        } else {
            action(natural).disabled()
        }
    }
}