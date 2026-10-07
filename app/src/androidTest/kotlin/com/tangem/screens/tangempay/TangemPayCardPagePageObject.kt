package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.NotificationTestTags
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TangemPayCardPagePageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayCardPagePageObject>(semanticsProvider = semanticsProvider) {

    val moreButton: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_PAGE_MORE_BUTTON)
        useUnmergedTree = true
    }

    val cardNameEditButton: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_NAME_EDIT_BUTTON)
    }

    val replaceCardMenuItem: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_card_details_reissue_card))
        useUnmergedTree = true
    }

    val closeCardMenuItem: KNode = child {
        hasTestTag(TangemPayTestTags.CONTEXT_MENU_ITEM)
        hasAnyDescendant(withText(getResourceString(CoreResR.string.tangem_pay_close_card_popup_primary_button_title)))
        useUnmergedTree = true
    }

    val closeCardMenuItemLastCardHint: KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.CONTEXT_MENU_ITEM))
        hasText(getResourceString(CoreResR.string.tangem_pay_close_card_disabled_last_card))
        useUnmergedTree = true
    }

    // Title and description are one combinedReference text node, so the title only matches as a substring.
    val closingInProgressBlock: KNode = child {
        hasText(
            text = getResourceString(CoreResR.string.tangempay_card_page_closing_banner_title),
            substring = true,
        )
        useUnmergedTree = true
    }

    val cardNumberShort: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_NUMBER_SHORT)
        useUnmergedTree = true
    }

    // Neighbouring pager pages stay composed, so a card is addressed by the digits printed on it.
    fun cardNumberShortWith(lastDigits: String): KNode = child {
        hasTestTag(TangemPayTestTags.CARD_NUMBER_SHORT)
        hasText(text = lastDigits, substring = true)
        useUnmergedTree = true
    }

    val reissueInProgressBlock: KNode = child {
        hasText(
            text = getResourceString(CoreResR.string.tangempay_reissue_card_in_progress),
            substring = true,
        )
        useUnmergedTree = true
    }

    // Title and description are one combinedReference text node, so the title only matches as a substring.
    val issuingInProgressBlock: KNode = child {
        hasText(
            text = getResourceString(CoreResR.string.tangempay_issuing_new_digital_card_title),
            substring = true,
        )
        useUnmergedTree = true
    }

    val activatingInProgressBlock: KNode = child {
        hasText(
            text = getResourceString(CoreResR.string.tangempay_card_page_activating_banner_title),
            substring = true,
        )
        useUnmergedTree = true
    }

    val deliveryContactSupportButton: KNode = child {
        hasTestTag(TangemPayTestTags.DELIVERY_CONTACT_SUPPORT_BUTTON)
        useUnmergedTree = true
    }

    val deliveryActivateButton: KNode = child {
        hasTestTag(TangemPayTestTags.DELIVERY_ACTIVATE_BUTTON)
        useUnmergedTree = true
    }

    fun deliveryDescription(email: String): KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_card_details_delivery_description, email))
        useUnmergedTree = true
    }

    val changePinRow: KNode = child {
        hasTestTag(TangemPayTestTags.CHANGE_PIN_ROW)
        useUnmergedTree = true
    }

    val freezeCardRow: KNode = child {
        hasTestTag(TangemPayTestTags.FREEZE_CARD_ROW)
        useUnmergedTree = true
    }

    // Single FREEZE_CARD_ROW toggles its title Freeze<->Unfreeze by state; text pins the card as active vs frozen.
    val freezeCardRowActive: KNode = child {
        hasTestTag(TangemPayTestTags.FREEZE_CARD_ROW)
        hasAnyDescendant(withText(getResourceString(CoreResR.string.tangem_pay_freeze_card_freeze)))
        useUnmergedTree = true
    }

    val unfreezeCardRow: KNode = child {
        hasTestTag(TangemPayTestTags.FREEZE_CARD_ROW)
        hasAnyDescendant(withText(getResourceString(CoreResR.string.tangem_pay_freeze_card_unfreeze)))
        useUnmergedTree = true
    }

    // CARD_FROZEN_BADGE is the Pending-state spinner (TangemPayCardDetailsBlock) — a settled Frozen card
    // renders no badge at all, so assert [unfreezeCardRow] for that instead.
    val cardPendingFreezeSpinner: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_FROZEN_BADGE)
        useUnmergedTree = true
    }

    val showDetailsButton: KNode = child {
        hasTestTag(TangemPayTestTags.SHOW_DETAILS_ROW)
        useUnmergedTree = true
    }

    val hideDetailsButton: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_DETAILS_HIDE_BUTTON)
        useUnmergedTree = true
    }

    val numberValue: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_DETAILS_NUMBER_VALUE)
        useUnmergedTree = true
    }

    val expirationValue: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_DETAILS_EXPIRATION_VALUE)
        useUnmergedTree = true
    }

    val cvcValue: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_DETAILS_CVC_VALUE)
        useUnmergedTree = true
    }

    val copyNumberButton: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_DETAILS_COPY_NUMBER)
        useUnmergedTree = true
    }

    val copyExpirationButton: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_DETAILS_COPY_EXPIRATION)
        useUnmergedTree = true
    }

    val copyCvcButton: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_DETAILS_COPY_CVC)
        useUnmergedTree = true
    }

    val addToWalletBanner: KNode = child {
        hasTestTag(TangemPayTestTags.ADD_TO_WALLET_BANNER)
        useUnmergedTree = true
    }

    // The banner is a shared TangemMessage, so its inner NOTIFICATION_* tags need the banner as ancestor.
    val addToWalletBannerTitle: KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.ADD_TO_WALLET_BANNER))
        hasTestTag(NotificationTestTags.TITLE)
        useUnmergedTree = true
    }

    val addToWalletBannerSubtitle: KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.ADD_TO_WALLET_BANNER))
        hasTestTag(NotificationTestTags.MESSAGE)
        useUnmergedTree = true
    }

    val addToWalletBannerCloseButton: KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.ADD_TO_WALLET_BANNER))
        hasTestTag(NotificationTestTags.CLOSE_BUTTON)
        useUnmergedTree = true
    }

    val dailyLimitChangeButton: KNode = child {
        hasTestTag(TangemPayTestTags.DAILY_LIMIT_CHANGE_BUTTON)
        useUnmergedTree = true
    }

    val dailyLimitValue: KNode = child {
        hasTestTag(TangemPayTestTags.DAILY_LIMIT_CURRENT_VALUE)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayCardPageScreen(function: TangemPayCardPagePageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)