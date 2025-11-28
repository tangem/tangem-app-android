package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.BaseDialogTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import com.tangem.common.ui.R as CommonUIR

class SwapIsNotSupportedDialogPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SwapIsNotSupportedDialogPageObject>(semanticsProvider = semanticsProvider) {

    fun text(currencyName: String): KNode = child {
        hasTestTag(BaseDialogTestTags.TEXT)
        hasText(
            getResourceString(
                CommonUIR.string.token_button_unavailability_reason_not_exchangeable,
                currencyName
            )
        )
        useUnmergedTree = true
    }

    val okButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasText(getResourceString(R.string.common_ok))
    }
}

internal fun BaseTestCase.onSwapIsNotSupportedDialog(function: SwapIsNotSupportedDialogPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)