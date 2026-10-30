package com.tangem.feature.tokendetails.presentation.tokendetails.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.tokens.model.warnings.HederaWarnings
import com.tangem.domain.tokens.model.warnings.KaspaWarnings
import com.tangem.test.core.ProvideTestModels
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import arrow.core.right
import io.mockk.coEvery
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import kotlin.reflect.KClass

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TokenDetailsModelLocalNotificationsTest : TokenDetailsModelTestBase() {

    private val rentWarning = CryptoCurrencyWarning.Rent(
        rent = BigDecimal("0.001"),
        exemptionAmount = BigDecimal("0.002"),
        cryptoCurrency = ethereum,
    )

    private val kaspaWarning = KaspaWarnings.IncompleteTransaction(
        currency = ethereum,
        amount = BigDecimal.ONE,
        currencySymbol = "KAS",
        currencyDecimals = 8,
    )

    @Test
    fun `GIVEN rent info shown WHEN close clicked THEN removed at once and next status brings it back`() = runTest {
        // Arrange
        extraWarnings = setOf(rentWarning)
        val model = createModel(testScope = this)
        val clickIntents: TokenDetailsClickIntents = model
        val rentInfo = TokenDetailsNotification.RentInfo(
            rentInfo = rentWarning,
            onCloseClick = clickIntents::onCloseRentInfoNotification,
        )
        val notifications = recordNotifications(model)
        emitStatus(createLoadedStatus(networkSource = StatusSource.ACTUAL))
        advanceUntilIdle()

        // Act
        model.onCloseRentInfoNotification()
        val notificationsAfterClose = model.uiState.value.notifications.toList()
        emitStatus(createLoadedStatus(networkSource = StatusSource.ACTUAL, amount = BigDecimal.TEN))
        advanceUntilIdle()

        // Assert
        assertThat(notificationsAfterClose).isEmpty()
        assertThat(notifications)
            .containsExactly(
                emptyList<TokenDetailsNotification>(),
                listOf(rentInfo),
                emptyList<TokenDetailsNotification>(),
                listOf(rentInfo),
            )
            .inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN kaspa incomplete tx shown WHEN dismiss confirmed THEN removed and next status brings it back`() =
        runTest {
            // Arrange
            extraWarnings = setOf(kaspaWarning)
            val model = createModel(testScope = this)
            val notifications = recordNotifications(model)
            emitStatus(createLoadedStatus(networkSource = StatusSource.ACTUAL))
            advanceUntilIdle()

            // Act
            model.onConfirmDismissIncompleteTransactionClick()
            advanceUntilIdle()
            val notificationsAfterDismiss = model.uiState.value.notifications.toList()
            emitStatus(createLoadedStatus(networkSource = StatusSource.ACTUAL, amount = BigDecimal.TEN))
            advanceUntilIdle()

            // Assert
            assertThat(notificationsAfterDismiss).isEmpty()
            assertThat(notifications.map { list -> list.map { it::class } })
                .containsExactly(
                    emptyList<Any>(),
                    listOf(TokenDetailsNotification.KaspaIncompleteTransactionWarning::class),
                    emptyList<Any>(),
                    listOf(TokenDetailsNotification.KaspaIncompleteTransactionWarning::class),
                )
                .inOrder()
            model.onDestroy()
        }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN notification shown WHEN its action succeeds THEN removed and next status brings it back`(
        scenario: ActionScenario,
    ) = runTest {
        // Arrange
        extraWarnings = setOf(scenario.warning)
        scenario.stubSuccess()
        val model = createModel(testScope = this)
        val notifications = recordNotifications(model)
        emitStatus(createLoadedStatus(networkSource = StatusSource.ACTUAL))
        advanceUntilIdle()

        // Act
        scenario.click(model)
        advanceUntilIdle()
        val notificationsAfterAction = model.uiState.value.notifications.toList()
        emitStatus(createLoadedStatus(networkSource = StatusSource.ACTUAL, amount = BigDecimal.TEN))
        advanceUntilIdle()

        // Assert
        assertThat(notificationsAfterAction).isEmpty()
        assertThat(notifications.map { list -> list.map { it::class } })
            .containsExactly(
                emptyList<Any>(),
                listOf(scenario.notification),
                emptyList<Any>(),
                listOf(scenario.notification),
            )
            .inOrder()
        model.onDestroy()
    }

    internal data class ActionScenario(
        val warning: CryptoCurrencyWarning,
        val notification: KClass<out TokenDetailsNotification>,
        val stubSuccess: () -> Unit,
        val click: (TokenDetailsModel) -> Unit,
    )

    private fun provideTestModels() = listOf(
        ActionScenario(
            warning = kaspaWarning,
            notification = TokenDetailsNotification.KaspaIncompleteTransactionWarning::class,
            stubSuccess = { coEvery { retryIncompleteTransactionUseCase(any(), any()) } returns Unit.right() },
            click = TokenDetailsModel::onRetryIncompleteTransactionClick,
        ),
        ActionScenario(
            warning = HederaWarnings.AssociateWarning(currency = ethereum),
            notification = TokenDetailsNotification.HederaAssociateWarning::class,
            stubSuccess = { coEvery { associateAssetUseCase(any(), any()) } returns Unit.right() },
            click = TokenDetailsModel::onAssociateClick,
        ),
        ActionScenario(
            warning = CryptoCurrencyWarning.RequiredTrustline(
                currency = ethereum,
                currencySymbol = "XRP",
                requiredAmount = BigDecimal.ONE,
                currencyDecimals = 6,
            ),
            notification = TokenDetailsNotification.RequiredTrustlineWarning::class,
            stubSuccess = { coEvery { openTrustlineUseCase(any(), any()) } returns Unit.right() },
            click = TokenDetailsModel::onOpenTrustlineClick,
        ),
    )
}