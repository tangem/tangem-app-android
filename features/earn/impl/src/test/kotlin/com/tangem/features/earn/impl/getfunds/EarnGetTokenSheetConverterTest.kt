package com.tangem.features.earn.impl.getfunds

import androidx.compose.ui.graphics.vector.ImageVector
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_down_20
import com.tangem.core.ui.res.generated.icons.ic_card_20
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState.ActionState
import com.tangem.features.earn.impl.R
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class EarnGetTokenSheetConverterTest {

    private val currency = MockCryptoCurrencyFactory().ethereum
    private val symbol = wrappedList<Any>(currency.symbol)

    @Nested
    inner class Convert {

        @Test
        fun `GIVEN all three actions WHEN converted THEN the title names the symbol`() {
            // Arrange
            val actions = actions()

            // Act
            val actual = convert(actions)

            // Assert
            assertThat(actual.title).isEqualTo(resourceReference(R.string.get_token_title, symbol))
        }

        @Test
        fun `GIVEN the actions in the domain order WHEN converted THEN the rows are Deposit, Swap, Buy`() {
            // Arrange
            val actions = listOf(
                ActionState.Buy(ScenarioUnavailabilityReason.None),
                ActionState.Swap(ScenarioUnavailabilityReason.None, shouldShowBadge = false),
                ActionState.Receive(ScenarioUnavailabilityReason.None),
            )

            // Act
            val actual = convert(actions)

            // Assert
            assertThat(actual.rows.map { it.title }).containsExactly(
                resourceReference(R.string.earn_get_token_deposit_title, symbol),
                resourceReference(R.string.earn_get_token_swap_title, symbol),
                resourceReference(R.string.earn_get_token_buy_title, symbol),
            ).inOrder()
        }

        @Test
        fun `GIVEN no Buy in the action list WHEN converted THEN there are only the Deposit and Swap rows`() {
            // Arrange
            val actions = actions().filterNot { it is ActionState.Buy }

            // Act
            val actual = convert(actions)

            // Assert
            assertThat(actual.rows.map { it.title }).containsExactly(
                resourceReference(R.string.earn_get_token_deposit_title, symbol),
                resourceReference(R.string.earn_get_token_swap_title, symbol),
            ).inOrder()
        }

        @Test
        fun `GIVEN a row WHEN clicked THEN the action of that row is reported once`() {
            // Arrange
            val clicks = mutableListOf<EarnGetTokenAction>()
            val sheet = convert(actions(), onActionClick = { clicks += it })

            // Act
            sheet.rows[1].onClick()

            // Assert
            assertThat(clicks).containsExactly(EarnGetTokenAction.Swap)
        }

        @Test
        fun `GIVEN the sheet WHEN dismissed THEN the callback is invoked once`() {
            // Arrange
            var dismissals = 0
            val sheet = convert(actions(), onDismiss = { dismissals++ })

            // Act
            sheet.onDismiss()

            // Assert
            assertThat(dismissals).isEqualTo(1)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RowTexts {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN an action WHEN converted THEN its row has the agreed icon and texts`(model: TextsModel) {
            // Arrange
            val actions = actions()

            // Act
            val row = convert(actions).rows[model.action.ordinal]

            // Assert
            assertThat(row.icon).isEqualTo(model.icon)
            assertThat(row.title).isEqualTo(resourceReference(model.titleRes, symbol))
            assertThat(row.subtitle).isEqualTo(resourceReference(model.subtitleRes))
        }

        private fun provideTestModels() = listOf(
            TextsModel(
                action = EarnGetTokenAction.Deposit,
                icon = Icons.ic_arrow_down_20,
                titleRes = R.string.earn_get_token_deposit_title,
                subtitleRes = R.string.earn_get_token_deposit_subtitle,
            ),
            TextsModel(
                action = EarnGetTokenAction.Swap,
                icon = Icons.ic_arrow_up_down_20,
                titleRes = R.string.earn_get_token_swap_title,
                subtitleRes = R.string.earn_get_token_swap_subtitle,
            ),
            TextsModel(
                action = EarnGetTokenAction.Buy,
                icon = Icons.ic_card_20,
                titleRes = R.string.earn_get_token_buy_title,
                subtitleRes = R.string.earn_get_token_buy_subtitle,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RowLook {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN an unavailability reason WHEN converted THEN the row look follows the shared rule`(
            model: EnabledModel,
        ) {
            // Arrange
            val actions = actions(reasons = mapOf(model.action to model.reason))

            // Act
            val row = convert(actions).rows[model.action.ordinal]

            // Assert
            assertThat(row.isEnabled).isEqualTo(model.expectedEnabled)
        }

        private fun provideTestModels() = listOf(
            EnabledModel(EarnGetTokenAction.Deposit, ScenarioUnavailabilityReason.None, expectedEnabled = true),
            EnabledModel(EarnGetTokenAction.Deposit, ScenarioUnavailabilityReason.JointAccount, expectedEnabled = true),
            EnabledModel(
                EarnGetTokenAction.Deposit,
                ScenarioUnavailabilityReason.TrustlineRequired,
                expectedEnabled = false,
            ),
            EnabledModel(EarnGetTokenAction.Swap, ScenarioUnavailabilityReason.None, expectedEnabled = true),
            EnabledModel(
                EarnGetTokenAction.Swap,
                ScenarioUnavailabilityReason.ExpressUnreachable(cryptoCurrencyName = "Ethereum"),
                expectedEnabled = true,
            ),
            EnabledModel(
                EarnGetTokenAction.Swap,
                ScenarioUnavailabilityReason.NotExchangeable(cryptoCurrencyName = "Ethereum"),
                expectedEnabled = false,
            ),
            EnabledModel(EarnGetTokenAction.Buy, ScenarioUnavailabilityReason.None, expectedEnabled = true),
            EnabledModel(
                EarnGetTokenAction.Buy,
                ScenarioUnavailabilityReason.ExpressUnreachable(cryptoCurrencyName = "Ethereum"),
                expectedEnabled = false,
            ),
            EnabledModel(
                EarnGetTokenAction.Buy,
                ScenarioUnavailabilityReason.BuyUnavailable(cryptoCurrencyName = "Ethereum"),
                expectedEnabled = false,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RowLoading {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a reason and a network source WHEN converted THEN the row loads only while the action is unknown`(
            model: LoadingModel,
        ) {
            // Arrange
            val actions = actions(reasons = mapOf(model.action to model.reason))

            // Act
            val row = convert(actions, isReceiveCached = model.isReceiveCached).rows[model.action.ordinal]

            // Assert
            assertThat(row.isLoading).isEqualTo(model.expectedLoading)
        }

        private fun provideTestModels() = listOf(
            LoadingModel(EarnGetTokenAction.Swap, ScenarioUnavailabilityReason.None, false, expectedLoading = false),
            LoadingModel(
                EarnGetTokenAction.Swap,
                ScenarioUnavailabilityReason.ExpressLoading(cryptoCurrencyName = "Ethereum"),
                isReceiveCached = false,
                expectedLoading = true,
            ),
            LoadingModel(
                EarnGetTokenAction.Buy,
                ScenarioUnavailabilityReason.DataLoading,
                isReceiveCached = false,
                expectedLoading = true,
            ),
            LoadingModel(EarnGetTokenAction.Buy, ScenarioUnavailabilityReason.None, true, expectedLoading = false),
            LoadingModel(EarnGetTokenAction.Deposit, ScenarioUnavailabilityReason.None, true, expectedLoading = true),
            LoadingModel(EarnGetTokenAction.Deposit, ScenarioUnavailabilityReason.None, false, expectedLoading = false),
        )
    }

    @Nested
    inner class ReasonIn {

        @Test
        fun `GIVEN an action list without Swap WHEN the Swap reason is asked THEN it is null`() {
            // Arrange
            val actions = actions().filterNot { it is ActionState.Swap }

            // Act
            val actual = EarnGetTokenAction.Swap.reasonIn(actions)

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN Buy with a reason WHEN the Buy reason is asked THEN it is that reason`() {
            // Arrange
            val reason = ScenarioUnavailabilityReason.BuyUnavailable(cryptoCurrencyName = "Ethereum")
            val actions = actions(reasons = mapOf(EarnGetTokenAction.Buy to reason))

            // Act
            val actual = EarnGetTokenAction.Buy.reasonIn(actions)

            // Assert
            assertThat(actual).isEqualTo(reason)
        }
    }

    private fun convert(
        actions: List<ActionState>,
        isReceiveCached: Boolean = false,
        onActionClick: (EarnGetTokenAction) -> Unit = {},
        onDismiss: () -> Unit = {},
    ) = EarnGetTokenSheetConverter.convert(
        currency = currency,
        actions = actions,
        isReceiveCached = isReceiveCached,
        onActionClick = onActionClick,
        onDismiss = onDismiss,
    )

    private fun actions(reasons: Map<EarnGetTokenAction, ScenarioUnavailabilityReason> = emptyMap()) = listOf(
        ActionState.Receive(reasons[EarnGetTokenAction.Deposit] ?: ScenarioUnavailabilityReason.None),
        ActionState.Swap(
            unavailabilityReason = reasons[EarnGetTokenAction.Swap] ?: ScenarioUnavailabilityReason.None,
            shouldShowBadge = false,
        ),
        ActionState.Buy(reasons[EarnGetTokenAction.Buy] ?: ScenarioUnavailabilityReason.None),
    )

    internal data class TextsModel(
        val action: EarnGetTokenAction,
        val icon: ImageVector,
        val titleRes: Int,
        val subtitleRes: Int,
    )

    internal data class LoadingModel(
        val action: EarnGetTokenAction,
        val reason: ScenarioUnavailabilityReason,
        val isReceiveCached: Boolean,
        val expectedLoading: Boolean,
    )

    internal data class EnabledModel(
        val action: EarnGetTokenAction,
        val reason: ScenarioUnavailabilityReason,
        val expectedEnabled: Boolean,
    )
}