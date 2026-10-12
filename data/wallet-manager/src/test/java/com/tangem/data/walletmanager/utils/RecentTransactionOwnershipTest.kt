package com.tangem.data.walletmanager.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ApprovalERC20TokenCallData
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionExtras
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RecentTransactionOwnershipTest {

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val coin = currencyFactory.createCoin(Blockchain.Polygon)
    private val usdc = currencyFactory.createToken(
        blockchain = Blockchain.Polygon,
        contractAddress = USDC_CONTRACT,
        symbol = "USDC",
        decimals = 6,
    )
    private val usdt = currencyFactory.createToken(
        blockchain = Blockchain.Polygon,
        contractAddress = USDT_CONTRACT,
        symbol = "USDT",
        decimals = 6,
    )

    @ParameterizedTest
    @ProvideTestModels
    fun belongsToHistoryOf(model: OwnershipModel) {
        // Act
        val actual = model.transaction.belongsToHistoryOf(model.currency)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        // An approval is denominated in the approved token but belongs to the coin's history, where the explorer
        // reports it — the token's history carries Transfer events only.
        OwnershipModel(transaction = approval(), currency = coin, expected = true),
        OwnershipModel(transaction = approval(), currency = usdc, expected = false),
        OwnershipModel(transaction = approval(), currency = usdt, expected = false),
        // Plain transfers keep being matched by their own amount.
        OwnershipModel(transaction = coinTransfer(), currency = coin, expected = true),
        OwnershipModel(transaction = coinTransfer(), currency = usdc, expected = false),
        OwnershipModel(transaction = tokenTransfer(), currency = usdc, expected = true),
        OwnershipModel(transaction = tokenTransfer(), currency = usdt, expected = false),
        OwnershipModel(transaction = tokenTransfer(), currency = coin, expected = false),
    )

    internal data class OwnershipModel(
        val transaction: TransactionData.Uncompiled,
        val currency: CryptoCurrency,
        val expected: Boolean,
    )

    /** The approval the swap flow builds: a zero amount of the approved token, sent to that token's contract. */
    private fun approval(): TransactionData.Uncompiled = transaction(
        amount = Amount(token = sdkUsdc, value = BigDecimal.ZERO),
        destinationAddress = USDC_CONTRACT,
        extras = EthereumTransactionExtras(
            callData = ApprovalERC20TokenCallData(spenderAddress = SPENDER_ADDRESS, amount = null),
        ),
    )

    private fun coinTransfer(): TransactionData.Uncompiled = transaction(
        amount = Amount(value = BigDecimal.ONE, blockchain = Blockchain.Polygon),
        destinationAddress = RECIPIENT_ADDRESS,
    )

    private fun tokenTransfer(): TransactionData.Uncompiled = transaction(
        amount = Amount(token = sdkUsdc, value = BigDecimal.ONE),
        destinationAddress = RECIPIENT_ADDRESS,
    )

    private fun transaction(
        amount: Amount,
        destinationAddress: String,
        extras: TransactionExtras? = null,
    ): TransactionData.Uncompiled = TransactionData.Uncompiled(
        amount = amount,
        fee = null,
        sourceAddress = OWN_ADDRESS,
        destinationAddress = destinationAddress,
        extras = extras,
    )

    private val sdkUsdc = Token(name = "USD Coin", symbol = "USDC", contractAddress = USDC_CONTRACT, decimals = 6)

    private companion object {
        const val USDC_CONTRACT = "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359"
        const val USDT_CONTRACT = "0xc2132D05D31c914a87C6611C10748AEb04B58e8F"
        const val SPENDER_ADDRESS = "0x1111111254EEB25477B68fb85Ed929f73A960582"
        const val RECIPIENT_ADDRESS = "0x2222222222222222222222222222222222222222"
        const val OWN_ADDRESS = "0x3333333333333333333333333333333333333333"
    }
}