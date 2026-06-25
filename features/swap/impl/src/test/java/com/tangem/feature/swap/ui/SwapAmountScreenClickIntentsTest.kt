package com.tangem.feature.swap.ui

import com.google.common.truth.Truth.assertThat
import com.tangem.feature.swap.models.UiActions
import org.junit.jupiter.api.Test

// PER_METHOD (the JUnit5 default): a fresh instance per test, so the recorded-call fields never leak.
internal class SwapAmountScreenClickIntentsTest {

    private var changedValue: String? = null
    private var maxClicked = false
    private var currencyChangeIsFiat: Boolean? = null

    // Real UiActions: the three wired callbacks record their invocation, the rest are no-ops.
    private val actions = UiActions(
        onAmountChanged = { changedValue = it },
        onCurrencyChange = { currencyChangeIsFiat = it },
        onAmountSelected = {},
        onSwapClick = {},
        onTransferClick = {},
        onChangeCardsClicked = {},
        onBackClicked = {},
        onMaxAmountSelected = { maxClicked = true },
        onPredefinedPercentSelected = {},
        onReduceToAmount = {},
        onReduceByAmount = { _, _ -> },
        onApproveClick = {},
        onApproveTypeSelect = {},
        onRetryClick = {},
        onProviderClick = {},
        onProviderSelect = {},
        onProviderFilterSelect = {},
        openTokenDetailsScreen = {},
        onSelectTokenClick = {},
        onSuccess = {},
        onLinkClick = {},
        onReceiveCardWarningClick = {},
        onSwapUIModeChange = {},
        onSwapTypeMenuOpened = {},
        onTronBannerShown = {},
    )

    private val sut = SwapAmountScreenClickIntents(actions)

    @Test
    fun `GIVEN value WHEN onAmountValueChange THEN delegates to onAmountChanged`() {
        // Act
        sut.onAmountValueChange("12.34")

        // Assert
        assertThat(changedValue).isEqualTo("12.34")
    }

    @Test
    fun `GIVEN max clicked WHEN onMaxValueClick THEN delegates to onMaxAmountSelected`() {
        // Act
        sut.onMaxValueClick()

        // Assert
        assertThat(maxClicked).isTrue()
    }

    @Test
    fun `GIVEN fiat toggle WHEN onCurrencyChangeClick THEN delegates to onCurrencyChange`() {
        // Act
        sut.onCurrencyChangeClick(isFiat = true)

        // Assert
        assertThat(currencyChangeIsFiat).isTrue()
    }

    @Test
    fun `GIVEN paste dismiss and next WHEN invoked THEN they are no-ops and do not delegate`() {
        // Act
        sut.onAmountPasteTriggerDismiss()
        sut.onAmountNext()

        // Assert — none of the wired callbacks fired
        assertThat(changedValue).isNull()
        assertThat(maxClicked).isFalse()
        assertThat(currencyChangeIsFiat).isNull()
    }
}