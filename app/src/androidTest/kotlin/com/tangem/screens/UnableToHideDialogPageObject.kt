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

class UnableToHideDialogPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<UnableToHideDialogPageObject>(semanticsProvider = semanticsProvider) {

    fun unableToHideTokenTitle(tokenName: String): KNode = child {
        hasTestTag(BaseDialogTestTags.TITLE)
        hasText(getResourceString(
            R.string.token_details_unable_hide_alert_title,
            tokenName))
    }

    fun unableToHideTokenMessage(tokenName: String, tokenSymbol: String, networkName: String): KNode = child {
        hasTestTag(BaseDialogTestTags.TEXT)
        hasText(
            getResourceString(
                R.string.token_details_unable_hide_alert_message,
                tokenName,
                tokenSymbol,
                networkName
            )
        )
    }

    val okButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasText(getResourceString(R.string.common_ok))
    }
}

internal fun BaseTestCase.onUnableToHideDialog(function: UnableToHideDialogPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)