package com.tangem.data.walletconnect.network.ethereum

import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.domain.blockaid.models.transaction.SimulationResult
import com.domain.blockaid.models.transaction.ValidationResult
import com.domain.blockaid.models.transaction.simultation.ApproveInfo
import com.domain.blockaid.models.transaction.simultation.SimulationData
import com.domain.blockaid.models.transaction.simultation.TokenInfo
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.walletconnect.model.WcApprovedAmount
import com.tangem.domain.walletconnect.model.WcEthTransactionParams
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class WcEthTxHelperTest {

    private val helper = WcEthTxHelper(
        singleAccountListSupplier = mockk(),
        ethSpecificFee = mockk(),
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateTransactionData {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN evm network WHEN createTransactionData THEN wei value is converted with 18 decimals`(
            blockchain: Blockchain,
        ) {
            // Arrange
            val network = MockCryptoCurrencyFactory().createCoin(blockchain).network

            // Act
            val actual = helper.createTransactionData(dAppFee = null, network = network, txParams = txParams())

            // Assert
            assertThat(actual!!.amount.value).isEquivalentAccordingToCompareTo(BigDecimal.ONE)
            assertThat(actual.amount.decimals).isEqualTo(18)
        }

        private fun provideTestModels() = listOf(Blockchain.Ethereum, Blockchain.Arc)

        private fun txParams() = WcEthTransactionParams(
            from = "0xSender",
            to = "0xRecipient",
            data = null,
            gas = null,
            gasPrice = null,
            value = "0x0de0b6b3a7640000",
            nonce = null,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetApprovedAmount {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN approve calldata and scan result WHEN getApprovedAmount THEN amount is taken from calldata`(
            model: GetApprovedAmountModel,
        ) {
            // Act
            val actual = helper.getApprovedAmount(txData = model.txData, result = model.result)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            GetApprovedAmountModel(
                txData = approveCallData(amountHex = MAX_UINT256_HEX),
                result = approveScanResult(exposureAmount = BigDecimal.ZERO),
                expected = approvedAmount(value = null),
            ),
            GetApprovedAmountModel(
                txData = approveCallData(amountHex = MAX_UINT256_HEX),
                result = approveScanResult(exposureAmount = BigDecimal("500")),
                expected = approvedAmount(value = null),
            ),
            GetApprovedAmountModel(
                txData = approveCallData(amountHex = TEN_USDC_HEX),
                result = approveScanResult(exposureAmount = BigDecimal("100")),
                expected = approvedAmount(value = BigDecimal.TEN.stripTrailingZeros()),
            ),
            GetApprovedAmountModel(
                txData = approveCallData(amountHex = TEN_USDC_HEX).uppercase().replace("0X", "0x"),
                result = approveScanResult(exposureAmount = BigDecimal.ZERO),
                expected = approvedAmount(value = BigDecimal.TEN.stripTrailingZeros()),
            ),
            GetApprovedAmountModel(
                txData = TRANSFER_METHOD_ID + SPENDER_WORD + TEN_USDC_HEX,
                result = approveScanResult(exposureAmount = BigDecimal.ZERO),
                expected = null,
            ),
            GetApprovedAmountModel(
                txData = null,
                result = approveScanResult(exposureAmount = BigDecimal.ZERO),
                expected = null,
            ),
            GetApprovedAmountModel(
                txData = approveCallData(amountHex = MAX_UINT256_HEX),
                result = CheckTransactionResult(
                    validation = ValidationResult.SAFE,
                    simulation = SimulationResult.FailedToSimulate,
                ),
                expected = null,
            ),
        )

        private fun approveCallData(amountHex: String) = APPROVE_METHOD_ID + SPENDER_WORD + amountHex

        private fun approveScanResult(exposureAmount: BigDecimal) = CheckTransactionResult(
            validation = ValidationResult.SAFE,
            simulation = SimulationResult.Success(
                SimulationData.Approve(
                    items = listOf(
                        ApproveInfo.Amount(approvedAmount = exposureAmount, isUnlimited = false, tokenInfo = usdc),
                    ),
                ),
            ),
        )

        private fun approvedAmount(value: BigDecimal?) = WcApprovedAmount(
            amount = Amount(currencySymbol = usdc.symbol, value = value, decimals = usdc.decimals),
            chainId = usdc.chainId,
            logoUrl = usdc.logoUrl,
        )
    }

    data class GetApprovedAmountModel(
        val txData: String?,
        val result: CheckTransactionResult,
        val expected: WcApprovedAmount?,
    )

    private companion object {
        const val APPROVE_METHOD_ID = "0x095ea7b3"
        const val TRANSFER_METHOD_ID = "0xa9059cbb"
        const val SPENDER_WORD = "000000000000000000000000a2d0379bf274a2a5f92b300e5cba236f98a32201"
        const val MAX_UINT256_HEX = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
        const val TEN_USDC_HEX = "0000000000000000000000000000000000000000000000000000000000989680"

        val usdc = TokenInfo(chainId = 1, logoUrl = "logo", symbol = "USDC", decimals = 6)
    }
}