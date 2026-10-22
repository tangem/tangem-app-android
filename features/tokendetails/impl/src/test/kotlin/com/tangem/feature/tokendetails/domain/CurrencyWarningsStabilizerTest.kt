package com.tangem.feature.tokendetails.domain

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import org.junit.jupiter.api.Test

internal class CurrencyWarningsStabilizerTest {

    private val stabilizer = CurrencyWarningsStabilizer()

    private val outdated = CryptoCurrencyWarning.UsedOutdatedDataWarning
    private val unreachable = CryptoCurrencyWarning.SomeNetworksUnreachable

    @Test
    fun `GIVEN outdated warning WHEN stabilize THEN warnings are returned unchanged`() {
        // Act
        val actual = stabilizer.stabilize(setOf(outdated), StatusSource.ONLY_CACHE)

        // Assert
        assertThat(actual).containsExactly(outdated)
    }

    @Test
    fun `GIVEN no outdated warning ever shown WHEN source is cache THEN banner is not added`() {
        // Act
        val actual = stabilizer.stabilize(emptySet(), StatusSource.CACHE)

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN outdated warning shown WHEN source resets to cache THEN banner stays and nothing is re-emitted`() {
        // Arrange
        stabilizer.stabilize(setOf(outdated), StatusSource.ONLY_CACHE)

        // Act
        val actual = stabilizer.stabilize(emptySet(), StatusSource.CACHE)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN banner held WHEN other warnings arrive on cache THEN banner stays next to them`() {
        // Arrange
        stabilizer.stabilize(setOf(outdated), StatusSource.ONLY_CACHE)

        // Act
        val actual = stabilizer.stabilize(setOf(unreachable), StatusSource.CACHE)

        // Assert
        assertThat(actual).containsExactly(outdated, unreachable)
    }

    @Test
    fun `GIVEN banner held WHEN source becomes actual THEN banner is removed`() {
        // Arrange
        stabilizer.stabilize(setOf(outdated), StatusSource.ONLY_CACHE)
        stabilizer.stabilize(emptySet(), StatusSource.CACHE)

        // Act
        val actual = stabilizer.stabilize(emptySet(), StatusSource.ACTUAL)

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN same warnings WHEN stabilize twice THEN second call returns null`() {
        // Arrange
        stabilizer.stabilize(setOf(outdated), StatusSource.ONLY_CACHE)

        // Act
        val actual = stabilizer.stabilize(setOf(outdated), StatusSource.ONLY_CACHE)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN notification changed locally WHEN invalidate THEN same warnings are returned again`() {
        // Arrange
        stabilizer.stabilize(setOf(unreachable), StatusSource.ACTUAL)
        stabilizer.invalidate()

        // Act
        val actual = stabilizer.stabilize(setOf(unreachable), StatusSource.ACTUAL)

        // Assert
        assertThat(actual).containsExactly(unreachable)
    }
}