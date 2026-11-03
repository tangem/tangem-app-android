package com.tangem.feature.swap.model

import androidx.compose.runtime.MutableState
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.GaslessProtocolData
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.feature.swap.domain.fee.TransactionFeeResult
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.ui.FeeBucket
import com.tangem.feature.swap.domain.models.ui.IntegratedApprovalData
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.domain.models.ui.SwapFee
import com.tangem.feature.swap.domain.models.ui.SwapState
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

/** Integrated approve on the EVM gasless DEX fee path: native fee adds a separate approve, gasless batches it. */
internal class SwapModelEvmGaslessDexFeeTest : SwapModelTestBase() {

    private val swapDataModel: SwapDataModel = mockk(relaxed = true)
    private val fromStatus = swapCurrencyStatus(currency = ethToken())
    private val provider = swapProvider()
    private val integratedPermission = PermissionDataState.PermissionSettings(
        type = ApproveType.LIMITED,
        spenderAddress = SPENDER,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(swapInteractor, swapFeatureToggles, isEvmGaslessFeeSupportedForNetwork)
        setUpBase()
        every { swapFeatureToggles.isEvmGaslessDexSwapEnabled } returns true
        every { isEvmGaslessFeeSupportedForNetwork(any()) } returns true
    }

    @Test
    fun `GIVEN integrated approve and gasless fee WHEN loadFeeExtended THEN fee is returned as is without a separate approve`() =
        runTest {
            // Arrange
            val gaslessFee = TransactionFeeExtended(
                transactionFee = TransactionFee.Single(normal = SWAP_FEE),
                feeTokenId = mockk(relaxed = true),
                gaslessProtocolData = GaslessProtocolData.Evm,
            )
            stubLoadSwapFee(gaslessFee)
            val model = createLoadedModel(permissionState = integratedPermission)

            // Act
            val result = model.feeSelectorRepository.loadFeeExtended(selectedToken = null)

            // Assert
            assertThat(result).isEqualTo(gaslessFee.right())
            coVerify(exactly = 0) { swapInteractor.loadIntegratedApprovalData(any(), any(), any(), any()) }
        }

    @Test
    fun `GIVEN integrated approve and native fee WHEN loadFeeExtended THEN approve fee is added and the approve stored`() =
        runTest {
            // Arrange
            val nativeFee = nativeFeeExtended()
            val approvalData = IntegratedApprovalData(
                approvalTransaction = mockk<TransactionData.Uncompiled>(),
                approvalFee = TransactionFee.Single(normal = APPROVE_FEE),
                approveType = ApproveType.LIMITED,
            )
            stubLoadSwapFee(nativeFee)
            coEvery {
                swapInteractor.loadIntegratedApprovalData(
                    fromStatus = fromStatus,
                    spenderAddress = SPENDER,
                    approveType = ApproveType.LIMITED,
                    approvalAmount = BigDecimal("10"),
                )
            } returns approvalData.right()
            val model = createLoadedModel(permissionState = integratedPermission)

            // Act
            val result = model.feeSelectorRepository.loadFeeExtended(selectedToken = null)

            // Assert
            val combinedFee = TransactionFee.Single(
                normal = SWAP_FEE.copy(
                    amount = SWAP_FEE.amount.copy(value = BigDecimal("0.0015")),
                    gasLimit = BigInteger("150000"),
                ),
            )
            assertThat(result).isEqualTo(nativeFee.copy(transactionFee = combinedFee, nativeFee = combinedFee).right())
            val storedState = model.dataState.lastLoadedSwapStates[provider] as SwapState.QuotesLoadedState
            assertThat(storedState.integratedApprovalData).isEqualTo(approvalData)
        }

    @Test
    fun `GIVEN integrated approve data fails WHEN loadFeeExtended THEN returns its error`() = runTest {
        // Arrange
        stubLoadSwapFee(nativeFeeExtended())
        coEvery {
            swapInteractor.loadIntegratedApprovalData(any(), any(), any(), any())
        } returns GetFeeError.UnknownError.left()
        val model = createLoadedModel(permissionState = integratedPermission)

        // Act
        val result = model.feeSelectorRepository.loadFeeExtended(selectedToken = null)

        // Assert
        assertThat(result).isEqualTo(GetFeeError.UnknownError.left())
    }

    @Test
    fun `GIVEN no integrated approve and native fee WHEN loadFeeExtended THEN fee is returned as is`() = runTest {
        // Arrange
        val nativeFee = nativeFeeExtended()
        stubLoadSwapFee(nativeFee)
        val model = createLoadedModel(permissionState = PermissionDataState.Empty)

        // Act
        val result = model.feeSelectorRepository.loadFeeExtended(selectedToken = null)

        // Assert
        assertThat(result).isEqualTo(nativeFee.right())
        coVerify(exactly = 0) { swapInteractor.loadIntegratedApprovalData(any(), any(), any(), any()) }
    }

    private fun nativeFeeExtended() = TransactionFeeExtended(
        transactionFee = TransactionFee.Single(normal = SWAP_FEE),
        feeTokenId = mockk(relaxed = true),
        nativeFee = TransactionFee.Single(normal = SWAP_FEE),
    )

    private fun stubLoadSwapFee(feeExtended: TransactionFeeExtended) {
        coEvery {
            swapInteractor.loadSwapFee(
                quotesLoadedState = any(),
                fromStatus = any(),
                toStatus = any(),
                amount = any(),
                swapData = any(),
                selectedFeeToken = any(),
                isGasless = true,
                txType = any(),
            )
        } returns SwapFee(
            fee = feeExtended.transactionFee.normal,
            transactionFeeResult = TransactionFeeResult.LoadedExtended(feeExtended),
            selectedFeeToken = mockk(relaxed = true),
            otherNativeFee = BigDecimal.ZERO,
            feeBucket = FeeBucket.MARKET,
        ).right()
    }

    private fun createLoadedModel(permissionState: PermissionDataState): SwapModel {
        val quoteState = quotesLoadedState(provider = provider, permissionState = permissionState).also {
            every { it.txType } returns null
            every { it.swapDataModel } returns swapDataModel
        }
        return createModel().apply {
            dataState = dataState.copy(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = swapCurrencyStatus(),
                selectedProvider = provider,
                lastLoadedSwapStates = mapOf(provider to quoteState),
            )
            setLastAmount("10")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun SwapModel.setLastAmount(amount: String) {
        val field = SwapModel::class.java.getDeclaredField("lastAmount").apply { isAccessible = true }
        (field.get(this) as MutableState<String>).value = amount
    }

    private fun ethToken(): CryptoCurrency.Token = mockk(relaxed = true) {
        every { network.rawId } returns Blockchain.Ethereum.toNetworkId()
        every { decimals } returns 6
    }

    private companion object {
        const val SPENDER = "0xRouter"

        val SWAP_FEE = Fee.Ethereum.Legacy(
            amount = Amount(currencySymbol = "ETH", value = BigDecimal("0.001"), decimals = 18),
            gasLimit = BigInteger("100000"),
            gasPrice = BigInteger("10000000000"),
        )
        val APPROVE_FEE = Fee.Ethereum.Legacy(
            amount = Amount(currencySymbol = "ETH", value = BigDecimal("0.0005"), decimals = 18),
            gasLimit = BigInteger("50000"),
            gasPrice = BigInteger("10000000000"),
        )
    }
}