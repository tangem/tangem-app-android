package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText
import com.tangem.core.res.R as CoreResR

class AddAccountTypeBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<AddAccountTypeBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    val cryptoAccountButton: KNode = child {
        hasClickAction()
        hasAnyDescendant(withText(getResourceString(CoreResR.string.common_crypto_account)))
        useUnmergedTree = true
    }

    val jointAccountButton: KNode = child {
        hasClickAction()
        hasAnyDescendant(withText(getResourceString(CoreResR.string.common_joint_account)))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onAddAccountTypeBottomSheet(function: AddAccountTypeBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)