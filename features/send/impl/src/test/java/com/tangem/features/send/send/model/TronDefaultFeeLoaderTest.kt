package com.tangem.features.send.send.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.models.tron.TronGaslessQuote
import com.tangem.domain.transaction.usecase.gasless.GetTronGaslessFeeUseCase
import com.tangem.domain.transaction.usecase.gasless.IsTronGaslessSupportedUseCase
import com.tangem.features.send.api.SendFeatureToggles
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TronDefaultFeeLoaderTest {

    private val getTronGaslessFeeUseCase: GetTronGaslessFeeUseCase = mockk()
    private val isTronGaslessSupportedUseCase: IsTronGaslessSupportedUseCase = mockk()
    private val sendFeatureToggles: SendFeatureToggles = mockk()

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val usdt = currencyFactory.createToken(blockchain = Blockchain.Tron, id = "usdt", contractAddress = "TUsdt")
    private val trx = currencyFactory.createCoin(blockchain = Blockchain.Tron)
    private val transactionData: TransactionData = mockk()
    private val quote: TronGaslessQuote = mockk()

    private val nativeFeeStub = NativeFeeStub()

    private lateinit var loader: TronDefaultFeeLoader

    @BeforeEach
    fun setUp() {
        clearMocks(getTronGaslessFeeUseCase, isTronGaslessSupportedUseCase, sendFeatureToggles)
        every { sendFeatureToggles.isTronGaslessEnabled } returns true
        coEvery { isTronGaslessSupportedUseCase(any(), any()) } returns true
        coEvery { getTronGaslessFeeUseCase(any(), any()) } returns gaslessFee(compensation = "2.41").right()
        nativeFeeStub.reset(nativeFee(value = "6.43").right())
        loader = TronDefaultFeeLoader(getTronGaslessFeeUseCase, isTronGaslessSupportedUseCase, sendFeatureToggles)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun decide(model: DecideModel) = runTest {
        // Arrange
        every { sendFeatureToggles.isTronGaslessEnabled } returns model.isToggleEnabled
        coEvery { isTronGaslessSupportedUseCase(any(), any()) } returns model.isSupported
        val sentCurrency = if (model.sendsCoin) trx else usdt

        // Act
        val actual = load(sentCurrency = sentCurrency, sentBalance = "100", nativeBalance = "50", amount = "1")

        // Assert
        val expected = if (model.expectsGasless) gaslessFee(compensation = "2.41") else nativeFee(value = "6.43")
        assertThat(actual).isEqualTo(expected.right())
        coVerify(exactly = if (model.expectsGasless) 1 else 0) { getTronGaslessFeeUseCase(any(), any()) }
        assertThat(nativeFeeStub.calls).isEqualTo(if (model.expectsGasless) 0 else 1)
    }

    @Test
    fun `GIVEN TRX covers native fee WHEN quote fails THEN native fee is loaded and kept on next load`() = runTest {
        // Arrange
        coEvery { getTronGaslessFeeUseCase(any(), any()) } returns quoteError()

        // Act
        val first = load(sentBalance = "100", nativeBalance = "50", amount = "1")
        coEvery { getTronGaslessFeeUseCase(any(), any()) } returns gaslessFee(compensation = "2.41").right()
        val second = load(sentBalance = "100", nativeBalance = "50", amount = "1")

        // Assert
        assertThat(first).isEqualTo(nativeFee(value = "6.43").right())
        assertThat(second).isEqualTo(nativeFee(value = "6.43").right())
        coVerify(exactly = 1) { getTronGaslessFeeUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN TRX short WHEN quote fails THEN the next load quotes again`() = runTest {
        // Arrange
        coEvery { getTronGaslessFeeUseCase(any(), any()) } returns quoteError()

        // Act
        val first = load(sentBalance = "100", nativeBalance = "1", amount = "1")
        coEvery { getTronGaslessFeeUseCase(any(), any()) } returns gaslessFee(compensation = "2.41").right()
        val second = load(sentBalance = "100", nativeBalance = "1", amount = "1")

        // Assert
        assertThat(first).isEqualTo(nativeFee(value = "6.43").right())
        assertThat(second).isEqualTo(gaslessFee(compensation = "2.41").right())
    }

    @Test
    fun `GIVEN support check failed once WHEN loading again THEN gasless is re-evaluated`() = runTest {
        // Arrange
        coEvery { isTronGaslessSupportedUseCase(any(), any()) } returnsMany listOf(false, true)

        // Act
        val first = load(sentBalance = "100", nativeBalance = "50", amount = "1")
        val second = load(sentBalance = "100", nativeBalance = "50", amount = "1")

        // Assert
        assertThat(first).isEqualTo(nativeFee(value = "6.43").right())
        assertThat(second).isEqualTo(gaslessFee(compensation = "2.41").right())
    }

    @Test
    fun `GIVEN fee status is not the network coin WHEN token is short THEN gasless fee is kept`() = runTest {
        // Arrange
        val actual = loader.load(
            sentStatus = status(usdt, "2"),
            nativeStatus = status(usdt, "2"),
            transactionData = transactionData,
            sentAmount = BigDecimal("2"),
            loadNativeFee = nativeFeeStub::load,
        )

        // Assert
        assertThat(actual).isEqualTo(gaslessFee(compensation = "2.41").right())
    }

    @Test
    fun `GIVEN TRX balance unknown and zero native fee WHEN token is short THEN gasless fee is kept`() = runTest {
        // Arrange
        nativeFeeStub.reset(nativeFee(value = "0").right())

        // Act
        val actual = loader.load(
            sentStatus = status(usdt, "2"),
            nativeStatus = status(trx, balance = null),
            transactionData = transactionData,
            sentAmount = BigDecimal("2"),
            loadNativeFee = nativeFeeStub::load,
        )

        // Assert
        assertThat(actual).isEqualTo(gaslessFee(compensation = "2.41").right())
    }

    @Test
    fun `GIVEN sent token balance unknown WHEN load THEN gasless fee without native estimate`() = runTest {
        // Act
        val actual = loader.load(
            sentStatus = status(usdt, balance = null),
            nativeStatus = status(trx, "50"),
            transactionData = transactionData,
            sentAmount = BigDecimal("2"),
            loadNativeFee = nativeFeeStub::load,
        )

        // Assert
        assertThat(actual).isEqualTo(gaslessFee(compensation = "2.41").right())
        assertThat(nativeFeeStub.calls).isEqualTo(0)
    }

    @Test
    fun `GIVEN token covers amount and fee WHEN load THEN gasless fee without native estimate`() = runTest {
        // Act
        val actual = load(sentBalance = "3.41", nativeBalance = "50", amount = "1")

        // Assert
        assertThat(actual).isEqualTo(gaslessFee(compensation = "2.41").right())
        assertThat(nativeFeeStub.calls).isEqualTo(0)
    }

    @Test
    fun `GIVEN unknown amount WHEN load THEN gasless fee without coverage check`() = runTest {
        // Act
        val actual = load(sentBalance = "0.5", nativeBalance = "50", amount = null)

        // Assert
        assertThat(actual).isEqualTo(gaslessFee(compensation = "2.41").right())
        assertThat(nativeFeeStub.calls).isEqualTo(0)
    }

    @Test
    fun `GIVEN token short and TRX covers native fee WHEN load THEN switches to native and stays`() = runTest {
        // Act
        val first = load(sentBalance = "2", nativeBalance = "50", amount = "2")
        val second = load(sentBalance = "2", nativeBalance = "50", amount = "0.1")

        // Assert
        assertThat(first).isEqualTo(nativeFee(value = "6.43").right())
        assertThat(second).isEqualTo(nativeFee(value = "6.43").right())
        coVerify(exactly = 1) { getTronGaslessFeeUseCase(any(), any()) }
        assertThat(nativeFeeStub.calls).isEqualTo(2)
    }

    @Test
    fun `GIVEN token short and TRX short too WHEN load THEN gasless fee is kept`() = runTest {
        // Act
        val first = load(sentBalance = "2", nativeBalance = "1", amount = "2")
        val second = load(sentBalance = "2", nativeBalance = "1", amount = "2")

        // Assert
        assertThat(first).isEqualTo(gaslessFee(compensation = "2.41").right())
        assertThat(second).isEqualTo(gaslessFee(compensation = "2.41").right())
        coVerify(exactly = 2) { getTronGaslessFeeUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN token short and native estimate fails WHEN load THEN gasless fee is kept`() = runTest {
        // Arrange
        nativeFeeStub.reset(GetFeeError.UnknownError.left())

        // Act
        val actual = load(sentBalance = "2", nativeBalance = "50", amount = "2")

        // Assert
        assertThat(actual).isEqualTo(gaslessFee(compensation = "2.41").right())
    }

    @Test
    fun `GIVEN quote fails and native fails WHEN load THEN native error is returned`() = runTest {
        // Arrange
        coEvery { getTronGaslessFeeUseCase(any(), any()) } returns GetFeeError.GaslessError.DataError(
            IllegalStateException("Quote unavailable"),
        ).left()
        nativeFeeStub.reset(GetFeeError.UnknownError.left())

        // Act
        val actual = load(sentBalance = "100", nativeBalance = "50", amount = "1")

        // Assert
        assertThat(actual).isEqualTo(GetFeeError.UnknownError.left())
    }

    private suspend fun load(
        sentCurrency: CryptoCurrency = usdt,
        sentBalance: String,
        nativeBalance: String,
        amount: String?,
    ): Either<GetFeeError, TransactionFeeExtended> = loader.load(
        sentStatus = status(sentCurrency, sentBalance),
        nativeStatus = status(trx, nativeBalance),
        transactionData = transactionData,
        sentAmount = amount?.let(::BigDecimal),
        loadNativeFee = nativeFeeStub::load,
    )

    private fun quoteError() = GetFeeError.GaslessError.DataError(IllegalStateException("Quote unavailable")).left()

    private fun status(currency: CryptoCurrency, balance: String?): CryptoCurrencyStatus = mockk {
        every { this@mockk.currency } returns currency
        every { value } returns if (balance == null) {
            CryptoCurrencyStatus.Loading
        } else {
            mockk<CryptoCurrencyStatus.Loaded> { every { amount } returns BigDecimal(balance) }
        }
    }

    private fun nativeFee(value: String) = TransactionFeeExtended(
        transactionFee = TransactionFee.Single(normal = Fee.Common(Amount(BigDecimal(value), Blockchain.Tron))),
        feeTokenId = trx.id,
    )

    private fun gaslessFee(compensation: String) = TransactionFeeExtended(
        transactionFee = TransactionFee.Single(
            normal = Fee.Common(Amount(BigDecimal(compensation), Blockchain.Tron)),
        ),
        feeTokenId = usdt.id,
        tronGaslessQuote = quote,
    )

    internal data class DecideModel(
        val expectsGasless: Boolean,
        val isToggleEnabled: Boolean = true,
        val isSupported: Boolean = true,
        val sendsCoin: Boolean = false,
    )

    private fun provideTestModels() = listOf(
        DecideModel(expectsGasless = true),
        DecideModel(isToggleEnabled = false, expectsGasless = false),
        DecideModel(isSupported = false, expectsGasless = false),
        DecideModel(sendsCoin = true, expectsGasless = false),
    )

    private class NativeFeeStub {
        private lateinit var result: Either<GetFeeError, TransactionFeeExtended>
        var calls: Int = 0
            private set

        fun reset(result: Either<GetFeeError, TransactionFeeExtended>) {
            this.result = result
            calls = 0
        }

        fun load(): Either<GetFeeError, TransactionFeeExtended> {
            calls++
            return result
        }
    }
}