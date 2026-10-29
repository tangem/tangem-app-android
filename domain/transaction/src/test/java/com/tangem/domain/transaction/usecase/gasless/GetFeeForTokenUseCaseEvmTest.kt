package com.tangem.domain.transaction.usecase.gasless

import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.provider.NonPortfolioCurrenciesProvider
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.transaction.models.TransactionFeeExtended
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class GetFeeForTokenUseCaseEvmTest {

    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val nonPortfolioCurrenciesProvider: NonPortfolioCurrenciesProvider = mockk()
    private val currencyChecksRepository: CurrencyChecksRepository = mockk()
    private val evmGaslessFeeResolver: EvmGaslessFeeResolver = mockk()
    private val tronGaslessFeeResolver: TronGaslessFeeResolver = mockk()
    private val accountStatusList: AccountStatusList = mockk(relaxed = true)

    private val userWalletId = UserWalletId(stringValue = "0123456789")
    private val userWallet: UserWallet = mockk {
        every { walletId } returns userWalletId
    }

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val coin: CryptoCurrency.Coin = currencyFactory.createCoin(Blockchain.Ethereum)
    private val token: CryptoCurrency.Token = currencyFactory.createToken(Blockchain.Ethereum)
    private val nativeStatus = CryptoCurrencyStatus(currency = coin, value = mockk(relaxed = true))
    private val tokenStatus = CryptoCurrencyStatus(currency = token, value = mockk(relaxed = true))

    private val useCase = GetFeeForTokenUseCase(
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        nonPortfolioCurrenciesProvider = nonPortfolioCurrenciesProvider,
        currencyChecksRepository = currencyChecksRepository,
        evmGaslessFeeResolver = evmGaslessFeeResolver,
        tronGaslessFeeResolver = tronGaslessFeeResolver,
    )

    @BeforeEach
    fun setUp() {
        mockkObject(CryptoCurrencyStatusOperations)
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns accountStatusList
        every { currencyChecksRepository.isNetworkSupportedForGaslessTx(token.network) } returns true
        coEvery {
            tronGaslessFeeResolver.resolveWithSelectedFeeToken(userWalletId, token, any())
        } returns null
        with(CryptoCurrencyStatusOperations) {
            every { accountStatusList.getCoinStatus(token.network) } returns nativeStatus.some()
            every { accountStatusList.getCryptoCurrencyStatus(currency = token) } returns tokenStatus.some()
        }
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(CryptoCurrencyStatusOperations)
    }

    @Test
    fun `GIVEN upgrade-wrapped yield send WHEN invoke THEN EVM resolver gets bare yield send`() = runTest {
        // Arrange
        val wrapped = wrappedYieldSendTransaction()
        val fee = Fee.Common(amount = Amount(blockchain = Blockchain.Ethereum, value = BigDecimal.ONE))
        val expected = TransactionFeeExtended(
            transactionFee = TransactionFee.Single(normal = fee),
            feeTokenId = token.id,
        )
        coEvery {
            evmGaslessFeeResolver.resolveWithSelectedFeeToken(
                userWallet = userWallet,
                network = token.network,
                transactionData = bareYieldSendTransaction(),
                nativeCurrencyStatus = nativeStatus,
                tokenCurrencyStatus = tokenStatus,
                nativeTransactionData = wrapped,
                sentAmount = null,
            )
        } returns expected.right()

        // Act
        val actual = useCase(userWallet = userWallet, token = token, transactionData = wrapped)

        // Assert
        assertThat(actual).isEqualTo(expected.right())
    }
}