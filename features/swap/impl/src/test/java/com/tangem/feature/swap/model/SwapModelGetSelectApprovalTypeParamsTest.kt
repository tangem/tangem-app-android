package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.domain.models.ui.SwapState
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for [SwapModel.getSelectApprovalTypeParams].
 *
 * Builds [com.tangem.features.approval.api.SelectApprovalTypeComponent.Params] from the current
 * `fromSwapCurrencyStatus` and the provider's loaded swap state. Returns `null` when prerequisites
 * are missing. `initialApproveType` is taken from [PermissionDataState.PermissionSettings.type] or
 * falls back to [ApproveType.LIMITED].
 */
internal class SwapModelGetSelectApprovalTypeParamsTest : SwapModelTestBase() {

    @BeforeEach
    fun setUp() {
        setUpBase()
    }

    @Test
    fun `GIVEN null fromSwapCurrencyStatus THEN returns null`() {
        val provider = swapProvider()
        val model = createModel()
        model.dataState = model.dataState.copy(
            fromSwapCurrencyStatus = null,
            lastLoadedSwapStates = mapOf(provider to quotesLoadedState(provider, permissionSettings())),
        )

        assertThat(model.getSelectApprovalTypeParams(provider)).isNull()
    }

    @Test
    fun `GIVEN provider state is not QuotesLoadedState THEN returns null`() {
        val provider = swapProvider()
        val notLoaded: SwapState.EmptyAmountState = mockk(relaxed = true)
        val model = createModel()
        model.dataState = model.dataState.copy(
            fromSwapCurrencyStatus = swapCurrencyStatus(),
            lastLoadedSwapStates = mapOf(provider to notLoaded),
        )

        assertThat(model.getSelectApprovalTypeParams(provider)).isNull()
    }

    @Test
    fun `GIVEN PermissionSettings present THEN uses its type as initialApproveType`() {
        val provider = swapProvider(name = "ParaSwap")
        val currency = mockk<CryptoCurrency>(relaxed = true) { every { symbol } returns "USDT" }
        val status = mockk<CryptoCurrencyStatus>(relaxed = true)
        val from = swapCurrencyStatus(status = status, currency = currency)
        val model = createModel()
        model.dataState = model.dataState.copy(
            fromSwapCurrencyStatus = from,
            lastLoadedSwapStates = mapOf(
                provider to quotesLoadedState(provider, permissionSettings(type = ApproveType.UNLIMITED)),
            ),
        )

        val params = model.getSelectApprovalTypeParams(provider)

        assertThat(params).isNotNull()
        assertThat(params!!.userWalletId).isEqualTo(userWalletId)
        assertThat(params.cryptoCurrencyStatus).isEqualTo(status)
        assertThat(params.initialApproveType).isEqualTo(ApproveType.UNLIMITED)
        assertThat(params.callback).isSameInstanceAs(model.approvalSelectorCallback)
    }
}