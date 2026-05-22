package com.tangem.feature.swap.domain.fee

import android.util.Base64
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.solana.SolanaTransactionHelper
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.CreateTransactionDataExtrasUseCase
import com.tangem.domain.transaction.usecase.GetEthSpecificFeeUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForTokenUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.feature.swap.domain.buildSwapCurrencyStatus
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Unit tests for [DexSwapFeeCalculator] ([REDACTED_TASK_KEY] — Phase 2).
 *
 * Mirrors the cases from `SwapInteractorImplLoadFeeForDexTest` and
 * `SwapInteractorImplOtherNativeFeeTest` but exercises the calculator directly with a
 * minimal set of mocks instead of going through the public `findBestQuote` entry point.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DexSwapFeeCalculatorTest {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()
    private val solanaNetwork = Blockchain.Solana.toNetworkId()

    private val getFeeUseCase: GetFeeUseCase = mockk(relaxed = true)
    private val getEthSpecificFeeUseCase: GetEthSpecificFeeUseCase = mockk(relaxed = true)
    private val getFeeForTokenUseCase: GetFeeForTokenUseCase = mockk(relaxed = true)
    private val createTransactionExtrasUseCase: CreateTransactionDataExtrasUseCase = mockk(relaxed = true)
    private val walletManagersFacade: WalletManagersFacade = mockk(relaxed = true)

    private val dexBump = PatchEthGasLimitForSwap(percentage = PatchEthGasLimitForSwap.DEX_PERCENTAGE)

    private val sut: DexSwapFeeCalculator by lazy {
        DexSwapFeeCalculator(
            getFeeUseCase = getFeeUseCase,
            getEthSpecificFeeUseCase = getEthSpecificFeeUseCase,
            getFeeForTokenUseCase = getFeeForTokenUseCase,
            createTransactionExtrasUseCase = createTransactionExtrasUseCase,
            walletManagersFacade = walletManagersFacade,
            patchEthGasLimitForSwap = dexBump,
        )
    }

    @BeforeEach
    fun setup() {
        // Default: native balance is plenty.
        coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal("10")
        every { createTransactionExtrasUseCase.invoke(data = any(), network = any()) } returns
            mockk<TransactionExtras>(relaxed = true).right()
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    // -------------------------------------------------------------------------
    // EVM happy path
    // -------------------------------------------------------------------------

    @Test
    fun `EVM DEX swap propagates extras destinationAddress sourceAddress and amount to getFeeUseCase`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val transaction = buildDex(
            txValue = "1000000000000000", // 0.001 ETH
            txTo = "0xRecipient",
            txFrom = "0xSender",
            txData = "0xPayload",
        )
        val capturedTxData = slot<TransactionData>()
        coEvery {
            getFeeUseCase.invoke(
                userWallet = any(),
                network = any(),
                transactionData = capture(capturedTxData),
            )
        } returns mockk<TransactionFee.Single>(relaxed = true).right()

        sut.calculate(fromStatus, transaction)

        assertThat(capturedTxData.isCaptured).isTrue()
        val uncompiled = capturedTxData.captured as TransactionData.Uncompiled
        assertThat(uncompiled.destinationAddress).isEqualTo("0xRecipient")
        assertThat(uncompiled.sourceAddress).isEqualTo("0xSender")
        // amount.value is the txValue moved-point-left by native decimals (18 for ETH) → 0.001
        assertThat(uncompiled.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.001"))
        // extras came from createTransactionExtrasUseCase
        assertThat(uncompiled.extras).isNotNull()
    }

    // -------------------------------------------------------------------------
    // EVM zero-balance short-circuit
    // -------------------------------------------------------------------------

    @Test
    fun `EVM DEX swap with native balance ZERO returns Left UnknownError`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val transaction = buildDex(txValue = "0")
        coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal.ZERO

        val result = sut.calculate(fromStatus, transaction)

        assertThat(result.isLeft()).isTrue()
        result.onLeft { assertThat(it).isEqualTo(ExpressDataError.UnknownError()) }
        // getFeeUseCase should not have been called because balance check short-circuits first.
        // Use a more permissive verify to avoid clashing with the other overload signatures.
        coVerify(exactly = 0) {
            getFeeUseCase.invoke(
                userWallet = any(),
                network = any(),
                transactionData = any<TransactionData>(),
            )
        }
    }

    // -------------------------------------------------------------------------
    // EVM IllegalStateException → fallback to GetEthSpecificFeeUseCase
    // -------------------------------------------------------------------------

    @Test
    fun `EVM DEX swap falls back to getEthSpecificFeeUseCase when txValue is null`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val gas = BigInteger.valueOf(150_000L)
        val transaction = buildDex(txValue = null, gas = gas)

        coEvery {
            getEthSpecificFeeUseCase.invoke(
                userWallet = any(),
                cryptoCurrency = any(),
                gasLimit = any(),
                gasPrice = any(),
            )
        } returns mockk<TransactionFee.Choosable>(relaxed = true).right()

        sut.calculate(fromStatus, transaction)

        coVerify(exactly = 1) {
            getEthSpecificFeeUseCase.invoke(
                userWallet = any(),
                cryptoCurrency = any(),
                gasLimit = gas,
                gasPrice = any(),
            )
        }
    }

    @Test
    fun `EVM DEX swap falls back to getEthSpecificFeeUseCase when createTransactionExtrasUseCase fails`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val gas = BigInteger.valueOf(75_000L)
        val transaction = buildDex(txValue = "1000000000000000", gas = gas)

        every {
            createTransactionExtrasUseCase.invoke(data = any(), network = any())
        } returns IllegalStateException("forced fail").left()

        coEvery {
            getEthSpecificFeeUseCase.invoke(
                userWallet = any(),
                cryptoCurrency = any(),
                gasLimit = any(),
                gasPrice = any(),
            )
        } returns mockk<TransactionFee.Choosable>(relaxed = true).right()

        sut.calculate(fromStatus, transaction)

        coVerify(exactly = 1) {
            getEthSpecificFeeUseCase.invoke(
                userWallet = any(),
                cryptoCurrency = any(),
                gasLimit = gas,
                gasPrice = any(),
            )
        }
    }

    @Test
    fun `EVM DEX swap falls back to getEthSpecificFeeUseCase when getFeeUseCase returns Left`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val gas = BigInteger.valueOf(50_000L)
        val transaction = buildDex(txValue = "1000000000000000", gas = gas)

        coEvery {
            getFeeUseCase.invoke(userWallet = any(), network = any(), transactionData = any())
        } returns GetFeeError.UnknownError.left()

        coEvery {
            getEthSpecificFeeUseCase.invoke(
                userWallet = any(),
                cryptoCurrency = any(),
                gasLimit = any(),
                gasPrice = any(),
            )
        } returns mockk<TransactionFee.Choosable>(relaxed = true).right()

        sut.calculate(fromStatus, transaction)

        coVerify(exactly = 1) {
            getEthSpecificFeeUseCase.invoke(
                userWallet = any(),
                cryptoCurrency = any(),
                gasLimit = gas,
                gasPrice = any(),
            )
        }
    }

    // -------------------------------------------------------------------------
    // 12% gas patch — golden numbers
    // -------------------------------------------------------------------------

    @Test
    fun `EVM DEX swap applies 12 percent gas-limit bump on Ethereum Legacy fee`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val transaction = buildDex(txValue = "1000000000000000")

        // amount = 100_000 * 20e9 / 1e18 = 0.000002 ETH (decimals = 18)
        val rawFee = Fee.Ethereum.Legacy(
            amount = Amount(currencySymbol = "ETH", value = BigDecimal("0.000002"), decimals = 18),
            gasLimit = BigInteger.valueOf(100_000),
            gasPrice = BigInteger.valueOf(20_000_000_000),
        )
        coEvery {
            getFeeUseCase.invoke(userWallet = any(), network = any(), transactionData = any())
        } returns TransactionFee.Single(normal = rawFee).right()

        val result = sut.calculate(fromStatus, transaction)

        assertThat(result.isRight()).isTrue()
        result.onRight { dexFeeResult ->
            val patched = (dexFeeResult.transactionFee as TransactionFeeResult.Loaded).fee
            val patchedFee = (patched as TransactionFee.Single).normal as Fee.Ethereum.Legacy
            // 100_000 * 112 / 100 = 112_000
            assertThat(patchedFee.gasLimit).isEqualTo(BigInteger.valueOf(112_000))
            // 112_000 * 20_000_000_000 / 1e18 = 0.00000224
            assertThat(patchedFee.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.00000224"))
            // Gas is propagated for downstream consumers
            assertThat(dexFeeResult.gas).isEqualTo(transaction.gas)
        }
    }

    // -------------------------------------------------------------------------
    // Solana DEX path
    // -------------------------------------------------------------------------

    @Test
    fun `Solana DEX uses TransactionData Compiled and skips the 12 percent gas patch`() = runTest {
        mockkStatic(Base64::class)
        every { Base64.decode(any<String>(), any()) } returns ByteArray(64)
        mockkObject(SolanaTransactionHelper)
        every { SolanaTransactionHelper.removeSignaturesPlaceholders(any()) } returns ByteArray(64)

        val fromStatus = buildSwapCurrencyStatus(networkRawId = solanaNetwork, isCoin = true)
        val transaction = buildDex(txData = "U29sYW5h")

        val rawFeeAmount = BigDecimal("0.005000")
        val rawFee: Fee = Fee.Common(
            amount = Amount(currencySymbol = "SOL", value = rawFeeAmount, decimals = 9),
        )
        val txFee = TransactionFee.Single(normal = rawFee)
        val capturedTxData = slot<TransactionData>()
        coEvery {
            getFeeUseCase.invoke(
                userWallet = any(),
                network = any(),
                transactionData = capture(capturedTxData),
            )
        } returns txFee.right()

        val result = sut.calculate(fromStatus, transaction)

        assertThat(capturedTxData.isCaptured).isTrue()
        assertThat(capturedTxData.captured).isInstanceOf(TransactionData.Compiled::class.java)
        result.onRight { dexFeeResult ->
            // No bump: Fee.Common is non-Ethereum even on the EVM path; on Solana the bump isn't
            // applied at all. The raw value is preserved.
            val patched = (dexFeeResult.transactionFee as TransactionFeeResult.Loaded).fee
            val solFee = (patched as TransactionFee.Single).normal as Fee.Common
            assertThat(solFee.amount.value).isEquivalentAccordingToCompareTo(rawFeeAmount)
            // Solana path leaves gas null (caller doesn't need it).
            assertThat(dexFeeResult.gas).isNull()
        }
    }

    @Test
    fun `Solana DEX size guard returns Left TooLargeSolanaTransactionError on Cold wallet`() = runTest {
        mockkStatic(Base64::class)
        val oversizedBytes = ByteArray(1300)
        every { Base64.decode(any<String>(), any()) } returns oversizedBytes
        mockkObject(SolanaTransactionHelper)
        every { SolanaTransactionHelper.removeSignaturesPlaceholders(any()) } returns oversizedBytes

        val coldWallet = mockk<UserWallet.Cold>(relaxed = true)
        val baseStatus = buildSwapCurrencyStatus(networkRawId = solanaNetwork, isCoin = true)
        val fromStatus = SwapCurrencyStatus(
            userWallet = coldWallet,
            status = baseStatus.status,
            account = baseStatus.account,
        )
        val transaction = buildDex(txData = "very-long-base64-content==")

        val result = sut.calculate(fromStatus, transaction)

        assertThat(result.isLeft()).isTrue()
        result.onLeft { error ->
            assertThat(error).isEqualTo(ExpressDataError.TooLargeSolanaTransactionError())
        }
        // No fee is computed when the size guard trips
        coVerify(exactly = 0) {
            getFeeUseCase.invoke(userWallet = any(), network = any(), transactionData = any())
        }
    }

    // -------------------------------------------------------------------------
    // otherNativeFee propagation (bridge protocol fee)
    // -------------------------------------------------------------------------

    @Test
    fun `EVM DEX swap propagates otherNativeFee with native decimals when otherNativeFeeWei is set`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        // 0.5 ETH expressed in wei (1e18)
        val transaction = buildDex(
            txValue = "1000000000000000",
            otherNativeFeeWei = BigDecimal("500000000000000000"),
        )
        coEvery {
            getFeeUseCase.invoke(userWallet = any(), network = any(), transactionData = any())
        } returns TransactionFee.Single(normal = ethLegacyFee()).right()

        val result = sut.calculate(fromStatus, transaction)

        result.onRight { dexFeeResult ->
            assertThat(dexFeeResult.otherNativeFee).isEquivalentAccordingToCompareTo(BigDecimal("0.5"))
        }
    }

    @Test
    fun `EVM DEX swap returns ZERO otherNativeFee when otherNativeFeeWei is null`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val transaction = buildDex(txValue = "1000000000000000", otherNativeFeeWei = null)
        coEvery {
            getFeeUseCase.invoke(userWallet = any(), network = any(), transactionData = any())
        } returns TransactionFee.Single(normal = ethLegacyFee()).right()

        val result = sut.calculate(fromStatus, transaction)

        result.onRight { dexFeeResult ->
            assertThat(dexFeeResult.otherNativeFee).isEqualTo(BigDecimal.ZERO)
        }
    }

    @Test
    fun `Solana DEX swap propagates otherNativeFee using native decimals`() = runTest {
        mockkStatic(Base64::class)
        every { Base64.decode(any<String>(), any()) } returns ByteArray(64)
        mockkObject(SolanaTransactionHelper)
        every { SolanaTransactionHelper.removeSignaturesPlaceholders(any()) } returns ByteArray(64)

        val fromStatus = buildSwapCurrencyStatus(networkRawId = solanaNetwork, isCoin = true, decimals = 9)
        // 1.5 SOL expressed with 9 decimals = 1_500_000_000
        val transaction = buildDex(
            txData = "U29sYW5h",
            otherNativeFeeWei = BigDecimal("1500000000"),
        )
        coEvery {
            getFeeUseCase.invoke(userWallet = any(), network = any(), transactionData = any())
        } returns TransactionFee.Single(
            normal = Fee.Common(Amount(currencySymbol = "SOL", value = BigDecimal("0.005"), decimals = 9)),
        ).right()

        val result = sut.calculate(fromStatus, transaction)

        result.onRight { dexFeeResult ->
            assertThat(dexFeeResult.otherNativeFee).isEquivalentAccordingToCompareTo(BigDecimal("1.5"))
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun ethLegacyFee(): Fee.Ethereum.Legacy = Fee.Ethereum.Legacy(
        amount = Amount(currencySymbol = "ETH", value = BigDecimal("0.000002"), decimals = 18),
        gasLimit = BigInteger.valueOf(100_000),
        gasPrice = BigInteger.valueOf(20_000_000_000),
    )

    private fun buildDex(
        txData: String = "dGVzdA==",
        txValue: String? = "0",
        toAmount: BigDecimal = BigDecimal("0.5"),
        otherNativeFeeWei: BigDecimal? = null,
        gas: BigInteger = BigInteger.valueOf(21_000L),
        txTo: String = "0xRecipient",
        txFrom: String = "0xSender",
    ): ExpressTransactionModel.DEX = ExpressTransactionModel.DEX(
        fromAmount = SwapAmount(BigDecimal.ONE, 18),
        toAmount = SwapAmount(toAmount, 18),
        txValue = txValue,
        txId = "tx-id-123",
        txTo = txTo,
        txExtraId = null,
        txFrom = txFrom,
        txData = txData,
        otherNativeFeeWei = otherNativeFeeWei,
        gas = gas,
    )
}