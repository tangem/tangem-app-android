package com.tangem.domain.staking

import arrow.core.left
import arrow.core.right
import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.domain.blockaid.models.transaction.SimulationResult
import com.domain.blockaid.models.transaction.TransactionData
import com.domain.blockaid.models.transaction.TransactionParams
import com.domain.blockaid.models.transaction.ValidationResult
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.domain.blockaid.BlockAidVerifier
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import com.tangem.domain.staking.verification.StakingBlockAidRequestFactory
import com.tangem.domain.staking.verification.StakingTransactionRecognizer
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CheckStakingTransactionUseCaseTest {

    private val blockAidVerifier: BlockAidVerifier = mockk()
    private val recognizer: StakingTransactionRecognizer = mockk()
    private val requestFactory: StakingBlockAidRequestFactory = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val analyticsExceptionHandler: AnalyticsExceptionHandler = mockk(relaxed = true)

    private val useCase = CheckStakingTransactionUseCase(
        blockAidVerifier = blockAidVerifier,
        recognizer = recognizer,
        requestFactory = requestFactory,
        analyticsEventHandler = analyticsEventHandler,
        analyticsExceptionHandler = analyticsExceptionHandler,
    )

    private val transactionData = TransactionData(
        chain = "polygon",
        accountAddress = "0xaa",
        method = "eth_sendTransaction",
        domainUrl = "",
        params = TransactionParams.Evm("[{}]"),
    )

    @BeforeEach
    fun setup() {
        clearMocks(
            blockAidVerifier,
            recognizer,
            requestFactory,
            analyticsEventHandler,
            analyticsExceptionHandler,
        )
        every { requestFactory.create(any(), any(), any()) } returns transactionData
    }

    private fun blockAidResult(validation: ValidationResult) = CheckTransactionResult(
        validation = validation,
        description = "desc",
        simulation = SimulationResult.FailedToSimulate,
    )

    private fun stubBlockAid(validation: ValidationResult) {
        coEvery { blockAidVerifier.verifyTransaction(transactionData) } returns blockAidResult(validation).right()
    }

    private fun stubRecognized(network: NetworkType, unsigned: String, recognized: Boolean) {
        every { recognizer.isRecognizedStakingTransaction(network, unsigned) } returns recognized
    }

    private fun verifyScamEvent(token: String, blockchain: String, blockaid: String, mobileCheck: String) {
        verify(exactly = 1) {
            analyticsEventHandler.send(
                StakingAnalyticsEvent.ScamVerification(
                    token = token,
                    blockchain = blockchain,
                    provider = "StakeKit",
                    blockaid = blockaid,
                    mobileCheck = mobileCheck,
                ),
            )
        }
    }

    private suspend fun checkEvm(unsigned: String? = "{}") =
        useCase(NetworkType.BINANCE, "0xaa", unsigned, "BNB", "BSC", "StakeKit")

    private suspend fun checkEthereum() = useCase(NetworkType.ETHEREUM, "0xaa", "{}", "ETH", "Ethereum", "StakeKit")

    private suspend fun checkSolana(unsigned: String? = "00") =
        useCase(NetworkType.SOLANA, "0xaa", unsigned, "SOL", "Solana", "StakeKit")

    private suspend fun checkTron() = useCase(NetworkType.TRON, "addr", "{}", "TRX", "Tron", "StakeKit")

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NullPayload {

        @Test
        fun `GIVEN null unsigned tx WHEN invoke THEN UNSAFE and no analytics`() = runTest {
            // Act
            val actual = checkEvm(unsigned = null)

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.UNSAFE)
            verify(exactly = 0) { analyticsEventHandler.send(any()) }
        }

        @Test
        fun `GIVEN null unsigned tx WHEN invoke THEN non-fatal not sent`() = runTest {
            // Act
            checkSolana(unsigned = null)

            // Assert
            verify(exactly = 0) { analyticsExceptionHandler.sendException(any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Binance {

        @Test
        fun `GIVEN evm not recognized locally WHEN invoke THEN UNSAFE and blockaid not called`() = runTest {
            // Arrange
            stubRecognized(NetworkType.BINANCE, "{}", recognized = false)

            // Act
            val actual = checkEvm()

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.UNSAFE)
            coVerify(exactly = 0) { blockAidVerifier.verifyTransaction(any()) }
        }

        @Test
        fun `GIVEN evm not recognized WHEN invoke THEN event has Blockaid not performed and Mobile_check false`() =
            runTest {
                // Arrange
                stubRecognized(NetworkType.BINANCE, "{}", recognized = false)

                // Act
                checkEvm()

                // Assert
                verifyScamEvent("BNB", "BSC", blockaid = "Not performed", mobileCheck = "false")
            }

        @Test
        fun `GIVEN evm recognized AND blockaid SAFE WHEN invoke THEN SAFE`() = runTest {
            // Arrange
            stubRecognized(NetworkType.BINANCE, "{}", recognized = true)
            stubBlockAid(ValidationResult.SAFE)

            // Act
            val actual = checkEvm()

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.SAFE)
            verify(exactly = 1) { requestFactory.create(NetworkType.BINANCE, "0xaa", "{}") }
        }

        @Test
        fun `GIVEN evm recognized AND blockaid SAFE WHEN invoke THEN event has Blockaid Safe and Mobile_check true`() =
            runTest {
                // Arrange
                stubRecognized(NetworkType.BINANCE, "{}", recognized = true)
                stubBlockAid(ValidationResult.SAFE)

                // Act
                checkEvm()

                // Assert
                verifyScamEvent("BNB", "BSC", blockaid = "Safe", mobileCheck = "true")
            }

        @Test
        fun `GIVEN evm recognized AND blockaid WARNING WHEN invoke THEN WARNING`() = runTest {
            // Arrange
            stubRecognized(NetworkType.BINANCE, "{}", recognized = true)
            stubBlockAid(ValidationResult.WARNING)

            // Act
            val actual = checkEvm()

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.WARNING)
        }

        @Test
        fun `GIVEN evm recognized AND blockaid WARNING WHEN invoke THEN event has Blockaid Warning`() = runTest {
            // Arrange
            stubRecognized(NetworkType.BINANCE, "{}", recognized = true)
            stubBlockAid(ValidationResult.WARNING)

            // Act
            checkEvm()

            // Assert
            verifyScamEvent("BNB", "BSC", blockaid = "Warning", mobileCheck = "true")
        }

        @Test
        fun `GIVEN evm recognized AND blockaid UNSAFE WHEN invoke THEN UNSAFE`() = runTest {
            // Arrange
            stubRecognized(NetworkType.BINANCE, "{}", recognized = true)
            stubBlockAid(ValidationResult.UNSAFE)

            // Act
            val actual = checkEvm()

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.UNSAFE)
        }

        @Test
        fun `GIVEN evm recognized AND blockaid UNSAFE WHEN invoke THEN event has Blockaid Unsafe`() = runTest {
            // Arrange
            stubRecognized(NetworkType.BINANCE, "{}", recognized = true)
            stubBlockAid(ValidationResult.UNSAFE)

            // Act
            checkEvm()

            // Assert
            verifyScamEvent("BNB", "BSC", blockaid = "Unsafe", mobileCheck = "true")
        }

        @Test
        fun `GIVEN evm recognized AND blockaid FAILED_TO_VALIDATE WHEN invoke THEN SAFE`() = runTest {
            // Arrange
            stubRecognized(NetworkType.BINANCE, "{}", recognized = true)
            stubBlockAid(ValidationResult.FAILED_TO_VALIDATE)

            // Act
            val actual = checkEvm()

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.SAFE)
        }

        @Test
        fun `GIVEN evm recognized AND blockaid FAILED_TO_VALIDATE WHEN invoke THEN event has Blockaid Failed to validate`() =
            runTest {
                // Arrange
                stubRecognized(NetworkType.BINANCE, "{}", recognized = true)
                stubBlockAid(ValidationResult.FAILED_TO_VALIDATE)

                // Act
                checkEvm()

                // Assert
                verifyScamEvent("BNB", "BSC", blockaid = "Failed to validate", mobileCheck = "true")
            }

        @Test
        fun `GIVEN evm recognized AND blockaid returns left WHEN invoke THEN SAFE`() = runTest {
            // Arrange
            stubRecognized(NetworkType.BINANCE, "{}", recognized = true)
            coEvery { blockAidVerifier.verifyTransaction(transactionData) } returns RuntimeException("network").left()

            // Act
            val actual = checkEvm()

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.SAFE)
        }

        @Test
        fun `GIVEN evm recognized AND blockaid throws WHEN invoke THEN SAFE`() = runTest {
            // Arrange
            stubRecognized(NetworkType.BINANCE, "{}", recognized = true)
            coEvery { blockAidVerifier.verifyTransaction(transactionData) } throws RuntimeException("boom")

            // Act
            val actual = checkEvm()

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.SAFE)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Ethereum {

        @Test
        fun `GIVEN ethereum WHEN invoke THEN blockaid called without local recognition`() = runTest {
            // Arrange
            stubBlockAid(ValidationResult.SAFE)

            // Act
            val actual = checkEthereum()

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.SAFE)
            coVerify(exactly = 1) { blockAidVerifier.verifyTransaction(transactionData) }
            verify(exactly = 0) { recognizer.isRecognizedStakingTransaction(any(), any()) }
        }

        @Test
        fun `GIVEN ethereum AND blockaid SAFE WHEN invoke THEN event Blockaid Safe and Mobile_check true`() = runTest {
            // Arrange
            stubBlockAid(ValidationResult.SAFE)

            // Act
            checkEthereum()

            // Assert
            verifyScamEvent("ETH", "Ethereum", blockaid = "Safe", mobileCheck = "true")
        }

        @Test
        fun `GIVEN ethereum AND blockaid UNSAFE WHEN invoke THEN UNSAFE`() = runTest {
            // Arrange
            stubBlockAid(ValidationResult.UNSAFE)

            // Act
            val actual = checkEthereum()

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.UNSAFE)
        }

        @Test
        fun `GIVEN ethereum AND blockaid no answer WHEN invoke THEN SAFE`() = runTest {
            // Arrange
            coEvery { blockAidVerifier.verifyTransaction(transactionData) } returns RuntimeException("network").left()

            // Act
            val actual = checkEthereum()

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.SAFE)
        }

        @Test
        fun `GIVEN ethereum WHEN invoke THEN request built with network address and payload`() = runTest {
            // Arrange
            stubBlockAid(ValidationResult.SAFE)

            // Act
            useCase(NetworkType.ETHEREUM, "0xaa", "{}", "ETH", "Ethereum", "StakeKit")

            // Assert
            verify(exactly = 1) { requestFactory.create(NetworkType.ETHEREUM, "0xaa", "{}") }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Solana {

        @Test
        fun `GIVEN solana recognized AND blockaid SAFE WHEN invoke THEN SAFE`() = runTest {
            // Arrange
            stubRecognized(NetworkType.SOLANA, "00", recognized = true)
            stubBlockAid(ValidationResult.SAFE)

            // Act
            val actual = checkSolana()

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.SAFE)
            verify(exactly = 1) { requestFactory.create(NetworkType.SOLANA, "0xaa", "00") }
        }

        @Test
        fun `GIVEN blockaid throws CancellationException WHEN invoke THEN exception rethrown`() = runTest {
            // Arrange
            stubRecognized(NetworkType.SOLANA, "00", recognized = true)
            coEvery { blockAidVerifier.verifyTransaction(transactionData) } throws CancellationException("cancelled")

            // Act
            val error = runCatching { checkSolana() }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(CancellationException::class.java)
            verify(exactly = 0) { analyticsEventHandler.send(any()) }
            verify(exactly = 0) { analyticsExceptionHandler.sendException(any()) }
        }

        @Test
        fun `GIVEN request factory throws WHEN invoke THEN SAFE with failed to validate event`() = runTest {
            // Arrange
            stubRecognized(NetworkType.SOLANA, "00", recognized = true)
            every { requestFactory.create(NetworkType.SOLANA, any(), any()) } throws
                IllegalArgumentException("unsupported")

            // Act
            val actual = checkSolana()

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.SAFE)
            coVerify(exactly = 0) { blockAidVerifier.verifyTransaction(any()) }
            verifyScamEvent("SOL", "Solana", blockaid = "Failed to validate", mobileCheck = "true")
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Tron {

        @Test
        fun `GIVEN tron recognized WHEN invoke THEN SAFE and blockaid not called`() = runTest {
            // Arrange
            stubRecognized(NetworkType.TRON, "{}", recognized = true)

            // Act
            val actual = checkTron()

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.SAFE)
            coVerify(exactly = 0) { blockAidVerifier.verifyTransaction(any()) }
        }

        @Test
        fun `GIVEN tron recognized WHEN invoke THEN event has Blockaid not performed and Mobile_check true`() =
            runTest {
                // Arrange
                stubRecognized(NetworkType.TRON, "{}", recognized = true)

                // Act
                checkTron()

                // Assert
                verifyScamEvent("TRX", "Tron", blockaid = "Not performed", mobileCheck = "true")
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Cosmos {

        @Test
        fun `GIVEN cosmos not recognized WHEN invoke THEN UNSAFE and non-fatal sent`() = runTest {
            // Arrange
            stubRecognized(NetworkType.COSMOS, "00", recognized = false)

            // Act
            val actual = useCase(NetworkType.COSMOS, "addr", "00", "ATOM", "Cosmos", "StakeKit")

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.UNSAFE)
            verifyScamEvent("ATOM", "Cosmos", blockaid = "Not performed", mobileCheck = "false")
            verify(exactly = 1) {
                analyticsExceptionHandler.sendException(
                    match<ExceptionAnalyticsEvent> {
                        it.exception is SuspiciousStakingTransactionException && it.params["verdict"] == "UNSAFE"
                    },
                )
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Cardano {

        @Test
        fun `GIVEN cardano not recognized WHEN invoke THEN UNSAFE`() = runTest {
            // Arrange
            stubRecognized(NetworkType.CARDANO, "00", recognized = false)

            // Act
            val actual = useCase(NetworkType.CARDANO, "addr", "00", "ADA", "Cardano", "StakeKit")

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.UNSAFE)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PassThrough {

        @ParameterizedTest
        @EnumSource(
            NetworkType::class,
            names = ["ETHEREUM", "BINANCE", "SOLANA", "TRON", "COSMOS", "CARDANO"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `GIVEN network without verification WHEN invoke THEN SAFE without checks`(network: NetworkType) = runTest {
            // Act
            val actual = useCase(network, "addr", "00", "T", "B", "StakeKit")

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.SAFE)
            verify(exactly = 0) { recognizer.isRecognizedStakingTransaction(any(), any()) }
            coVerify(exactly = 0) { blockAidVerifier.verifyTransaction(any()) }
            verify(exactly = 0) { analyticsEventHandler.send(any()) }
            verify(exactly = 0) { analyticsExceptionHandler.sendException(any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NonFatalReporting {

        @Test
        fun `GIVEN verdict UNSAFE WHEN invoke THEN non-fatal sent with verdict param`() = runTest {
            // Arrange
            stubRecognized(NetworkType.SOLANA, "00", recognized = false)

            // Act
            checkSolana()

            // Assert
            verify {
                analyticsExceptionHandler.sendException(
                    match<ExceptionAnalyticsEvent> {
                        it.exception is SuspiciousStakingTransactionException &&
                            it.params["verdict"] == "UNSAFE" &&
                            it.params["blockchain"] == "Solana" &&
                            it.params["token"] == "SOL" &&
                            it.params["provider"] == "StakeKit"
                    },
                )
            }
        }

        @Test
        fun `GIVEN verdict WARNING WHEN invoke THEN non-fatal sent with verdict param`() = runTest {
            // Arrange
            stubBlockAid(ValidationResult.WARNING)

            // Act
            checkEthereum()

            // Assert
            verify {
                analyticsExceptionHandler.sendException(
                    match<ExceptionAnalyticsEvent> {
                        it.exception is SuspiciousStakingTransactionException && it.params["verdict"] == "WARNING"
                    },
                )
            }
        }

        @Test
        fun `GIVEN verdict SAFE WHEN invoke THEN non-fatal not sent`() = runTest {
            // Arrange
            stubBlockAid(ValidationResult.SAFE)

            // Act
            checkEthereum()

            // Assert
            verify(exactly = 0) { analyticsExceptionHandler.sendException(any()) }
        }
    }
}