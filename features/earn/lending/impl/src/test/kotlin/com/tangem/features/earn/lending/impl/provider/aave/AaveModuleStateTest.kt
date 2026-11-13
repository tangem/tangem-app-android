package com.tangem.features.earn.lending.impl.provider.aave

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionStatus
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus
import com.tangem.features.earn.lending.impl.provider.LendingDepositGate
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.yield.MockYieldSupply
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToDepositGate {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN module state WHEN toDepositGate THEN gate follows the table`(model: GateModel) {
            // Act
            val gate = model.state.toDepositGate()

            // Assert
            assertThat(gate).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            GateModel(AaveModuleState.ReadFailed, LendingDepositGate.ReadFailed),
            GateModel(AaveModuleState.NotDeployed, LendingDepositGate.Open),
            GateModel(AaveModuleState.UpgradeUnavailable, LendingDepositGate.Unavailable),
            GateModel(AaveModuleState.StatusUnknown(isUpgradeRequired = true), LendingDepositGate.Loading),
            GateModel(AaveModuleState.StatusUnknown(isUpgradeRequired = false), LendingDepositGate.Loading),
            GateModel(AaveModuleState.NeedsInit(isUpgradeRequired = true), LendingDepositGate.Open),
            GateModel(AaveModuleState.NeedsInit(isUpgradeRequired = false), LendingDepositGate.Open),
            GateModel(AaveModuleState.NeedsReactivation(isUpgradeRequired = true), LendingDepositGate.Open),
            GateModel(AaveModuleState.NeedsReactivation(isUpgradeRequired = false), LendingDepositGate.Open),
            GateModel(AaveModuleState.Ready(isUpgradeRequired = true), LendingDepositGate.Open),
            GateModel(AaveModuleState.Ready(isUpgradeRequired = false), LendingDepositGate.Open),
        )
    }

    data class GateModel(val state: AaveModuleState, val expected: LendingDepositGate)

    @Test
    fun `GIVEN reads differing only in isUpgradeRequired WHEN planKey THEN keys differ`() {
        // Arrange
        val outdated = AaveModuleRead(readyStatus, upgradeAvailable, withdrawLiquidity = null)
        val current = AaveModuleRead(readyStatus, YieldModuleVersionStatus.UpToDate, withdrawLiquidity = null)

        // Act
        val keys = listOf(outdated.planKey, current.planKey)

        // Assert
        assertThat(keys.distinct()).hasSize(2)
    }

    @Test
    fun `GIVEN upgrade available WHEN latestImplementation THEN returns it`() {
        // Arrange
        val read = AaveModuleRead(readyStatus, upgradeAvailable, withdrawLiquidity = null)

        // Assert
        assertThat(read.latestImplementation).isEqualTo(LATEST_IMPLEMENTATION)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsUpgradeRequired {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN module version WHEN isUpgradeRequired THEN only an available upgrade requires it`(
            model: UpgradeRequiredModel,
        ) {
            // Arrange
            val read = AaveModuleRead(readyStatus, model.version, withdrawLiquidity = null)

            // Act
            val actual = read.isUpgradeRequired

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            UpgradeRequiredModel(upgradeAvailable, expected = true),
            UpgradeRequiredModel(YieldModuleVersionStatus.UpToDate, expected = false),
            UpgradeRequiredModel(YieldModuleVersionStatus.NotDeployed, expected = false),
            UpgradeRequiredModel(YieldModuleVersionStatus.UpgradeUnavailable(OLD_IMPLEMENTATION), expected = false),
            UpgradeRequiredModel(YieldModuleVersionStatus.Indeterminate(reason = "rpc"), expected = false),
        )
    }

    data class UpgradeRequiredModel(val version: YieldModuleVersionStatus, val expected: Boolean) {
        override fun toString(): String = "${version::class.simpleName} -> $expected"
    }

    @Test
    fun `GIVEN module up to date WHEN latestImplementation THEN null`() {
        // Arrange
        val read = AaveModuleRead(readyStatus, YieldModuleVersionStatus.UpToDate, withdrawLiquidity = null)

        // Assert
        assertThat(read.latestImplementation).isNull()
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
            version = upgradeAvailable,
            status = null,
            expectedState = AaveModuleState.StatusUnknown(true),
            expectedGate = LendingDepositGate.Loading,
        ),
        StateModel(
            version = upgradeAvailable,
            status = readyStatus.copy(isInitialized = false),
            expectedState = AaveModuleState.NeedsInit(true),
            expectedGate = LendingDepositGate.Open,
        ),
        StateModel(
            version = upgradeAvailable,
            status = readyStatus.copy(isActive = false),
            expectedState = AaveModuleState.NeedsReactivation(true),
            expectedGate = LendingDepositGate.Open,
        ),
        StateModel(
            version = upgradeAvailable,
            status = readyStatus,
            expectedState = AaveModuleState.Ready(true),
            expectedGate = LendingDepositGate.Open,
        ),
        StateModel(
            version = YieldModuleVersionStatus.UpToDate,
            status = null,
            expectedState = AaveModuleState.StatusUnknown(false),
            expectedGate = LendingDepositGate.Loading,
        ),
        StateModel(
            version = YieldModuleVersionStatus.UpToDate,
            status = readyStatus.copy(isInitialized = false),
            expectedState = AaveModuleState.NeedsInit(false),
            expectedGate = LendingDepositGate.Open,
        ),
        StateModel(
            version = YieldModuleVersionStatus.UpToDate,
            status = readyStatus.copy(isActive = false),
            expectedState = AaveModuleState.NeedsReactivation(false),
            expectedGate = LendingDepositGate.Open,
        ),
        StateModel(
            version = YieldModuleVersionStatus.UpToDate,
            status = readyStatus,
            expectedState = AaveModuleState.Ready(false),
            expectedGate = LendingDepositGate.Open,
        ),
    )

    private companion object {
        const val OLD_IMPLEMENTATION = "0x0000000000000000000000000000000000000001"
        const val LATEST_IMPLEMENTATION = "0x0000000000000000000000000000000000000002"

        val upgradeAvailable = YieldModuleVersionStatus.UpgradeAvailable(
            currentImplementation = OLD_IMPLEMENTATION,
            latestImplementation = LATEST_IMPLEMENTATION,
        )

        val readyStatus = MockYieldSupply.moduleStatus(tokenAddress = "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2")
    }
}