package com.tangem.domain.transaction.usecase.gasless

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.transaction.GaslessYieldRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

internal class EvmGaslessFeeResolverNativeFeeTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val gaslessTransactionRepository: GaslessTransactionRepository = mockk()
    private val walletManager: EthereumWalletManager = mockk()

    private val userWalletId = UserWalletId(stringValue = "0123456789")
    private val userWallet: UserWallet = mockk<UserWallet.Hot> {
        every { walletId } returns userWalletId
    }

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val coin: CryptoCurrency.Coin = currencyFactory.createCoin(Blockchain.Ethereum)
    private val token: CryptoCurrency.Token = currencyFactory.createToken(Blockchain.Ethereum)
    private val nativeStatus = CryptoCurrencyStatus(
        currency = coin,
        value = mockk(relaxed = true) {
            every { amount } returns BigDecimal.TEN
            every { fiatRate } returns BigDecimal("2000")
        },
    )
    private val tokenStatus = CryptoCurrencyStatus(
        currency = token,
        value = mockk(relaxed = true) {
            every { amount } returns BigDecimal("1000000")
            every { fiatRate } returns BigDecimal.ONE
            every { yieldSupplyStatus } returns null
        },
    )
    private val accountStatusList: AccountStatusList = mockk {
        every { flattenPersonalCurrencies() } returns emptyList()
    }

    private val wrapped = wrappedYieldSendTransaction()
    private val bare = bareYieldSendTransaction()

    private val plain = uncompiledWithCallData(callData = null)

    private val bareFee = TransactionFee.Single(normal = ethereumFee(gasLimit = 100_000L, value = "0.1"))
    private val wrappedFee = TransactionFee.Single(normal = ethereumFee(gasLimit = 160_000L, value = "0.2"))
    private val plainFee = TransactionFee.Single(normal = ethereumFee(gasLimit = 65_000L, value = "0.065"))

    private val bareFeeWithReserve = TransactionFee.Single(
        normal = ethereumFee(gasLimit = 140_000L, value = "0.140000000000000000"),
    )
    private val wrappedFeeWithReserve = TransactionFee.Single(
        normal = ethereumFee(gasLimit = 224_000L, value = "0.280000000000000000"),
    )

    private val resolver = EvmGaslessFeeResolver(
        walletManagersFacade = walletManagersFacade,
        demoConfig = mockk<DemoConfig>(),
        gaslessTransactionRepository = gaslessTransactionRepository,
        gaslessYieldRepository = mockk<GaslessYieldRepository>(),
        resolveGaslessFeePlanUseCase = mockk(),
    )

    init {
        coEvery {
            walletManagersFacade.getOrCreateWalletManager(userWalletId = userWalletId, network = coin.network)
        } returns walletManager
        coEvery { walletManager.getFee(bare) } returns Result.Success(bareFee)
        coEvery { walletManager.getFee(wrapped) } returns Result.Success(wrappedFee)
        coEvery { walletManager.getFee(plain) } returns Result.Success(plainFee)
        coEvery { walletManager.getGasLimit(any(), any(), any()) } returns Result.Success(BigInteger("60000"))
        coEvery { gaslessTransactionRepository.getTokenFeeReceiverAddress() } returns "0xFeeReceiver"
        every { gaslessTransactionRepository.getBaseGasForTransaction() } returns BigInteger("21000")
    }

    @Test
    fun `GIVEN native coin covers fee WHEN resolveWithAutoFeeToken THEN fee is estimated on the original transaction`() =
        runTest {
            // Arrange
            val expected = wrappedFeeWithReserve

            // Act
            val actual = resolver.resolveWithAutoFeeToken(
                userWallet = userWallet,
                network = coin.network,
                transactionData = bare,
                nativeCurrencyStatus = nativeStatus,
                accountStatusList = accountStatusList,
                nativeTransactionData = wrapped,
            )

            // Assert
            val fee = actual.getOrNull()
            assertThat(fee?.transactionFee).isEqualTo(expected)
            assertThat(fee?.nativeFee).isEqualTo(expected)
        }

    @Test
    fun `GIVEN selected fee token WHEN resolveWithSelectedFeeToken THEN token fee is based on bare and native fee on original`() =
        runTest {
            // Arrange
            val expectedMainGasLimit = BigInteger.valueOf(140_000L)

            // Act
            val actual = resolver.resolveWithSelectedFeeToken(
                userWallet = userWallet,
                network = coin.network,
                transactionData = bare,
                nativeCurrencyStatus = nativeStatus,
                tokenCurrencyStatus = tokenStatus,
                nativeTransactionData = wrapped,
            )

            // Assert
            val fee = actual.getOrNull()
            assertThat(fee?.nativeFee).isEqualTo(wrappedFeeWithReserve)
            assertThat(fee?.callGasLimits).isEqualTo(listOf(expectedMainGasLimit))
        }

    @Test
    fun `GIVEN same transaction for both WHEN resolveWithAutoFeeToken THEN fee is estimated once`() = runTest {
        // Arrange
        val expected = bareFeeWithReserve.right()

        // Act
        val actual = resolver.resolveWithAutoFeeToken(
            userWallet = userWallet,
            network = coin.network,
            transactionData = bare,
            nativeCurrencyStatus = nativeStatus,
            accountStatusList = accountStatusList,
            nativeTransactionData = bare,
        )

        // Assert
        assertThat(actual.map { it.transactionFee }).isEqualTo(expected)
        coVerify(exactly = 1) { walletManager.getFee(bare) }
    }

    @Test
    fun `GIVEN same transaction for both WHEN resolveWithSelectedFeeToken THEN fee is estimated once`() = runTest {
        // Act
        val actual = resolver.resolveWithSelectedFeeToken(
            userWallet = userWallet,
            network = coin.network,
            transactionData = bare,
            nativeCurrencyStatus = nativeStatus,
            tokenCurrencyStatus = tokenStatus,
            nativeTransactionData = bare,
        )

        // Assert
        assertThat(actual.getOrNull()?.nativeFee).isEqualTo(bareFeeWithReserve)
        coVerify(exactly = 1) { walletManager.getFee(bare) }
    }

    @Test
    fun `GIVEN not a yield send WHEN resolveWithAutoFeeToken THEN native fee is the bare estimate`() = runTest {
        // Arrange
        val expected = plainFee.right()

        // Act
        val actual = resolver.resolveWithAutoFeeToken(
            userWallet = userWallet,
            network = coin.network,
            transactionData = plain,
            nativeCurrencyStatus = nativeStatus,
            accountStatusList = accountStatusList,
            nativeTransactionData = plain,
        )

        // Assert
        assertThat(actual.map { it.nativeFee }).isEqualTo(expected)
    }

    private fun ethereumFee(gasLimit: Long, value: String): Fee.Ethereum.EIP1559 {
        return Fee.Ethereum.EIP1559(
            amount = Amount(blockchain = Blockchain.Ethereum, value = BigDecimal(value)),
            gasLimit = BigInteger.valueOf(gasLimit),
            maxFeePerGas = BigInteger.valueOf(10L),
            priorityFee = BigInteger.ONE,
        )
    }
}