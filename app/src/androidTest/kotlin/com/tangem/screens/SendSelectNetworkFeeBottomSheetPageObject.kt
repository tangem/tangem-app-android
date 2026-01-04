package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.SendSelectNetworkFeeBottomSheetTestTags
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class SendSelectNetworkFeeBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SendSelectNetworkFeeBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(BaseBottomSheetTestTags.TITLE)
        hasText(getResourceString(R.string.common_network_fee_title))
        useUnmergedTree = true
    }

    fun regularFeeSelectorItem(title: String): KNode = child {
        hasTestTag(SendSelectNetworkFeeBottomSheetTestTags.REGULAR_FEE_ITEM)
        hasAnyDescendant(withText(title))
        hasAnyDescendant(withTestTag(SendSelectNetworkFeeBottomSheetTestTags.REGULAR_ITEM_ICON))
        hasAnyDescendant(withTestTag(SendSelectNetworkFeeBottomSheetTestTags.REGULAR_ITEM_TITLE))
        hasAnyDescendant(withTestTag(SendSelectNetworkFeeBottomSheetTestTags.TOKEN_AMOUNT))
        hasAnyDescendant(withTestTag(SendSelectNetworkFeeBottomSheetTestTags.FIAT_AMOUNT))
        hasAnyDescendant(withTestTag(SendSelectNetworkFeeBottomSheetTestTags.DOT_SIGN))
        useUnmergedTree = true
    }

    val customSelectorItem: KNode = child {
        hasTestTag(SendSelectNetworkFeeBottomSheetTestTags.CUSTOM_FEE_ITEM)
        hasAnyDescendant(withText(getResourceString(R.string.common_custom)))
        hasAnyDescendant(withTestTag(SendSelectNetworkFeeBottomSheetTestTags.CUSTOM_ITEM_ICON))
        hasAnyDescendant(withTestTag(SendSelectNetworkFeeBottomSheetTestTags.CUSTOM_ITEM_TITLE))
        useUnmergedTree = true
    }

    fun customInputItem(title: String, hasFiatAmount: Boolean = false): KNode = child {
        hasTestTag(SendSelectNetworkFeeBottomSheetTestTags.CUSTOM_INPUT_ITEM)
        hasAnyDescendant(withText(title))
        hasAnyDescendant(withTestTag(SendSelectNetworkFeeBottomSheetTestTags.CUSTOM_INPUT_ITEM_TITLE))
        hasAnyDescendant(withTestTag(SendSelectNetworkFeeBottomSheetTestTags.CUSTOM_INPUT_ITEM_TOOLTIP_ICON))
        hasAnyDescendant(withTestTag(SendSelectNetworkFeeBottomSheetTestTags.CUSTOM_INPUT_ITEM_INPUT_TEXT_FIELD))
        useUnmergedTree = true
        if (hasFiatAmount) {
            hasAnyDescendant(withTestTag(SendSelectNetworkFeeBottomSheetTestTags.CUSTOM_INPUT_ITEM_FIAT_AMOUNT))
        }
    }

    val nonceInputItem: KNode = child {
        hasTestTag(SendSelectNetworkFeeBottomSheetTestTags.NONCE_INPUT_ITEM)
        hasAnyDescendant(withText(getResourceString(R.string.send_nonce)))
        hasAnyDescendant(withText(getResourceString(R.string.send_nonce_hint)))
        useUnmergedTree = true
    }

    fun tooltipIcon(title: String): KNode = child {
        hasAnySibling(withText(title))
        useUnmergedTree = true
    }

    fun tooltipText(text: String): KNode = child {
        hasText(text)
        useUnmergedTree = true
    }

    private fun inputTextField(title: String): KNode = child {
        hasTestTag(SendSelectNetworkFeeBottomSheetTestTags.CUSTOM_INPUT_ITEM_INPUT_TEXT_FIELD)
        hasAnySibling(withText(title))
        useUnmergedTree = true
    }

    fun inputTextFieldValue(title: String): KNode = inputTextField(title).child {
        hasParent(withTestTag(SendSelectNetworkFeeBottomSheetTestTags.CUSTOM_INPUT_ITEM_INPUT_TEXT_FIELD))
        useUnmergedTree = true
    }

    val nonceInputTextField: KNode = child {
        hasTestTag(SendSelectNetworkFeeBottomSheetTestTags.NONCE_INPUT_TEXT_FIELD)
        useUnmergedTree = true
    }

    val customInputItemFiatAmount: KNode = child {
        hasTestTag(SendSelectNetworkFeeBottomSheetTestTags.CUSTOM_INPUT_ITEM_FIAT_AMOUNT)
        useUnmergedTree = true
    }

    val doneButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_done))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onSendSelectNetworkFeeBottomSheet(function: SendSelectNetworkFeeBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)