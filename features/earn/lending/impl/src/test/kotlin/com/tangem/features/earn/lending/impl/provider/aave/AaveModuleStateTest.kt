package com.tangem.features.earn.lending.impl.provider.aave

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionStatus
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.Network
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus
import com.tangem.features.earn.lending.impl.provider.LendingDepositGate
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AaveModuleStateTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN module read WHEN toState THEN state and deposit gate follow the table`(model: StateModel) {
        // Arrange
        val read = AaveModuleRead(status = model.status, version = model.version, withdrawLiquidity = null)

        // Act
        val state = read.toState()

        // Assert
        assertThat(state).isEqualTo(model.expectedState)
        assertThat(state.toDepositGate()).isEqualTo(model.expectedGate)
    }

    data class StateModel(
        val version: YieldModuleVersionStatus,
        val status: YieldModuleStatus?,
        val expectedState: AaveModuleState,
        val expectedGate: LendingDepositGate,
    )

    private fun provideTestModels() = listOf(
        StateModel(
            version = YieldModuleVersionStatus.Indeterminate(reason = "rpc"),
            status = readyStatus,
            expectedState = AaveModuleState.ReadFailed,
            expectedGate = LendingDepositGate.ReadFailed,
        ),
        StateModel(
            version = YieldModuleVersionStatus.NotDeployed,
            status = null,
            expectedState = AaveModuleState.NotDeployed,
            expectedGate = LendingDepositGate.Open,
        ),
        StateModel(
            version = YieldModuleVersionStatus.UpgradeUnavailable(currentImplementation = OLD_IMPLEMENTATION),
            status = readyStatus,
            expectedState = AaveModuleState.UpgradeUnavailable,
            expectedGate = LendingDepositGate.Unavailable,
        ),
        StateModel(
            version = YieldModuleVersionStatus.UpgradeUnavailable(currentImplementation = OLD_IMPLEMENTATION),
            status = null,
            expectedState = AaveModuleState.UpgradeUnavailable,
            expectedGate = LendingDepositGate.Unavailable,
        ),
        StateModel(
            version = YieldModuleVersionStatus.UpgradeAvailable(
                currentImplementation = OLD_IMPLEMENTATION,
                latestImplementation = LATEST_IMPLEMENTATION,
            ),
            status = readyStatus,
            expectedState = AaveModuleState.UpgradeAvailable,
            expectedGate = LendingDepositGate.Open,
        ),
        StateModel(
            version = YieldModuleVersionStatus.UpToDate,
            status = null,
            expectedState = AaveModuleState.StatusUnknown,
            expectedGate = LendingDepositGate.Loading,
        ),
        StateModel(
            version = YieldModuleVersionStatus.UpToDate,
            status = readyStatus.copy(isInitialized = false),
            expectedState = AaveModuleState.NeedsActivation,
            expectedGate = LendingDepositGate.Open,
        ),
        StateModel(
            version = YieldModuleVersionStatus.UpToDate,
            status = readyStatus.copy(isActive = false),
            expectedState = AaveModuleState.NeedsActivation,
            expectedGate = LendingDepositGate.Open,
        ),
        StateModel(
            version = YieldModuleVersionStatus.UpToDate,
            status = readyStatus,
            expectedState = AaveModuleState.Ready,
            expectedGate = LendingDepositGate.Open,
        ),
    )

    private companion object {
        const val OLD_IMPLEMENTATION = "0x0000000000000000000000000000000000000001"
        const val LATEST_IMPLEMENTATION = "0x0000000000000000000000000000000000000002"

        val readyStatus = YieldModuleStatus(
            networkId = Network.ID(value = "ethereum", derivationPath = Network.DerivationPath.None),
            tokenAddress = "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2",
            isActive = true,
            isInitialized = true,
            isAllowedToSpend = true,
            effectiveProtocolBalance = null,
            source = StatusSource.ACTUAL,
        )
    }
}