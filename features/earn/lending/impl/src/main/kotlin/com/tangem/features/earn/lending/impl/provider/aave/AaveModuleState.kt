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
 * @property withdrawLiquidity null when the pool liquidity could not be read: a withdrawal is capped by the position only
 */
internal data class AaveModuleRead(
    val status: YieldModuleStatus?,
    val version: YieldModuleVersionStatus,
    val withdrawLiquidity: BigDecimal?,
) : LendingProviderState {

    override val planKey: String
        get() = toState().toString()

    /** The first matching row wins: the module version goes before the reserve status */
    fun toState(): AaveModuleState = when (version) {
        is YieldModuleVersionStatus.Indeterminate -> AaveModuleState.ReadFailed
        is YieldModuleVersionStatus.NotDeployed -> AaveModuleState.NotDeployed
        is YieldModuleVersionStatus.UpgradeUnavailable -> AaveModuleState.UpgradeUnavailable
        is YieldModuleVersionStatus.UpgradeAvailable -> AaveModuleState.UpgradeAvailable
        is YieldModuleVersionStatus.UpToDate -> when {
            status == null -> AaveModuleState.StatusUnknown
            !status.isInitialized || !status.isActive -> AaveModuleState.NeedsActivation
            else -> AaveModuleState.Ready
        }
    }
}

internal sealed interface AaveModuleState {

    data object ReadFailed : AaveModuleState

    data object NotDeployed : AaveModuleState

    data object UpgradeAvailable : AaveModuleState

    data object UpgradeUnavailable : AaveModuleState

    /** The module is up to date, but the reserve status has not arrived yet */
    data object StatusUnknown : AaveModuleState

    data object NeedsActivation : AaveModuleState

    data object Ready : AaveModuleState

    fun toDepositGate(): LendingDepositGate = when (this) {
        ReadFailed -> LendingDepositGate.ReadFailed
        UpgradeUnavailable -> LendingDepositGate.Unavailable
        StatusUnknown -> LendingDepositGate.Loading
        NotDeployed, UpgradeAvailable, NeedsActivation, Ready -> LendingDepositGate.Open
    }
}