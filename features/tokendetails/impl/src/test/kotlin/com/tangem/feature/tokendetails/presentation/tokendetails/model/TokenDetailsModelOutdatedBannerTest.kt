package com.tangem.feature.tokendetails.presentation.tokendetails.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification
import com.tangem.test.core.ProvideTestModels
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TokenDetailsModelOutdatedBannerTest : TokenDetailsModelTestBase() {

    @Test
    fun `GIVEN banner shown WHEN refresh goes CACHE then ONLY_CACHE THEN banner is never hidden`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        val notifications = recordNotifications(model)
        emitStatus(createLoadedStatus(networkSource = StatusSource.ONLY_CACHE))
        advanceUntilIdle()

        // Act
        emitStatus(createLoadedStatus(networkSource = StatusSource.CACHE))
        advanceUntilIdle()
        emitStatus(createLoadedStatus(networkSource = StatusSource.ONLY_CACHE, amount = BANNER_AMOUNT))
        advanceUntilIdle()

        // Assert
        assertThat(notifications)
            .containsExactly(emptyList<TokenDetailsNotification>(), listOf(TokenDetailsNotification.UsedOutdatedData))
            .inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN banner and other warning WHEN refresh goes CACHE then ONLY_CACHE THEN notifications keep their order`() =
        runTest {
            // Arrange
            extraWarnings = setOf(CryptoCurrencyWarning.SomeNetworksUnreachable)
            val model = createModel(testScope = this)
            val notifications = recordNotifications(model)
            emitStatus(createLoadedStatus(networkSource = StatusSource.ONLY_CACHE))
            advanceUntilIdle()

            // Act
            emitStatus(createLoadedStatus(networkSource = StatusSource.CACHE))
            advanceUntilIdle()
            emitStatus(createLoadedStatus(networkSource = StatusSource.ONLY_CACHE, amount = BANNER_AMOUNT))
            advanceUntilIdle()

            // Assert
            assertThat(notifications)
                .containsExactly(
                    emptyList<TokenDetailsNotification>(),
                    listOf(TokenDetailsNotification.UsedOutdatedData, TokenDetailsNotification.NetworksUnreachable),
                )
                .inOrder()
            model.onDestroy()
        }

    @Test
    fun `GIVEN banner shown WHEN refresh goes CACHE then ACTUAL THEN banner is kept until ACTUAL`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        val notifications = recordNotifications(model)
        emitStatus(createLoadedStatus(networkSource = StatusSource.ONLY_CACHE))
        advanceUntilIdle()

        // Act
        emitStatus(createLoadedStatus(networkSource = StatusSource.CACHE))
        advanceUntilIdle()
        val notificationsOnCache = model.uiState.value.notifications.toList()
        emitStatus(createLoadedStatus(networkSource = StatusSource.ACTUAL))
        advanceUntilIdle()

        // Assert
        assertThat(notificationsOnCache).containsExactly(TokenDetailsNotification.UsedOutdatedData)
        assertThat(notifications)
            .containsExactly(
                emptyList<TokenDetailsNotification>(),
                listOf(TokenDetailsNotification.UsedOutdatedData),
                emptyList<TokenDetailsNotification>(),
            )
            .inOrder()
        model.onDestroy()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN banner shown WHEN status value has default ACTUAL sources THEN banner is removed`(
        model: DefaultSourcesModel,
    ) = runTest {
        // Arrange
        val tokenDetailsModel = createModel(testScope = this)
        val notifications = recordNotifications(tokenDetailsModel)
        emitStatus(createLoadedStatus(networkSource = StatusSource.ONLY_CACHE))
        advanceUntilIdle()

        // Act
        emitStatus(CryptoCurrencyStatus(currency = ethereum, value = model.value))
        advanceUntilIdle()

        // Assert
        assertThat(notifications)
            .containsExactly(
                emptyList<TokenDetailsNotification>(),
                listOf(TokenDetailsNotification.UsedOutdatedData),
                emptyList<TokenDetailsNotification>(),
            )
            .inOrder()
        tokenDetailsModel.onDestroy()
    }

    data class DefaultSourcesModel(val value: CryptoCurrencyStatus.Value)

    private fun provideTestModels() = listOf(
        DefaultSourcesModel(value = CryptoCurrencyStatus.Loading),
        DefaultSourcesModel(
            value = CryptoCurrencyStatus.Unreachable(priceChange = null, fiatRate = null, networkAddress = null),
        ),
    )

    private companion object {
        val BANNER_AMOUNT = 2.toBigDecimal()
    }
}