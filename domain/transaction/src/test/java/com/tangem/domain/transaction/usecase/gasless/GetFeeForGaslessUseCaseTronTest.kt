package com.tangem.domain.transaction.usecase.gasless

import arrow.core.left
import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.GaslessProtocolData
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.models.tron.TronGaslessQuote
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Auto-selection of the fee payment currency on Tron: the fee is quoted in the sent token first, and TRX
 * is taken only when the token cannot cover the compensation but TRX covers the native fee.
 */
internal class GetFeeForGaslessUseCaseTronTest {

    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val currencyChecksRepository: CurrencyChecksRepository = mockk(relaxed = true)
    private val getFeeUseCase: GetFeeUseCase = mockk()
    private val evmGaslessFeeResolver: EvmGaslessFeeResolver = mockk()
    private val getTronGaslessFeeUseCase: GetTronGaslessFeeUseCase = mockk()
    private val isTronGaslessSupportedUseCase: IsTronGaslessSupportedUseCase = mockk()

    private val accountStatusList: AccountStatusList = mockk()
    private val userWalletId = UserWalletId(stringValue = "0123456789")
    private val userWallet: UserWallet = mockk {
        every { walletId } returns userWalletId
    }

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val trx: CryptoCurrency.Coin = currencyFactory.createCoin(Blockchain.Tron)
    private val usdt: CryptoCurrency.Token = currencyFactory.createToken(
        blockchain = Blockchain.Tron,
        contractAddress = USDT_CONTRACT,
    )
    private val network: Network = trx.network

    private val transferTransaction = TransactionData.Uncompiled(
        amount = Amount(token = USDT_SDK_TOKEN, value = BigDecimal("50")),
        fee = null,
        sourceAddress = "TFrom",
        destinationAddress = "TTo",
    )

    private val nativeFee = TransactionFee.Single(
        normal = Fee.Common(amount = Amount(blockchain = Blockchain.Tron, value = NATIVE_FEE)),
    )
    private val nativeFeeExtended = TransactionFeeExtended(
        transactionFee = nativeFee,
        feeTokenId = trx.id,
        nativeFee = nativeFee,
    )

    private val tronGaslessFee = TransactionFeeExtended(
        transactionFee = TransactionFee.Single(
            normal = Fee.Common(amount = Amount(token = USDT_SDK_TOKEN, value = COMPENSATION)),
        ),
        feeTokenId = usdt.id,
        gaslessProtocolData = GaslessProtocolData.Tron(quote = QUOTE),
    )

    private val useCase = GetFeeForGaslessUseCase(
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        currencyChecksRepository = currencyChecksRepository,
        getFeeUseCase = getFeeUseCase,
        evmGaslessFeeResolver = evmGaslessFeeResolver,
        tronGaslessFeeResolver = TronGaslessFeeResolver(
            getTronGaslessFeeUseCase = getTronGaslessFeeUseCase,
            isTronGaslessSupportedUseCase = isTronGaslessSupportedUseCase,
        ),
    )

    private var nativeFeePreferredCalls = 0

    @BeforeEach
    fun setUp() {
        nativeFeePreferredCalls = 0
        clearMocks(
            singleAccountStatusListSupplier,
            getFeeUseCase,
            getTronGaslessFeeUseCase,
            isTronGaslessSupportedUseCase,
            accountStatusList,
        )
        mockkObject(CryptoCurrencyStatusOperations)
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns accountStatusList
        coEvery { isTronGaslessSupportedUseCase(network, usdt) } returns true
        coEvery { getTronGaslessFeeUseCase(userWalletId, transferTransaction, usdt) } returns tronGaslessFee.right()
        coEvery { getFeeUseCase.invoke(userWallet, network, transferTransaction) } returns nativeFee.right()
        givenBalances(sentToken = BigDecimal("50"), native = BigDecimal.TEN)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(CryptoCurrencyStatusOperations)
    }

    @Test
    fun `GIVEN sent token covers the compensation and TRX dearer WHEN invoke THEN gasless fee`() = runTest {
        // Act
        val actual = invoke(transferTransaction)

        // Assert
        assertThat(actual).isEqualTo(tronGaslessFee.right())
        verify { evmGaslessFeeResolver wasNot Called }
        assertThat(nativeFeePreferredCalls).isEqualTo(0)
    }

    @Test
    fun `GIVEN token short and TRX covers the native fee WHEN invoke THEN native fee`() = runTest {
        // Arrange
        givenBalances(sentToken = BigDecimal.ONE, native = NATIVE_FEE + BigDecimal.ONE)

        // Act
        val actual = invoke(transferTransaction)

        // Assert
        assertThat(actual).isEqualTo(nativeFeeExtended.right())
        assertThat(nativeFeePreferredCalls).isEqualTo(1)
    }

    @Test
    fun `GIVEN token short and TRX short too WHEN invoke THEN gasless fee is kept`() = runTest {
        // Arrange
        givenBalances(sentToken = BigDecimal.ONE, native = BigDecimal.ZERO)

        // Act
        val actual = invoke(transferTransaction)

        // Assert
        assertThat(actual).isEqualTo(tronGaslessFee.right())
        assertThat(nativeFeePreferredCalls).isEqualTo(0)
    }

    @Test
    fun `GIVEN token short and native fee fails WHEN invoke THEN gasless fee is kept`() = runTest {
        // Arrange
        givenBalances(sentToken = BigDecimal.ONE, native = BigDecimal.TEN)
        coEvery { getFeeUseCase.invoke(userWallet, network, transferTransaction) } returns
            GetFeeError.UnknownError.left()

        // Act
        val actual = invoke(transferTransaction)

        // Assert
        assertThat(actual).isEqualTo(tronGaslessFee.right())
        assertThat(nativeFeePreferredCalls).isEqualTo(0)
    }

    @Test
    fun `GIVEN quote fails WHEN invoke THEN native fee is used`() = runTest {
        // Arrange
        coEvery { getTronGaslessFeeUseCase(userWalletId, transferTransaction, usdt) } returns
            GetFeeError.UnknownError.left()

        // Act
        val actual = invoke(transferTransaction)

        // Assert
        assertThat(actual).isEqualTo(nativeFeeExtended.right())
        assertThat(nativeFeePreferredCalls).isEqualTo(1)
    }

    @Test
    fun `GIVEN quote fails and native fee fails WHEN invoke THEN the native fee error is surfaced`() = runTest {
        // Arrange
        coEvery { getTronGaslessFeeUseCase(userWalletId, transferTransaction, usdt) } returns
            GetFeeError.UnknownError.left()
        coEvery { getFeeUseCase.invoke(userWallet, network, transferTransaction) } returns
            GetFeeError.GaslessError.NotEnoughFunds.left()

        // Act
        val actual = invoke(transferTransaction)

        // Assert
        assertThat(actual).isEqualTo(GetFeeError.GaslessError.NotEnoughFunds.left())
        assertThat(nativeFeePreferredCalls).isEqualTo(0)
    }

    @Test
    fun `GIVEN activation is refused WHEN invoke THEN the refusal is surfaced`() = runTest {
        // Arrange
        val refusal = GetFeeError.GaslessError.TronActivationRateLimited(isPerWallet = true)
        coEvery { getTronGaslessFeeUseCase(userWalletId, transferTransaction, usdt) } returns refusal.left()

        // Act
        val actual = invoke(transferTransaction)

        // Assert
        assertThat(actual).isEqualTo(refusal.left())
        assertThat(nativeFeePreferredCalls).isEqualTo(0)
    }

    @Test
    fun `GIVEN an unsupported token WHEN invoke THEN native fee without a quote`() = runTest {
        // Arrange
        coEvery { isTronGaslessSupportedUseCase(network, usdt) } returns false

        // Act
        val actual = invoke(transferTransaction)

        // Assert
        assertThat(actual).isEqualTo(nativeFeeExtended.right())
        coVerify(exactly = 0) { getTronGaslessFeeUseCase(any(), any(), any()) }
        assertThat(nativeFeePreferredCalls).isEqualTo(0)
    }

    @Test
    fun `GIVEN a zero-amount transaction WHEN invoke THEN native fee without a quote`() = runTest {
        // Arrange
        val approval = transferTransaction.copy(amount = transferTransaction.amount.copy(value = BigDecimal.ZERO))
        coEvery { getFeeUseCase.invoke(userWallet, network, approval) } returns nativeFee.right()

        // Act
        val actual = invoke(approval)

        // Assert
        assertThat(actual).isEqualTo(nativeFeeExtended.right())
        coVerify(exactly = 0) { getTronGaslessFeeUseCase(any(), any(), any()) }
        assertThat(nativeFeePreferredCalls).isEqualTo(0)
    }

    private suspend fun invoke(transactionData: TransactionData) = useCase(
        userWallet = userWallet,
        network = network,
        transactionData = transactionData,
        onNativeFeePreferred = { nativeFeePreferredCalls++ },
    )

    private fun givenBalances(sentToken: BigDecimal, native: BigDecimal) {
        every { accountStatusList.flattenCurrencies() } returns listOf(statusOf(usdt, sentToken))
        with(CryptoCurrencyStatusOperations) {
            every { accountStatusList.getCoinStatus(network) } returns statusOf(trx, native).some()
        }
    }

    /** Rates keep the TRX fee dearer in fiat than the USDT compensation. */
    private fun statusOf(currency: CryptoCurrency, balance: BigDecimal): CryptoCurrencyStatus {
        val rate = if (currency is CryptoCurrency.Coin) TRX_FIAT_RATE else USDT_FIAT_RATE
        return CryptoCurrencyStatus(
            currency = currency,
            value = mockk<CryptoCurrencyStatus.Loaded> {
                every { amount } returns balance
                every { fiatRate } returns rate
            },
        )
    }

    private companion object {
        const val USDT_CONTRACT = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
        val USDT_SDK_TOKEN = Token(symbol = "USDT", contractAddress = USDT_CONTRACT, decimals = 6)
        val NATIVE_FEE: BigDecimal = BigDecimal("8.0722")
        val COMPENSATION: BigDecimal = BigDecimal("2.75")
        val TRX_FIAT_RATE: BigDecimal = BigDecimal("0.4")
        val USDT_FIAT_RATE: BigDecimal = BigDecimal.ONE

        val QUOTE = TronGaslessQuote(
            quoteId = "q_1",
            quotedAmountRaw = BigInteger("50000000"),
            quotedToAddress = "TTo",
            feeRecipient = "TFee",
            compensationToken = USDT_CONTRACT,
            compensationAmountRaw = BigInteger("2750000"),
            compensationAmountDecimal = COMPENSATION,
            energy = 78_000,
            bandwidth = 345,
            trxCost = NATIVE_FEE,
            expiresAtEpochMs = 1_780_489_496_000,
        )
    }
}