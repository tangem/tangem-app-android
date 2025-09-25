package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.SendAddressScreenTestTags
import com.tangem.features.send.v2.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class SendAddressPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SendAddressPageObject>(semanticsProvider = semanticsProvider) {

    val addressTextField: KNode = child {
        hasTestTag(SendAddressScreenTestTags.ADDRESS_TEXT_FIELD)
        useUnmergedTree = true
    }

    val nextButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_next))
        useUnmergedTree = true
    }

}

internal fun BaseTestCase.onSendAddressScreen(function: SendAddressPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)