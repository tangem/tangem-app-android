package com.tangem.features.earn.lending.impl.session

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.models.GaslessProtocolData
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.gasless.GetFeeForGaslessUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForTokenUseCase
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.ethFee
import com.tangem.test.mock.yield.MockYieldSupply
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LendingGaslessFeesTest {

    private val getFeeForGaslessUseCase: GetFeeForGaslessUseCase = mockk()
    private val getFeeForTokenUseCase: GetFeeForTokenUseCase = mockk()

    private val gaslessFees = LendingGaslessFees(
        getFeeForGaslessUseCase = getFeeForGaslessUseCase,
        getFeeForTokenUseCase = getFeeForTokenUseCase,
        singleAccountStatusListSupplier = mockk(),
    )

    private val userWallet = MockUserWalletFactory.create()
    private val currencies = MockCryptoCurrencyFactory()
    private val eth = currencies.createCoin(Blockchain.Ethereum)
    private val usdc = currencies.createToken(
        blockchain = Blockchain.Ethereum,
        contractAddress = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
        symbol = "USDC",
        decimals = 6,
    )
    private val withdrawal: TransactionData.Uncompiled = mockk()
    private val levels = TransactionFee.Single(normal = ethFee(BigInteger.valueOf(150_000)))

    @BeforeEach
    fun resetMocks() {
        clearMocks(getFeeForGaslessUseCase, getFeeForTokenUseCase)
    }

    @ParameterizedTest
    @MethodSource("provideAutoQuotes")
    fun `GIVEN no token picked WHEN quote THEN Smart Gas chooses who pays`(model: AutoQuoteModel) = runTest {
        // Arrange
        coEvery {
            getFeeForGaslessUseCase(
                userWallet = userWallet,
                network = eth.network,
                transactionData = withdrawal,
                sentAmount = any(),
                sentCurrencyStatus = any(),
                approval = any(),
                onNativeFeePreferred = any(),
            )
        } returns model.quote.right()

        // Act
        val actual = gaslessFees.quote(
            userWallet = userWallet,
            currency = eth,
            withdrawal = withdrawal,
            nativeFee = levels,
            feeToken = model.feeToken,
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected.right())
    }

    private fun provideAutoQuotes() = listOf(
        AutoQuoteModel(
            name = "a token pays",
            feeToken = null,
            quote = tokenQuote(),
            expected = tokenQuote().copy(nativeFee = levels),
        ),
        AutoQuoteModel(
            name = "the coin pays, at the session's own levels",
            feeToken = null,
            quote = TransactionFeeExtended(TransactionFee.Single(ethFee(BigInteger.ONE)), feeTokenId = eth.id),
            expected = TransactionFeeExtended(transactionFee = levels, feeTokenId = eth.id, nativeFee = levels),
        ),
        AutoQuoteModel(
            name = "the coin picked counts as no token",
            feeToken = MockYieldSupply.loadedStatus(currency = eth),
            quote = tokenQuote(),
            expected = tokenQuote().copy(nativeFee = levels),
        ),
    )

    @Test
    fun `GIVEN token picked WHEN quote THEN the withdrawal is quoted in that token`() = runTest {
        // Arrange
        coEvery {
            getFeeForTokenUseCase(
                userWallet = userWallet,
                token = usdc,
                transactionData = withdrawal,
                sentAmount = any(),
                approval = any(),
            )
        } returns tokenQuote().right()

        // Act
        val actual = gaslessFees.quote(
            userWallet = userWallet,
            currency = eth,
            withdrawal = withdrawal,
            nativeFee = levels,
            feeToken = MockYieldSupply.loadedStatus(currency = usdc),
        )

        // Assert
        assertThat(actual).isEqualTo(tokenQuote().copy(nativeFee = levels).right())
        coVerify(exactly = 0) {
            getFeeForGaslessUseCase(
                userWallet = any(),
                network = any(),
                transactionData = any(),
                sentAmount = any(),
                sentCurrencyStatus = any(),
                approval = any(),
                onNativeFeePreferred = any(),
            )
        }
    }

    private fun tokenQuote() = TransactionFeeExtended(
        transactionFee = TransactionFee.Single(
            normal = Fee.Common(Amount(currencySymbol = "USDC", value = BigDecimal("1.5"), decimals = 6)),
        ),
        feeTokenId = usdc.id,
        callGasLimits = listOf(BigInteger.valueOf(210_000)),
        gaslessProtocolData = GaslessProtocolData.Evm,
    )

    data class AutoQuoteModel(
        val name: String,
        val feeToken: CryptoCurrencyStatus?,
        val quote: TransactionFeeExtended,
        val expected: TransactionFeeExtended,
    ) {
        override fun toString(): String = name
    }
}