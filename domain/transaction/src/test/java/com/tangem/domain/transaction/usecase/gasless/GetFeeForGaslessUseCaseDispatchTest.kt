package com.tangem.domain.transaction.usecase.gasless

import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
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
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.GetFeeUseCase
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

/** Locks which resolver runs, not what it computes. */
internal class GetFeeForGaslessUseCaseDispatchTest {

    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk(relaxed = true)
    private val currencyChecksRepository: CurrencyChecksRepository = mockk()
    private val getFeeUseCase: GetFeeUseCase = mockk()
    private val evmGaslessFeeResolver: EvmGaslessFeeResolver = mockk()
    private val tronGaslessFeeResolver: TronGaslessFeeResolver = mockk()

    private val accountStatusList: AccountStatusList = mockk(relaxed = true)
    private val userWalletId = UserWalletId(stringValue = "0123456789")
    private val userWallet: UserWallet = mockk {
        every { walletId } returns userWalletId
    }

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val coin: CryptoCurrency.Coin = currencyFactory.createCoin(Blockchain.Ethereum)
    private val network: Network = coin.network
    private val nativeStatus = CryptoCurrencyStatus(
        currency = coin,
        value = mockk(relaxed = true) { every { amount } returns BigDecimal.TEN },
    )

    private val tronCoin: CryptoCurrency.Coin = currencyFactory.createCoin(Blockchain.Tron)
    private val tronNetwork: Network = tronCoin.network
    private val tronNativeStatus = CryptoCurrencyStatus(
        currency = tronCoin,
        value = mockk(relaxed = true) { every { amount } returns BigDecimal.TEN },
    )

    private val transactionData: TransactionData = mockk()

    private val nativeFee = TransactionFee.Single(
        normal = Fee.Common(amount = Amount(blockchain = Blockchain.Ethereum, value = BigDecimal.ONE)),
    )

    private val sut = GetFeeForGaslessUseCase(
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        currencyChecksRepository = currencyChecksRepository,
        getFeeUseCase = getFeeUseCase,
        evmGaslessFeeResolver = evmGaslessFeeResolver,
        tronGaslessFeeResolver = tronGaslessFeeResolver,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(currencyChecksRepository, getFeeUseCase, evmGaslessFeeResolver, tronGaslessFeeResolver)
        mockkObject(CryptoCurrencyStatusOperations)
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns accountStatusList
        with(CryptoCurrencyStatusOperations) {
            every { accountStatusList.getCoinStatus(network) } returns nativeStatus.some()
            every { accountStatusList.getCoinStatus(tronNetwork) } returns tronNativeStatus.some()
        }
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(CryptoCurrencyStatusOperations)
    }

    @Test
    fun `GIVEN an EVM gasless network WHEN invoke THEN the EVM resolver decides and nothing is pinned`() = runTest {
        // Arrange
        var nativeFeePreferredCalls = 0
        val expected = TransactionFeeExtended(transactionFee = nativeFee, feeTokenId = coin.id, nativeFee = nativeFee)
        every { currencyChecksRepository.isNetworkSupportedForGaslessTx(network) } returns true
        coEvery {
            evmGaslessFeeResolver.resolveWithAutoFeeToken(
                userWallet = userWallet,
                network = network,
                transactionData = transactionData,
                nativeCurrencyStatus = nativeStatus,
                accountStatusList = accountStatusList,
                sentAmount = null,
            )
        } returns expected.right()

        // Act
        val actual = sut(
            userWallet = userWallet,
            network = network,
            transactionData = transactionData,
            onNativeFeePreferred = { nativeFeePreferredCalls++ },
        )

        // Assert
        assertThat(actual).isEqualTo(expected.right())
        assertThat(nativeFeePreferredCalls).isEqualTo(0)
        verify { tronGaslessFeeResolver wasNot Called }
        verify { getFeeUseCase wasNot Called }
    }

    @Test
    fun `GIVEN a network the gasless service does not cover WHEN invoke THEN the native fee is offered to Tron`() =
        runTest {
            // Arrange
            every { currencyChecksRepository.isNetworkSupportedForGaslessTx(network) } returns false
            coEvery { getFeeUseCase.invoke(userWallet, network, transactionData) } returns nativeFee.right()
            coEvery {
                tronGaslessFeeResolver.resolveWithAutoFeeToken(
                    userWalletId = userWalletId,
                    network = network,
                    transactionData = transactionData,
                    nativeCurrencyStatus = nativeStatus,
                    accountStatusList = accountStatusList,
                    loadNativeFee = any(),
                    onNativeFeePreferred = any(),
                )
            } returns null

            // Act
            val actual = sut(userWallet, network, transactionData)

            // Assert — Tron declined, so the plain native fee stands.
            assertThat(actual).isEqualTo(
                TransactionFeeExtended(transactionFee = nativeFee, feeTokenId = coin.id, nativeFee = nativeFee).right(),
            )
            verify { evmGaslessFeeResolver wasNot Called }
        }

    @Test
    fun `GIVEN the gasless service claims a non-EVM network WHEN invoke THEN the EVM resolver stays out`() = runTest {
        // Arrange — the service's network list is remote configuration and may name a non-EVM chain.
        val expected = TransactionFeeExtended(transactionFee = nativeFee, feeTokenId = tronCoin.id)
        every { currencyChecksRepository.isNetworkSupportedForGaslessTx(tronNetwork) } returns true
        coEvery {
            tronGaslessFeeResolver.resolveWithAutoFeeToken(
                userWalletId = userWalletId,
                network = tronNetwork,
                transactionData = transactionData,
                nativeCurrencyStatus = tronNativeStatus,
                accountStatusList = accountStatusList,
                loadNativeFee = any(),
                onNativeFeePreferred = any(),
            )
        } returns expected.right()

        // Act
        val actual = sut(userWallet, tronNetwork, transactionData)

        // Assert
        assertThat(actual).isEqualTo(expected.right())
        verify { evmGaslessFeeResolver wasNot Called }
        verify { getFeeUseCase wasNot Called }
    }
}