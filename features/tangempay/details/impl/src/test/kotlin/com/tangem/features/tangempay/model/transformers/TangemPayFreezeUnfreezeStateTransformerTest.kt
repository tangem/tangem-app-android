package com.tangem.features.tangempay.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.features.tangempay.entity.TangemPayDetailsBalanceBlockState
import com.tangem.features.tangempay.entity.TangemPayDetailsTopBarConfig
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class TangemPayFreezeUnfreezeStateTransformerTest {

    @ParameterizedTest
    @MethodSource("provideCases")
    fun `GIVEN content state WHEN transform THEN buttons enabled only when fresh and unfrozen`(case: Case) {
        // Arrange
        val state = contentState(actionButtonsEnabled = true)

        // Act
        val result = TangemPayFreezeUnfreezeStateTransformer(
            cardFrozenState = case.frozenState,
            isDataFresh = case.isDataFresh,
        ).transform(state)

        // Assert
        val enabled = result.balanceBlockState.actionButtons.map { it.isEnabled }
        assertThat(enabled).containsExactly(case.expectedEnabled, case.expectedEnabled)
    }

    @Test
    fun `GIVEN non-content state WHEN transform THEN state is unchanged`() {
        // Arrange
        val state = loadingState()

        // Act
        val result = TangemPayFreezeUnfreezeStateTransformer(
            cardFrozenState = TangemPayCardFrozenState.Unfrozen,
            isDataFresh = true,
        ).transform(state)

        // Assert
        assertThat(result).isEqualTo(state)
    }

    private fun contentState(actionButtonsEnabled: Boolean): TangemPayDetailsUM = baseState(
        balanceBlockState = TangemPayDetailsBalanceBlockState.Content(
            actionButtons = persistentListOf(
                actionButton(isEnabled = actionButtonsEnabled),
                actionButton(isEnabled = actionButtonsEnabled),
            ),
            cardsBlockState = null,
            fiatBalance = TextReference.EMPTY,
            isBalanceFlickering = false,
        ),
    )

    private fun loadingState(): TangemPayDetailsUM = baseState(
        balanceBlockState = TangemPayDetailsBalanceBlockState.Loading(
            actionButtons = persistentListOf(actionButton(isEnabled = true)),
            cardsBlockState = null,
        ),
    )

    private fun baseState(balanceBlockState: TangemPayDetailsBalanceBlockState) = TangemPayDetailsUM(
        topBarConfig = TangemPayDetailsTopBarConfig(
            onBackClick = {},
            onOpenMenu = {},
            items = persistentListOf(),
            itemsV2 = persistentListOf(),
        ),
        pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
        balanceBlockState = balanceBlockState,
        addToWalletBlockState = null,
        isBalanceHidden = false,
        errorNotificationConfig = null,
        accountDeactivatedNotificationConfig = null,
    )

    private fun actionButton(isEnabled: Boolean) = ActionButtonConfig(
        text = TextReference.EMPTY,
        iconResId = 0,
        onClick = {},
        isEnabled = isEnabled,
    )

    internal data class Case(
        val frozenState: TangemPayCardFrozenState,
        val isDataFresh: Boolean,
        val expectedEnabled: Boolean,
    )

    private companion object {
        @JvmStatic
        fun provideCases() = listOf(
            Case(frozenState = TangemPayCardFrozenState.Unfrozen, isDataFresh = true, expectedEnabled = true),
            Case(frozenState = TangemPayCardFrozenState.Unfrozen, isDataFresh = false, expectedEnabled = false),
            Case(frozenState = TangemPayCardFrozenState.Frozen, isDataFresh = true, expectedEnabled = false),
            Case(frozenState = TangemPayCardFrozenState.Pending, isDataFresh = true, expectedEnabled = false),
            Case(frozenState = TangemPayCardFrozenState.Frozen, isDataFresh = false, expectedEnabled = false),
            Case(frozenState = TangemPayCardFrozenState.Pending, isDataFresh = false, expectedEnabled = false),
        )
    }
}