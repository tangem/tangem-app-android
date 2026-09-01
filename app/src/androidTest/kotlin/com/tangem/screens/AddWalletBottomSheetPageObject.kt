package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import com.tangem.core.res.R as CoreResR

class AddWalletBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<AddWalletBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    val addHardwareWalletButton: KNode = child {
        hasText(getResourceString(CoreResR.string.user_wallet_add_hardware_title))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onAddWalletBottomSheet(function: AddWalletBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)