package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

class NFTAssetDetailsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<NFTAssetDetailsPageObject>(semanticsProvider = semanticsProvider) {

    fun topAppBarTitle(assetName: String): KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        hasText(assetName)
        useUnmergedTree = true
    }

    val sendButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasAnyDescendant(withText(getResourceString(R.string.common_send)))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onNFTAssetDetailsScreen(function: NFTAssetDetailsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)