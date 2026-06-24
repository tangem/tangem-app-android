package com.tangem.features.tangempay.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayAction
import com.tangem.features.tangempay.entity.TangemPayActionButtonUM
import com.tangem.features.tangempay.entity.TangemPayDetailsBalanceBlockState
import com.tangem.features.tangempay.entity.TangemPayDetailsTopBarConfig
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

internal class TangemPayActionButtonsTransformerTest {

    @Test
    fun `GIVEN content balance block WHEN transform THEN action buttons are replaced`() {
        // Arrange
        val newButtons = persistentListOf(actionButton(enabled = false))
        val transformer = TangemPayActionButtonsTransformer(newButtons)

        // Act
        val result = transformer.transform(contentState())

        // Assert
        assertThat(result.balanceBlockState.actionButtons).isEqualTo(newButtons)
    }

    @Test
    fun `GIVEN non-content balance block WHEN transform THEN state is unchanged`() {
        // Arrange
        val transformer = TangemPayActionButtonsTransformer(persistentListOf(actionButton()))
        val state = contentState().copy(
            balanceBlockState = TangemPayDetailsBalanceBlockState.Loading(
                actionButtons = persistentListOf(),
                cardsBlockState = null,
            ),
        )

        // Act
        val result = transformer.transform(state)

        // Assert
        assertThat(result).isEqualTo(state)
    }

    private fun contentState(): TangemPayDetailsUM = TangemPayDetailsUM(
        topBarConfig = TangemPayDetailsTopBarConfig(
            onBackClick = {},
            onOpenMenu = {},
            items = persistentListOf(),
            itemsV2 = persistentListOf(),
        ),
        pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
        balanceBlockState = TangemPayDetailsBalanceBlockState.Content(
            actionButtons = persistentListOf(actionButton()),
            cardsBlockState = null,
            fiatBalance = TextReference.EMPTY,
            isBalanceFlickering = false,
        ),
        addToWalletBlockState = null,
        isBalanceHidden = false,
        errorNotificationConfig = null,
        accountDeactivatedNotificationConfig = null,
    )

    private fun actionButton(enabled: Boolean = true): TangemPayActionButtonUM = TangemPayActionButtonUM(
        action = TangemPayAction.Withdraw,
        config = ActionButtonConfig(
            text = resourceReference(R.string.tangempay_card_details_withdraw),
            iconResId = R.drawable.ic_arrow_up_24,
            onClick = {},
            isEnabled = enabled,
        ),
    )
}