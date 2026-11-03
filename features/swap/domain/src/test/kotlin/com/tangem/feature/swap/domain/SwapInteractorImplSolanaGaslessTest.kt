package com.tangem.feature.swap.domain

import com.tangem.domain.swap.models.SwapPairModel
import com.tangem.domain.models.wallet.UserWalletId
import android.util.Base64
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.express.models.ExpressOperationType
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.error.SolanaGaslessError
import com.tangem.domain.transaction.error.SolanaGaslessSendError
import com.tangem.feature.swap.domain.fee.TransactionFeeResult
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.domain.PreparedSwapConfigState
import com.tangem.features.send.api.subcomponents.feeSelector.params.SponsoredFeeAvailability
import com.tangem.feature.swap.domain.models.domain.SwapBalanceStatus
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.test.core.ProvideTestModels
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Tests for [SwapInteractorImpl.applySwapFee] sponsoring a Solana DEX swap fee.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplSolanaGaslessTest : SwapInteractorImplTestBase() {

    private val solanaNetwork = Blockchain.Solana.toNetworkId()
    private val lastReducedBalanceBy = BigDecimal.ZERO

    @BeforeEach
    fun setup() {
        coEvery {
            getCurrencyCheckUseCase.invoke(
                userWalletId = any(),
                currencyStatus = any(),
                feeCurrencyStatus = any(),
                amount = any(),
                fee = any(),
                feeCurrencyBalanceAfterTransaction = any(),
                recipientAddress = any(),
            )
        } returns buildCurrencyCheck()
        coEvery {
            validateTransactionUseCase.invoke(
                amount = any(),
                fee = any(),
                memo = any(),
                destination = any(),
                userWalletId = any(),
                network = any(),
            )
        } returns Unit.right()
        coEvery { getFeePaidCryptoCurrencyStatusSyncUseCase.invoke(any(), any()) } returns null.right()
        coEvery { currenciesRepository.createCoinCurrency(any()) } returns buildCoinCurrency(networkRawId = solanaNetwork)
        coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
    }

    @Test
    fun `GIVEN sponsored fee picked AND resolver returns fee payer WHEN applySwapFee THEN balanceStatus is SponsoredFee`() =
        runTest {
            // Arrange
            coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal.ZERO
            coEvery { solanaGaslessSwapResolver.resolveFeePayer(any(), any(), any()) } returns "FeePayer123"
            val state = buildSolanaQuotesLoadedState(fromBalance = BigDecimal("10"))
            val swapFee = buildSolanaSwapFee(feeValue = BigDecimal.ZERO, isSponsored = true)

            // Act
            val patched = sut.applySwapFee(state, swapFee, lastReducedBalanceBy)

            // Assert
            assertThat(patched.preparedSwapConfigState.balanceStatus)
                .isEqualTo(SwapBalanceStatus.SponsoredFee(feePayer = "FeePayer123"))
            coVerify {
                solanaGaslessSwapResolver.resolveFeePayer(
                    fromSwapCurrencyStatus = state.fromTokenInfo.swapCurrencyStatus,
                    provider = state.swapProvider,
                    swapData = state.swapDataModel,
                )
            }
        }

    @Test
    fun `GIVEN sponsored fee picked AND no sponsor WHEN applySwapFee THEN empty SOL balance reads as InsufficientFee`() =
        runTest {
            // Arrange
            coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal.ZERO
            coEvery { solanaGaslessSwapResolver.resolveFeePayer(any(), any(), any()) } returns null
            val state = buildSolanaQuotesLoadedState(fromBalance = BigDecimal("10"))
            val swapFee = buildSolanaSwapFee(feeValue = BigDecimal.ZERO, isSponsored = true)

            // Act
            val patched = sut.applySwapFee(state, swapFee, lastReducedBalanceBy)

            // Assert
            assertThat(patched.preparedSwapConfigState.balanceStatus)
                .isInstanceOf(SwapBalanceStatus.InsufficientFee::class.java)
        }

    @Test
    fun `GIVEN sponsored fee picked AND amount above balance WHEN applySwapFee THEN InsufficientAmount`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal.ZERO
        coEvery { solanaGaslessSwapResolver.resolveFeePayer(any(), any(), any()) } returns "FeePayer123"
        val state = buildSolanaQuotesLoadedState(fromBalance = BigDecimal("0.5"))
        val swapFee = buildSolanaSwapFee(feeValue = BigDecimal.ZERO, isSponsored = true)

        // Act
        val patched = sut.applySwapFee(state, swapFee, lastReducedBalanceBy)

        // Assert
        assertThat(patched.preparedSwapConfigState.balanceStatus).isEqualTo(SwapBalanceStatus.InsufficientAmount)
    }

    @Test
    fun `GIVEN fee paid in SOL AND not enough SOL WHEN applySwapFee THEN InsufficientFee and resolver not called`() =
        runTest {
            // Arrange
            coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal.ZERO
            coEvery { solanaGaslessSwapResolver.resolveFeePayer(any(), any(), any()) } returns "FeePayer123"
            val state = buildSolanaQuotesLoadedState(fromBalance = BigDecimal("10"))
            val swapFee = buildSolanaSwapFee(feeValue = BigDecimal("0.01"))

            // Act
            val patched = sut.applySwapFee(state, swapFee, lastReducedBalanceBy)

            // Assert
            assertThat(patched.preparedSwapConfigState.balanceStatus)
                .isInstanceOf(SwapBalanceStatus.InsufficientFee::class.java)
            coVerify(exactly = 0) { solanaGaslessSwapResolver.resolveFeePayer(any(), any(), any()) }
        }

    @ParameterizedTest
    @ProvideTestModels
    fun getSponsoredFeeAvailability(model: SponsoredFeeAvailabilityModel) = runTest {
        // Arrange
        coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns model.nativeBalance
        coEvery { solanaGaslessSwapResolver.resolveFeePayer(any(), any(), any()) } returns model.feePayer
        val state = buildSolanaQuotesLoadedState(fromBalance = BigDecimal("10"))

        // Act
        val offer = sut.getSponsoredFeeAvailability(state, userPaidFee = BigDecimal("0.01"), lastReducedBalanceBy)

        // Assert
        assertThat(offer).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN pair from a Solana token WHEN getPair THEN providers pass the resolver for the pair's from currency`() =
        runTest {
            // Arrange
            val pairModel = buildSwapPairModel(
                fromNetworkRawId = solanaNetwork,
                fromContractAddress = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
                toNetworkRawId = solanaNetwork,
            )
            coEvery { getSwapPairUseCase.invoke(any(), any(), any(), any()) } returns listOf(pairModel).right()
            coEvery {
                solanaGaslessSwapResolver.excludeUnsupportedProviders(any(), pairModel.from, any())
            } returns emptyList()

            // Act
            val pairs = sut.getPair(
                fromSwapCurrencyStatus = buildSwapCurrencyStatus(networkRawId = solanaNetwork),
                toSwapCurrencyStatus = buildSwapCurrencyStatus(networkRawId = solanaNetwork),
                filterProviderTypes = listOf(ExchangeProviderType.DEX),
            ).getOrNull()

            // Assert
            assertThat(pairs?.single()?.providers).isEmpty()
            coVerify(exactly = 1) {
                solanaGaslessSwapResolver.excludeUnsupportedProviders(any(), pairModel.from, any())
            }
        }

    @Test
    fun `GIVEN tokens in different wallets WHEN getPair THEN each direction is resolved in its from token's wallet`() =
        runTest {
            // Arrange
            val fromWalletId = UserWalletId("aa")
            val toWalletId = UserWalletId("bb")
            val from = buildSwapCurrencyStatus(networkRawId = solanaNetwork, userWalletId = fromWalletId)
            val to = buildSwapCurrencyStatus(networkRawId = solanaNetwork, userWalletId = toWalletId)
            val providers = listOf(buildExpressProvider())
            val pairs = listOf(
                SwapPairModel(from = from.status, to = to.status, providers = providers),
                SwapPairModel(from = to.status, to = from.status, providers = providers),
            )
            coEvery { getSwapPairUseCase.invoke(any(), any(), any(), any()) } returns pairs.right()

            // Act
            sut.getPair(
                fromSwapCurrencyStatus = from,
                toSwapCurrencyStatus = to,
                filterProviderTypes = listOf(ExchangeProviderType.DEX),
            )

            // Assert
            coVerify(exactly = 1) {
                solanaGaslessSwapResolver.excludeUnsupportedProviders(fromWalletId, from.status, any())
                solanaGaslessSwapResolver.excludeUnsupportedProviders(toWalletId, to.status, any())
            }
        }

    data class SponsoredFeeAvailabilityModel(
        val feePayer: String?,
        val nativeBalance: BigDecimal,
        val expected: SponsoredFeeAvailability,
    )

    private fun provideTestModels() = listOf(
        SponsoredFeeAvailabilityModel(feePayer = null, nativeBalance = BigDecimal.ZERO, expected = SponsoredFeeAvailability.Unavailable),
        SponsoredFeeAvailabilityModel(feePayer = "FeePayer123", nativeBalance = BigDecimal.ZERO, expected = SponsoredFeeAvailability.Preferred),
        SponsoredFeeAvailabilityModel(feePayer = "FeePayer123", nativeBalance = BigDecimal("10"), expected = SponsoredFeeAvailability.Available),
    )

    @Test
    fun `GIVEN SponsoredFee WHEN applySwapFee THEN getCurrencyCheckUseCase and validateTransactionUseCase receive zero fee`() =
        runTest {
            // Arrange
            coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal.ZERO
            coEvery { solanaGaslessSwapResolver.resolveFeePayer(any(), any(), any()) } returns "FeePayer123"
            val state = buildSolanaQuotesLoadedState(fromBalance = BigDecimal("10"))
            val swapFee = buildSolanaSwapFee(feeValue = BigDecimal.ZERO, isSponsored = true)

            // Act
            sut.applySwapFee(state, swapFee, lastReducedBalanceBy)

            // Assert
            coVerify {
                getCurrencyCheckUseCase.invoke(
                    userWalletId = any(),
                    currencyStatus = any(),
                    feeCurrencyStatus = any(),
                    amount = any(),
                    fee = BigDecimal.ZERO,
                    feeCurrencyBalanceAfterTransaction = any(),
                    recipientAddress = any(),
                )
            }
            coVerify {
                validateTransactionUseCase.invoke(
                    amount = any(),
                    fee = match { (it as? Fee.Common)?.amount?.value == BigDecimal.ZERO },
                    memo = any(),
                    destination = any(),
                    userWalletId = any(),
                    network = any(),
                )
            }
        }

    @Test
    fun `GIVEN SponsoredFee with dust SOL balance WHEN applySwapFee THEN no rent warning is raised`() = runTest {
        // Arrange: 0 < nativeBalance < fee — dust below the rent-exempt minimum, but left untouched by the sponsor
        coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal("0.0001")
        coEvery { solanaGaslessSwapResolver.resolveFeePayer(any(), any(), any()) } returns "FeePayer123"
        val state = buildSolanaQuotesLoadedState(fromBalance = BigDecimal("10"))
        val swapFee = buildSolanaSwapFee(feeValue = BigDecimal.ZERO, isSponsored = true)

        // Act
        sut.applySwapFee(state, swapFee, lastReducedBalanceBy)

        // Assert — null (not the dust balance) so GetCurrencyCheckUseCase's rent-exemption check never fires
        coVerify {
            getCurrencyCheckUseCase.invoke(
                userWalletId = any(),
                currencyStatus = any(),
                feeCurrencyStatus = any(),
                amount = any(),
                fee = any(),
                feeCurrencyBalanceAfterTransaction = null,
                recipientAddress = any(),
            )
        }
    }

    // region onSwap

    @Test
    fun `GIVEN SponsoredFee WHEN onSwap THEN gasless use case called with fee payer and sendTransactionUseCase not called, result TxSent with backend signature`() =
        runTest {
            // Arrange
            coEvery { solanaGaslessSwapResolver.resolveFeePayer(any(), any(), any()) } returns "FeePayer123"
            mockkStatic(Base64::class)
            every { Base64.decode(any<String>(), any()) } returns ByteArray(64)
            coEvery {
                createAndSendSolanaGaslessTransactionUseCase(
                    userWallet = any(),
                    network = any(),
                    transactionData = any(),
                    feePayer = "FeePayer123",
                )
            } returns "sig".right()

            // Act
            val result = sut.onSwap(
                fromSwapCurrencyStatus = solanaSwapCurrencyStatus(),
                toSwapCurrencyStatus = solanaSwapCurrencyStatus(),
                swapProvider = buildSwapProvider(ExchangeProviderType.DEX),
                swapData = solanaDexSwapData(),
                amountToSwap = "1.0",
                balanceStatus = SwapBalanceStatus.SponsoredFee(feePayer = "FeePayer123"),
                fee = null,
                expressOperationType = ExpressOperationType.SWAP,
                isTangemPayWithdrawal = false,
            )

            // Assert
            assertThat(result).isInstanceOf(SwapTransactionState.TxSent::class.java)
            assertThat((result as SwapTransactionState.TxSent).txHash).isEqualTo("sig")
            coVerify(exactly = 0) {
                sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
            }
            coVerify {
                repository.exchangeSent(
                    userWallet = any(),
                    txId = any(),
                    fromNetwork = any(),
                    fromAddress = any(),
                    payInAddress = any(),
                    txHash = "sig",
                    payInExtraId = any(),
                )
            }
        }

    @Test
    fun `GIVEN SponsoredFee AND service returns BlockhashExpired WHEN onSwap THEN Error SolanaGaslessError BlockhashExpired`() =
        runTest {
            // Arrange
            coEvery { solanaGaslessSwapResolver.resolveFeePayer(any(), any(), any()) } returns "FeePayer123"
            mockkStatic(Base64::class)
            every { Base64.decode(any<String>(), any()) } returns ByteArray(64)
            coEvery {
                createAndSendSolanaGaslessTransactionUseCase(
                    userWallet = any(),
                    network = any(),
                    transactionData = any(),
                    feePayer = any(),
                )
            } returns SolanaGaslessSendError.Service(SolanaGaslessError.BlockhashExpired).left()

            // Act
            val result = sut.onSwap(
                fromSwapCurrencyStatus = solanaSwapCurrencyStatus(),
                toSwapCurrencyStatus = solanaSwapCurrencyStatus(),
                swapProvider = buildSwapProvider(ExchangeProviderType.DEX),
                swapData = solanaDexSwapData(),
                amountToSwap = "1.0",
                balanceStatus = SwapBalanceStatus.SponsoredFee(feePayer = "FeePayer123"),
                fee = null,
                expressOperationType = ExpressOperationType.SWAP,
                isTangemPayWithdrawal = false,
            )

            // Assert
            assertThat(result).isEqualTo(SwapTransactionState.Error.SolanaGaslessError(SolanaGaslessError.BlockhashExpired))
        }

    @Test
    fun `GIVEN SponsoredFee AND service may have sent with signature WHEN onSwap THEN TxSent tracked`() =
        runTest {
            // Arrange
            coEvery { solanaGaslessSwapResolver.resolveFeePayer(any(), any(), any()) } returns "FeePayer123"
            mockkStatic(Base64::class)
            every { Base64.decode(any<String>(), any()) } returns ByteArray(64)
            val from = solanaSwapCurrencyStatus()
            coEvery {
                createAndSendSolanaGaslessTransactionUseCase(
                    userWallet = any(),
                    network = any(),
                    transactionData = any(),
                    feePayer = any(),
                )
            } returns SolanaGaslessSendError.Service(SolanaGaslessError.MaybeSent(signature = "sig")).left()

            // Act
            val result = sut.onSwap(
                fromSwapCurrencyStatus = from,
                toSwapCurrencyStatus = solanaSwapCurrencyStatus(),
                swapProvider = buildSwapProvider(ExchangeProviderType.DEX),
                swapData = solanaDexSwapData(),
                amountToSwap = "1.0",
                balanceStatus = SwapBalanceStatus.SponsoredFee(feePayer = "FeePayer123"),
                fee = null,
                expressOperationType = ExpressOperationType.SWAP,
                isTangemPayWithdrawal = false,
            )

            // Assert
            assertThat((result as SwapTransactionState.TxSent).txHash).isEqualTo("sig")
            coVerify {
                repository.exchangeSent(
                    userWallet = any(),
                    txId = any(),
                    fromNetwork = any(),
                    fromAddress = any(),
                    payInAddress = any(),
                    txHash = "sig",
                    payInExtraId = any(),
                )
            }
        }

    @Test
    fun `GIVEN SponsoredFee AND service may have sent without signature WHEN onSwap THEN MaybeSent error`() =
        runTest {
            // Arrange
            coEvery { solanaGaslessSwapResolver.resolveFeePayer(any(), any(), any()) } returns "FeePayer123"
            mockkStatic(Base64::class)
            every { Base64.decode(any<String>(), any()) } returns ByteArray(64)
            val from = solanaSwapCurrencyStatus()
            coEvery {
                createAndSendSolanaGaslessTransactionUseCase(
                    userWallet = any(),
                    network = any(),
                    transactionData = any(),
                    feePayer = any(),
                )
            } returns SolanaGaslessSendError.Service(SolanaGaslessError.MaybeSent(signature = null)).left()

            // Act
            val result = sut.onSwap(
                fromSwapCurrencyStatus = from,
                toSwapCurrencyStatus = solanaSwapCurrencyStatus(),
                swapProvider = buildSwapProvider(ExchangeProviderType.DEX),
                swapData = solanaDexSwapData(),
                amountToSwap = "1.0",
                balanceStatus = SwapBalanceStatus.SponsoredFee(feePayer = "FeePayer123"),
                fee = null,
                expressOperationType = ExpressOperationType.SWAP,
                isTangemPayWithdrawal = false,
            )

            // Assert
            assertThat(result)
                .isEqualTo(SwapTransactionState.Error.SolanaGaslessError(SolanaGaslessError.MaybeSent(signature = null)))
            coVerify(exactly = 0) {
                repository.exchangeSent(any(), any(), any(), any(), any(), any(), any())
            }
        }

    @Test
    fun `GIVEN SponsoredFee AND signing cancelled WHEN onSwap THEN Error TransactionError UserCancelledError`() =
        runTest {
            // Arrange
            coEvery { solanaGaslessSwapResolver.resolveFeePayer(any(), any(), any()) } returns "FeePayer123"
            mockkStatic(Base64::class)
            every { Base64.decode(any<String>(), any()) } returns ByteArray(64)
            coEvery {
                createAndSendSolanaGaslessTransactionUseCase(
                    userWallet = any(),
                    network = any(),
                    transactionData = any(),
                    feePayer = any(),
                )
            } returns SolanaGaslessSendError.Signing(SendTransactionError.UserCancelledError).left()

            // Act
            val result = sut.onSwap(
                fromSwapCurrencyStatus = solanaSwapCurrencyStatus(),
                toSwapCurrencyStatus = solanaSwapCurrencyStatus(),
                swapProvider = buildSwapProvider(ExchangeProviderType.DEX),
                swapData = solanaDexSwapData(),
                amountToSwap = "1.0",
                balanceStatus = SwapBalanceStatus.SponsoredFee(feePayer = "FeePayer123"),
                fee = null,
                expressOperationType = ExpressOperationType.SWAP,
                isTangemPayWithdrawal = false,
            )

            // Assert
            assertThat(result)
                .isEqualTo(SwapTransactionState.Error.TransactionError(SendTransactionError.UserCancelledError))
        }

    @Test
    fun `GIVEN sponsored fee picked AND quote still Pending WHEN onSwap THEN sent sponsored by the resolved payer`() =
        runTest {
            // Arrange
            mockkStatic(Base64::class)
            every { Base64.decode(any<String>(), any()) } returns ByteArray(64)
            coEvery { solanaGaslessSwapResolver.resolveFeePayer(any(), any(), any()) } returns "FeePayer123"
            coEvery {
                createAndSendSolanaGaslessTransactionUseCase(
                    userWallet = any(),
                    network = any(),
                    transactionData = any(),
                    feePayer = any(),
                )
            } returns "sig".right()

            // Act
            val result = sut.onSwap(
                fromSwapCurrencyStatus = solanaSwapCurrencyStatus(),
                toSwapCurrencyStatus = solanaSwapCurrencyStatus(),
                swapProvider = buildSwapProvider(ExchangeProviderType.DEX),
                swapData = solanaDexSwapData(),
                amountToSwap = "1.0",
                balanceStatus = SwapBalanceStatus.Pending,
                fee = buildSolanaSwapFee(feeValue = BigDecimal.ZERO, isSponsored = true),
                expressOperationType = ExpressOperationType.SWAP,
                isTangemPayWithdrawal = false,
            )

            // Assert
            assertThat((result as SwapTransactionState.TxSent).txHash).isEqualTo("sig")
            coVerify(exactly = 1) {
                createAndSendSolanaGaslessTransactionUseCase(
                    userWallet = any(),
                    network = any(),
                    transactionData = any(),
                    feePayer = "FeePayer123",
                )
            }
            coVerify(exactly = 0) { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) }
        }

    @ParameterizedTest
    @MethodSource("provideNoSponsorModels")
    fun `GIVEN sponsored swap AND no sponsor at send WHEN onSwap THEN not sent at all`(model: NoSponsorModel) =
        runTest {
            // Arrange
            mockkStatic(Base64::class)
            every { Base64.decode(any<String>(), any()) } returns ByteArray(64)
            coEvery { solanaGaslessSwapResolver.resolveFeePayer(any(), any(), any()) } returns null

            // Act
            val result = sut.onSwap(
                fromSwapCurrencyStatus = solanaSwapCurrencyStatus(),
                toSwapCurrencyStatus = solanaSwapCurrencyStatus(),
                swapProvider = buildSwapProvider(ExchangeProviderType.DEX),
                swapData = solanaDexSwapData(),
                amountToSwap = "1.0",
                balanceStatus = model.balanceStatus,
                fee = buildSolanaSwapFee(feeValue = BigDecimal.ZERO, isSponsored = true),
                expressOperationType = ExpressOperationType.SWAP,
                isTangemPayWithdrawal = false,
            )

            // Assert
            assertThat(result).isEqualTo(
                SwapTransactionState.Error.SolanaGaslessError(SolanaGaslessError.ServiceUnavailable(code = null)),
            )
            coVerify(exactly = 0) {
                createAndSendSolanaGaslessTransactionUseCase(
                    userWallet = any(),
                    network = any(),
                    transactionData = any(),
                    feePayer = any(),
                )
            }
            coVerify(exactly = 0) { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) }
        }

    data class NoSponsorModel(val balanceStatus: SwapBalanceStatus)

    private fun provideNoSponsorModels() = listOf(
        NoSponsorModel(balanceStatus = SwapBalanceStatus.Pending),
        // A sponsor stored in the quote that is gone by the send
        NoSponsorModel(balanceStatus = SwapBalanceStatus.SponsoredFee(feePayer = "FeePayer123")),
    )

    @Test
    fun `GIVEN InsufficientFee WHEN onSwap THEN old path sendTransactionUseCase`() = runTest {
        // Arrange
        mockkStatic(Base64::class)
        every { Base64.decode(any<String>(), any()) } returns ByteArray(64)
        coEvery {
            sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
        } returns "sig".right()

        // Act
        val result = sut.onSwap(
            fromSwapCurrencyStatus = solanaSwapCurrencyStatus(),
            toSwapCurrencyStatus = solanaSwapCurrencyStatus(),
            swapProvider = buildSwapProvider(ExchangeProviderType.DEX),
            swapData = solanaDexSwapData(),
            amountToSwap = "1.0",
            balanceStatus = SwapBalanceStatus.InsufficientFee(
                feeCurrencyName = "Solana",
                feeCurrencySymbol = "SOL",
            ),
            fee = null,
            expressOperationType = ExpressOperationType.SWAP,
            isTangemPayWithdrawal = false,
        )

        // Assert
        assertThat(result).isInstanceOf(SwapTransactionState.TxSent::class.java)
        coVerify(exactly = 0) {
            createAndSendSolanaGaslessTransactionUseCase(
                userWallet = any(),
                network = any(),
                transactionData = any(),
                feePayer = any(),
            )
        }
        coVerify(exactly = 1) {
            sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
        }
    }

    @Test
    fun `GIVEN quote still SponsoredFee AND paid fee picked WHEN onSwap THEN sent with the paid fee`() = runTest {
        // Arrange
        mockkStatic(Base64::class)
        every { Base64.decode(any<String>(), any()) } returns ByteArray(64)
        coEvery { solanaGaslessSwapResolver.resolveFeePayer(any(), any(), any()) } returns "FeePayer123"
        coEvery {
            sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
        } returns "sig".right()

        // Act
        val result = sut.onSwap(
            fromSwapCurrencyStatus = solanaSwapCurrencyStatus(),
            toSwapCurrencyStatus = solanaSwapCurrencyStatus(),
            swapProvider = buildSwapProvider(ExchangeProviderType.DEX),
            swapData = solanaDexSwapData(),
            amountToSwap = "1.0",
            balanceStatus = SwapBalanceStatus.SponsoredFee(feePayer = "FeePayer123"),
            fee = buildSolanaSwapFee(feeValue = BigDecimal("0.000005"), isSponsored = false),
            expressOperationType = ExpressOperationType.SWAP,
            isTangemPayWithdrawal = false,
        )

        // Assert
        assertThat(result).isInstanceOf(SwapTransactionState.TxSent::class.java)
        coVerify(exactly = 0) {
            createAndSendSolanaGaslessTransactionUseCase(
                userWallet = any(),
                network = any(),
                transactionData = any(),
                feePayer = any(),
            )
        }
        coVerify(exactly = 1) { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) }
    }

    /** Backed by an explicit plain [UserWallet] mock so the `is UserWallet.Cold` demo check is false. */
    private fun solanaSwapCurrencyStatus(): SwapCurrencyStatus {
        val wallet = mockk<UserWallet>(relaxed = true)
        return buildSwapCurrencyStatus(networkRawId = solanaNetwork, decimals = 9).let {
            SwapCurrencyStatus(userWallet = wallet, status = it.status, account = it.account)
        }
    }

    private fun solanaDexSwapData(): SwapDataModel = SwapDataModel(
        toTokenAmount = SwapAmount(BigDecimal("0.5"), 9),
        transaction = ExpressTransactionModel.DEX(
            fromAmount = SwapAmount(BigDecimal("1.0"), 9),
            toAmount = SwapAmount(BigDecimal("0.5"), 9),
            txValue = null,
            txId = "solana-tx-id",
            txTo = "SolanaRouter",
            txExtraId = null,
            txFrom = "SolanaFrom",
            txData = "c29sYW5hLXR4LWRhdGE=",
            otherNativeFeeWei = null,
            gas = null,
            allowanceContract = null,
        ),
    )

    // endregion

    private fun buildCurrencyCheck(): CryptoCurrencyCheck = CryptoCurrencyCheck(
        dustValue = null,
        reserveAmount = null,
        minimumSendAmount = null,
        existentialDeposit = null,
        utxoAmountLimit = null,
        isAccountFunded = true,
        rentWarning = null,
        isMemoRequired = false,
    )

    private fun buildSolanaQuotesLoadedState(fromBalance: BigDecimal): SwapState.QuotesLoadedState {
        val from = buildSwapCurrencyStatus(
            networkRawId = solanaNetwork,
            isCoin = false,
            amount = fromBalance,
            decimals = 9,
        )
        val to = buildSwapCurrencyStatus(networkRawId = solanaNetwork, decimals = 9)
        return SwapState.QuotesLoadedState(
            fromTokenInfo = TokenSwapInfo(
                tokenAmount = SwapAmount(BigDecimal("1"), 9),
                swapCurrencyStatus = from,
                amountFiat = BigDecimal.ZERO,
            ),
            toTokenInfo = TokenSwapInfo(
                tokenAmount = SwapAmount(BigDecimal("0.5"), 9),
                swapCurrencyStatus = to,
                amountFiat = BigDecimal.ZERO,
            ),
            priceImpact = PriceImpact.Empty,
            preparedSwapConfigState = PreparedSwapConfigState(
                balanceStatus = SwapBalanceStatus.Pending,
                hasOutgoingTransaction = false,
            ),
            permissionState = PermissionDataState.Empty,
            swapDataModel = null,
            currencyCheck = null,
            validationResult = null,
            minAdaValue = null,
            swapProvider = buildSwapProvider(ExchangeProviderType.DEX),
        )
    }

    private fun buildSolanaSwapFee(feeValue: BigDecimal, isSponsored: Boolean = false): SwapFee {
        val amount = mockk<Amount>(relaxed = true) {
            every { value } returns feeValue
        }
        val fee = mockk<Fee.Common>(relaxed = true) {
            every { this@mockk.amount } returns amount
        }
        val feeTokenStatus = mockk<CryptoCurrencyStatus>(relaxed = true) {
            every { currency } returns buildCoinCurrency(networkRawId = solanaNetwork)
        }
        return SwapFee(
            fee = fee,
            transactionFeeResult = TransactionFeeResult.Loaded(mockk<TransactionFee.Single>(relaxed = true)),
            selectedFeeToken = feeTokenStatus,
            otherNativeFee = BigDecimal.ZERO,
            feeBucket = FeeBucket.MARKET,
            isSponsored = isSponsored,
        )
    }
}