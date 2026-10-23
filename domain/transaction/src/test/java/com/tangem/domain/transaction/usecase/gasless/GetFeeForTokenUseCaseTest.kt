package com.tangem.domain.transaction.usecase.gasless

import arrow.core.none
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.provider.NonPortfolioCurrenciesProvider
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
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

internal class GetFeeForTokenUseCaseTest {

    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val nonPortfolioCurrenciesProvider: NonPortfolioCurrenciesProvider = mockk()
    private val currencyChecksRepository: CurrencyChecksRepository = mockk()
    private val evmGaslessFeeResolver: EvmGaslessFeeResolver = mockk()
    private val tronGaslessFeeResolver: TronGaslessFeeResolver = mockk()

    private val accountStatusList: AccountStatusList = mockk()
    private val userWalletId = UserWalletId(stringValue = "0123456789")
    private val userWallet: UserWallet = mockk {
        every { walletId } returns userWalletId
    }

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val accountUsdc = currencyFactory.createToken(blockchain = Blockchain.Polygon)
    private val accountNetwork = accountUsdc.network
    private val accountCoinStatus = CryptoCurrencyStatus(
        currency = currencyFactory.createCoin(Blockchain.Polygon),
        value = mockk(relaxed = true),
    )
    private val accountUsdcStatus = CryptoCurrencyStatus(currency = accountUsdc, value = mockk(relaxed = true))

    private val transactionData: TransactionData = mockk()

    private val sut = GetFeeForTokenUseCase(
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        nonPortfolioCurrenciesProvider = nonPortfolioCurrenciesProvider,
        currencyChecksRepository = currencyChecksRepository,
        evmGaslessFeeResolver = evmGaslessFeeResolver,
        tronGaslessFeeResolver = tronGaslessFeeResolver,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(
            singleAccountStatusListSupplier,
            nonPortfolioCurrenciesProvider,
            currencyChecksRepository,
            evmGaslessFeeResolver,
            tronGaslessFeeResolver,
        )
        mockkObject(CryptoCurrencyStatusOperations)
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns accountStatusList
        with(CryptoCurrencyStatusOperations) {
            every { accountStatusList.getCoinStatus(accountNetwork) } returns none()
            every { accountStatusList.getCryptoCurrencyStatus(currency = accountUsdc) } returns none()
        }
        every { currencyChecksRepository.isNetworkSupportedForGaslessTx(accountNetwork) } returns true
        coEvery {
            tronGaslessFeeResolver.resolveWithSelectedFeeToken(userWalletId, accountUsdc, transactionData)
        } returns null
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(CryptoCurrencyStatusOperations)
    }

    @Test
    fun `GIVEN token of a non-portfolio account picked WHEN invoke THEN the fee is quoted with its coin and token`() =
        runTest {
            // Arrange
            val tokenFee = TransactionFee.Single(
                normal = Fee.Common(amount = Amount(blockchain = Blockchain.Polygon, value = BigDecimal.ONE)),
            )
            val expected = TransactionFeeExtended(transactionFee = tokenFee, feeTokenId = accountUsdc.id)
            coEvery {
                nonPortfolioCurrenciesProvider.getStatuses(userWalletId, accountNetwork)
            } returns listOf(accountCoinStatus, accountUsdcStatus)
            coEvery {
                evmGaslessFeeResolver.resolveWithSelectedFeeToken(
                    userWallet = userWallet,
                    network = accountNetwork,
                    transactionData = transactionData,
                    nativeCurrencyStatus = accountCoinStatus,
                    tokenCurrencyStatus = accountUsdcStatus,
                    sentAmount = null,
                )
            } returns expected.right()

            // Act
            val actual = sut(userWallet = userWallet, token = accountUsdc, transactionData = transactionData)

            // Assert
            assertThat(actual).isEqualTo(expected.right())
        }

    @Test
    fun `GIVEN no account holds the token network WHEN invoke THEN data error`() = runTest {
        // Arrange
        coEvery { nonPortfolioCurrenciesProvider.getStatuses(userWalletId, accountNetwork) } returns emptyList()

        // Act
        val actual = sut(userWallet = userWallet, token = accountUsdc, transactionData = transactionData)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(GetFeeError.DataError::class.java)
        verify { evmGaslessFeeResolver wasNot Called }
    }
}