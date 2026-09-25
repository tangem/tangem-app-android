package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.IntegratedApprovalData
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.domain.models.ui.SwapState
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for [SwapModel.withCarriedIntegratedApprovals].
 *
 * The fee selector stores the approve transaction on the selected provider's
 * [SwapState.QuotesLoadedState]; a quote refresh rebuilds that state with `integratedApprovalData = null`
 * and the model has to carry the stored data over, or the swap would be sent without its approve.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SwapModelIntegratedApprovalCarryTest : SwapModelTestBase() {

    @BeforeEach
    fun setUp() {
        setUpBase()
    }

    @Test
    fun `GIVEN stored approval WHEN quotes refreshed for the same approval THEN data is carried over`() = runTest {
        // Arrange
        val provider = swapProvider()
        val stored = integratedApproval(ApproveType.LIMITED)
        val model = createModel()
        model.dataState = model.dataState.copy(
            lastLoadedSwapStates = mapOf(
                provider to quotesLoadedState(provider, permissionSettings(ApproveType.LIMITED), stored),
            ),
        )
        val fresh = mapOf(provider to quotesLoadedState(provider, permissionSettings(ApproveType.LIMITED)))

        // Act
        val carried = with(model) { fresh.withCarriedIntegratedApprovals() }

        // Assert
        assertThat(carried.approvalOf(provider)).isSameInstanceAs(stored)
    }

    @Test
    fun `GIVEN stored approval WHEN approve type changed THEN fresh state keeps null`() = runTest {
        // Arrange
        val provider = swapProvider()
        val model = createModel()
        model.dataState = model.dataState.copy(
            lastLoadedSwapStates = mapOf(
                provider to quotesLoadedState(
                    provider,
                    permissionSettings(ApproveType.LIMITED),
                    integratedApproval(ApproveType.LIMITED),
                ),
            ),
        )
        val fresh = mapOf(provider to quotesLoadedState(provider, permissionSettings(ApproveType.UNLIMITED)))

        // Act
        val carried = with(model) { fresh.withCarriedIntegratedApprovals() }

        // Assert
        assertThat(carried.approvalOf(provider)).isNull()
    }

    @Test
    fun `GIVEN stored approval WHEN spender changed THEN fresh state keeps null`() = runTest {
        // Arrange
        val provider = swapProvider()
        val model = createModel()
        model.dataState = model.dataState.copy(
            lastLoadedSwapStates = mapOf(
                provider to quotesLoadedState(
                    provider,
                    permissionSettings(ApproveType.LIMITED, spender = "0xOldSpender"),
                    integratedApproval(ApproveType.LIMITED),
                ),
            ),
        )
        val fresh = mapOf(provider to quotesLoadedState(provider, permissionSettings(ApproveType.LIMITED)))

        // Act
        val carried = with(model) { fresh.withCarriedIntegratedApprovals() }

        // Assert
        assertThat(carried.approvalOf(provider)).isNull()
    }

    @Test
    fun `GIVEN fresh state already has approval data WHEN carried THEN it is not replaced`() = runTest {
        // Arrange
        val provider = swapProvider()
        val model = createModel()
        model.dataState = model.dataState.copy(
            lastLoadedSwapStates = mapOf(
                provider to quotesLoadedState(
                    provider,
                    permissionSettings(ApproveType.LIMITED),
                    integratedApproval(ApproveType.LIMITED),
                ),
            ),
        )
        val own = integratedApproval(ApproveType.LIMITED)
        val fresh = mapOf(provider to quotesLoadedState(provider, permissionSettings(ApproveType.LIMITED), own))

        // Act
        val carried = with(model) { fresh.withCarriedIntegratedApprovals() }

        // Assert
        assertThat(carried.approvalOf(provider)).isSameInstanceAs(own)
    }

    @Test
    fun `GIVEN no previous state WHEN quotes loaded THEN the map is passed through`() = runTest {
        // Arrange
        val provider = swapProvider()
        val model = createModel()
        val fresh = mapOf(provider to quotesLoadedState(provider, permissionSettings(ApproveType.LIMITED)))

        // Act
        val carried = with(model) { fresh.withCarriedIntegratedApprovals() }

        // Assert
        assertThat(carried.approvalOf(provider)).isNull()
        assertThat(carried[provider]).isSameInstanceAs(fresh[provider])
    }

    private fun integratedApproval(type: ApproveType) = IntegratedApprovalData(
        approvalTransaction = mockk(relaxed = true),
        approvalFee = mockk(relaxed = true),
        approveType = type,
    )

    private fun Map<SwapProvider, SwapState>.approvalOf(provider: SwapProvider): IntegratedApprovalData? {
        return (this[provider] as SwapState.QuotesLoadedState).integratedApprovalData
    }
}
