package com.tangem.feature.swap.mcp.ui

import android.os.Trace
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.extensions.rememberLastNonNull
import com.tangem.core.ui.extensions.stringReference
import com.tangem.feature.swap.mcp.model.ExchangeIntentAgentUM
import com.tangem.feature.swap.mcp.model.ExchangeIntentButtonUM
import com.tangem.feature.swap.mcp.model.ExchangeIntentUM
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.Runs
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ExchangeIntentDisplayRetentionTest {

    @BeforeEach
    fun setUpTracing() {
        mockkStatic(Trace::class)
        every { Trace.beginSection(any()) } just Runs
        every { Trace.endSection() } just Runs
    }

    @AfterEach
    fun tearDownTracing() {
        unmockkStatic(Trace::class)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `GIVEN offer transitions WHEN loading hides the modal THEN last offer remains and reopening uses new callback`() =
        runTest {
            // Arrange
            val source = mutableStateOf<ExchangeIntentUM>(ExchangeIntentUM.Loading)
            var displayedOffer: ExchangeIntentUM.Offer? = null
            val callbacks = mutableListOf<String>()
            val first = createOffer(title = "First", onClick = { callbacks.add("First") })
            val second = createOffer(title = "Second", onClick = { callbacks.add("Second") })
            val frameClock = BroadcastFrameClock()
            val recomposer = Recomposer(coroutineContext + frameClock)
            val composition = Composition(NoOpApplier(), recomposer)
            backgroundScope.launch(frameClock) { recomposer.runRecomposeAndApplyChanges() }
            try {
                composition.setContent {
                    val retained = rememberLastNonNull(source.value as? ExchangeIntentUM.Offer)
                    SideEffect { displayedOffer = retained }
                }

                // Act
                val displayedStates = mutableListOf(displayedOffer)
                listOf(first, ExchangeIntentUM.Loading, second, ExchangeIntentUM.Loading).forEachIndexed { index, state ->
                    source.value = state
                    Snapshot.sendApplyNotifications()
                    runCurrent()
                    frameClock.sendFrame(index.toLong())
                    runCurrent()
                    displayedStates.add(displayedOffer)
                }
                requireNotNull(displayedOffer).primaryButton.onClick()

                // Assert
                assertThat(displayedStates).containsExactly(null, first, first, second, second).inOrder()
                assertThat(callbacks).containsExactly("Second")
            } finally {
                composition.dispose()
                recomposer.cancel()
            }
        }

    private fun createOffer(title: String = "Offer", onClick: () -> Unit = {}) = ExchangeIntentUM.Offer(
        fromToken = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = null)),
        toToken = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = null)),
        title = stringReference(title),
        agent = ExchangeIntentAgentUM.Unknown(iconUrl = null, label = stringReference("Unknown")),
        description = stringReference("Description"),
        primaryButton = ExchangeIntentButtonUM(text = stringReference("Proceed"), onClick = onClick),
    )

    private class NoOpApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun onClear() = Unit
    }
}