package com.tangem.features.jointaccount.analytics

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.jointaccount.blockaid.JointOperationBlockaidGate.Verdict
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.BlockaidResult
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.BlockaidSource
import com.tangem.features.jointaccount.blockaid.JointBlockaidSheetKind
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointBlockaidAnalyticsSenderTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    private val sender = JointBlockaidAnalyticsSender(analyticsEventHandler = analyticsEventHandler)

    @BeforeEach
    fun resetMocks() {
        clearMocks(analyticsEventHandler)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a verdict WHEN sendCheck THEN Blockaid Check carries its result`(model: CheckModel) {
        // Act
        sender.sendCheck(source = BlockaidSource.Sign, verdict = model.verdict, blockchain = BLOCKCHAIN)

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(
                JointAccountAnalyticsEvent.BlockaidCheck(
                    source = BlockaidSource.Sign,
                    result = model.expected,
                    blockchain = BLOCKCHAIN,
                ),
            )
        }
    }

    @ParameterizedTest
    @EnumSource(BlockaidSource::class)
    fun `GIVEN a risky sheet WHEN sendSheetShown THEN Notice Risky Address is sent`(source: BlockaidSource) {
        // Act
        sender.sendSheetShown(source = source, kind = JointBlockaidSheetKind.RISKY, blockchain = BLOCKCHAIN)

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(
                JointAccountAnalyticsEvent.NoticeRiskyAddress(source = source, blockchain = BLOCKCHAIN),
            )
        }
    }

    @ParameterizedTest
    @EnumSource(BlockaidSource::class)
    fun `GIVEN a malicious sheet WHEN sendSheetShown THEN Notice Malicious Transaction is sent`(source: BlockaidSource) {
        // Act
        sender.sendSheetShown(source = source, kind = JointBlockaidSheetKind.MALICIOUS, blockchain = BLOCKCHAIN)

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(
                JointAccountAnalyticsEvent.NoticeMaliciousTransaction(source = source, blockchain = BLOCKCHAIN),
            )
        }
    }

    @ParameterizedTest
    @EnumSource(BlockaidSource::class)
    fun `GIVEN a source WHEN sendSignAnyway THEN Button Sign Anyway is sent`(source: BlockaidSource) {
        // Act
        sender.sendSignAnyway(source = source, blockchain = BLOCKCHAIN)

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(JointAccountAnalyticsEvent.ButtonSignAnyway(source = source, blockchain = BLOCKCHAIN))
        }
    }

    data class CheckModel(val verdict: Verdict, val expected: BlockaidResult)

    private fun provideTestModels() = listOf(
        CheckModel(Verdict.Safe, BlockaidResult.Benign),
        CheckModel(Verdict.Warning, BlockaidResult.Warning),
        CheckModel(Verdict.Malicious, BlockaidResult.Malicious),
        CheckModel(Verdict.NoVerdict(Verdict.NoVerdict.Reason.ERROR), BlockaidResult.Error),
        CheckModel(Verdict.NoVerdict(Verdict.NoVerdict.Reason.TIMEOUT), BlockaidResult.Timeout),
        CheckModel(Verdict.NoVerdict(Verdict.NoVerdict.Reason.UNSUPPORTED_NETWORK), BlockaidResult.UnsupportedNetwork),
    )

    private companion object {
        const val BLOCKCHAIN = "Polygon"
    }
}