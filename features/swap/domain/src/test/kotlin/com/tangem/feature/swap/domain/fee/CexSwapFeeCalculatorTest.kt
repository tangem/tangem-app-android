package com.tangem.feature.swap.domain.fee

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.EstimateFeeUseCase
import com.tangem.domain.transaction.usecase.gasless.EstimateFeeForGaslessTxUseCase
import com.tangem.domain.transaction.usecase.gasless.EstimateFeeForTokenUseCase
import com.tangem.feature.swap.domain.buildSwapCurrencyStatus
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
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

    private val sendBump = PatchEthGasLimitForSwap(percentage = PatchEthGasLimitForSwap.SEND_PERCENTAGE)

    private val sut: CexSwapFeeCalculator by lazy {
        CexSwapFeeCalculator(
            estimateFeeUseCase = estimateFeeUseCase,
            estimateFeeForTokenUseCase = estimateFeeForTokenUseCase,
            estimateFeeForGaslessTxUseCase = estimateFeeForGaslessTxUseCase,
            patchEthGasLimitForSwap = sendBump,
        )
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
        val expected = mockk<TransactionFeeExtended>(relaxed = true)
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
            val expected = mockk<TransactionFeeExtended>(relaxed = true)
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
}