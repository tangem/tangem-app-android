package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.GaslessProtocolData
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.models.tron.TronGaslessQuote
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

internal class TronGaslessFeeResolverTest {

    private val getTronGaslessFeeUseCase: GetTronGaslessFeeUseCase = mockk()
    private val isTronGaslessSupportedUseCase: IsTronGaslessSupportedUseCase = mockk()

    private val network = trx.network
    private val userWalletId = UserWalletId(stringValue = "0123456789")

    private val resolver = TronGaslessFeeResolver(
        getTronGaslessFeeUseCase = getTronGaslessFeeUseCase,
        isTronGaslessSupportedUseCase = isTronGaslessSupportedUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(getTronGaslessFeeUseCase, isTronGaslessSupportedUseCase)
        coEvery { isTronGaslessSupportedUseCase(any(), any(), any()) } returns true
        coEvery { getTronGaslessFeeUseCase(any(), any(), any()) } returns gaslessFee(GASLESS_FEE).right()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ResolveWithAutoFeeToken {

        private val nativeFeeStub = NativeFeeStub()

        @BeforeEach
        fun resetNativeFee() {
            nativeFeeStub.reset(nativeFee(NATIVE_FEE).right())
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN Tron gasless does not apply WHEN resolveWithAutoFeeToken THEN returns null without a quote`(
            model: NotApplicableModel,
        ) = runTest {
            // Arrange
            coEvery { isTronGaslessSupportedUseCase(any(), any(), any()) } returns model.isSupported

            // Act
            val actual = resolve(
                transactionData = model.transactionData,
                walletCurrencies = model.walletCurrencies,
            )

            // Assert
            assertThat(actual).isNull()
            coVerify(exactly = 0) { getTronGaslessFeeUseCase(any(), any(), any()) }
            assertThat(nativeFeeStub.calls).isEqualTo(0)
            assertThat(nativeFeeStub.preferredCalls).isEqualTo(0)
        }

        @Test
        fun `GIVEN the quote fails and TRX covers native WHEN resolveWithAutoFeeToken THEN native fee is preferred`() =
            runTest {
                // Arrange
                givenQuoteFails()

                // Act
                val actual = resolve(sentBalance = "100", nativeBalance = "50")

                // Assert
                assertThat(actual).isEqualTo(nativeFee(NATIVE_FEE).right())
                assertThat(nativeFeeStub.preferredCalls).isEqualTo(1)
            }

        @Test
        fun `GIVEN the quote fails and TRX short WHEN resolveWithAutoFeeToken THEN native fee is not preferred`() =
            runTest {
                // Arrange
                givenQuoteFails()

                // Act
                val actual = resolve(sentBalance = "100", nativeBalance = "1")

                // Assert
                assertThat(actual).isEqualTo(nativeFee(NATIVE_FEE).right())
                assertThat(nativeFeeStub.preferredCalls).isEqualTo(0)
            }

        @Test
        fun `GIVEN token covers the fee WHEN resolveWithAutoFeeToken THEN gasless fee without native estimate`() =
            runTest {
                // Act
                val actual = resolve(sentBalance = "3.41", nativeBalance = "50")

                // Assert
                assertThat(actual).isEqualTo(gaslessFee(GASLESS_FEE).right())
                assertThat(nativeFeeStub.calls).isEqualTo(0)
                assertThat(nativeFeeStub.preferredCalls).isEqualTo(0)
            }

        @Test
        fun `GIVEN sent token balance unknown WHEN resolveWithAutoFeeToken THEN gasless fee without native estimate`() =
            runTest {
                // Act
                val actual = resolve(sentBalance = null, nativeBalance = "50")

                // Assert
                assertThat(actual).isEqualTo(gaslessFee(GASLESS_FEE).right())
                assertThat(nativeFeeStub.calls).isEqualTo(0)
                assertThat(nativeFeeStub.preferredCalls).isEqualTo(0)
            }

        @Test
        fun `GIVEN token balance equals the fee and TRX covers native WHEN resolveWithAutoFeeToken THEN native fee`() =
            runTest {
                // Act
                val actual = resolve(sentBalance = GASLESS_FEE, nativeBalance = "50")

                // Assert
                assertThat(actual).isEqualTo(nativeFee(NATIVE_FEE).right())
                assertThat(nativeFeeStub.preferredCalls).isEqualTo(1)
            }

        @Test
        fun `GIVEN token short and TRX covers native WHEN resolveWithAutoFeeToken THEN native fee`() = runTest {
            // Act
            val actual = resolve(sentBalance = "2", nativeBalance = "50")

            // Assert
            assertThat(actual).isEqualTo(nativeFee(NATIVE_FEE).right())
            assertThat(nativeFeeStub.calls).isEqualTo(1)
            assertThat(nativeFeeStub.preferredCalls).isEqualTo(1)
        }

        @Test
        fun `GIVEN token short and TRX short too WHEN resolveWithAutoFeeToken THEN gasless fee is kept`() = runTest {
            // Act
            val actual = resolve(sentBalance = "2", nativeBalance = "1")

            // Assert
            assertThat(actual).isEqualTo(gaslessFee(GASLESS_FEE).right())
            assertThat(nativeFeeStub.preferredCalls).isEqualTo(0)
        }

        @Test
        fun `GIVEN token short and native estimate fails WHEN resolveWithAutoFeeToken THEN gasless fee is kept`() =
            runTest {
                // Arrange
                nativeFeeStub.reset(GetFeeError.UnknownError.left())

                // Act
                val actual = resolve(sentBalance = "2", nativeBalance = "50")

                // Assert
                assertThat(actual).isEqualTo(gaslessFee(GASLESS_FEE).right())
                assertThat(nativeFeeStub.preferredCalls).isEqualTo(0)
            }

        @Test
        fun `GIVEN token short and TRX balance unknown WHEN resolveWithAutoFeeToken THEN gasless fee is kept`() =
            runTest {
                // Arrange
                nativeFeeStub.reset(nativeFee("0").right())

                // Act
                val actual = resolve(sentBalance = "2", nativeBalance = null)

                // Assert
                assertThat(actual).isEqualTo(gaslessFee(GASLESS_FEE).right())
                assertThat(nativeFeeStub.preferredCalls).isEqualTo(0)
            }

        @Test
        fun `GIVEN a token transfer WHEN resolveWithAutoFeeToken THEN the quote is requested for the sent token`() =
            runTest {
                // Arrange
                val transactionData = tokenTransfer()

                // Act
                resolve(transactionData = transactionData)

                // Assert
                coVerify(exactly = 1) { isTronGaslessSupportedUseCase(userWalletId, network, usdt) }
                coVerify(exactly = 1) {
                    getTronGaslessFeeUseCase(
                        userWalletId = userWalletId,
                        transactionData = transactionData,
                        feeToken = usdt,
                    )
                }
            }

        @Test
        fun `GIVEN the tx contract differs in case WHEN resolveWithAutoFeeToken THEN no token is quoted`() =
            runTest {
                // Arrange
                val transactionData = tokenTransfer(contract = USDT_CONTRACT.lowercase())

                // Act
                val actual = resolve(transactionData = transactionData)

                // Assert
                assertThat(actual).isNull()
                coVerify(exactly = 0) { getTronGaslessFeeUseCase(any(), any(), any()) }
            }

        @Test
        fun `GIVEN the account list lags WHEN resolveWithAutoFeeToken THEN the caller's status is quoted`() =
            runTest {
                // Act
                val actual = resolve(walletCurrencies = emptyList(), sentCurrencyStatus = statusOf(usdt, "100"))

                // Assert
                assertThat(actual).isEqualTo(gaslessFee(GASLESS_FEE).right())
                coVerify(exactly = 1) { getTronGaslessFeeUseCase(userWalletId, any(), usdt) }
            }

        @Test
        fun `GIVEN the caller's status is another token WHEN resolveWithAutoFeeToken THEN returns null`() =
            runTest {
                // Act
                val actual = resolve(
                    walletCurrencies = emptyList(),
                    sentCurrencyStatus = statusOf(usdtOnEthereum, "100"),
                )

                // Assert
                assertThat(actual).isNull()
                coVerify(exactly = 0) { getTronGaslessFeeUseCase(any(), any(), any()) }
            }

        private suspend fun resolve(
            transactionData: TransactionData = tokenTransfer(),
            sentBalance: String? = "100",
            nativeBalance: String? = "50",
            walletCurrencies: List<CryptoCurrency> = listOf(usdt),
            sentCurrencyStatus: CryptoCurrencyStatus? = null,
        ): Either<GetFeeError, TransactionFeeExtended>? = resolver.resolveWithAutoFeeToken(
            userWalletId = userWalletId,
            network = network,
            transactionData = transactionData,
            nativeCurrencyStatus = statusOf(trx, nativeBalance),
            accountStatusList = accountStatusList(walletCurrencies.map { statusOf(it, sentBalance) }),
            sentCurrencyStatus = sentCurrencyStatus,
            loadNativeFee = nativeFeeStub::load,
            onNativeFeePreferred = nativeFeeStub::onPreferred,
        )

        private fun givenQuoteFails() {
            coEvery { getTronGaslessFeeUseCase(any(), any(), any()) } returns
                GetFeeError.GaslessError.DataError(IllegalStateException("Quote unavailable")).left()
        }

        private fun provideTestModels() = listOf(
            NotApplicableModel(isSupported = false),
            NotApplicableModel(transactionData = coinTransfer()),
            NotApplicableModel(transactionData = tokenTransfer(value = BigDecimal.ZERO)),
            NotApplicableModel(walletCurrencies = emptyList()),
            NotApplicableModel(walletCurrencies = listOf(usdtOnEthereum)),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ResolveWithAutoFeeTokenRefusals {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN activation is refused WHEN resolveWithAutoFeeToken THEN the refusal is returned`(
            refusal: GetFeeError.GaslessError,
        ) = runTest {
            // Arrange
            val nativeFeeStub = NativeFeeStub().apply { reset(nativeFee(NATIVE_FEE).right()) }
            coEvery { getTronGaslessFeeUseCase(any(), any(), any()) } returns refusal.left()

            // Act
            val actual = resolver.resolveWithAutoFeeToken(
                userWalletId = userWalletId,
                network = network,
                transactionData = tokenTransfer(),
                nativeCurrencyStatus = statusOf(trx, "50"),
                accountStatusList = accountStatusList(listOf(statusOf(usdt, "1"))),
                sentCurrencyStatus = null,
                loadNativeFee = nativeFeeStub::load,
                onNativeFeePreferred = nativeFeeStub::onPreferred,
            )

            // Assert
            assertThat(actual).isEqualTo(refusal.left())
            assertThat(nativeFeeStub.calls).isEqualTo(0)
            assertThat(nativeFeeStub.preferredCalls).isEqualTo(0)
        }

        private fun provideTestModels(): List<GetFeeError.GaslessError> = listOf(
            GetFeeError.GaslessError.TronActivationNotEnoughFunds(minimumAmount = BigDecimal(5), tokenSymbol = "USDT"),
            GetFeeError.GaslessError.TronActivationRateLimited(isPerWallet = true),
            GetFeeError.GaslessError.TronActivationRateLimited(isPerWallet = false),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ResolveWithSelectedFeeToken {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN Tron gasless does not apply WHEN resolveWithSelectedFeeToken THEN returns null without a quote`(
            model: SelectedNotApplicableModel,
        ) = runTest {
            // Arrange
            coEvery { isTronGaslessSupportedUseCase(any(), any(), any()) } returns model.isSupported

            // Act
            val actual = resolver.resolveWithSelectedFeeToken(
                userWalletId = userWalletId,
                feeToken = model.feeToken,
                transactionData = model.transactionData,
            )

            // Assert
            assertThat(actual).isNull()
            coVerify(exactly = 0) { getTronGaslessFeeUseCase(any(), any(), any()) }
        }

        @ParameterizedTest
        @MethodSource("provideQuoteResults")
        fun `GIVEN a supported token WHEN resolveWithSelectedFeeToken THEN the quote result is returned as is`(
            quoteResult: Either<GetFeeError, TransactionFeeExtended>,
        ) = runTest {
            // Arrange
            val transactionData = tokenTransfer()
            coEvery { getTronGaslessFeeUseCase(userWalletId, transactionData, usdt) } returns quoteResult

            // Act
            val actual = resolver.resolveWithSelectedFeeToken(
                userWalletId = userWalletId,
                feeToken = usdt,
                transactionData = transactionData,
            )

            // Assert
            assertThat(actual).isEqualTo(quoteResult)
            coVerify(exactly = 1) { isTronGaslessSupportedUseCase(userWalletId, usdt.network, usdt) }
        }

        private fun provideTestModels() = listOf(
            SelectedNotApplicableModel(feeToken = trx),
            SelectedNotApplicableModel(transactionData = coinTransfer()),
            SelectedNotApplicableModel(transactionData = tokenTransfer(value = BigDecimal.ZERO)),
            SelectedNotApplicableModel(isSupported = false),
        )

        fun provideQuoteResults(): List<Either<GetFeeError, TransactionFeeExtended>> = listOf(
            gaslessFee(GASLESS_FEE).right(),
            GetFeeError.GaslessError.NotEnoughFunds.left(),
        )
    }

    internal data class NotApplicableModel(
        val transactionData: TransactionData = tokenTransfer(),
        val walletCurrencies: List<CryptoCurrency> = listOf(usdt),
        val isSupported: Boolean = true,
    )

    internal data class SelectedNotApplicableModel(
        val feeToken: CryptoCurrency = usdt,
        val transactionData: TransactionData = tokenTransfer(),
        val isSupported: Boolean = true,
    )

    private fun accountStatusList(statuses: List<CryptoCurrencyStatus>): AccountStatusList = mockk {
        every { flattenCurrencies() } returns statuses
    }

    private fun statusOf(currency: CryptoCurrency, balance: String?): CryptoCurrencyStatus = CryptoCurrencyStatus(
        currency = currency,
        value = if (balance == null) {
            CryptoCurrencyStatus.Loading
        } else {
            mockk<CryptoCurrencyStatus.Loaded> { every { amount } returns BigDecimal(balance) }
        },
    )

    private fun nativeFee(value: String) = TransactionFeeExtended(
        transactionFee = TransactionFee.Single(normal = Fee.Common(Amount(BigDecimal(value), Blockchain.Tron))),
        feeTokenId = trx.id,
    )

    private class NativeFeeStub {
        private lateinit var result: Either<GetFeeError, TransactionFeeExtended>
        var calls: Int = 0
            private set
        var preferredCalls: Int = 0
            private set

        fun reset(result: Either<GetFeeError, TransactionFeeExtended>) {
            this.result = result
            calls = 0
            preferredCalls = 0
        }

        fun onPreferred() {
            preferredCalls++
        }

        fun load(): Either<GetFeeError, TransactionFeeExtended> {
            calls++
            return result
        }
    }

    private companion object {
        const val USDT_CONTRACT = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
        const val USDT_DECIMALS = 6
        const val GASLESS_FEE = "2.41"
        const val NATIVE_FEE = "6.43"

        val currencyFactory = MockCryptoCurrencyFactory()
        val trx: CryptoCurrency.Coin = currencyFactory.createCoin(Blockchain.Tron)
        val usdt: CryptoCurrency.Token = currencyFactory.createToken(
            blockchain = Blockchain.Tron,
            id = "usdt",
            contractAddress = USDT_CONTRACT,
            symbol = "USDT",
            decimals = USDT_DECIMALS,
        )
        val usdtOnEthereum: CryptoCurrency.Token = currencyFactory.createToken(
            blockchain = Blockchain.Ethereum,
            id = "usdt-eth",
            contractAddress = USDT_CONTRACT,
        )
        val quote: TronGaslessQuote = mockk()

        fun gaslessFee(compensation: String) = TransactionFeeExtended(
            transactionFee = TransactionFee.Single(
                normal = Fee.Common(Amount(token = usdtSdkToken(USDT_CONTRACT), value = BigDecimal(compensation))),
            ),
            feeTokenId = usdt.id,
            gaslessProtocolData = GaslessProtocolData.Tron(quote = quote),
        )

        fun usdtSdkToken(contract: String) = Token(
            symbol = "USDT",
            contractAddress = contract,
            decimals = USDT_DECIMALS,
        )

        fun tokenTransfer(
            contract: String = USDT_CONTRACT,
            value: BigDecimal = BigDecimal("50"),
        ): TransactionData.Uncompiled = TransactionData.Uncompiled(
            amount = Amount(token = usdtSdkToken(contract), value = value),
            fee = null,
            sourceAddress = "TFrom",
            destinationAddress = "TTo",
        )

        fun coinTransfer(): TransactionData.Uncompiled = TransactionData.Uncompiled(
            amount = Amount(BigDecimal("50"), Blockchain.Tron),
            fee = null,
            sourceAddress = "TFrom",
            destinationAddress = "TTo",
        )
    }
}