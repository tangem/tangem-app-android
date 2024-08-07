package com.tangem.core.ui.components.notifications

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference

/**
 * Currency notification component state
 *
 * @property title          title
 * @property subtitle       subtitle
 * @property buttonsState   buttons state
 * @property tokenIconState token icon state
 *
* [REDACTED_AUTHOR]
 */
data class CurrencyNotificationConfig(
    val title: TextReference,
    val subtitle: AnnotatedSubtitle,
    val tokenIconState: CurrencyIconState,
    val buttonsState: NotificationConfig.ButtonsState,
) {

    /**
     * Subtitle as [AnnotatedString]
     *
     * @property valueProvider composable function that provides [AnnotatedString]
     * @property onClick       lambda be invoked when text in specified position is clicked
     */
    data class AnnotatedSubtitle(
        val valueProvider: @Composable () -> AnnotatedString,
        val onClick: (value: AnnotatedString, position: Int) -> Unit,
    )
}
