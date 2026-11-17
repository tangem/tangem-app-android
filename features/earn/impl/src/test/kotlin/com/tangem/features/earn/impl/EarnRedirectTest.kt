package com.tangem.features.earn.impl

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.navigation.Route
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnRedirectTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a stack WHEN redirected THEN only the redirecting route is replaced`(model: RedirectModel) {
        // Act
        val actual = model.stack.redirected(current = Current, replacement = Replacement)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        RedirectModel(stack = listOf(Current), expected = listOf(Replacement)),
        RedirectModel(stack = listOf(Current, Top), expected = listOf(Replacement, Top)),
        RedirectModel(stack = listOf(Below, Current, Top), expected = listOf(Below, Replacement, Top)),
        RedirectModel(stack = listOf(Replacement, Current, Top), expected = listOf(Replacement, Top)),
        RedirectModel(stack = listOf(Below, Top), expected = listOf(Below, Top)),
    )

    internal data class RedirectModel(val stack: List<Route>, val expected: List<Route>)

    private data object Current : Route
    private data object Replacement : Route
    private data object Below : Route
    private data object Top : Route
}