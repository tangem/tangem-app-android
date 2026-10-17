package com.tangem.feature.swap.domain.fee

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.CreateTransferTransactionUseCase
import com.tangem.domain.transaction.usecase.EstimateFeeUseCase
import com.tangem.domain.transaction.usecase.gasless.EstimateFeeForGaslessTxUseCase
import com.tangem.domain.transaction.usecase.gasless.EstimateFeeForTokenUseCase
import com.tangem.domain.transaction.usecase.gasless.GetTronGaslessFeeUseCase
import com.tangem.domain.transaction.usecase.gasless.IsTronGaslessSupportedUseCase
import com.tangem.feature.swap.domain.buildSwapCurrencyStatus
import com.tangem.features.swap.SwapFeatureToggles
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Unit tests for [CexSwapFeeCalculator].
 *
 * Mirrors the CEX paths in `SwapInteractorImpl.loadFeeForSwapTransaction` (overload 2 native +
 * overload 1 token/gasless) and `getFeeForCex`, but exercises the new helper directly.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CexSwapFeeCalculatorTest {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()

    private val estimateFeeUseCase: EstimateFeeUseCase = mockk(relaxed = true)
    private val estimateFeeForTokenUseCase: EstimateFeeForTokenUseCase = mockk(relaxed = true)
    private val estimateFeeForGaslessTxUseCase: EstimateFeeForGaslessTxUseCase = mockk(relaxed = true)
    private val getTronGaslessFeeUseCase: GetTronGaslessFeeUseCase = mockk(relaxed = true)
    private val createTransferTransactionUseCase: CreateTransferTransactionUseCase = mockk(relaxed = true)
    private val isTronGaslessSupportedUseCase: IsTronGaslessSupportedUseCase = mockk(relaxed = true)
    private val swapFeatureToggles: SwapFeatureToggles = mockk()

    private val sendBump = PatchEthGasLimitForSwap(percentage = PatchEthGasLimitForSwap.SEND_PERCENTAGE)

    private val sut: CexSwapFeeCalculator by lazy {
        CexSwapFeeCalculator(
            estimateFeeUseCase = estimateFeeUseCase,
            estimateFeeForTokenUseCase = estimateFeeForTokenUseCase,
            estimateFeeForGaslessTxUseCase = estimateFeeForGaslessTxUseCase,
            getTronGaslessFeeUseCase = getTronGaslessFeeUseCase,
            createTransferTransactionUseCase = createTransferTransactionUseCase,
            isTronGaslessSupportedUseCase = isTronGaslessSupportedUseCase,
            swapFeatureToggles = swapFeatureToggles,
            patchEthGasLimitForSwap = sendBump,
        )
    }

    @BeforeEach
    fun setUp() {
        every { swapFeatureToggles.isTronGaslessSwapEnabled } returns true
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    // -------------------------------------------------------------------------
    // Zero amount short-circuit
    // -------------------------------------------------------------------------

    @Test
    fun `GIVEN zero amount WHEN calculate THEN returns Left UnknownError`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)

        val result = sut.calculate(
            userWallet = fromStatus.userWallet,
            fromSwapCurrencyStatus = fromStatus,
            amount = BigDecimal.ZERO,
            selectedFeeToken = null,
            isGasless = true,
        )

        assertThat(result.isLeft()).isTrue()
        result.onLeft { assertThat(it).isInstanceOf(GetFeeError.UnknownError::class.java) }
        // None of the fee use cases were invoked
        coVerify(exactly = 0) {
            estimateFeeUseCase.invoke(any(), any(), any())
            estimateFeeForTokenUseCase.invoke(
                userWallet = any(),
                feeTokenCurrencyStatus = any(),
                sendingTokenCurrencyStatus = any(),
                amount = any(),
            )
            estimateFeeForGaslessTxUseCase.invoke(any(), any(), any())
        }
    }

    // -------------------------------------------------------------------------
    // Gasless path (selectedFeeToken == null)
    // -------------------------------------------------------------------------

    @Test
    fun `GIVEN null selectedFeeToken WHEN calculate THEN delegates to estimateFeeForGaslessTxUseCase`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val expected = tokenPaidExtendedFee(fromStatus)
        coEvery {
            estimateFeeForGaslessTxUseCase(any(), any(), any())
        } returns expected.right()

        val result = sut.calculate(
            userWallet = fromStatus.userWallet,
            fromSwapCurrencyStatus = fromStatus,
            amount = BigDecimal("1.5"),
            selectedFeeToken = null,
            isGasless = true,
        )

        assertThat(result.isRight()).isTrue()
        result.onRight { cexResult ->
            val loaded = cexResult.transactionFee as TransactionFeeResult.LoadedExtended
            assertThat(loaded.fee).isSameInstanceAs(expected)
        }
        coVerify(exactly = 1) {
            estimateFeeForGaslessTxUseCase.invoke(
                amount = BigDecimal("1.5"),
                userWallet = fromStatus.userWallet,
                sendingTokenCurrencyStatus = fromStatus.status,
            )
        }
        // Other use cases are NOT called.
        coVerify(exactly = 0) {
            estimateFeeUseCase.invoke(any(), any(), any())
            estimateFeeForTokenUseCase.invoke(
                userWallet = any(),
                feeTokenCurrencyStatus = any(),
                sendingTokenCurrencyStatus = any(),
                amount = any(),
            )
        }
    }

    @Test
    fun `GIVEN gasless resolves to native Legacy fee WHEN calculate THEN 5 percent bump applied`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val rawExtended = TransactionFeeExtended(
            transactionFee = TransactionFee.Single(
                normal = Fee.Ethereum.Legacy(
                    amount = Amount(currencySymbol = "ETH", value = BigDecimal("0.000002"), decimals = 18),
                    gasLimit = BigInteger.valueOf(100_000),
                    gasPrice = BigInteger.valueOf(20_000_000_000),
                ),
            ),
            feeTokenId = fromStatus.status.currency.id,
        )
        coEvery {
            estimateFeeForGaslessTxUseCase(any(), any(), any())
        } returns rawExtended.right()

        val result = sut.calculate(
            userWallet = fromStatus.userWallet,
            fromSwapCurrencyStatus = fromStatus,
            amount = BigDecimal("1.5"),
            selectedFeeToken = null,
            isGasless = true,
        )

        assertThat(result.isRight()).isTrue()
        result.onRight { cexResult ->
            val loaded = cexResult.transactionFee as TransactionFeeResult.LoadedExtended
            val patched = (loaded.fee.transactionFee as TransactionFee.Single).normal as Fee.Ethereum.Legacy
            // 100_000 * 105 / 100 = 105_000
            assertThat(patched.gasLimit).isEqualTo(BigInteger.valueOf(105_000))
            // 105_000 * 20_000_000_000 / 1e18 = 0.0000021
            assertThat(patched.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.0000021"))
            // Extended-fee metadata is preserved.
            assertThat(loaded.fee.feeTokenId).isEqualTo(rawExtended.feeTokenId)
            assertThat(loaded.fee.gaslessFeePlan).isNull()
            assertThat(loaded.fee.mainTransactionGasLimit).isNull()
            assertThat(loaded.fee.withdrawGasLimit).isNull()
        }
    }

    @Test
    fun `GIVEN gasless resolves to native Choosable EIP1559 fee WHEN calculate THEN bump applied to all three legs`() =
        runTest {
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
            val maxFeePerGas = BigInteger.valueOf(10_000_000_000)
            val priorityFee = BigInteger.valueOf(1_000_000_000)
            fun eip1559(gasLimit: Long, amount: String) = Fee.Ethereum.EIP1559(
                amount = Amount(currencySymbol = "ETH", value = BigDecimal(amount), decimals = 18),
                gasLimit = BigInteger.valueOf(gasLimit),
                maxFeePerGas = maxFeePerGas,
                priorityFee = priorityFee,
            )
            val rawExtended = TransactionFeeExtended(
                transactionFee = TransactionFee.Choosable(
                    minimum = eip1559(gasLimit = 50_000, amount = "0.0000005"),
                    normal = eip1559(gasLimit = 100_000, amount = "0.000001"),
                    priority = eip1559(gasLimit = 150_000, amount = "0.0000015"),
                ),
                feeTokenId = fromStatus.status.currency.id,
            )
            coEvery {
                estimateFeeForGaslessTxUseCase(any(), any(), any())
            } returns rawExtended.right()

            val result = sut.calculate(
                userWallet = fromStatus.userWallet,
                fromSwapCurrencyStatus = fromStatus,
                amount = BigDecimal("1.0"),
                selectedFeeToken = null,
                isGasless = true,
            )

            result.onRight { cexResult ->
                val loaded = cexResult.transactionFee as TransactionFeeResult.LoadedExtended
                val patched = loaded.fee.transactionFee as TransactionFee.Choosable
                assertThat((patched.minimum as Fee.Ethereum.EIP1559).gasLimit)
                    .isEqualTo(BigInteger.valueOf(52_500))
                assertThat((patched.normal as Fee.Ethereum.EIP1559).gasLimit)
                    .isEqualTo(BigInteger.valueOf(105_000))
                assertThat((patched.priority as Fee.Ethereum.EIP1559).gasLimit)
                    .isEqualTo(BigInteger.valueOf(157_500))
            }
        }

    @Test
    fun `GIVEN gasless resolves to token-paid fee WHEN calculate THEN fee returned without bump`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val expected = tokenPaidExtendedFee(fromStatus)
        coEvery {
            estimateFeeForGaslessTxUseCase(any(), any(), any())
        } returns expected.right()

        val result = sut.calculate(
            userWallet = fromStatus.userWallet,
            fromSwapCurrencyStatus = fromStatus,
            amount = BigDecimal("1.0"),
            selectedFeeToken = null,
            isGasless = true,
        )

        result.onRight { cexResult ->
            val loaded = cexResult.transactionFee as TransactionFeeResult.LoadedExtended
            assertThat(loaded.fee).isSameInstanceAs(expected)
        }
    }

    @Test
    fun `GIVEN gasless resolves to non-Ethereum fee WHEN calculate THEN fee returned without bump`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val expected = TransactionFeeExtended(
            transactionFee = TransactionFee.Single(
                normal = Fee.Common(
                    amount = Amount(currencySymbol = "BTC", value = BigDecimal("0.0001"), decimals = 8),
                ),
            ),
            feeTokenId = fromStatus.status.currency.id,
        )
        coEvery {
            estimateFeeForGaslessTxUseCase(any(), any(), any())
        } returns expected.right()

        val result = sut.calculate(
            userWallet = fromStatus.userWallet,
            fromSwapCurrencyStatus = fromStatus,
            amount = BigDecimal("1.0"),
            selectedFeeToken = null,
            isGasless = true,
        )

        result.onRight { cexResult ->
            val loaded = cexResult.transactionFee as TransactionFeeResult.LoadedExtended
            assertThat(loaded.fee).isSameInstanceAs(expected)
        }
    }

    @Test
    fun `GIVEN gasless path returns Left WHEN calculate THEN error is propagated`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        coEvery {
            estimateFeeForGaslessTxUseCase(any(), any(), any())
        } returns GetFeeError.GaslessError.NoSupportedTokensFound.left()

        val result = sut.calculate(
            userWallet = fromStatus.userWallet,
            fromSwapCurrencyStatus = fromStatus,
            amount = BigDecimal("1.0"),
            selectedFeeToken = null,
            isGasless = true,
        )

        assertThat(result.isLeft()).isTrue()
        result.onLeft { error ->
            assertThat(error).isInstanceOf(GetFeeError.GaslessError.NoSupportedTokensFound::class.java)
        }
    }

    // -------------------------------------------------------------------------
    // Explicit token path (selectedFeeToken is Token)
    // -------------------------------------------------------------------------

    @Test
    fun `GIVEN explicit token selectedFeeToken WHEN calculate THEN delegates to estimateFeeForTokenUseCase`() =
        runTest {
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
            val tokenCurrency = mockk<CryptoCurrency.Token>(relaxed = true)
            val tokenStatus = mockk<CryptoCurrencyStatus>(relaxed = true) {
                every { currency } returns tokenCurrency
            }
            val expected = mockk<TransactionFeeExtended>(relaxed = true)
            coEvery {
                estimateFeeForTokenUseCase(
                    userWallet = any(),
                    feeTokenCurrencyStatus = any(),
                    sendingTokenCurrencyStatus = any(),
                    amount = any(),
                )
            } returns expected.right()

            val result = sut.calculate(
                userWallet = fromStatus.userWallet,
                fromSwapCurrencyStatus = fromStatus,
                amount = BigDecimal("2.0"),
                selectedFeeToken = tokenStatus,
                isGasless = true,
            )

            assertThat(result.isRight()).isTrue()
            result.onRight { cexResult ->
                val loaded = cexResult.transactionFee as TransactionFeeResult.LoadedExtended
                assertThat(loaded.fee).isSameInstanceAs(expected)
            }
            coVerify(exactly = 1) {
                estimateFeeForTokenUseCase.invoke(
                    userWallet = fromStatus.userWallet,
                    feeTokenCurrencyStatus = tokenStatus,
                    sendingTokenCurrencyStatus = fromStatus.status,
                    amount = BigDecimal("2.0"),
                )
            }
            coVerify(exactly = 0) {
                estimateFeeUseCase.invoke(any(), any(), any())
                estimateFeeForGaslessTxUseCase.invoke(any(), any(), any())
            }
        }

    // -------------------------------------------------------------------------
    // Tron gasless path (selectedFeeToken is a Token on Tron)
    // -------------------------------------------------------------------------

    @Test
    fun `GIVEN Tron token selectedFeeToken WHEN calculate THEN quotes Tron gasless fee against estimation address`() =
        runTest {
            // Arrange
            val fromStatus = buildSwapCurrencyStatus(
                networkRawId = Blockchain.Tron.toNetworkId(),
                contractAddress = TRON_USDT,
                isCoin = false,
                decimals = 6,
            )
            val txData = mockk<TransactionData.Uncompiled>()
            val expected = mockk<TransactionFeeExtended>()
            coEvery {
                createTransferTransactionUseCase(
                    amount = any(), memo = any(), destination = any(), userWalletId = any(), network = any(),
                )
            } returns txData.right()
            coEvery { getTronGaslessFeeUseCase(any(), any(), any()) } returns expected.right()

            // Act
            val result = sut.calculate(
                userWallet = fromStatus.userWallet,
                fromSwapCurrencyStatus = fromStatus,
                amount = BigDecimal("10"),
                selectedFeeToken = fromStatus.status,
                isGasless = true,
            )

            // Assert
            val loaded = result.getOrNull()?.transactionFee as? TransactionFeeResult.LoadedExtended
            assertThat(loaded?.fee).isSameInstanceAs(expected)
            val userWalletId = fromStatus.userWallet.walletId
            val network = fromStatus.currency.network
            val feeToken = fromStatus.currency as CryptoCurrency.Token
            coVerify(exactly = 1) {
                createTransferTransactionUseCase(
                    amount = match { it.value == BigDecimal("10") },
                    memo = null,
                    destination = TRON_ESTIMATION_ADDRESS,
                    userWalletId = userWalletId,
                    network = network,
                )
                getTronGaslessFeeUseCase(userWalletId = userWalletId, transactionData = txData, feeToken = feeToken)
            }
            coVerify(exactly = 0) {
                estimateFeeForTokenUseCase.invoke(
                    userWallet = any(),
                    feeTokenCurrencyStatus = any(),
                    sendingTokenCurrencyStatus = any(),
                    amount = any(),
                )
            }
        }

    @Test
    fun `GIVEN Tron gasless quote fails WHEN calculate THEN error is propagated`() = runTest {
        // Arrange
        val fromStatus = buildSwapCurrencyStatus(
            networkRawId = Blockchain.Tron.toNetworkId(),
            contractAddress = TRON_USDT,
            isCoin = false,
            decimals = 6,
        )
        val error = GetFeeError.GaslessError.TronActivationRateLimited(isPerWallet = true)
        coEvery {
            createTransferTransactionUseCase(
                amount = any(), memo = any(), destination = any(), userWalletId = any(), network = any(),
            )
        } returns mockk<TransactionData.Uncompiled>().right()
        coEvery { getTronGaslessFeeUseCase(any(), any(), any()) } returns error.left()

        // Act
        val result = sut.calculate(
            userWallet = fromStatus.userWallet,
            fromSwapCurrencyStatus = fromStatus,
            amount = BigDecimal("10"),
            selectedFeeToken = fromStatus.status,
            isGasless = true,
        )

        // Assert
        assertThat(result).isEqualTo(error.left())
    }

    @Test
    fun `GIVEN no fee token and Tron gasless supported WHEN calculate THEN quotes Tron gasless fee`() = runTest {
        // Arrange
        val fromStatus = tronUsdtStatus()
        val expected = mockk<TransactionFeeExtended>()
        coEvery { isTronGaslessSupportedUseCase(any(), any(), any()) } returns true
        stubTronTransferTransaction()
        coEvery { getTronGaslessFeeUseCase(any(), any(), any()) } returns expected.right()

        // Act
        val result = sut.calculate(
            userWallet = fromStatus.userWallet,
            fromSwapCurrencyStatus = fromStatus,
            amount = BigDecimal("10"),
            selectedFeeToken = null,
            isGasless = true,
        )

        // Assert
        val loaded = result.getOrNull()?.transactionFee as? TransactionFeeResult.LoadedExtended
        assertThat(loaded?.fee).isSameInstanceAs(expected)
        coVerify(exactly = 0) {
            estimateFeeForGaslessTxUseCase(amount = any(), userWallet = any(), sendingTokenCurrencyStatus = any())
        }
    }

    @Test
    fun `GIVEN no fee token and Tron gasless quote unavailable WHEN calculate THEN falls back to native fee`() =
        runTest {
            // Arrange
            val fromStatus = tronUsdtStatus()
            val nativeFee = TransactionFeeExtended(
                transactionFee = TransactionFee.Single(normal = mockk<Fee.Common>(relaxed = true)),
                feeTokenId = fromStatus.currency.id,
            )
            coEvery { isTronGaslessSupportedUseCase(any(), any(), any()) } returns true
            stubTronTransferTransaction()
            coEvery { getTronGaslessFeeUseCase(any(), any(), any()) } returns
                GetFeeError.GaslessError.DataError(null).left()
            coEvery {
                estimateFeeForGaslessTxUseCase(amount = any(), userWallet = any(), sendingTokenCurrencyStatus = any())
            } returns nativeFee.right()

            // Act
            val result = sut.calculate(
                userWallet = fromStatus.userWallet,
                fromSwapCurrencyStatus = fromStatus,
                amount = BigDecimal("10"),
                selectedFeeToken = null,
                isGasless = true,
            )

            // Assert
            val loaded = result.getOrNull()?.transactionFee as? TransactionFeeResult.LoadedExtended
            assertThat(loaded?.fee).isSameInstanceAs(nativeFee)
        }

    @Test
    fun `GIVEN no fee token and Tron activation refused WHEN calculate THEN error is propagated`() = runTest {
        // Arrange
        val fromStatus = tronUsdtStatus()
        val error = GetFeeError.GaslessError.TronActivationRateLimited(isPerWallet = true)
        coEvery { isTronGaslessSupportedUseCase(any(), any(), any()) } returns true
        stubTronTransferTransaction()
        coEvery { getTronGaslessFeeUseCase(any(), any(), any()) } returns error.left()

        // Act
        val result = sut.calculate(
            userWallet = fromStatus.userWallet,
            fromSwapCurrencyStatus = fromStatus,
            amount = BigDecimal("10"),
            selectedFeeToken = null,
            isGasless = true,
        )

        // Assert
        assertThat(result).isEqualTo(error.left())
        coVerify(exactly = 0) {
            estimateFeeForGaslessTxUseCase(amount = any(), userWallet = any(), sendingTokenCurrencyStatus = any())
        }
    }

    @Test
    fun `GIVEN Tron gasless swap toggle disabled WHEN calculate without fee token THEN native fee path is used`() =
        runTest {
            // Arrange
            val fromStatus = tronUsdtStatus()
            val nativeFee = TransactionFeeExtended(
                transactionFee = TransactionFee.Single(normal = mockk<Fee.Common>(relaxed = true)),
                feeTokenId = fromStatus.currency.id,
            )
            every { swapFeatureToggles.isTronGaslessSwapEnabled } returns false
            coEvery {
                estimateFeeForGaslessTxUseCase(amount = any(), userWallet = any(), sendingTokenCurrencyStatus = any())
            } returns nativeFee.right()

            // Act
            val result = sut.calculate(
                userWallet = fromStatus.userWallet,
                fromSwapCurrencyStatus = fromStatus,
                amount = BigDecimal("10"),
                selectedFeeToken = null,
                isGasless = true,
            )

            // Assert
            val loaded = result.getOrNull()?.transactionFee as? TransactionFeeResult.LoadedExtended
            assertThat(loaded?.fee).isSameInstanceAs(nativeFee)
            coVerify(exactly = 0) {
                isTronGaslessSupportedUseCase(any(), any(), any())
                getTronGaslessFeeUseCase(any(), any(), any())
            }
        }

    private fun tronUsdtStatus(): SwapCurrencyStatus = buildSwapCurrencyStatus(
        networkRawId = Blockchain.Tron.toNetworkId(),
        contractAddress = TRON_USDT,
        isCoin = false,
        decimals = 6,
    )

    private fun stubTronTransferTransaction() {
        coEvery {
            createTransferTransactionUseCase(
                amount = any(), memo = any(), destination = any(), userWalletId = any(), network = any(),
            )
        } returns mockk<TransactionData.Uncompiled>().right()
    }

    // -------------------------------------------------------------------------
    // Explicit native path (selectedFeeToken is Coin) — applies 5% bump
    // -------------------------------------------------------------------------

    @Test
    fun `GIVEN explicit native selectedFeeToken WHEN calculate THEN delegates to estimateFeeUseCase and applies 5 percent bump on Ethereum`() =
        runTest {
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
            val coinCurrency = mockk<CryptoCurrency.Coin>(relaxed = true)
            val coinStatus = mockk<CryptoCurrencyStatus>(relaxed = true) {
                every { currency } returns coinCurrency
            }
            val rawFee = Fee.Ethereum.Legacy(
                amount = Amount(currencySymbol = "ETH", value = BigDecimal("0.000002"), decimals = 18),
                gasLimit = BigInteger.valueOf(100_000),
                gasPrice = BigInteger.valueOf(20_000_000_000),
            )
            coEvery {
                estimateFeeUseCase(any(), any(), any())
            } returns TransactionFee.Single(normal = rawFee).right()

            val result = sut.calculate(
                userWallet = fromStatus.userWallet,
                fromSwapCurrencyStatus = fromStatus,
                amount = BigDecimal("3.0"),
                selectedFeeToken = coinStatus,
                isGasless = true,
            )

            assertThat(result.isRight()).isTrue()
            result.onRight { cexResult ->
                val loaded = cexResult.transactionFee as TransactionFeeResult.Loaded
                val patched = (loaded.fee as TransactionFee.Single).normal as Fee.Ethereum.Legacy
                // 100_000 * 105 / 100 = 105_000
                assertThat(patched.gasLimit).isEqualTo(BigInteger.valueOf(105_000))
                // 105_000 * 20_000_000_000 / 1e18 = 0.0000021
                assertThat(patched.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.0000021"))
            }
            coVerify(exactly = 1) {
                estimateFeeUseCase.invoke(
                    amount = BigDecimal("3.0"),
                    userWallet = fromStatus.userWallet,
                    cryptoCurrencyStatus = fromStatus.status,
                )
            }
            coVerify(exactly = 0) {
                estimateFeeForTokenUseCase.invoke(
                    userWallet = any(),
                    feeTokenCurrencyStatus = any(),
                    sendingTokenCurrencyStatus = any(),
                    amount = any(),
                )
                estimateFeeForGaslessTxUseCase.invoke(any(), any(), any())
            }
        }

    @Test
    fun `GIVEN explicit native selectedFeeToken with non-Ethereum fee WHEN calculate THEN bump is a no-op`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val coinCurrency = mockk<CryptoCurrency.Coin>(relaxed = true)
        val coinStatus = mockk<CryptoCurrencyStatus>(relaxed = true) {
            every { currency } returns coinCurrency
        }
        val rawFee = Fee.Common(
            amount = Amount(currencySymbol = "BTC", value = BigDecimal("0.0001"), decimals = 8),
        )
        coEvery {
            estimateFeeUseCase(any(), any(), any())
        } returns TransactionFee.Single(normal = rawFee).right()

        val result = sut.calculate(
            userWallet = fromStatus.userWallet,
            fromSwapCurrencyStatus = fromStatus,
            amount = BigDecimal("1.0"),
            selectedFeeToken = coinStatus,
            isGasless = true,
        )

        result.onRight { cexResult ->
            val loaded = cexResult.transactionFee as TransactionFeeResult.Loaded
            val unchanged = (loaded.fee as TransactionFee.Single).normal as Fee.Common
            assertThat(unchanged).isSameInstanceAs(rawFee)
        }
    }

    @Test
    fun `GIVEN native path returns Left WHEN calculate THEN error is propagated`() = runTest {
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val coinCurrency = mockk<CryptoCurrency.Coin>(relaxed = true)
        val coinStatus = mockk<CryptoCurrencyStatus>(relaxed = true) {
            every { currency } returns coinCurrency
        }
        coEvery {
            estimateFeeUseCase(any(), any(), any())
        } returns GetFeeError.UnknownError.left()

        val result = sut.calculate(
            userWallet = fromStatus.userWallet,
            fromSwapCurrencyStatus = fromStatus,
            amount = BigDecimal("1.0"),
            selectedFeeToken = coinStatus,
            isGasless = true,
        )

        assertThat(result.isLeft()).isTrue()
        result.onLeft { assertThat(it).isInstanceOf(GetFeeError.UnknownError::class.java) }
    }

    // -------------------------------------------------------------------------
    // Choosable Ethereum fee (multiple legs) — bump applied to every leg
    // -------------------------------------------------------------------------

    @Test
    fun `GIVEN explicit native with Choosable Ethereum fee WHEN calculate THEN bump applied to all three legs`() =
        runTest {
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
            val coinCurrency = mockk<CryptoCurrency.Coin>(relaxed = true)
            val coinStatus = mockk<CryptoCurrencyStatus>(relaxed = true) {
                every { currency } returns coinCurrency
            }
            val gasPrice = BigInteger.valueOf(10_000_000_000)
            val rawFee = TransactionFee.Choosable(
                minimum = Fee.Ethereum.Legacy(
                    amount = Amount(currencySymbol = "ETH", value = BigDecimal("0.000001"), decimals = 18),
                    gasLimit = BigInteger.valueOf(50_000),
                    gasPrice = gasPrice,
                ),
                normal = Fee.Ethereum.Legacy(
                    amount = Amount(currencySymbol = "ETH", value = BigDecimal("0.000002"), decimals = 18),
                    gasLimit = BigInteger.valueOf(100_000),
                    gasPrice = gasPrice,
                ),
                priority = Fee.Ethereum.Legacy(
                    amount = Amount(currencySymbol = "ETH", value = BigDecimal("0.000003"), decimals = 18),
                    gasLimit = BigInteger.valueOf(150_000),
                    gasPrice = gasPrice,
                ),
            )
            coEvery {
                estimateFeeUseCase(any(), any(), any())
            } returns rawFee.right()

            val result = sut.calculate(
                userWallet = fromStatus.userWallet,
                fromSwapCurrencyStatus = fromStatus,
                amount = BigDecimal("1.0"),
                selectedFeeToken = coinStatus,
                isGasless = true,
            )

            result.onRight { cexResult ->
                val loaded = cexResult.transactionFee as TransactionFeeResult.Loaded
                val patched = loaded.fee as TransactionFee.Choosable
                assertThat((patched.minimum as Fee.Ethereum.Legacy).gasLimit)
                    .isEqualTo(BigInteger.valueOf(52_500))
                assertThat((patched.normal as Fee.Ethereum.Legacy).gasLimit)
                    .isEqualTo(BigInteger.valueOf(105_000))
                assertThat((patched.priority as Fee.Ethereum.Legacy).gasLimit)
                    .isEqualTo(BigInteger.valueOf(157_500))
            }
        }

    // -------------------------------------------------------------------------
    // userWallet propagation
    // -------------------------------------------------------------------------

    @Test
    fun `GIVEN gasless path WHEN calculate THEN userWallet is propagated to estimateFeeForGaslessTxUseCase`() =
        runTest {
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
            val customWallet = mockk<UserWallet>(relaxed = true)
            val expected = tokenPaidExtendedFee(fromStatus)
            coEvery {
                estimateFeeForGaslessTxUseCase(any(), any(), any())
            } returns expected.right()

            sut.calculate(
                userWallet = customWallet,
                fromSwapCurrencyStatus = fromStatus,
                amount = BigDecimal("1.0"),
                selectedFeeToken = null,
                isGasless = true,
            )

            coVerify(exactly = 1) {
                estimateFeeForGaslessTxUseCase.invoke(
                    amount = BigDecimal("1.0"),
                    userWallet = customWallet,
                    sendingTokenCurrencyStatus = fromStatus.status,
                )
            }
        }

    private fun tokenPaidExtendedFee(fromStatus: SwapCurrencyStatus): TransactionFeeExtended {
        return TransactionFeeExtended(
            transactionFee = TransactionFee.Single(
                normal = Fee.Ethereum.TokenCurrency(
                    amount = Amount(currencySymbol = "USDT", value = BigDecimal("1.2"), decimals = 6),
                    gasLimit = BigInteger.valueOf(100_000),
                    coinPriceInToken = BigInteger.ONE,
                    feeTransferGasLimit = BigInteger.valueOf(50_000),
                    baseGas = BigInteger.valueOf(21_000),
                ),
            ),
            feeTokenId = fromStatus.status.currency.id,
        )
    }

    private companion object {
        const val TRON_USDT = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
        const val TRON_ESTIMATION_ADDRESS = "TA4Tkaj2nAJjkVbDHdUQDxYCbLfsZzS8pA"
    }
}