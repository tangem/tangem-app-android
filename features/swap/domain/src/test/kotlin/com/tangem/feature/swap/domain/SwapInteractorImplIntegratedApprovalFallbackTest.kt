package com.tangem.feature.swap.domain

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.feature.swap.domain.fee.DexFeeResult
import com.tangem.feature.swap.domain.fee.TransactionFeeResult
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.ui.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Tests for the integrated-approval fallback context in [SwapInteractorImpl]
 * ([SwapInteractorImpl.integratedApprovalFallback] / private `hasIntegratedApprovalFallenBack`).
 *
 * The fallback context is keyed by `(userWalletId, currency.id, spenderAddress)`. Once
 * [SwapInteractorImpl.integratedApprovalFallback] records a context, a subsequent
 * [SwapInteractorImpl.loadSwapFee] with a matching `PermissionSettings.spenderAddress` must
 * downgrade the calculator's `permissionState` to [PermissionDataState.Empty] (legacy
 * separate-approval flow). A DIFFERENT spender must NOT be downgraded.
 *
 * Observed through the public `loadSwapFee` path: the `DexSwapFeeCalculator` is mocked and its
 * `permissionState` argument is captured.
 *
 * NOTE: the fallback key uses `currency.id`, which is a relaxed mock with reference equality.
 * Each `buildSwapCurrencyStatus(...)` call produces a fresh `currency.id`, so the SAME
 * `fromStatus` instance must be reused across the `integratedApprovalFallback` call and the
 * `loadSwapFee` call for the key to match.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplIntegratedApprovalFallbackTest : SwapInteractorImplTestBase() {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()
    private val nativeFeeTokenStatus = mockk<CryptoCurrencyStatus>(relaxed = true)

    @BeforeEach
    fun setup() {
        coEvery {
            getFeePaidCryptoCurrencyStatusSyncUseCase.invoke(any(), any())
        } returns nativeFeeTokenStatus.right()
    }

    @Test
    fun `GIVEN fallback recorded for matching spender THEN loadSwapFee downgrades permissionState to Empty`() =
        runTest {
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = false)
            val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
            val permissionSlot = slot<PermissionDataState>()
            stubCalculatorCapturing(permissionSlot)

            sut.integratedApprovalFallback(fromSwapCurrencyStatus = fromStatus, spenderAddress = SPENDER)

            sut.loadSwapFee(
                quotesLoadedState = buildPermissionSettingsState(SPENDER),
                fromStatus = fromStatus,
                toStatus = toStatus,
                amount = SwapAmount(BigDecimal.ONE, 18),
                swapData = buildSwapData(),
                selectedFeeToken = null,
                isGasless = false
            )

            assertThat(permissionSlot.captured).isEqualTo(PermissionDataState.Empty)
        }

    @Test
    fun `GIVEN no fallback recorded THEN loadSwapFee passes the original PermissionSettings`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = false)
        val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val permissionSlot = slot<PermissionDataState>()
        stubCalculatorCapturing(permissionSlot)

        sut.loadSwapFee(
            quotesLoadedState = buildPermissionSettingsState(SPENDER),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = buildSwapData(),
            selectedFeeToken = null,
            isGasless = false
        )

        assertThat(permissionSlot.captured).isInstanceOf(PermissionDataState.PermissionSettings::class.java)
        assertThat((permissionSlot.captured as PermissionDataState.PermissionSettings).spenderAddress)
            .isEqualTo(SPENDER)
    }

    @Test
    fun `GIVEN fallback recorded for a different spender THEN loadSwapFee is NOT downgraded`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = false)
        val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val permissionSlot = slot<PermissionDataState>()
        stubCalculatorCapturing(permissionSlot)

        // Record the fallback for a DIFFERENT spender than the one in the loaded state.
        sut.integratedApprovalFallback(fromSwapCurrencyStatus = fromStatus, spenderAddress = OTHER_SPENDER)

        sut.loadSwapFee(
            quotesLoadedState = buildPermissionSettingsState(SPENDER),
            fromStatus = fromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = buildSwapData(),
            selectedFeeToken = null,
            isGasless = false
        )

        assertThat(permissionSlot.captured).isInstanceOf(PermissionDataState.PermissionSettings::class.java)
        assertThat((permissionSlot.captured as PermissionDataState.PermissionSettings).spenderAddress)
            .isEqualTo(SPENDER)
    }

    @Test
    fun `GIVEN fallback recorded for a different from-currency THEN loadSwapFee is NOT downgraded`() = runTest {
        // Fallback recorded for one currency instance, fee loaded for a different instance with
        // the same spender → keys differ on currency.id → no downgrade.
        val fallbackFromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = false)
        val feeFromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = false)
        val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val permissionSlot = slot<PermissionDataState>()
        stubCalculatorCapturing(permissionSlot)

        sut.integratedApprovalFallback(fromSwapCurrencyStatus = fallbackFromStatus, spenderAddress = SPENDER)

        sut.loadSwapFee(
            quotesLoadedState = buildPermissionSettingsState(SPENDER),
            fromStatus = feeFromStatus,
            toStatus = toStatus,
            amount = SwapAmount(BigDecimal.ONE, 18),
            swapData = buildSwapData(),
            selectedFeeToken = null,
            isGasless = false
        )

        assertThat(permissionSlot.captured).isInstanceOf(PermissionDataState.PermissionSettings::class.java)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun stubCalculatorCapturing(permissionSlot: io.mockk.CapturingSlot<PermissionDataState>) {
        coEvery {
            dexSwapFeeCalculator.calculate(
                fromSwapCurrencyStatus = any(),
                transaction = any(),
                selectedToken = any(),
                permissionState = capture(permissionSlot),
            )
        } returns DexFeeResult(
            transactionFee = TransactionFeeResult.Loaded(
                TransactionFee.Single(normal = mockk<Fee.Common>(relaxed = true)),
            ),
            otherNativeFee = BigDecimal.ZERO,
            gas = BigInteger.valueOf(21_000L),
        ).right()
    }

    private fun buildPermissionSettingsState(spender: String): SwapState.QuotesLoadedState {
        val from = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = false)
        val to = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        return SwapState.QuotesLoadedState(
            fromTokenInfo = TokenSwapInfo(
                tokenAmount = SwapAmount(BigDecimal.ONE, 18),
                swapCurrencyStatus = from,
                amountFiat = BigDecimal.ZERO,
            ),
            toTokenInfo = TokenSwapInfo(
                tokenAmount = SwapAmount(BigDecimal("0.5"), 18),
                swapCurrencyStatus = to,
                amountFiat = BigDecimal.ZERO,
            ),
            priceImpact = PriceImpact.Empty,
            preparedSwapConfigState = PreparedSwapConfigState(
                balanceStatus = SwapBalanceStatus.Pending,
                hasOutgoingTransaction = false,
            ),
            permissionState = PermissionDataState.PermissionSettings(
                type = ApproveType.UNLIMITED,
                spenderAddress = spender,
            ),
            swapDataModel = null,
            currencyCheck = null,
            validationResult = null,
            minAdaValue = null,
            swapProvider = buildSwapProvider(ExchangeProviderType.DEX),
        )
    }

    private fun buildSwapData(): SwapDataModel = SwapDataModel(
        toTokenAmount = SwapAmount(BigDecimal("0.5"), 18),
        transaction = ExpressTransactionModel.DEX(
            fromAmount = SwapAmount(BigDecimal.ONE, 18),
            toAmount = SwapAmount(BigDecimal("0.5"), 18),
            txValue = "1000000000000000",
            txId = "tx-id",
            txTo = "0xTo",
            txExtraId = null,
            txFrom = "0xFrom",
            txData = "dGVzdA==",
            otherNativeFeeWei = null,
            gas = BigInteger.valueOf(21_000L),
            allowanceContract = null,
        ),
    )

    private companion object {
        const val SPENDER = "0xSpender"
        const val OTHER_SPENDER = "0xOtherSpender"
    }
}