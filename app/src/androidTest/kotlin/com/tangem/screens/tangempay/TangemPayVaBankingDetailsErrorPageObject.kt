package com.tangem.screens.tangempay

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

class TangemPayVaBankingDetailsErrorPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayVaBankingDetailsErrorPageObject>(semanticsProvider = semanticsProvider) {

    val icon: KNode = child {
        hasTestTag(TangemPayTestTags.ERROR_CONTENT_ICON)
        useUnmergedTree = true
    }

    val title: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_va_banking_details_error_title))
        useUnmergedTree = true
    }

    val description: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_va_banking_details_error_description))
        useUnmergedTree = true
    }

    val contactSupportButton: KNode = actionButton(CoreResR.string.common_contact_support)

    val retryButton: KNode = actionButton(CoreResR.string.common_retry)

    private fun actionButton(@StringRes textRes: Int): KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasAnyDescendant(withText(getResourceString(textRes)))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayVaBankingDetailsErrorSheet(
    function: TangemPayVaBankingDetailsErrorPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)