package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.swap.models.SwapPermissionUM
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for [SwapModel.getApprovalParams].
 *
 * Builds [com.tangem.features.approval.api.GiveApprovalComponent.Params] only when:
 * - `uiState.permissionUM` is [SwapPermissionUM.PermissionRequired]
 * - `fromSwapCurrencyStatus` is non-null
 * - `feePaidCryptoCurrency` is non-null
 */
internal class SwapModelGetApprovalParamsTest : SwapModelTestBase() {

    @BeforeEach
    fun setUp() {
        setUpBase()
    }

    @Test
    fun `GIVEN permissionUM is not PermissionRequired THEN returns null`() {
        val model = createModel()
        // default uiState has permissionUM == Empty
        model.dataState = model.dataState.copy(
            fromSwapCurrencyStatus = swapCurrencyStatus(),
            feePaidCryptoCurrency = mockk(relaxed = true),
        )

        assertThat(model.getApprovalParams()).isNull()
    }

    @Test
    fun `GIVEN null fromSwapCurrencyStatus THEN returns null`() {
        val model = createModel()
        model.uiState = model.uiState.copy(
            permissionUM = SwapPermissionUM.PermissionRequired(isResetApproval = false, spenderAddress = "0xSpender"),
        )
        model.dataState = model.dataState.copy(
            fromSwapCurrencyStatus = null,
            feePaidCryptoCurrency = mockk(relaxed = true),
        )

        assertThat(model.getApprovalParams()).isNull()
    }

    @Test
    fun `GIVEN null feePaidCryptoCurrency THEN returns null`() {
        val model = createModel()
        model.uiState = model.uiState.copy(
            permissionUM = SwapPermissionUM.PermissionRequired(isResetApproval = false, spenderAddress = "0xSpender"),
        )
        model.dataState = model.dataState.copy(
            fromSwapCurrencyStatus = swapCurrencyStatus(),
            feePaidCryptoCurrency = null,
        )

        assertThat(model.getApprovalParams()).isNull()
    }

    @Test
    fun `GIVEN give-approval prerequisites met THEN builds params with give footer`() {
        val model = createModel()
        val coldWallet = mockk<UserWallet.Cold>(relaxed = true)
        val currency = mockk<CryptoCurrency>(relaxed = true) { every { symbol } returns "DAI" }
        val status = mockk<CryptoCurrencyStatus>(relaxed = true)
        val from = swapCurrencyStatus(wallet = coldWallet, status = status, currency = currency)
        val feeStatus: CryptoCurrencyStatus = mockk(relaxed = true)
        val provider = swapProvider(name = "1inch")

        model.uiState = model.uiState.copy(
            permissionUM = SwapPermissionUM.PermissionRequired(isResetApproval = false, spenderAddress = "0xSpender"),
        )
        model.dataState = model.dataState.copy(
            fromSwapCurrencyStatus = from,
            feePaidCryptoCurrency = feeStatus,
            selectedProvider = provider,
            amount = "12.5",
        )

        val params = model.getApprovalParams()

        assertThat(params).isNotNull()
        assertThat(params!!.userWalletId).isEqualTo(userWalletId)
        assertThat(params.cryptoCurrencyStatus).isEqualTo(status)
        assertThat(params.feeCryptoCurrencyStatus).isEqualTo(feeStatus)
        assertThat(params.amount).isEqualTo("12.5")
        assertThat(params.spenderAddress).isEqualTo("0xSpender")
        assertThat(params.isResetApproval).isFalse()
        // Cold wallet -> not hold-to-confirm
        assertThat(params.isHoldToConfirm).isFalse()
        assertThat(params.callback).isSameInstanceAs(model.approvalFullCallback)
    }

    @Test
    fun `GIVEN hot wallet THEN isHoldToConfirm is true`() {
        val model = createModel()
        val hotWallet = mockk<UserWallet.Hot>(relaxed = true)
        val from = swapCurrencyStatus(wallet = hotWallet)

        model.uiState = model.uiState.copy(
            permissionUM = SwapPermissionUM.PermissionRequired(isResetApproval = false, spenderAddress = "0xSpender"),
        )
        model.dataState = model.dataState.copy(
            fromSwapCurrencyStatus = from,
            feePaidCryptoCurrency = mockk(relaxed = true),
            amount = "1",
        )

        val params = model.getApprovalParams()

        assertThat(params).isNotNull()
        assertThat(params!!.isHoldToConfirm).isTrue()
    }

    @Test
    fun `GIVEN reset approval THEN isResetApproval is true`() {
        val model = createModel()
        val from = swapCurrencyStatus()

        model.uiState = model.uiState.copy(
            permissionUM = SwapPermissionUM.PermissionRequired(isResetApproval = true, spenderAddress = "0xSpender"),
        )
        model.dataState = model.dataState.copy(
            fromSwapCurrencyStatus = from,
            feePaidCryptoCurrency = mockk(relaxed = true),
            amount = "1",
        )

        val params = model.getApprovalParams()

        assertThat(params).isNotNull()
        assertThat(params!!.isResetApproval).isTrue()
    }
}