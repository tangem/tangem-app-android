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
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.CreateTransactionDataExtrasUseCase
import com.tangem.domain.transaction.usecase.GetEthSpecificFeeUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForTokenUseCase
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.yield.supply.usecase.WrapYieldSwapCallDataWithUpgradeUseCase
import com.tangem.feature.swap.domain.buildSwapCurrencyStatus
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import io.mockk.*
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
    private val wrapYieldSwapCallDataWithUpgradeUseCase: WrapYieldSwapCallDataWithUpgradeUseCase = mockk(relaxed = true)

    private val dexBump = PatchEthGasLimitForSwap(percentage = PatchEthGasLimitForSwap.DEX_PERCENTAGE)

    private val sut: DexSwapFeeCalculator by lazy {
        DexSwapFeeCalculator(
            getFeeUseCase = getFeeUseCase,
            getEthSpecificFeeUseCase = getEthSpecificFeeUseCase,
            getFeeForTokenUseCase = getFeeForTokenUseCase,
            createTransactionExtrasUseCase = createTransactionExtrasUseCase,
            walletManagersFacade = walletManagersFacade,
            patchEthGasLimitForSwap = dexBump,
            wrapYieldSwapCallDataWithUpgradeUseCase = wrapYieldSwapCallDataWithUpgradeUseCase,
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
        result.onLeft { assertThat(it).isEqualTo(GetFeeError.UnknownError) }
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

    @Test
    fun `EVM DEX swap raises UnknownError when getFeeUseCase fails and transaction gas is null`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val transaction = buildDex(txValue = "1000000000000000", gas = null)

        // Force ISE in the main path so we enter the gas-fallback branch.
        coEvery {
            getFeeUseCase.invoke(userWallet = any(), network = any(), transactionData = any())
        } returns GetFeeError.UnknownError.left()

        val result = sut.calculate(fromStatus, transaction)

        assertThat(result.isLeft()).isTrue()
        result.onLeft { error ->
            assertThat(error).isEqualTo(GetFeeError.UnknownError)
        }
        // Fallback use-case must NOT be invoked when gas is null — there's nothing to feed it.
        coVerify(exactly = 0) {
            getEthSpecificFeeUseCase.invoke(
                userWallet = any(),
                cryptoCurrency = any(),
                gasLimit = any(),
                gasPrice = any(),
            )
        }
    }

    @Test
    fun `EVM DEX swap falls back to getEthSpecificFeeUseCase when getFeeForTokenUseCase returns Left`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val selectedToken = buildSwapCurrencyStatus(
            networkRawId = ethNetwork,
            contractAddress = "0xToken",
            isCoin = false,
        ).status
        val gas = BigInteger.valueOf(99_000L)
        val transaction = buildDex(txValue = "1000000000000000", gas = gas)

        coEvery {
            getFeeForTokenUseCase.invoke(userWallet = any(), token = any(), transactionData = any())
        } returns GetFeeError.UnknownError.left()

        coEvery {
            getEthSpecificFeeUseCase.invoke(
                userWallet = any(),
                cryptoCurrency = any(),
                gasLimit = any(),
                gasPrice = any(),
            )
        } returns mockk<TransactionFee.Choosable>(relaxed = true).right()

        val result = sut.calculate(fromStatus, transaction, selectedToken = selectedToken)

        // The token branch normally yields LoadedExtended, but on Left we fall back to the
        // eth-specific Loaded fee — mirroring the exception path.
        assertThat(result.isRight()).isTrue()
        result.onRight { dexFeeResult ->
            assertThat(dexFeeResult.transactionFee).isInstanceOf(TransactionFeeResult.Loaded::class.java)
        }
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
    fun `EVM DEX swap surfaces error when getFeeForTokenUseCase fails and transaction gas is null`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val selectedToken = buildSwapCurrencyStatus(
            networkRawId = ethNetwork,
            contractAddress = "0xToken",
            isCoin = false,
        ).status
        val transaction = buildDex(txValue = "1000000000000000", gas = null)

        coEvery {
            getFeeForTokenUseCase.invoke(userWallet = any(), token = any(), transactionData = any())
        } returns GetFeeError.UnknownError.left()

        val result = sut.calculate(fromStatus, transaction, selectedToken = selectedToken)

        // gas is null → the original left error is surfaced, the fallback use case is not invoked.
        assertThat(result.isLeft()).isTrue()
        result.onLeft { error ->
            assertThat(error).isEqualTo(GetFeeError.UnknownError)
        }
        coVerify(exactly = 0) {
            getEthSpecificFeeUseCase.invoke(
                userWallet = any(),
                cryptoCurrency = any(),
                gasLimit = any(),
                gasPrice = any(),
            )
        }
    }

    @Test
    fun `EVM DEX swap raises DataError when exception path is hit and transaction gas is null`() = runTest {
        // The exception (catch) branch wraps the thrown Throwable as GetFeeError.DataError when gas
        // is null — distinct from the Either.Left branches, which surface the original Left error.
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        // txValue == null forces error("unable to get txValue") inside the catch block.
        val transaction = buildDex(txValue = null, gas = null)

        val result = sut.calculate(fromStatus, transaction)

        assertThat(result.isLeft()).isTrue()
        result.onLeft { error ->
            assertThat(error).isInstanceOf(GetFeeError.DataError::class.java)
        }
        coVerify(exactly = 0) {
            getEthSpecificFeeUseCase.invoke(
                userWallet = any(),
                cryptoCurrency = any(),
                gasLimit = any(),
                gasPrice = any(),
            )
        }
    }

    @Test
    fun `EVM DEX swap propagates fallback error when getFeeUseCase Left and getEthSpecificFeeUseCase also Left`() =
        runTest {
            // Both the primary fee call and the eth-specific fallback fail. The fallback uses .bind(),
            // so its Left error must be surfaced verbatim.
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
            val gas = BigInteger.valueOf(80_000L)
            val transaction = buildDex(txValue = "1000000000000000", gas = gas)

            coEvery {
                getFeeUseCase.invoke(userWallet = any(), network = any(), transactionData = any())
            } returns GetFeeError.UnknownError.left()

            val fallbackError = GetFeeError.DataError(IllegalStateException("eth specific failed"))
            coEvery {
                getEthSpecificFeeUseCase.invoke(
                    userWallet = any(),
                    cryptoCurrency = any(),
                    gasLimit = any(),
                    gasPrice = any(),
                )
            } returns fallbackError.left()

            val result = sut.calculate(fromStatus, transaction)

            assertThat(result.isLeft()).isTrue()
            result.onLeft { error ->
                assertThat(error).isEqualTo(fallbackError)
            }
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
    fun `EVM DEX swap token branch returns LoadedExtended on success and does not call fallback`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val selectedToken = buildSwapCurrencyStatus(
            networkRawId = ethNetwork,
            contractAddress = "0xToken",
            isCoin = false,
        ).status
        val transaction = buildDex(txValue = "1000000000000000")

        coEvery {
            getFeeForTokenUseCase.invoke(userWallet = any(), token = any(), transactionData = any())
        } returns mockk<TransactionFeeExtended>(relaxed = true).right()

        val result = sut.calculate(fromStatus, transaction, selectedToken = selectedToken)

        assertThat(result.isRight()).isTrue()
        result.onRight { dexFeeResult ->
            assertThat(dexFeeResult.transactionFee).isInstanceOf(TransactionFeeResult.LoadedExtended::class.java)
            assertThat(dexFeeResult.gas).isEqualTo(transaction.gas)
        }
        // On the happy token path neither the eth-specific fallback nor the native getFeeUseCase fires.
        coVerify(exactly = 0) {
            getEthSpecificFeeUseCase.invoke(
                userWallet = any(),
                cryptoCurrency = any(),
                gasLimit = any(),
                gasPrice = any(),
            )
        }
    }

    @Test
    fun `EVM DEX swap token branch falls back to getEthSpecificFeeUseCase when exception path is hit`() = runTest {
        // selectedToken is a Token, but createTransactionExtrasUseCase fails before the token branch is
        // reached, so the exception catch fires. With gas present the eth-specific fallback applies.
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val selectedToken = buildSwapCurrencyStatus(
            networkRawId = ethNetwork,
            contractAddress = "0xToken",
            isCoin = false,
        ).status
        val gas = BigInteger.valueOf(123_000L)
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

        val result = sut.calculate(fromStatus, transaction, selectedToken = selectedToken)

        assertThat(result.isRight()).isTrue()
        result.onRight { dexFeeResult ->
            // Fallback always yields Loaded, never LoadedExtended, even on the token branch.
            assertThat(dexFeeResult.transactionFee).isInstanceOf(TransactionFeeResult.Loaded::class.java)
        }
        coVerify(exactly = 1) {
            getEthSpecificFeeUseCase.invoke(
                userWallet = any(),
                cryptoCurrency = any(),
                gasLimit = gas,
                gasPrice = any(),
            )
        }
        // The token use case is never reached because extras creation throws first.
        coVerify(exactly = 0) {
            getFeeForTokenUseCase.invoke(userWallet = any(), token = any(), transactionData = any())
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
            assertThat(error).isEqualTo(GetFeeError.BlockchainErrors.TooLargeSolanaTransactionError)
        }
        // No fee is computed when the size guard trips
        coVerify(exactly = 0) {
            getFeeUseCase.invoke(userWallet = any(), network = any(), transactionData = any())
        }
    }

    // -------------------------------------------------------------------------
    // Integrated-approve simulated estimation override ([REDACTED_TASK_KEY])
    //
    // The end-to-end EstimateOverrideError → legacy-fallback recompute is exercised at the
    // interactor level in
    // [com.tangem.feature.swap.domain.SwapInteractorImplLoadSwapFeeTest] (which owns the
    // session-fallback state machine). Here we only assert the calculator's branch selection:
    // PermissionSettings → simulated estimation; Empty → plain getFee path.
    // -------------------------------------------------------------------------

    @Test
    fun `EVM DEX swap with PermissionSettings uses the simulated estimation path`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val transaction = buildDex(txValue = "1000000000000000")
        val permissionState = PermissionDataState.PermissionSettings(
            type = ApproveType.LIMITED,
            spenderAddress = "0xSpender",
        )

        coEvery {
            getFeeUseCase.invoke(
                userWallet = any(),
                network = any(),
                transactionData = any(),
                spenderAddress = any(),
                isSimulateEstimation = true,
            )
        } returns TransactionFee.Single(normal = ethLegacyFee()).right()

        sut.calculate(
            fromSwapCurrencyStatus = fromStatus,
            transaction = transaction,
            permissionState = permissionState,
        )

        // PermissionSettings must drive the simulated estimation (isSimulateEstimation = true) with
        // the spender carried through from the permission state.
        coVerify(exactly = 1) {
            getFeeUseCase.invoke(
                userWallet = any(),
                network = any(),
                transactionData = any(),
                spenderAddress = "0xSpender",
                isSimulateEstimation = true,
            )
        }
    }

    @Test
    fun `EVM DEX swap with Empty permission uses plain getFee path and does not simulate`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val transaction = buildDex(txValue = "1000000000000000")

        coEvery {
            getFeeUseCase.invoke(
                userWallet = any(),
                network = any(),
                transactionData = any(),
                spenderAddress = any(),
                isSimulateEstimation = false,
            )
        } returns TransactionFee.Single(normal = ethLegacyFee()).right()

        val result = sut.calculate(
            fromSwapCurrencyStatus = fromStatus,
            transaction = transaction,
            permissionState = PermissionDataState.Empty,
        )

        assertThat(result.isRight()).isTrue()
        // The simulated estimation must not be used when there is no PermissionSettings context.
        coVerify(exactly = 0) {
            getFeeUseCase.invoke(
                userWallet = any(),
                network = any(),
                transactionData = any(),
                spenderAddress = any(),
                isSimulateEstimation = true,
            )
        }
    }

    @Test
    fun `EVM DEX swap raises EstimateOverrideError without eth-specific fallback even when gas is present`() =
        runTest {
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
            // gas is present — the legacy fallback would normally kick in for a plain Left,
            // but an EstimateOverrideError must be raised verbatim so the model can trigger
            // the integrated-approval fallback instead of silently using the eth-specific fee.
            val transaction = buildDex(txValue = "1000000000000000", gas = BigInteger.valueOf(50_000L))
            val permissionState = PermissionDataState.PermissionSettings(
                type = ApproveType.LIMITED,
                spenderAddress = "0xSpender",
            )
            val overrideError = GetFeeError.EstimateOverrideError(
                blockchain = "ethereum",
                tokenSymbol = "USDT",
                rpcProvider = "infura",
                error = "execution reverted",
            )

            coEvery {
                getFeeUseCase.invoke(
                    userWallet = any(),
                    network = any(),
                    transactionData = any(),
                    spenderAddress = any(),
                    isSimulateEstimation = true,
                )
            } returns overrideError.left()

            val result = sut.calculate(
                fromSwapCurrencyStatus = fromStatus,
                transaction = transaction,
                permissionState = permissionState,
            )

            assertThat(result.isLeft()).isTrue()
            result.onLeft { error ->
                assertThat(error).isEqualTo(overrideError)
            }
            // The eth-specific fallback must NOT be invoked for EstimateOverrideError, even though
            // gas is present — otherwise the model would never see the override and the
            // integrated-approval fallback would not trigger.
            coVerify(exactly = 0) {
                getEthSpecificFeeUseCase.invoke(
                    userWallet = any(),
                    cryptoCurrency = any(),
                    gasLimit = any(),
                    gasPrice = any(),
                )
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
        gas: BigInteger? = BigInteger.valueOf(21_000L),
        txTo: String = "0xRecipient",
        txFrom: String = "0xSender",
        allowanceContract: String? = null,
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
        allowanceContract = allowanceContract,
    )
}