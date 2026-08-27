package com.tangem.domain.tokens

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.repository.CurrenciesRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsAmountSubtractAvailableUseCaseTest {

    private val currenciesRepository: CurrenciesRepository = mockk()

    private val useCase = IsAmountSubtractAvailableUseCase(currenciesRepository)

    private val userWalletId: UserWalletId = mockk()

    private val factory = MockCryptoCurrencyFactory()
    private val tronUsdt = factory.createToken(blockchain = Blockchain.Tron, id = "usdt", contractAddress = "TUsdt")
    private val tronUsdc = factory.createToken(blockchain = Blockchain.Tron, id = "usdc", contractAddress = "TUsdc")
    private val ethUsdt = factory.createToken(blockchain = Blockchain.Ethereum, id = "usdt", contractAddress = "0xUsdt")

    @BeforeEach
    fun setup() {
        // Both Tron and Ethereum pay their regular fees in the native coin.
        coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
    }

    @Test
    fun `GIVEN Tron gasless fee in the sent token WHEN invoke THEN subtract available`() = runTest {
        // Act — Tron gasless carries a token amount inside Fee.Common, not Fee.Ethereum.TokenCurrency.
        val actual = useCase(
            userWalletId = userWalletId,
            currency = tronUsdt,
            maybeGaslessFee = tronUsdt.id to tronGaslessFee(tronUsdt),
        )

        // Assert
        assertThat(actual.getOrNull()).isTrue()
    }

    @Test
    fun `GIVEN Tron gasless fee in another token WHEN invoke THEN subtract unavailable`() = runTest {
        // Act — the compensation is charged to the USDC balance, so the sent USDT must not be reduced.
        val actual = useCase(
            userWalletId = userWalletId,
            currency = tronUsdt,
            maybeGaslessFee = tronUsdc.id to tronGaslessFee(tronUsdc),
        )

        // Assert
        assertThat(actual.getOrNull()).isFalse()
    }

    @Test
    fun `GIVEN EVM gasless fee in the sent token WHEN invoke THEN subtract available`() = runTest {
        // Act
        val actual = useCase(
            userWalletId = userWalletId,
            currency = ethUsdt,
            maybeGaslessFee = ethUsdt.id to evmGaslessFee(ethUsdt),
        )

        // Assert
        assertThat(actual.getOrNull()).isTrue()
    }

    @Test
    fun `GIVEN token paying a coin fee WHEN invoke THEN subtract unavailable`() = runTest {
        // Act — no gasless fee at all: the regular FeePaidCurrency rules apply.
        val actual = useCase(userWalletId = userWalletId, currency = tronUsdt)

        // Assert
        assertThat(actual.getOrNull()).isFalse()
    }

    @Test
    fun `GIVEN coin paying a coin fee WHEN invoke THEN subtract available`() = runTest {
        // Act
        val actual = useCase(
            userWalletId = userWalletId,
            currency = factory.createCoin(blockchain = Blockchain.Tron),
        )

        // Assert
        assertThat(actual.getOrNull()).isTrue()
    }

    @Test
    fun `GIVEN coin send WHEN gasless fee is a token THEN subtract decided by the fee paid currency`() = runTest {
        // Arrange — a coin has no contract address, so it can never be the token paying a gasless fee.
        val trx = factory.createCoin(blockchain = Blockchain.Tron)
        coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Token(
            tokenId = tronUsdt.id,
            name = tronUsdt.name,
            symbol = tronUsdt.symbol,
            contractAddress = "TUsdt",
            balance = BigDecimal.ZERO,
        )

        // Act
        val actual = useCase(
            userWalletId = userWalletId,
            currency = trx,
            maybeGaslessFee = trx.id to tronGaslessFee(tronUsdt),
        )

        // Assert
        assertThat(actual.getOrNull()).isFalse()
    }

    private fun tronGaslessFee(feeToken: CryptoCurrency.Token): Fee = Fee.Common(amount = tokenAmount(feeToken))

    private fun evmGaslessFee(feeToken: CryptoCurrency.Token): Fee = Fee.Ethereum.TokenCurrency(
        amount = tokenAmount(feeToken),
        gasLimit = BigInteger.ONE,
        coinPriceInToken = BigInteger.ONE,
        feeTransferGasLimit = BigInteger.ONE,
        baseGas = BigInteger.ONE,
    )

    private fun tokenAmount(feeToken: CryptoCurrency.Token) = Amount(
        token = Token(
            symbol = feeToken.symbol,
            contractAddress = feeToken.contractAddress,
            decimals = feeToken.decimals,
        ),
        value = BigDecimal("2.74"),
    )
}