package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.FooterTestTags
import com.tangem.core.ui.test.SendAddressScreenTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import com.tangem.features.send.v2.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText
import com.tangem.core.ui.R as CoreUiR

class SendAddressPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SendAddressPageObject>(semanticsProvider = semanticsProvider) {

    val container: KNode = child {
        hasTestTag(SendAddressScreenTestTags.CONTAINER)
        useUnmergedTree = true
    }

    val topAppBarTitle: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        hasText(getResourceString(CoreUiR.string.common_address))
        useUnmergedTree = true
    }

    val addressTextFieldTitle: KNode = child {
        hasTestTag(SendAddressScreenTestTags.ADDRESS_TEXT_FIELD_TITLE)
        useUnmergedTree = true
    }

    val addressTextField: KNode = child {
        hasTestTag(SendAddressScreenTestTags.ADDRESS_TEXT_FIELD)
        useUnmergedTree = true
    }

    val addressTextFieldHint: KNode = child {
        hasParent(withTestTag(SendAddressScreenTestTags.ADDRESS_TEXT_FIELD))
        useUnmergedTree = true
    }

    fun recipientNetworkCaution(network: String): KNode = child {
        val expectedText = getResourceString(R.string.send_recipient_address_footer, network)
            .replace("**", "")
        hasText(expectedText, substring = true)
        hasTestTag(FooterTestTags.FOOTER_TEXT)
        useUnmergedTree = true
    }

    val addressPasteButton: KNode = child {
        hasTestTag(SendAddressScreenTestTags.ADDRESS_PASTE_BUTTON)
        useUnmergedTree = true
    }

    val qrButton: KNode = child {
        hasTestTag(SendAddressScreenTestTags.QR_BUTTON)
        useUnmergedTree = true
    }

    val clearTextFieldButton: KNode = child {
        hasContentDescription(getResourceString(CoreUiR.string.common_close))
        useUnmergedTree = true
    }

    val resolvedAddress: KNode = child {
        hasTestTag(SendAddressScreenTestTags.RESOLVED_ADDRESS)
    }

    val recentAddressesTitle: KNode = child {
        hasText(getResourceString(CoreUiR.string.send_recent_transactions))
        useUnmergedTree = true
    }

    val myWalletsTitle: KNode = child {
        hasText(getResourceString(CoreUiR.string.send_recipient_wallets_title))
        useUnmergedTree = true
    }

    fun recentAddressItem(
        recipientAddress: String,
        isMyWallet: Boolean = false,
        description: String? = null
    ): KNode = child {
        hasTestTag(SendAddressScreenTestTags.RECENT_ADDRESS_ITEM)
        hasAnyChild(withTestTag(SendAddressScreenTestTags.RECENT_ADDRESS_ICON))
        hasAnyDescendant(withText(recipientAddress))
        hasAnyDescendant(withTestTag(SendAddressScreenTestTags.RECENT_ADDRESS_TEXT))
        useUnmergedTree = true
        if (description != null) {
            hasAnyDescendant(withText(description, substring = true))
        }
        if (isMyWallet) {
            hasAnySibling(withText(getResourceString(CoreUiR.string.send_recipient_wallets_title)))
            hasAnyDescendant(withText(getResourceString(CoreUiR.string.manage_tokens_network_selector_wallet)))
        } else {
            hasAnySibling(withText(getResourceString(CoreUiR.string.send_recent_transactions)))
            hasAnyDescendant(withTestTag(SendAddressScreenTestTags.RECENT_ADDRESS_TRANSACTION_ICON))
        }
    }

    fun destinationTagBlockTitle(isMemoCorrectOrEmpty: Boolean = true): KNode = child {
        hasTestTag(SendAddressScreenTestTags.DESTINATION_TAG_TEXT_FIELD_TITLE)
        useUnmergedTree = true
        if(isMemoCorrectOrEmpty) {
            hasText(getResourceString(CoreUiR.string.send_destination_tag_field))
        } else {
            hasText(getResourceString(CoreUiR.string.send_memo_destination_tag_error))
        }
    }

    val destinationTagTextField: KNode = child {
        hasTestTag(SendAddressScreenTestTags.DESTINATION_TAG_TEXT_FIELD)
        useUnmergedTree = true
    }

    val destinationTagTextFieldHint: KNode = child {
        hasParent(withTestTag(SendAddressScreenTestTags.DESTINATION_TAG_TEXT_FIELD))
        useUnmergedTree = true
    }

    val destinationTagBlockText: KNode = child {
        hasText(
            getResourceString(CoreUiR.string.send_recipient_memo_footer_v2) + "\n" +
                getResourceString(CoreUiR.string.send_recipient_memo_footer_v2_highlighted)
        )
        useUnmergedTree = true
    }

    val destinationTagBlockCaution: KNode = child {
        hasText(
            getResourceString(
                CoreUiR.string.send_recipient_memo_footer_v2,
                CoreUiR.string.send_recipient_memo_footer_v2_highlighted
            ),
            substring = true
        )
        useUnmergedTree = true
    }

    val destinationTagPasteButton: KNode = child {
        hasTestTag(SendAddressScreenTestTags.DESTINATION_TAG_PASTE_BUTTON)
        useUnmergedTree = true
    }

    val clearDestinationTagTextFieldButton: KNode = child {
        hasTestTag(SendAddressScreenTestTags.DESTINATION_TAG_CLEAR_TEXT_FIELD_BUTTON)
        useUnmergedTree = true
    }

    val nextButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasAnyDescendant(withText(getResourceString(R.string.common_next)))
        useUnmergedTree = true
    }

    val continueButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_continue))
        useUnmergedTree = true
    }

}

internal fun BaseTestCase.onSendAddressScreen(function: SendAddressPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)