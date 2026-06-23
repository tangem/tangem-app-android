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
import com.tangem.domain.blockaid.BlockAidVerifier
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import com.tangem.domain.staking.toggles.StakingFeatureToggles
import com.tangem.domain.staking.verification.StakingBlockAidRequestFactory
import com.tangem.domain.staking.verification.StakingTransactionRecognizer
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CheckStakingTransactionUseCaseTest {

    private val blockAidVerifier: BlockAidVerifier = mockk()
    private val recognizer: StakingTransactionRecognizer = mockk()
    private val requestFactory: StakingBlockAidRequestFactory = mockk()
    private val featureToggles: StakingFeatureToggles = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    private val useCase = CheckStakingTransactionUseCase(
        blockAidVerifier = blockAidVerifier,
        recognizer = recognizer,
        requestFactory = requestFactory,
        stakingFeatureToggles = featureToggles,
        analyticsEventHandler = analyticsEventHandler,
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
        clearMocks(blockAidVerifier, recognizer, requestFactory, featureToggles, analyticsEventHandler)
        every { featureToggles.isTransactionValidationEnabled } returns true
        every { requestFactory.create(any(), any(), any()) } returns transactionData
    }

    private fun blockAidResult(validation: ValidationResult) = CheckTransactionResult(
        validation = validation,
        description = "desc",
        simulation = SimulationResult.FailedToSimulate,
    )

    // ───────────────────────────── early-exit paths ─────────────────────────────

    @Test
    fun `GIVEN toggle off WHEN invoke THEN SAFE and no analytics`() = runTest {
        every { featureToggles.isTransactionValidationEnabled } returns false
        assertThat(useCase(NetworkType.POLYGON, "0xaa", "{}", "MATIC", "Polygon", "StakeKit"))
            .isEqualTo(StakingTransactionVerdict.SAFE)
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN null unsigned tx WHEN invoke THEN UNSAFE and no analytics`() = runTest {
        assertThat(useCase(NetworkType.POLYGON, "0xaa", null, "MATIC", "Polygon", "StakeKit"))
            .isEqualTo(StakingTransactionVerdict.UNSAFE)
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    // ───────────────────────────── EVM paths ────────────────────────────────────

    @Test
    fun `GIVEN evm not recognized locally WHEN invoke THEN UNSAFE and blockaid not called`() = runTest {
        every { recognizer.isRecognizedStakingTransaction(NetworkType.POLYGON, "{}") } returns false

        assertThat(useCase(NetworkType.POLYGON, "0xaa", "{}", "MATIC", "Polygon", "StakeKit"))
            .isEqualTo(StakingTransactionVerdict.UNSAFE)
        coVerify(exactly = 0) { blockAidVerifier.verifyTransaction(any()) }
    }

    @Test
    fun `GIVEN evm not recognized WHEN invoke THEN event has Blockaid not performed and Mobile_check false`() = runTest {
        every { recognizer.isRecognizedStakingTransaction(NetworkType.POLYGON, "{}") } returns false

        useCase(NetworkType.POLYGON, "0xaa", "{}", "MATIC", "Polygon", "StakeKit")

        verify {
            analyticsEventHandler.send(
                match<StakingAnalyticsEvent.ScamVerification> {
                    it.blockaid == "Not performed" && it.mobileCheck == "false"
                },
            )
        }
    }

    @Test
    fun `GIVEN evm recognized AND blockaid SAFE WHEN invoke THEN SAFE`() = runTest {
        every { recognizer.isRecognizedStakingTransaction(NetworkType.POLYGON, "{}") } returns true
        coEvery { blockAidVerifier.verifyTransaction(transactionData) } returns blockAidResult(ValidationResult.SAFE).right()

        assertThat(useCase(NetworkType.POLYGON, "0xaa", "{}", "MATIC", "Polygon", "StakeKit"))
            .isEqualTo(StakingTransactionVerdict.SAFE)
    }

    @Test
    fun `GIVEN evm recognized AND blockaid SAFE WHEN invoke THEN event has Blockaid Safe and Mobile_check true`() = runTest {
        every { recognizer.isRecognizedStakingTransaction(NetworkType.POLYGON, "{}") } returns true
        coEvery { blockAidVerifier.verifyTransaction(transactionData) } returns blockAidResult(ValidationResult.SAFE).right()

        useCase(NetworkType.POLYGON, "0xaa", "{}", "MATIC", "Polygon", "StakeKit")

        verify {
            analyticsEventHandler.send(
                match<StakingAnalyticsEvent.ScamVerification> {
                    it.blockaid == "Safe" && it.mobileCheck == "true"
                },
            )
        }
    }

    @Test
    fun `GIVEN evm recognized AND blockaid WARNING WHEN invoke THEN WARNING`() = runTest {
        every { recognizer.isRecognizedStakingTransaction(NetworkType.POLYGON, "{}") } returns true
        coEvery { blockAidVerifier.verifyTransaction(transactionData) } returns blockAidResult(ValidationResult.WARNING).right()

        assertThat(useCase(NetworkType.POLYGON, "0xaa", "{}", "MATIC", "Polygon", "StakeKit"))
            .isEqualTo(StakingTransactionVerdict.WARNING)
    }

    @Test
    fun `GIVEN evm recognized AND blockaid WARNING WHEN invoke THEN event has Blockaid Warning`() = runTest {
        every { recognizer.isRecognizedStakingTransaction(NetworkType.POLYGON, "{}") } returns true
        coEvery { blockAidVerifier.verifyTransaction(transactionData) } returns blockAidResult(ValidationResult.WARNING).right()

        useCase(NetworkType.POLYGON, "0xaa", "{}", "MATIC", "Polygon", "StakeKit")

        verify {
            analyticsEventHandler.send(
                match<StakingAnalyticsEvent.ScamVerification> {
                    it.blockaid == "Warning" && it.mobileCheck == "true"
                },
            )
        }
    }

    @Test
    fun `GIVEN evm recognized AND blockaid UNSAFE WHEN invoke THEN UNSAFE`() = runTest {
        every { recognizer.isRecognizedStakingTransaction(NetworkType.POLYGON, "{}") } returns true
        coEvery { blockAidVerifier.verifyTransaction(transactionData) } returns blockAidResult(ValidationResult.UNSAFE).right()

        assertThat(useCase(NetworkType.POLYGON, "0xaa", "{}", "MATIC", "Polygon", "StakeKit"))
            .isEqualTo(StakingTransactionVerdict.UNSAFE)
    }

    @Test
    fun `GIVEN evm recognized AND blockaid UNSAFE WHEN invoke THEN event has Blockaid Unsafe`() = runTest {
        every { recognizer.isRecognizedStakingTransaction(NetworkType.POLYGON, "{}") } returns true
        coEvery { blockAidVerifier.verifyTransaction(transactionData) } returns blockAidResult(ValidationResult.UNSAFE).right()

        useCase(NetworkType.POLYGON, "0xaa", "{}", "MATIC", "Polygon", "StakeKit")

        verify {
            analyticsEventHandler.send(
                match<StakingAnalyticsEvent.ScamVerification> {
                    it.blockaid == "Unsafe" && it.mobileCheck == "true"
                },
            )
        }
    }

    @Test
    fun `GIVEN evm recognized AND blockaid FAILED_TO_VALIDATE WHEN invoke THEN SAFE`() = runTest {
        every { recognizer.isRecognizedStakingTransaction(NetworkType.POLYGON, "{}") } returns true
        coEvery { blockAidVerifier.verifyTransaction(transactionData) } returns blockAidResult(ValidationResult.FAILED_TO_VALIDATE).right()

        assertThat(useCase(NetworkType.POLYGON, "0xaa", "{}", "MATIC", "Polygon", "StakeKit"))
            .isEqualTo(StakingTransactionVerdict.SAFE)
    }

    @Test
    fun `GIVEN evm recognized AND blockaid FAILED_TO_VALIDATE WHEN invoke THEN event has Blockaid Failed to validate`() = runTest {
        every { recognizer.isRecognizedStakingTransaction(NetworkType.POLYGON, "{}") } returns true
        coEvery { blockAidVerifier.verifyTransaction(transactionData) } returns blockAidResult(ValidationResult.FAILED_TO_VALIDATE).right()

        useCase(NetworkType.POLYGON, "0xaa", "{}", "MATIC", "Polygon", "StakeKit")

        verify {
            analyticsEventHandler.send(
                match<StakingAnalyticsEvent.ScamVerification> {
                    it.blockaid == "Failed to validate" && it.mobileCheck == "true"
                },
            )
        }
    }

    @Test
    fun `GIVEN evm recognized AND blockaid returns left WHEN invoke THEN SAFE`() = runTest {
        every { recognizer.isRecognizedStakingTransaction(NetworkType.POLYGON, "{}") } returns true
        coEvery { blockAidVerifier.verifyTransaction(transactionData) } returns RuntimeException("network").left()

        assertThat(useCase(NetworkType.POLYGON, "0xaa", "{}", "MATIC", "Polygon", "StakeKit"))
            .isEqualTo(StakingTransactionVerdict.SAFE)
    }

    @Test
    fun `GIVEN evm recognized AND blockaid throws WHEN invoke THEN SAFE`() = runTest {
        every { recognizer.isRecognizedStakingTransaction(NetworkType.POLYGON, "{}") } returns true
        coEvery { blockAidVerifier.verifyTransaction(transactionData) } throws RuntimeException("boom")

        assertThat(useCase(NetworkType.POLYGON, "0xaa", "{}", "MATIC", "Polygon", "StakeKit"))
            .isEqualTo(StakingTransactionVerdict.SAFE)
    }

    // ───────────────────────────── Ethereum (Blockaid-only) ─────────────────────

    @Test
    fun `GIVEN ethereum WHEN invoke THEN blockaid called without local recognition`() = runTest {
        coEvery { blockAidVerifier.verifyTransaction(transactionData) } returns blockAidResult(ValidationResult.SAFE).right()

        val result = useCase(NetworkType.ETHEREUM, "0xaa", "{}", "ETH", "Ethereum", "StakeKit")

        assertThat(result).isEqualTo(StakingTransactionVerdict.SAFE)
        coVerify(exactly = 1) { blockAidVerifier.verifyTransaction(transactionData) }
        verify(exactly = 0) { recognizer.isRecognizedStakingTransaction(any(), any()) }
    }

    @Test
    fun `GIVEN ethereum AND blockaid SAFE WHEN invoke THEN event Blockaid Safe and Mobile_check true`() = runTest {
        coEvery { blockAidVerifier.verifyTransaction(transactionData) } returns blockAidResult(ValidationResult.SAFE).right()

        useCase(NetworkType.ETHEREUM, "0xaa", "{}", "ETH", "Ethereum", "StakeKit")

        verify {
            analyticsEventHandler.send(
                match<StakingAnalyticsEvent.ScamVerification> {
                    it.blockaid == "Safe" && it.mobileCheck == "true"
                },
            )
        }
    }

    @Test
    fun `GIVEN ethereum AND blockaid UNSAFE WHEN invoke THEN UNSAFE`() = runTest {
        coEvery { blockAidVerifier.verifyTransaction(transactionData) } returns blockAidResult(ValidationResult.UNSAFE).right()

        assertThat(useCase(NetworkType.ETHEREUM, "0xaa", "{}", "ETH", "Ethereum", "StakeKit"))
            .isEqualTo(StakingTransactionVerdict.UNSAFE)
    }

    @Test
    fun `GIVEN ethereum AND blockaid no answer WHEN invoke THEN SAFE`() = runTest {
        coEvery { blockAidVerifier.verifyTransaction(transactionData) } returns RuntimeException("network").left()

        assertThat(useCase(NetworkType.ETHEREUM, "0xaa", "{}", "ETH", "Ethereum", "StakeKit"))
            .isEqualTo(StakingTransactionVerdict.SAFE)
    }

    // ───────────────────────────── Solana paths ─────────────────────────────────

    @Test
    fun `GIVEN solana recognized AND blockaid SAFE WHEN invoke THEN SAFE`() = runTest {
        every { recognizer.isRecognizedStakingTransaction(NetworkType.SOLANA, "00") } returns true
        coEvery { blockAidVerifier.verifyTransaction(transactionData) } returns blockAidResult(ValidationResult.SAFE).right()

        assertThat(useCase(NetworkType.SOLANA, "0xaa", "00", "SOL", "Solana", "StakeKit"))
            .isEqualTo(StakingTransactionVerdict.SAFE)
    }

    @Test
    fun `GIVEN solana not recognized WHEN invoke THEN UNSAFE`() = runTest {
        every { recognizer.isRecognizedStakingTransaction(NetworkType.SOLANA, "00") } returns false

        assertThat(useCase(NetworkType.SOLANA, "0xaa", "00", "SOL", "Solana", "StakeKit"))
            .isEqualTo(StakingTransactionVerdict.UNSAFE)
    }

    // ───────────────────────────── Local-only chains ─────────────────────────────

    @Test
    fun `GIVEN tron recognized WHEN invoke THEN SAFE`() = runTest {
        every { recognizer.isRecognizedStakingTransaction(NetworkType.TRON, "{}") } returns true

        assertThat(useCase(NetworkType.TRON, "addr", "{}", "TRX", "Tron", "StakeKit"))
            .isEqualTo(StakingTransactionVerdict.SAFE)
    }

    @Test
    fun `GIVEN tron recognized WHEN invoke THEN event has Blockaid not performed and Mobile_check true`() = runTest {
        every { recognizer.isRecognizedStakingTransaction(NetworkType.TRON, "{}") } returns true

        useCase(NetworkType.TRON, "addr", "{}", "TRX", "Tron", "StakeKit")

        verify {
            analyticsEventHandler.send(
                match<StakingAnalyticsEvent.ScamVerification> {
                    it.blockaid == "Not performed" && it.mobileCheck == "true"
                },
            )
        }
    }

    @Test
    fun `GIVEN cardano not recognized WHEN invoke THEN UNSAFE`() = runTest {
        every { recognizer.isRecognizedStakingTransaction(NetworkType.CARDANO, "00") } returns false

        assertThat(useCase(NetworkType.CARDANO, "addr", "00", "ADA", "Cardano", "StakeKit"))
            .isEqualTo(StakingTransactionVerdict.UNSAFE)
    }

    // ───────────────────────────── Pass-through ─────────────────────────────────

    @Test
    fun `GIVEN unsupported network ton WHEN invoke THEN SAFE pass-through and no analytics`() = runTest {
        assertThat(useCase(NetworkType.TON, "addr", "00", "TON", "TON", "StakeKit"))
            .isEqualTo(StakingTransactionVerdict.SAFE)
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }
}