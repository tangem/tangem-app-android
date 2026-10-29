package com.tangem.feature.swap.domain.fee

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.solana.SolanaTransactionHelper
import com.tangem.blockchain.blockchains.solana.gasless.SolanaGaslessSimulation
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.SolanaGaslessTransactionRepository
import com.tangem.domain.transaction.error.SolanaGaslessError
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.feature.swap.domain.buildSwapCurrencyStatus
import com.tangem.feature.swap.domain.buildSwapProvider
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.features.swap.SwapFeatureToggles
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Base64

/**
 * Unit tests for [SolanaGaslessSwapResolver].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SolanaGaslessSwapResolverTest {

    private val solanaNetwork = Blockchain.Solana.toNetworkId()
    private val ethNetwork = Blockchain.Ethereum.toNetworkId()

    private val swapFeatureToggles: SwapFeatureToggles = mockk()
    private val solanaGaslessTransactionRepository: SolanaGaslessTransactionRepository = mockk()
    private val walletManagersFacade: WalletManagersFacade = mockk()

    // Recreated per test: the resolver caches its last resolution
    private lateinit var sut: SolanaGaslessSwapResolver

    @BeforeEach
    fun setup() {
        sut = createResolver()
        clearMocks(swapFeatureToggles, solanaGaslessTransactionRepository, walletManagersFacade)
        every { swapFeatureToggles.isSolanaGaslessEnabled } returns true
        coEvery {
            walletManagersFacade.simulateSolanaGaslessTransaction(
                userWalletId = any(), network = any(), transactionData = any(), feePayer = any(),
            )
        } returns Result.Success(SolanaGaslessSimulation.Succeeded)
    }

    @Test
    fun `GIVEN toggle disabled WHEN resolveFeePayer THEN returns null and getFeePayer is not called`() = runTest {
        // Arrange
        every { swapFeatureToggles.isSolanaGaslessEnabled } returns false
        val from = buildSwapCurrencyStatus(networkRawId = solanaNetwork, isCoin = false)
        val swapData = buildDexSwapData(message = providerMessage(size = SIZE_FITS))

        // Act
        val actual = sut.resolveFeePayer(from, swapData)

        // Assert
        assertThat(actual).isNull()
        coVerify(exactly = 0) { solanaGaslessTransactionRepository.getFeePayer() }
    }

    @Test
    fun `GIVEN network is not Solana WHEN resolveFeePayer THEN returns null`() = runTest {
        // Arrange
        val from = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = false)
        val swapData = buildDexSwapData(message = providerMessage(size = SIZE_FITS))

        // Act
        val actual = sut.resolveFeePayer(from, swapData)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN from currency is the SOL coin WHEN resolveFeePayer THEN returns null`() = runTest {
        // Arrange
        val from = buildSwapCurrencyStatus(networkRawId = solanaNetwork, isCoin = true)
        val swapData = buildDexSwapData(message = providerMessage(size = SIZE_FITS))

        // Act
        val actual = sut.resolveFeePayer(from, swapData)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN message exceeds gasless size limit WHEN resolveFeePayer THEN returns null`() = runTest {
        // Arrange
        val from = buildSwapCurrencyStatus(networkRawId = solanaNetwork, isCoin = false)
        val swapData = buildDexSwapData(message = providerMessage(size = SIZE_TOO_BIG))

        // Act
        val actual = sut.resolveFeePayer(from, swapData)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN getFeePayer returns Unauthorized WHEN resolveFeePayer THEN returns null`() = runTest {
        // Arrange
        val from = buildSwapCurrencyStatus(networkRawId = solanaNetwork, isCoin = false)
        val swapData = buildDexSwapData(message = providerMessage(size = SIZE_FITS))
        coEvery { solanaGaslessTransactionRepository.getFeePayer() } returns SolanaGaslessError.Unauthorized.left()

        // Act
        val actual = sut.resolveFeePayer(from, swapData)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN same quote resolved before WHEN resolveFeePayer THEN service and node are not asked again`() = runTest {
        // Arrange
        val from = buildSwapCurrencyStatus(networkRawId = solanaNetwork, isCoin = false)
            .copy(userWallet = mockk<UserWallet.Hot>(relaxed = true))
        val swapData = buildDexSwapData(message = providerMessage(size = SIZE_FITS))
        coEvery { solanaGaslessTransactionRepository.getFeePayer() } returns "FeePayer".right()
        sut.resolveFeePayer(from, swapData)

        // Act
        val actual = sut.resolveFeePayer(from, swapData)

        // Assert
        assertThat(actual).isEqualTo("FeePayer")
        coVerify(exactly = 1) { solanaGaslessTransactionRepository.getFeePayer() }
        coVerify(exactly = 1) {
            walletManagersFacade.simulateSolanaGaslessTransaction(
                userWalletId = any(), network = any(), transactionData = any(), feePayer = any(),
            )
        }
    }

    @Test
    fun `GIVEN quote too big resolved before WHEN resolveFeePayer for the same quote THEN tx is not parsed again`() =
        runTest {
            // Arrange
            val from = buildSwapCurrencyStatus(networkRawId = solanaNetwork, isCoin = false)
                .copy(userWallet = mockk<UserWallet.Hot>(relaxed = true))
            val swapData = buildDexSwapData(message = providerMessage(size = SIZE_TOO_BIG))
            mockkObject(SolanaTransactionHelper)
            try {
                sut.resolveFeePayer(from, swapData)

                // Act
                val actual = sut.resolveFeePayer(from, swapData)

                // Assert
                assertThat(actual).isNull()
                verify(exactly = 1) { SolanaTransactionHelper.requiredSignerCount(any()) }
            } finally {
                unmockkObject(SolanaTransactionHelper)
            }
        }

    @Test
    fun `GIVEN getFeePayer failed before WHEN resolveFeePayer for the same quote THEN service is asked again`() =
        runTest {
            // Arrange
            val from = buildSwapCurrencyStatus(networkRawId = solanaNetwork, isCoin = false)
                .copy(userWallet = mockk<UserWallet.Hot>(relaxed = true))
            val swapData = buildDexSwapData(message = providerMessage(size = SIZE_FITS))
            coEvery { solanaGaslessTransactionRepository.getFeePayer() } returnsMany listOf(
                SolanaGaslessError.Unauthorized.left(),
                "FeePayer".right(),
            )
            sut.resolveFeePayer(from, swapData)

            // Act
            val actual = sut.resolveFeePayer(from, swapData)

            // Assert
            assertThat(actual).isEqualTo("FeePayer")
        }

    @Test
    fun `GIVEN quote carries a bridge fee WHEN resolveFeePayer THEN returns null and getFeePayer is not called`() =
        runTest {
            // Arrange
            val from = buildSwapCurrencyStatus(networkRawId = solanaNetwork, isCoin = false)
            val swapData = buildDexSwapData(
                message = providerMessage(size = SIZE_FITS),
                otherNativeFeeWei = BigDecimal("5000"),
            )

            // Act
            val actual = sut.resolveFeePayer(from, swapData)

            // Assert
            assertThat(actual).isNull()
            coVerify(exactly = 0) { solanaGaslessTransactionRepository.getFeePayer() }
        }

    @Test
    fun `GIVEN everything fits WHEN resolveFeePayer THEN returns the fee payer`() = runTest {
        // Arrange
        val from = buildSwapCurrencyStatus(networkRawId = solanaNetwork, isCoin = false)
            .copy(userWallet = mockk<UserWallet.Hot>(relaxed = true))
        val swapData = buildDexSwapData(message = providerMessage(size = SIZE_FITS))
        coEvery { solanaGaslessTransactionRepository.getFeePayer() } returns "FeePayer".right()

        // Act
        val actual = sut.resolveFeePayer(from, swapData)

        // Assert
        assertThat(actual).isEqualTo("FeePayer")
    }

    @Test
    fun `GIVEN simulation fails WHEN resolveFeePayer THEN returns null`() = runTest {
        // Arrange
        val from = buildSwapCurrencyStatus(networkRawId = solanaNetwork, isCoin = false)
            .copy(userWallet = mockk<UserWallet.Hot>(relaxed = true))
        val swapData = buildDexSwapData(message = providerMessage(size = SIZE_FITS))
        coEvery { solanaGaslessTransactionRepository.getFeePayer() } returns "FeePayer".right()
        coEvery {
            walletManagersFacade.simulateSolanaGaslessTransaction(
                userWalletId = any(), network = any(), transactionData = any(), feePayer = "FeePayer",
            )
        } returns Result.Success(SolanaGaslessSimulation.Failed(error = "InsufficientFundsForRent", logs = emptyList()))

        // Act
        val actual = sut.resolveFeePayer(from, swapData)

        // Assert
        assertThat(actual).isNull()
    }

    @ParameterizedTest
    @MethodSource("provideUnavailableSimulations")
    fun `GIVEN simulation unavailable WHEN resolveFeePayer THEN returns the fee payer`(
        simulation: Result<SolanaGaslessSimulation>?,
    ) = runTest {
        // Arrange
        val from = buildSwapCurrencyStatus(networkRawId = solanaNetwork, isCoin = false)
            .copy(userWallet = mockk<UserWallet.Hot>(relaxed = true))
        val swapData = buildDexSwapData(message = providerMessage(size = SIZE_FITS))
        coEvery { solanaGaslessTransactionRepository.getFeePayer() } returns "FeePayer".right()
        coEvery {
            walletManagersFacade.simulateSolanaGaslessTransaction(
                userWalletId = any(), network = any(), transactionData = any(), feePayer = any(),
            )
        } returns simulation

        // Act
        val actual = sut.resolveFeePayer(from, swapData)

        // Assert
        assertThat(actual).isEqualTo("FeePayer")
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN wallet type and message size WHEN resolveFeePayer THEN offered within the wallet's limit`(
        model: SizeLimitModel,
    ) = runTest {
        // Arrange
        val from = buildSwapCurrencyStatus(networkRawId = solanaNetwork, isCoin = false)
            .copy(userWallet = model.userWallet)
        val swapData = buildDexSwapData(message = providerMessage(size = model.messageSize))
        coEvery { solanaGaslessTransactionRepository.getFeePayer() } returns "FeePayer".right()

        // Act
        val actual = sut.resolveFeePayer(from, swapData)

        // Assert
        assertThat(actual).isEqualTo(if (model.isOffered) "FeePayer" else null)
    }

    @Test
    fun `GIVEN provider tx needs a second signer WHEN resolveFeePayer THEN returns null`() = runTest {
        // Arrange
        val from = buildSwapCurrencyStatus(networkRawId = solanaNetwork, isCoin = false)
            .copy(userWallet = mockk<UserWallet.Hot>(relaxed = true))
        val swapData = buildDexSwapData(message = providerMessage(size = SIZE_FITS, signerCount = 2), signerCount = 2)
        coEvery { solanaGaslessTransactionRepository.getFeePayer() } returns "FeePayer".right()

        // Act
        val actual = sut.resolveFeePayer(from, swapData)

        // Assert
        assertThat(actual).isNull()
        coVerify(exactly = 0) { solanaGaslessTransactionRepository.getFeePayer() }
    }

    @ParameterizedTest
    @MethodSource("provideUnsupportedProviderModels")
    fun `GIVEN LI_FI among providers WHEN excludeUnsupportedProviders THEN dropped only where the swap is sponsorable`(
        model: UnsupportedProviderModel,
    ) {
        // Arrange
        every { swapFeatureToggles.isSolanaGaslessEnabled } returns model.isToggleEnabled
        val from = buildSwapCurrencyStatus(networkRawId = model.fromNetwork, isCoin = model.isFromCoin)
        val liFi = buildSwapProvider(providerId = "li-fi")
        val jupiter = buildSwapProvider(providerId = "jupiter")

        // Act
        val actual = sut.excludeUnsupportedProviders(from.currency, listOf(liFi, jupiter))

        // Assert
        val expected = if (model.isLiFiKept) listOf(liFi, jupiter) else listOf(jupiter)
        assertThat(actual).containsExactlyElementsIn(expected).inOrder()
    }

    data class UnsupportedProviderModel(
        val fromNetwork: String,
        val isFromCoin: Boolean,
        val isToggleEnabled: Boolean,
        val isLiFiKept: Boolean,
    )

    data class SizeLimitModel(val userWallet: UserWallet, val messageSize: Int, val isOffered: Boolean)

    private fun createResolver() = SolanaGaslessSwapResolver(
        swapFeatureToggles = swapFeatureToggles,
        solanaGaslessTransactionRepository = solanaGaslessTransactionRepository,
        walletManagersFacade = walletManagersFacade,
    )

    private fun provideTestModels() = listOf(
        SizeLimitModel(
            userWallet = mockk<UserWallet.Cold>(relaxed = true),
            messageSize = COLD_SIZE_FITS,
            isOffered = true,
        ),
        SizeLimitModel(
            userWallet = mockk<UserWallet.Cold>(relaxed = true),
            messageSize = COLD_SIZE_FITS + 1,
            isOffered = false,
        ),
        SizeLimitModel(userWallet = mockk<UserWallet.Hot>(relaxed = true), messageSize = SIZE_FITS, isOffered = true),
        SizeLimitModel(
            userWallet = mockk<UserWallet.Hot>(relaxed = true),
            messageSize = SIZE_TOO_BIG,
            isOffered = false,
        ),
    )

    private fun provideUnsupportedProviderModels() = listOf(
        UnsupportedProviderModel(
            fromNetwork = solanaNetwork,
            isFromCoin = false,
            isToggleEnabled = true,
            isLiFiKept = false,
        ),
        UnsupportedProviderModel(
            fromNetwork = solanaNetwork,
            isFromCoin = false,
            isToggleEnabled = false,
            isLiFiKept = true,
        ),
        UnsupportedProviderModel(
            fromNetwork = solanaNetwork,
            isFromCoin = true,
            isToggleEnabled = true,
            isLiFiKept = true,
        ),
        UnsupportedProviderModel(
            fromNetwork = ethNetwork,
            isFromCoin = false,
            isToggleEnabled = true,
            isLiFiKept = true,
        ),
    )

    private fun provideUnavailableSimulations() = listOf(
        Result.Failure(BlockchainSdkError.CustomError("node unreachable")),
        null,
    )

    /** A legacy message header with [signerCount] required signers, padded with zeros to [size] bytes. */
    private fun providerMessage(size: Int, signerCount: Int = 1): ByteArray {
        val header = byteArrayOf(signerCount.toByte(), 0, 0, signerCount.toByte()) +
            ByteArray(signerCount * PUBLIC_KEY_LENGTH) { 0x11 }
        return header + ByteArray(size - header.size)
    }

    private fun buildDexSwapData(
        message: ByteArray,
        signerCount: Int = 1,
        otherNativeFeeWei: BigDecimal? = null,
    ): SwapDataModel {
        val txData = Base64.getEncoder().encodeToString(
            byteArrayOf(signerCount.toByte()) + ByteArray(signerCount * SIGNATURE_LENGTH) + message,
        )
        return SwapDataModel(
            toTokenAmount = SwapAmount(BigDecimal("0.5"), 9),
            transaction = ExpressTransactionModel.DEX(
                fromAmount = SwapAmount(BigDecimal.ONE, 9),
                toAmount = SwapAmount(BigDecimal("0.5"), 9),
                txValue = "0",
                txId = "tx-id-123",
                txTo = "SolanaProgram",
                txExtraId = null,
                txFrom = "SolanaSender",
                txData = txData,
                otherNativeFeeWei = otherNativeFeeWei,
                gas = BigInteger.ZERO,
            ),
        )
    }

    private companion object {
        const val SIGNATURE_LENGTH = 64
        const val SIZE_FITS = 931 // exactly SolanaTransactionHelper.MAX_GASLESS_MESSAGE_SIZE — boundary case
        const val SIZE_TOO_BIG = 932
        const val PUBLIC_KEY_LENGTH = 32

        // The card signs up to 930 bytes and the fee payer rewrite adds a 32-byte key
        const val COLD_SIZE_FITS = 898
    }
}