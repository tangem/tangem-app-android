package com.tangem.domain.transaction.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.walletmanager.WalletManagersFacade
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Unit tests for [GetFeeUseCase].
 *
 * Focus is the Yield Mode gas-limit logic introduced on this branch
 * (uncompiled Ethereum transactions whose call data is [EthereumYieldSupplySendCallData]
 * get their gas limit increased by 40%), plus error mapping, null/exception handling,
 * demo card routing, and crypto-currency-to-amount conversion in the second overload.
 */
class GetFeeUseCaseTest {

    private lateinit var walletManagersFacade: WalletManagersFacade
    private lateinit var demoConfig: DemoConfig
    private lateinit var useCase: GetFeeUseCase

    private lateinit var walletManager: WalletManager
    private lateinit var network: Network
    private lateinit var userWallet: UserWallet.Hot
    private lateinit var userWalletId: UserWalletId

    @Before
    fun setup() {
        walletManagersFacade = mockk()
        demoConfig = mockk()
        useCase = GetFeeUseCase(walletManagersFacade, demoConfig)

        walletManager = mockk()
        network = mockk()
        userWalletId = mockk()
        userWallet = mockk<UserWallet.Hot>()

        every { demoConfig.isDemoCardId(any()) } returns false
        every { userWallet.walletId } returns userWalletId
        coEvery {
            walletManagersFacade.getOrCreateWalletManager(userWalletId, network)
        } returns walletManager
    }

    // region invoke(userWallet, network, transactionData) — Yield Mode gas-limit logic

    @Test
    fun `yield supply uncompiled eth tx increases gas limit by 40 percent for Choosable fee`() = runTest {
        // Given
        val txData = yieldSupplyTransactionData()
        val original = TransactionFee.Choosable(
            minimum = eip1559Fee(gasLimit = BigInteger("10000"), value = BigDecimal("0.001")),
            normal = eip1559Fee(gasLimit = BigInteger("21000"), value = BigDecimal("0.0021")),
            priority = legacyFee(gasLimit = BigInteger("30000"), value = BigDecimal("0.003")),
        )
        coEvery { walletManager.getFee(txData) } returns Result.Success(original)

        // When
        val result = useCase(userWallet, network, txData)

        // Then
        assertThat(result.isRight()).isTrue()
        val fee = result.getOrNull().requireAs<TransactionFee.Choosable>()

        assertGasLimitIncreased(
            patched = fee.normal,
            expectedGasLimit = BigInteger("29400"), // 21000 * 140 / 100
            expectedValue = BigDecimal("0.00294"),
        )
        assertGasLimitIncreased(
            patched = fee.minimum,
            expectedGasLimit = BigInteger("14000"), // 10000 * 140 / 100
            expectedValue = BigDecimal("0.0014"),
        )
        assertGasLimitIncreased(
            patched = fee.priority,
            expectedGasLimit = BigInteger("42000"), // 30000 * 140 / 100
            expectedValue = BigDecimal("0.0042"),
        )
    }

    @Test
    fun `yield supply uncompiled eth tx increases gas limit by 40 percent for Single fee`() = runTest {
        // Given
        val txData = yieldSupplyTransactionData()
        val original = TransactionFee.Single(
            normal = eip1559Fee(gasLimit = BigInteger("21000"), value = BigDecimal("0.0021")),
        )
        coEvery { walletManager.getFee(txData) } returns Result.Success(original)

        // When
        val result = useCase(userWallet, network, txData)

        // Then
        assertThat(result.isRight()).isTrue()
        val fee = result.getOrNull().requireAs<TransactionFee.Single>()
        assertGasLimitIncreased(
            patched = fee.normal,
            expectedGasLimit = BigInteger("29400"),
            expectedValue = BigDecimal("0.00294"),
        )
    }

    @Test
    fun `compiled tx is returned unchanged even when fee is ethereum`() = runTest {
        // Given
        val txData = mockk<TransactionData.Compiled>()
        val original = TransactionFee.Single(
            normal = eip1559Fee(gasLimit = BigInteger("21000"), value = BigDecimal("0.0021")),
        )
        coEvery { walletManager.getFee(txData) } returns Result.Success(original)

        // When
        val result = useCase(userWallet, network, txData)

        // Then
        assertThat(result.getOrNull()).isEqualTo(original)
    }

    @Test
    fun `non ethereum extras returns fee unchanged`() = runTest {
        // Given
        val extras = mockk<TransactionExtras>()
        val txData = uncompiledTransactionData(extras)
        val original = TransactionFee.Single(
            normal = eip1559Fee(gasLimit = BigInteger("21000"), value = BigDecimal("0.0021")),
        )
        coEvery { walletManager.getFee(txData) } returns Result.Success(original)

        // When
        val result = useCase(userWallet, network, txData)

        // Then
        assertThat(result.getOrNull()).isEqualTo(original)
    }

    @Test
    fun `non yield supply call data returns fee unchanged`() = runTest {
        // Given
        val extras = EthereumTransactionExtras(callData = mockk<SmartContractCallData>())
        val txData = uncompiledTransactionData(extras)
        val original = TransactionFee.Single(
            normal = eip1559Fee(gasLimit = BigInteger("21000"), value = BigDecimal("0.0021")),
        )
        coEvery { walletManager.getFee(txData) } returns Result.Success(original)

        // When
        val result = useCase(userWallet, network, txData)

        // Then
        assertThat(result.getOrNull()).isEqualTo(original)
    }

    @Test
    fun `yield supply with non ethereum fee returns fee unchanged`() = runTest {
        // Given — call data matches, but the fee is not Fee.Ethereum
        val txData = yieldSupplyTransactionData()
        val nonEthFee = Fee.Common(amount = stubAmount(BigDecimal("0.5")))
        val original = TransactionFee.Single(normal = nonEthFee)
        coEvery { walletManager.getFee(txData) } returns Result.Success(original)

        // When
        val result = useCase(userWallet, network, txData)

        // Then — gas-limit logic is a no-op for non-Ethereum fees
        val fee = result.getOrNull().requireAs<TransactionFee.Single>()
        assertThat(fee.normal).isEqualTo(nonEthFee)
    }

    @Test
    fun `yield supply with token currency fee surfaces as DataError`() = runTest {
        // Given — increaseGasLimitBy throws for Fee.Ethereum.TokenCurrency (handled in [REDACTED_TASK_KEY])
        val txData = yieldSupplyTransactionData()
        val tokenFee = Fee.Ethereum.TokenCurrency(
            amount = stubAmount(BigDecimal("0.0021")),
            gasLimit = BigInteger("21000"),
            coinPriceInToken = BigInteger("1000"),
            feeTransferGasLimit = BigInteger("60000"),
            baseGas = BigInteger("21000"),
        )
        val original = TransactionFee.Single(normal = tokenFee)
        coEvery { walletManager.getFee(txData) } returns Result.Success(original)

        // When
        val result = useCase(userWallet, network, txData)

        // Then — the thrown error is caught and mapped to DataError
        assertThat(result.isLeft()).isTrue()
        assertThat(result.leftOrNull()).isInstanceOf(GetFeeError.DataError::class.java)
    }

    // endregion

    // region invoke(userWallet, network, transactionData) — error / null / exception handling

    @Test
    fun `result failure is mapped to fee error`() = runTest {
        // Given
        val txData = yieldSupplyTransactionData()
        val failure = Result.Failure(BlockchainSdkError.Tron.AccountActivationError(code = 1))
        coEvery { walletManager.getFee(txData) } returns failure

        // When
        val result = useCase(userWallet, network, txData)

        // Then
        assertThat(result.isLeft()).isTrue()
        assertThat(result.leftOrNull()).isEqualTo(GetFeeError.BlockchainErrors.TronActivationError)
    }

    @Test
    fun `null wallet manager produces DataError`() = runTest {
        // Given
        val txData = yieldSupplyTransactionData()
        coEvery { walletManagersFacade.getOrCreateWalletManager(userWalletId, network) } returns null

        // When
        val result = useCase(userWallet, network, txData)

        // Then
        assertThat(result.isLeft()).isTrue()
        val error = result.leftOrNull().requireAs<GetFeeError.DataError>()
        assertThat(error.cause?.message).isEqualTo("Fee is null")
    }

    @Test
    fun `exception in getFee produces DataError`() = runTest {
        // Given
        val txData = yieldSupplyTransactionData()
        val boom = IllegalStateException("boom")
        coEvery { walletManager.getFee(txData) } throws boom

        // When
        val result = useCase(userWallet, network, txData)

        // Then
        assertThat(result.isLeft()).isTrue()
        val error = result.leftOrNull().requireAs<GetFeeError.DataError>()
        assertThat(error.cause).isEqualTo(boom)
    }

    @Test
    fun `demo cold card routes through wallet manager for first overload`() = runTest {
        // Given
        val coldWallet = mockk<UserWallet.Cold>()
        every { coldWallet.walletId } returns userWalletId
        every { coldWallet.scanResponse.card.cardId } returns "DEMO_CARD"
        every { demoConfig.isDemoCardId("DEMO_CARD") } returns true

        // DemoTransactionSender may access walletManager.wallet.blockchain when producing stub fees
        val demoWallet = mockk<com.tangem.blockchain.common.Wallet>(relaxed = true)
        every { demoWallet.blockchain } returns com.tangem.blockchain.common.Blockchain.Ethereum
        every { walletManager.wallet } returns demoWallet

        val txData = yieldSupplyTransactionData()

        // When
        useCase(coldWallet, network, txData)

        // Then — demo sender is built from a wallet manager obtained via the facade
        coVerify { walletManagersFacade.getOrCreateWalletManager(userWalletId, network) }
    }

    // endregion

    // region invoke(amount, destination, userWallet, cryptoCurrency)

    @Test
    fun `second overload converts coin to amount and returns fee`() = runTest {
        // Given
        val coin = mockk<CryptoCurrency.Coin>()
        every { coin.network } returns network
        every { coin.symbol } returns "ETH"
        every { coin.decimals } returns 18

        val expectedFee = TransactionFee.Single(
            normal = eip1559Fee(gasLimit = BigInteger("21000"), value = BigDecimal("0.0021")),
        )
        val amountSlot = slot<Amount>()
        coEvery {
            walletManagersFacade.getFee(
                amount = capture(amountSlot),
                destination = "dest",
                userWalletId = userWalletId,
                network = network,
            )
        } returns Result.Success(expectedFee)

        // When
        val result = useCase.invoke(
            amount = BigDecimal("1.5"),
            destination = "dest",
            userWallet = userWallet,
            cryptoCurrency = coin,
        )

        // Then
        assertThat(result.getOrNull()).isEqualTo(expectedFee)
        val captured = amountSlot.captured
        assertThat(captured.type).isEqualTo(AmountType.Coin)
        assertThat(captured.currencySymbol).isEqualTo("ETH")
        assertThat(captured.decimals).isEqualTo(18)
        assertThat(captured.value).isEqualTo(BigDecimal("1.5"))
    }

    @Test
    fun `second overload converts token to amount with token type`() = runTest {
        // Given
        val token = mockk<CryptoCurrency.Token>()
        every { token.network } returns network
        every { token.symbol } returns "USDC"
        every { token.decimals } returns 6
        every { token.contractAddress } returns "0xUSDC"

        val expectedFee = TransactionFee.Single(
            normal = eip1559Fee(gasLimit = BigInteger("21000"), value = BigDecimal("0.0021")),
        )
        val amountSlot = slot<Amount>()
        coEvery {
            walletManagersFacade.getFee(
                amount = capture(amountSlot),
                destination = "dest",
                userWalletId = userWalletId,
                network = network,
            )
        } returns Result.Success(expectedFee)

        // When
        val result = useCase.invoke(
            amount = BigDecimal("100"),
            destination = "dest",
            userWallet = userWallet,
            cryptoCurrency = token,
        )

        // Then
        assertThat(result.getOrNull()).isEqualTo(expectedFee)
        val captured = amountSlot.captured
        val type = captured.type.requireAs<AmountType.Token>()
        assertThat(type.token.contractAddress).isEqualTo("0xUSDC")
        assertThat(type.token.symbol).isEqualTo("USDC")
        assertThat(type.token.decimals).isEqualTo(6)
    }

    @Test
    fun `second overload maps result failure to fee error`() = runTest {
        // Given
        val coin = mockk<CryptoCurrency.Coin>()
        every { coin.network } returns network
        every { coin.symbol } returns "KAS"
        every { coin.decimals } returns 8

        coEvery {
            walletManagersFacade.getFee(
                amount = any(),
                destination = any(),
                userWalletId = userWalletId,
                network = network,
            )
        } returns Result.Failure(BlockchainSdkError.Kaspa.ZeroUtxoError)

        // When
        val result = useCase.invoke(
            amount = BigDecimal("1"),
            destination = "dest",
            userWallet = userWallet,
            cryptoCurrency = coin,
        )

        // Then
        assertThat(result.leftOrNull()).isEqualTo(GetFeeError.BlockchainErrors.KaspaZeroUtxo)
    }

    @Test
    fun `second overload null fee produces DataError`() = runTest {
        // Given
        val coin = mockk<CryptoCurrency.Coin>()
        every { coin.network } returns network
        every { coin.symbol } returns "ETH"
        every { coin.decimals } returns 18

        coEvery {
            walletManagersFacade.getFee(
                amount = any(),
                destination = any(),
                userWalletId = userWalletId,
                network = network,
            )
        } returns null

        // When
        val result = useCase.invoke(
            amount = BigDecimal("1"),
            destination = "dest",
            userWallet = userWallet,
            cryptoCurrency = coin,
        )

        // Then
        val error = result.leftOrNull().requireAs<GetFeeError.DataError>()
        assertThat(error.cause?.message).isEqualTo("Fee is null")
    }

    // endregion

    // region helpers

    private fun yieldSupplyTransactionData(): TransactionData.Uncompiled {
        val callData = mockk<EthereumYieldSupplySendCallData>()
        return uncompiledTransactionData(EthereumTransactionExtras(callData = callData))
    }

    private fun uncompiledTransactionData(extras: TransactionExtras): TransactionData.Uncompiled {
        return TransactionData.Uncompiled(
            amount = stubAmount(BigDecimal.ONE),
            fee = null,
            sourceAddress = "src",
            destinationAddress = "dest",
            extras = extras,
        )
    }

    private fun eip1559Fee(gasLimit: BigInteger, value: BigDecimal): Fee.Ethereum.EIP1559 {
        return Fee.Ethereum.EIP1559(
            amount = stubAmount(value),
            gasLimit = gasLimit,
            maxFeePerGas = BigInteger("50000000000"),
            priorityFee = BigInteger("1000000000"),
        )
    }

    private fun legacyFee(gasLimit: BigInteger, value: BigDecimal): Fee.Ethereum.Legacy {
        return Fee.Ethereum.Legacy(
            amount = stubAmount(value),
            gasLimit = gasLimit,
            gasPrice = BigInteger("50000000000"),
        )
    }

    private fun stubAmount(value: BigDecimal): Amount = Amount(
        currencySymbol = "ETH",
        value = value,
        decimals = 18,
        type = AmountType.Coin,
    )

    private fun assertGasLimitIncreased(patched: Fee, expectedGasLimit: BigInteger, expectedValue: BigDecimal) {
        val eth = patched.requireAs<Fee.Ethereum>()
        assertThat(eth.gasLimit).isEqualTo(expectedGasLimit)
        val actualValue = requireNotNull(eth.amount.value) { "Fee amount value must not be null" }
        assertThat(actualValue.compareTo(expectedValue)).isEqualTo(0)
    }

    private inline fun <reified T> Any?.requireAs(): T {
        val value = this
        assertThat(value).isInstanceOf(T::class.java)
        return value as T
    }

    // endregion
}