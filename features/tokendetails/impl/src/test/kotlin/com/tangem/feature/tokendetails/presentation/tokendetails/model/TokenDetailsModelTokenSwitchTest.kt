package com.tangem.feature.tokendetails.presentation.tokendetails.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class TokenDetailsModelTokenSwitchTest : TokenDetailsModelTestBase() {

    @Test
    fun `GIVEN banner held on first token WHEN second token opened with CACHE then ACTUAL THEN no banner`() = runTest {
        // Arrange
        val firstModel = createModel(testScope = this, currency = ethereum)
        emitStatus(createLoadedStatus(currency = ethereum, networkSource = StatusSource.ONLY_CACHE))
        advanceUntilIdle()
        emitStatus(createLoadedStatus(currency = ethereum, networkSource = StatusSource.CACHE))
        advanceUntilIdle()
        val firstModelNotifications = firstModel.uiState.value.notifications.toList()
        firstModel.onDestroy()
        val cacheStatus = createLoadedStatus(currency = bitcoin, networkSource = StatusSource.CACHE)
        val actualStatus = createLoadedStatus(currency = bitcoin, networkSource = StatusSource.ACTUAL)

        // Act
        val secondModel = createModel(testScope = this, currency = bitcoin)
        val secondModelNotifications = recordNotifications(secondModel)
        emitStatus(cacheStatus)
        advanceUntilIdle()
        emitStatus(actualStatus)
        advanceUntilIdle()

        // Assert
        assertThat(firstModelNotifications).containsExactly(TokenDetailsNotification.UsedOutdatedData)
        assertThat(secondModelNotifications).containsExactly(emptyList<TokenDetailsNotification>())
        coVerify(exactly = 1) { getCurrencyWarningsUseCase(userWalletId, cacheStatus, any()) }
        coVerify(exactly = 1) { getCurrencyWarningsUseCase(userWalletId, actualStatus, any()) }
        secondModel.onDestroy()
    }
}