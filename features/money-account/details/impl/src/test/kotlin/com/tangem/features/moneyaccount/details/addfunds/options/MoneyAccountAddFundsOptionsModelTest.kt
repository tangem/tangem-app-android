package com.tangem.features.moneyaccount.details.addfunds.options

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents
import com.tangem.features.moneyaccount.details.impl.R
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MoneyAccountAddFundsOptionsModelTest {

    private val listener: MoneyAccountAddFundsOptionsListener = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(listener, analytics)
    }

    @Test
    fun `GIVEN options sheet WHEN model created THEN info screen event sent`() = runTest {
        // Act
        val model = createModel(depositAddress = DEPOSIT_ADDRESS)

        // Assert
        verify(exactly = 1) { analytics.send(ofType<MoneyAccountAnalyticsEvents.DepositInfoScreenOpened>()) }
        model.onDestroy()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN deposit address WHEN model created THEN the options it allows are shown`(model: OptionsModel) =
        runTest {
            // Act
            val optionsModel = createModel(depositAddress = model.depositAddress)

            // Assert
            assertThat(optionsModel.uiState.value.items.map { it.title })
                .containsExactlyElementsIn(model.expectedTitles)
                .inOrder()
            optionsModel.onDestroy()
        }

    @Test
    fun `GIVEN receive option WHEN clicked THEN listener receives the deposit address`() = runTest {
        // Arrange
        val model = createModel(depositAddress = DEPOSIT_ADDRESS)

        // Act
        model.uiState.value.items.single().onClick?.invoke()

        // Assert
        verify(exactly = 1) { listener.onReceiveClick(DEPOSIT_ADDRESS) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN model WHEN dismissed THEN listener notified`() = runTest {
        // Arrange
        val model = createModel(depositAddress = DEPOSIT_ADDRESS)

        // Act
        model.onDismiss()

        // Assert
        verify(exactly = 1) { listener.onDismissAddFunds() }
        model.onDestroy()
    }

    private fun TestScope.createModel(depositAddress: String?) = MoneyAccountAddFundsOptionsModel(
        paramsContainer = MutableParamsContainer(
            MoneyAccountAddFundsOptionsComponent.Params(depositAddress = depositAddress, listener = listener),
        ),
        dispatchers = createTestingCoroutineDispatcherProvider(),
        analytics = analytics,
    )

    private fun provideTestModels() = listOf(
        OptionsModel(
            depositAddress = DEPOSIT_ADDRESS,
            expectedTitles = listOf(resourceReference(R.string.tangempay_topup_receive_title)),
        ),
        OptionsModel(depositAddress = null, expectedTitles = emptyList()),
    )

    data class OptionsModel(val depositAddress: String?, val expectedTitles: List<TextReference>)

    private companion object {
        const val DEPOSIT_ADDRESS = "0xdeposit"
    }
}