package com.tangem.domain.tokens.actions

import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState.ActionState
import com.tangem.domain.transaction.models.AssetRequirementsCondition
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Base factory class for creating token actions.
 *
 * This class provides utility methods to determine the availability of actions and to create specific token actions
 * based on the provided conditions.
 *
 * @param walletManagersFacade the facade for managing wallet operations
 * @param rampStateManager     the manager for handling ramp state operations
 *
[REDACTED_AUTHOR]
 */
internal open class BaseActionsFactory(
    private val walletManagersFacade: WalletManagersFacade,
    private val rampStateManager: RampStateManager,
) {

    /** Checks if the provided network address [networkAddress] is available */
    protected fun isAddressAvailable(networkAddress: NetworkAddress?): Boolean {
        return networkAddress != null && networkAddress.defaultAddress.value.isNotEmpty()
    }

    /**
     * Retrieves the asset requirements for a specific user wallet and cryptocurrency.
     *
     * @param userWalletId The ID of the user wallet.
     * @param currency The cryptocurrency to check.
     * @return The asset requirements condition, or `null` if the operation times out.
     */
    protected suspend fun getAssetRequirements(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
    ): AssetRequirementsCondition? {
        return withTimeoutOrNull(timeMillis = 1000L) {
            walletManagersFacade.getAssetRequirements(userWalletId = userWalletId, currency = currency)
        }
    }

    /**
     * Determines the unavailability reason for the BUY action
     *
     * @param userWallet the user's cold wallet
     * @param currency   the cryptocurrency to check
     */
    protected suspend fun getOnrampUnavailabilityReason(
        userWallet: UserWallet.Cold,
        currency: CryptoCurrency,
    ): ScenarioUnavailabilityReason {
        return rampStateManager.availableForBuy(
            userWalletId = userWallet.walletId,
            scanResponse = userWallet.scanResponse,
            cryptoCurrency = currency,
        )
    }

    /**
     * Determines the unavailability reason for the SEND action
     *
     * @param userWalletId         the ID of the user's wallet
     * @param cryptoCurrencyStatus the status of the cryptocurrency
     */
    protected suspend fun getSendUnavailabilityReason(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): ScenarioUnavailabilityReason {
        return rampStateManager.getSendUnavailabilityReason(
            userWalletId = userWalletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
        )
    }

    /**
     * Determines the unavailability reason for the SELL action
     *
     * @param userWalletId             the ID of the user's wallet
     * @param status                   the status of the cryptocurrency
     * @param sendUnavailabilityReason the reason for unavailability of the send action
     */
    protected suspend fun getSellUnavailabilityReason(
        userWalletId: UserWalletId,
        status: CryptoCurrencyStatus,
        sendUnavailabilityReason: ScenarioUnavailabilityReason,
    ): ScenarioUnavailabilityReason {
        return rampStateManager.availableForSell(
            userWalletId = userWalletId,
            status = status,
            sendUnavailabilityReason = sendUnavailabilityReason,
        ).fold(
            ifLeft = { it },
            ifRight = { ScenarioUnavailabilityReason.None },
        )
    }

    /** Adds a "Copy Address" action to the builder if the address is available [isAddressAvailable] */
    protected fun ActionAvailabilityBuilder.addCopyAction(isAddressAvailable: Boolean) {
        if (isAddressAvailable) {
            ActionState.CopyAddress(unavailabilityReason = ScenarioUnavailabilityReason.None).active()
        }
    }

    /**
     * Adds a "Receive" action to the builder based on the address availability and asset requirements
     *
     * @param isAddressAvailable   indicates whether the address is available
     * @param requirementsDeferred a deferred object containing the asset requirements condition
     */
    protected suspend fun ActionAvailabilityBuilder.addReceiveAction(
        isAddressAvailable: Boolean,
        requirementsDeferred: Deferred<AssetRequirementsCondition?>?,
    ) {
        if (isAddressAvailable && requirementsDeferred != null) {
            val scenario = getReceiveScenario(requirements = requirementsDeferred.await())
            val action = ActionState.Receive(scenario)

            if (scenario == ScenarioUnavailabilityReason.None) {
                action.active()
            } else {
                action.disabled()
            }
        }
    }

    /** Adds a "Buy" action to the builder based on the unavailability [reason] */
    protected fun ActionAvailabilityBuilder.addBuyAction(reason: ScenarioUnavailabilityReason) {
        val action = ActionState.Buy(unavailabilityReason = reason)

        if (reason == ScenarioUnavailabilityReason.None) {
            action.active()
        } else {
            action.disabled()
        }
    }

    /** Adds a "Hide Token" action to the builder */
    protected fun ActionAvailabilityBuilder.addHideTokenAction() {
        ActionState.HideToken(unavailabilityReason = ScenarioUnavailabilityReason.None).active()
    }

    /**
     * Creates a staking action based on the staking availability
     *
     * @param currency            the cryptocurrency for staking
     * @param stakingAvailability the staking availability status
     */
    protected fun createStakingAction(
        currency: CryptoCurrency,
        stakingAvailability: StakingAvailability,
    ): ActionState.Stake {
        return if (stakingAvailability is StakingAvailability.Available) {
            ActionState.Stake(
                unavailabilityReason = ScenarioUnavailabilityReason.None,
                yield = stakingAvailability.yield,
            )
        } else {
            ActionState.Stake(
                unavailabilityReason = ScenarioUnavailabilityReason.StakingUnavailable(currency.name),
                yield = null,
            )
        }
    }

    private fun getReceiveScenario(requirements: AssetRequirementsCondition?): ScenarioUnavailabilityReason {
        return when (requirements) {
            AssetRequirementsCondition.PaidTransaction,
            is AssetRequirementsCondition.PaidTransactionWithFee,
            -> ScenarioUnavailabilityReason.UnassociatedAsset
            is AssetRequirementsCondition.IncompleteTransaction,
            null,
            -> ScenarioUnavailabilityReason.None
            is AssetRequirementsCondition.RequiredTrustline -> ScenarioUnavailabilityReason.TrustlineRequired
        }
    }
}