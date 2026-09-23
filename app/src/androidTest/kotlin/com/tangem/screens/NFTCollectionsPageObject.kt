package com.tangem.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.NFTCollectionsScreenTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

class NFTCollectionsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<NFTCollectionsPageObject>(semanticsProvider = semanticsProvider) {

    private val collectionsList: KNode = child {
        hasTestTag(NFTCollectionsScreenTestTags.LAZY_LIST)
    }

    val receiveButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasAnyDescendant(withText(getResourceString(R.string.nft_collections_receive)))
        useUnmergedTree = true
    }

    // In accounts mode the screen lists a section per account, so one collection name can appear several
    // times (every account holding it) — act on the first section, like the iOS test does.
    fun collectionItem(collectionName: String): KNode = collectionsList.child {
        hasTestTag(NFTCollectionsScreenTestTags.COLLECTION_ITEM)
        hasAnyDescendant(withText(collectionName))
        hasPosition(0)
        useUnmergedTree = true
    }

    fun assetItem(assetName: String): KNode = collectionsList.child {
        hasTestTag(NFTCollectionsScreenTestTags.ASSET_ITEM)
        hasAnyDescendant(withText(assetName))
        hasPosition(0)
        useUnmergedTree = true
    }

    @OptIn(ExperimentalTestApi::class)
    fun scrollToText(text: String) = collectionsList { performScrollToNode(withText(text)) }
}

internal fun BaseTestCase.onNFTCollectionsScreen(function: NFTCollectionsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)