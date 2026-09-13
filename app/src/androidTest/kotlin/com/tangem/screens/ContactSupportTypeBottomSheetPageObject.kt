package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class ContactSupportTypeBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ContactSupportTypeBottomSheetPageObject>(
        semanticsProvider = semanticsProvider
    ) {

    val openMailButton: KNode = child {
        hasText(getResourceString(R.string.support_selector_view_email_button))
        useUnmergedTree = true
    }

    val openChatButton: KNode = child {
        hasText(getResourceString(R.string.support_selector_view_chat_button))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onContactSupportTypeBottomSheet(
    function: ContactSupportTypeBottomSheetPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)