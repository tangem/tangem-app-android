package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.R
import com.tangem.core.ui.test.BaseButtonTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class CreateMobileWalletPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<CreateMobileWalletPageObject>(semanticsProvider = semanticsProvider) {

    val importExistingWalletButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.hw_import_existing_wallet))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onCreateMobileWalletScreen(function: CreateMobileWalletPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)