package com.tangem.features.earn.impl

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.features.earn.impl.fake.FakeEarnPosition
import com.tangem.features.earn.impl.fake.FakeEarnProductPreset
import com.tangem.features.earn.impl.fake.fakeReadyState
import com.tangem.features.earn.product.api.product.EarnBlock
import com.tangem.features.earn.product.api.product.EarnFreshness
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class EarnBlockingTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GuardedBy {

        @ParameterizedTest
        @ProvideTestModels
        fun guardedBy(model: GuardedByModel) {
            // Arrange
            val base = fakeReadyState(FakeEarnProductPreset.Lending, FakeEarnPosition.None)
            val ready = base.copy(
                freshness = model.freshness,
                primary = base.primary.copy(block = model.productBlock),
            )

            // Act
            val actual = ready.primary.guardedBy(ready)

            // Assert
            assertThat(actual).isEqualTo(ready.primary.copy(block = model.expectedBlock))
        }

        private fun provideTestModels() = listOf(
            GuardedByModel(freshness = null, productBlock = null, expectedBlock = null),
            GuardedByModel(freshness = FRESH, productBlock = PRODUCT_BLOCK, expectedBlock = PRODUCT_BLOCK),
            GuardedByModel(freshness = STALE, productBlock = null, expectedBlock = OUT_OF_DATE_BLOCK),
            GuardedByModel(freshness = STALE, productBlock = PRODUCT_BLOCK, expectedBlock = OUT_OF_DATE_BLOCK),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ShowReason {

        private val sender: UiMessageSender = mockk()

        @BeforeEach
        fun resetMocks() {
            clearMocks(sender)
        }

        @Test
        fun `GIVEN block WHEN showReason THEN sender gets one DialogMessage whose message is block reason`() {
            // Arrange
            val block = EarnBlock(reason = stringReference("reason"))
            val sent = slot<DialogMessage>()
            every { sender.send(capture(sent)) } just runs

            // Act
            sender.showReason(block)

            // Assert
            verify(exactly = 1) { sender.send(any()) }
            assertThat(sent.captured.message).isEqualTo(block.reason)
        }
    }

    internal data class GuardedByModel(
        val freshness: EarnFreshness?,
        val productBlock: EarnBlock?,
        val expectedBlock: EarnBlock?,
    )

    private companion object {

        val FRESH = EarnFreshness(asOf = Instant.fromEpochSeconds(0), isStale = false)
        val STALE = EarnFreshness(asOf = Instant.fromEpochSeconds(0), isStale = true)
        val PRODUCT_BLOCK = EarnBlock(reason = stringReference("product"))
        val OUT_OF_DATE_BLOCK = EarnBlock(
            reason = resourceReference(R.string.token_button_unavailability_reason_out_of_date_balance),
        )
    }
}