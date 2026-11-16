package com.tangem.features.earn.lending.impl.provider.aave

import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionStatus
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus
import com.tangem.features.earn.lending.impl.provider.LendingDepositGate
import com.tangem.features.earn.lending.impl.provider.LendingProviderState
import java.math.BigDecimal

/** What is read from the chain on each network update; the reserve status comes separately from its supplier */
internal data class AaveOnChainRead(
    val version: YieldModuleVersionStatus,
    val withdrawLiquidity: BigDecimal?,
)

/**
 * @property status           the reserve in the user's Yield Module; null until it is read
 * @property withdrawLiquidity null when the pool liquidity could not be read:
 * a withdrawal is capped by the position only
 */
internal data class AaveModuleRead(
    val status: YieldModuleStatus?,
    val version: YieldModuleVersionStatus,
    val withdrawLiquidity: BigDecimal?,
) : LendingProviderState {

    override val planKey: String
        get() = toState().toString()

    override val isUpgradeRequired: Boolean
        get() = latestImplementation != null

    /**
     * The module version and the reserve status are read independently:
     * an outdated module still needs its reserve set up
     */
    fun toState(): AaveModuleState = when (version) {
        is YieldModuleVersionStatus.Indeterminate -> AaveModuleState.ReadFailed
        is YieldModuleVersionStatus.NotDeployed -> AaveModuleState.NotDeployed
        is YieldModuleVersionStatus.UpgradeUnavailable -> AaveModuleState.UpgradeUnavailable
        is YieldModuleVersionStatus.UpgradeAvailable -> activationState(isUpgradeRequired = true)
        is YieldModuleVersionStatus.UpToDate -> activationState(isUpgradeRequired = false)
    }

    private fun activationState(isUpgradeRequired: Boolean): AaveModuleState = when {
        status == null -> AaveModuleState.StatusUnknown(isUpgradeRequired)
        !status.isInitialized -> AaveModuleState.NeedsInit(isUpgradeRequired)
        !status.isActive -> AaveModuleState.NeedsReactivation(isUpgradeRequired)
        else -> AaveModuleState.Ready(isUpgradeRequired)
    }
}

internal val AaveModuleRead.latestImplementation: String?
    get() = (version as? YieldModuleVersionStatus.UpgradeAvailable)?.latestImplementation

internal sealed interface AaveModuleState {

    data object ReadFailed : AaveModuleState

    data object NotDeployed : AaveModuleState

    data object UpgradeUnavailable : AaveModuleState

    /** The reserve status has not arrived yet */
    data class StatusUnknown(val isUpgradeRequired: Boolean) : AaveModuleState

    data class NeedsInit(val isUpgradeRequired: Boolean) : AaveModuleState

    data class NeedsReactivation(val isUpgradeRequired: Boolean) : AaveModuleState

    data class Ready(val isUpgradeRequired: Boolean) : AaveModuleState

    fun toDepositGate(): LendingDepositGate = when (this) {
        ReadFailed -> LendingDepositGate.ReadFailed
        UpgradeUnavailable -> LendingDepositGate.Unavailable
        is StatusUnknown -> LendingDepositGate.Loading
        NotDeployed, is NeedsInit, is NeedsReactivation, is Ready -> LendingDepositGate.Open
    }
}