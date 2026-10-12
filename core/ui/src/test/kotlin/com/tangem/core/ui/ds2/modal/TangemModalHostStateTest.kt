package com.tangem.core.ui.ds2.modal

import androidx.compose.runtime.Composable
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Test

internal class TangemModalHostStateTest {

    private val state = TangemModalHostState(scope = CoroutineScope(Dispatchers.Unconfined))

    private val content: @Composable () -> Unit = {}

    @Test
    fun `GIVEN a prewarmed key WHEN prewarmed again THEN the entry is replaced`() {
        // Arrange
        val key = Any()

        // Act
        state.prewarm(key = key, content = content)
        state.prewarm(key = key, content = content)

        // Assert
        assertThat(state.prewarmEntries.map { it.key }).containsExactly(key)
    }

    @Test
    fun `GIVEN two prewarmed keys WHEN one is stopped THEN the other stays`() {
        // Arrange
        val stopped = Any()
        val kept = Any()
        state.prewarm(key = stopped, content = content)
        state.prewarm(key = kept, content = content)

        // Act
        state.stopPrewarm(stopped)

        // Assert
        assertThat(state.prewarmEntries.map { it.key }).containsExactly(kept)
    }

    @Test
    fun `GIVEN only prewarmed modals WHEN hasVisibleModal THEN nothing is reported as shown`() {
        // Arrange
        state.prewarm(key = Any(), content = content)

        // Act
        val actual = state.hasVisibleModal

        // Assert
        assertThat(actual).isFalse()
        assertThat(state.entries).isEmpty()
    }

    @Test
    fun `GIVEN a shown modal WHEN the same key is prewarmed THEN the registries stay independent`() {
        // Arrange
        val key = Any()
        state.show(key = key, content = content)

        // Act
        state.prewarm(key = key, content = content)
        state.dismiss(key)

        // Assert
        assertThat(state.entries).isEmpty()
        assertThat(state.prewarmEntries.map { it.key }).containsExactly(key)
    }
}