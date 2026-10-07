package com.tangem.data.walletmanager.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyExitCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyInitTokenCallData
import com.tangem.blockchainsdk.models.UpdateWalletManagerResult.Address
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Calendar

internal class TransactionDataToTxHistoryItemConverterTest {

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val coin = currencyFactory.createCoin(Blockchain.Polygon)
    private val usdc = currencyFactory.createToken(
        blockchain = Blockchain.Polygon,
        contractAddress = USDC_CONTRACT,
        symbol = "USDC",
        decimals = 6,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TransactionAmount {

        @Test
        fun `GIVEN coin transfer WHEN convert THEN amount excludes the fee it carries`() {
            // Arrange
            val transaction = transaction(
                amount = Amount(value = BigDecimal("1.5"), blockchain = Blockchain.Polygon),
                fee = coinFee(BigDecimal("0.01")),
            )

            // Act
            val actual = createConverter(coin).convert(transaction)

            // Assert
            assertThat(actual?.amount).isEqualTo(BigDecimal("1.5"))
            assertThat(actual?.fee?.value).isEqualTo(BigDecimal("0.01"))
        }

        @Test
        fun `GIVEN token transfer WHEN convert THEN amount excludes the fee it carries`() {
            // Arrange
            val transaction = transaction(
                amount = Amount(token = sdkUsdc, value = BigDecimal("25")),
                fee = coinFee(BigDecimal("0.01")),
            )

            // Act
            val actual = createConverter(usdc).convert(transaction)

            // Assert
            assertThat(actual?.amount).isEqualTo(BigDecimal("25"))
            assertThat(actual?.fee?.value).isEqualTo(BigDecimal("0.01"))
        }

        @Test
        fun `GIVEN no amount WHEN convert THEN null`() {
            // Arrange
            val transaction = transaction(
                amount = Amount(value = null, blockchain = Blockchain.Polygon),
                fee = coinFee(BigDecimal("0.01")),
            )

            // Act
            val actual = createConverter(coin).convert(transaction)

            // Assert
            assertThat(actual).isNull()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CoinFeeOnlyFallback {

        @ParameterizedTest
        @ProvideTestModels
        fun convert(model: FallbackModel) {
            // Act
            val actual = createConverter(model.currency).convert(model.transaction)

            // Assert
            assertThat(actual?.type).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            // A yield call moves no coin, so the coin's history shows nothing but the fee it paid — the same shape
            // the confirmed row takes, rather than a yield row that changes once the explorer reports it. The call
            // rides along so the fee row can name what it paid for.
            FallbackModel(
                transaction = yieldCall(EthereumYieldSupplyEnterCallData(USDC_CONTRACT)),
                currency = coin,
                expected = TxInfo.TransactionType.Operation(
                    name = "",
                    recognized = TxInfo.TransactionType.YieldSupply.Enter(USDC_CONTRACT),
                ),
            ),
            FallbackModel(
                transaction = yieldCall(EthereumYieldSupplyExitCallData(USDC_CONTRACT)),
                currency = coin,
                expected = TxInfo.TransactionType.Operation(
                    name = "",
                    recognized = TxInfo.TransactionType.YieldSupply.Exit(USDC_CONTRACT),
                ),
            ),
            FallbackModel(
                transaction = yieldCall(
                    EthereumYieldSupplyInitTokenCallData(
                        tokenContractAddress = USDC_CONTRACT,
                        maxNetworkFee = Amount(value = BigDecimal.ONE, blockchain = Blockchain.Polygon),
                    ),
                ),
                currency = coin,
                expected = TxInfo.TransactionType.Operation(
                    name = "",
                    recognized = TxInfo.TransactionType.YieldSupply.InitializeToken(USDC_CONTRACT),
                ),
            ),
            // The token's own history is where the operation belongs, so it keeps its type there
            FallbackModel(
                transaction = yieldCall(EthereumYieldSupplyEnterCallData(USDC_CONTRACT)),
                currency = usdc,
                expected = TxInfo.TransactionType.YieldSupply.Enter(USDC_CONTRACT),
            ),
            // Somebody else's call: its fee is the sender's, so it is no fee row of ours
            FallbackModel(
                transaction = yieldCall(EthereumYieldSupplyEnterCallData(USDC_CONTRACT), isOutgoing = false),
                currency = coin,
                expected = TxInfo.TransactionType.YieldSupply.Enter(USDC_CONTRACT),
            ),
            // A wallet-level snapshot belongs to no single currency, so nothing is reclassified
            FallbackModel(
                transaction = yieldCall(EthereumYieldSupplyEnterCallData(USDC_CONTRACT)),
                currency = null,
                expected = TxInfo.TransactionType.YieldSupply.Enter(USDC_CONTRACT),
            ),
            // A call that did move coin is not a fee-only row
            FallbackModel(
                transaction = yieldCall(EthereumYieldSupplyEnterCallData(USDC_CONTRACT), moved = BigDecimal.ONE),
                currency = coin,
                expected = TxInfo.TransactionType.YieldSupply.Enter(USDC_CONTRACT),
            ),
            // An ordinary transfer keeps its type whatever it moved
            FallbackModel(
                transaction = transaction(
                    amount = Amount(value = BigDecimal.ZERO, blockchain = Blockchain.Polygon),
                    fee = coinFee(BigDecimal("0.01")),
                ),
                currency = coin,
                expected = TxInfo.TransactionType.Transfer,
            ),
        )
    }

    internal data class FallbackModel(
        val transaction: TransactionData.Uncompiled,
        val currency: CryptoCurrency?,
        val expected: TxInfo.TransactionType,
    )

    private fun createConverter(currency: CryptoCurrency?) = TransactionDataToTxHistoryItemConverter(
        walletAddresses = setOf(Address(value = OWN_ADDRESS, type = Address.Type.Primary)),
        currency = currency,
    )

    private fun yieldCall(
        callData: SmartContractCallData,
        isOutgoing: Boolean = true,
        moved: BigDecimal = BigDecimal.ZERO,
    ): TransactionData.Uncompiled = transaction(
        amount = Amount(value = moved, blockchain = Blockchain.Polygon),
        fee = coinFee(BigDecimal("0.01")),
        sourceAddress = if (isOutgoing) OWN_ADDRESS else FOREIGN_ADDRESS,
        extras = EthereumTransactionExtras(callData = callData),
    )

    private fun transaction(
        amount: Amount,
        fee: Fee?,
        sourceAddress: String = OWN_ADDRESS,
        extras: TransactionExtras? = null,
    ): TransactionData.Uncompiled = TransactionData.Uncompiled(
        amount = amount,
        fee = fee,
        sourceAddress = sourceAddress,
        destinationAddress = RECIPIENT_ADDRESS,
        hash = TX_HASH,
        extras = extras,
    ).apply { date = Calendar.getInstance() }

    private fun coinFee(value: BigDecimal): Fee =
        Fee.Common(amount = Amount(value = value, blockchain = Blockchain.Polygon))

    private val sdkUsdc = Token(name = "USD Coin", symbol = "USDC", contractAddress = USDC_CONTRACT, decimals = 6)

    private companion object {
        const val USDC_CONTRACT = "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359"
        const val RECIPIENT_ADDRESS = "0x2222222222222222222222222222222222222222"
        const val OWN_ADDRESS = "0x3333333333333333333333333333333333333333"
        const val FOREIGN_ADDRESS = "0x4444444444444444444444444444444444444444"
        const val TX_HASH = "0xhash"
    }
}