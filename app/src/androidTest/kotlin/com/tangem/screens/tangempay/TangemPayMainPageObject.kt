package com.tangem.screens.tangempay

import androidx.annotation.StringRes
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseActionButtonsBlockTestTags
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.EmptyTransactionBlockTestTags
import com.tangem.core.ui.test.NotificationTestTags
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.core.ui.test.TokenDetailsTopBarTestTags
import com.tangem.core.res.R as CoreResR
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class TangemPayMainPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayMainPageObject>(semanticsProvider = semanticsProvider) {

    val mainScreenTile: KNode = child {
        hasTestTag(TangemPayTestTags.MAIN_SCREEN_TILE)
        useUnmergedTree = true
    }

    fun tileWithSubtitle(subtitle: String): KNode = child {
        hasTestTag(TangemPayTestTags.MAIN_SCREEN_TILE)
        hasAnyDescendant(withText(subtitle))
        useUnmergedTree = true
    }

    fun tileWithSubtitleAndBalance(subtitle: String, balance: String): KNode = child {
        hasTestTag(TangemPayTestTags.MAIN_SCREEN_TILE)
        hasAnyDescendant(withText(subtitle))
        hasAnyDescendant(withText(balance, substring = true))
        useUnmergedTree = true
    }

    val tileBalance: KNode = child {
        hasTestTag(TangemPayTestTags.MAIN_SCREEN_TILE_BALANCE)
        useUnmergedTree = true
    }

    val tileCacheIcon: KNode = child {
        hasTestTag(TangemPayTestTags.MAIN_SCREEN_TILE_CACHE_ICON)
        useUnmergedTree = true
    }

    val balance: KNode = child {
        hasTestTag(TangemPayTestTags.PAYMENT_ACCOUNT_BALANCE)
        useUnmergedTree = true
    }

    val cardButton: KNode = child {
        hasTestTag(TangemPayTestTags.PAYMENT_ACCOUNT_CARD_BUTTON)
        useUnmergedTree = true
    }

    val cardFrozenIcon: KNode = child {
        hasTestTag(TangemPayTestTags.PAYMENT_ACCOUNT_CARD_FROZEN_ICON)
        useUnmergedTree = true
    }

    val addCardButton: KNode = child {
        hasTestTag(TangemPayTestTags.PAYMENT_ACCOUNT_ADD_CARD_BUTTON)
        useUnmergedTree = true
    }

    /** Every card tile carries the same tag, so a specific one is addressed by its position in the row. */
    fun cardButtonAt(index: Int): KNode = child {
        hasTestTag(TangemPayTestTags.PAYMENT_ACCOUNT_CARD_BUTTON)
        hasPosition(index)
        useUnmergedTree = true
    }

    /** Card tiles print the card's last digits, which is what tells one tile from another. */
    fun cardTileWithDigits(lastDigits: String): KNode = child {
        hasTestTag(TangemPayTestTags.PAYMENT_ACCOUNT_CARD_BUTTON)
        hasAnyDescendant(withText(lastDigits))
        useUnmergedTree = true
    }

    val cardAwaitingActivationTile: KNode = child {
        hasTestTag(TangemPayTestTags.PAYMENT_ACCOUNT_CARD_BUTTON)
        hasAnyDescendant(withText(""))
        useUnmergedTree = true
    }

    val paymentAccountTitle: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_payment_account))
        useUnmergedTree = true
    }

    val multinetworkSubtitle: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_multinetwork))
        useUnmergedTree = true
    }

    val topUpButton: KNode = child {
        hasTestTag(BaseActionButtonsBlockTestTags.ACTION_BUTTON)
        hasAnyDescendant(withText(getResourceString(CoreResR.string.tangempay_card_details_add_funds)))
        useUnmergedTree = true
    }

    val withdrawButton: KNode = child {
        hasTestTag(BaseActionButtonsBlockTestTags.ACTION_BUTTON)
        hasAnyDescendant(withText(getResourceString(CoreResR.string.tangempay_card_details_withdraw)))
        useUnmergedTree = true
    }

    val moreActionsButton: KNode = child {
        hasTestTag(TokenDetailsTopBarTestTags.MORE_BUTTON)
        useUnmergedTree = true
    }

    val accountClosedBannerTitle: KNode =
        bannerTitle(CoreResR.string.tangempay_account_deactivated_message_title)

    val accountClosedBannerMessage: KNode =
        bannerMessage(CoreResR.string.tangempay_account_deactivated_message_subtitle)

    // The deactivated screen shows no other message banner, so the start icon belongs to the account-closed one.
    val accountClosedBannerIcon: KNode = child {
        hasTestTag(NotificationTestTags.START_ICON)
        useUnmergedTree = true
    }

    // The confirmation dialog and the more-actions menu carry the same label, but neither is on screen
    // while this banner button is the one being clicked.
    val removeAccountBannerButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasAnyDescendant(withText(getResourceString(CoreResR.string.tangempay_remove_account)))
        useUnmergedTree = true
    }

    val removeAccountMenuItem: KNode = child {
        hasTestTag(TangemPayTestTags.CONTEXT_MENU_ITEM)
        hasAnyDescendant(withText(getResourceString(CoreResR.string.tangempay_remove_account)))
        useUnmergedTree = true
    }

    val currentPlanMenuItem: KNode = child {
        hasTestTag(TangemPayTestTags.CONTEXT_MENU_ITEM)
        hasAnyDescendant(withText(getResourceString(CoreResR.string.tangempay_current_plan_title)))
        useUnmergedTree = true
    }

    val paySupportMenuItem: KNode = child {
        hasTestTag(TangemPayTestTags.CONTEXT_MENU_ITEM)
        hasAnyDescendant(withText(getResourceString(CoreResR.string.tangempay_pay_support)))
        useUnmergedTree = true
    }

    fun cashbackWidgetWithTitle(title: String): KNode = child {
        hasTestTag(TangemPayTestTags.CASHBACK_WIDGET)
        hasAnyDescendant(withText(title))
        useUnmergedTree = true
    }

    val cashbackWidget: KNode = child {
        hasTestTag(TangemPayTestTags.CASHBACK_WIDGET)
        useUnmergedTree = true
    }

    fun cashbackMenuItem(month: String): KNode = child {
        hasTestTag(TangemPayTestTags.CONTEXT_MENU_ITEM)
        hasAnyDescendant(
            withText(getResourceString(CoreResR.string.tangempay_cashback_menu_item_title, month)),
        )
        useUnmergedTree = true
    }

    val cashbackDeactivatedBannerTitle: KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.CASHBACK_DEACTIVATED_BANNER))
        hasText(getResourceString(CoreResR.string.tangempay_cashback_deactivated_title))
        useUnmergedTree = true
    }

    val cashbackDeactivatedGotItButton: KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.CASHBACK_DEACTIVATED_BANNER))
        hasText(getResourceString(CoreResR.string.common_got_it))
        useUnmergedTree = true
    }

    fun transactionCashbackBadge(amount: String): KNode = child {
        hasTestTag(TangemPayTestTags.TRANSACTION_CASHBACK_BADGE)
        hasAnyDescendant(withText(amount))
        useUnmergedTree = true
    }

    // The action-button row keeps its label outside the button, so only the banner's is inside BASE_BUTTON.
    val awaitingDepositBannerAddFundsButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasAnyDescendant(withText(getResourceString(CoreResR.string.tangempay_card_details_add_funds)))
        useUnmergedTree = true
    }

    fun awaitingDepositBannerTitle(planFee: String): KNode = child {
        hasTestTag(NotificationTestTags.TITLE)
        hasText(getResourceString(CoreResR.string.tangempay_card_details_awaiting_deposit_title, planFee))
        useUnmergedTree = true
    }

    val sessionExpiredBannerTitle: KNode = bannerTitle(CoreResR.string.tangempay_sync_needed_title)

    val sessionExpiredBannerMessage: KNode = bannerMessage(CoreResR.string.tangempay_sync_needed_body)

    val connectionIssuesBannerTitle: KNode = bannerTitle(CoreResR.string.tangempay_temporarily_unavailable)

    val connectionIssuesBannerMessage: KNode =
        bannerMessage(CoreResR.string.tangempay_service_unreachable_try_later)

    val renewSessionButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasAnyDescendant(withText(getResourceString(CoreResR.string.tangempay_sync_needed_button)))
        useUnmergedTree = true
    }

    // The chip container carries the tag, but the enabled-state semantics sit on the button next to its label.
    // That button is the DS2 one — it has no BASE_BUTTON tag, so it is addressed by its label sibling.
    val addFundsButtonControl: KNode = actionButtonControl(CoreResR.string.tangempay_card_details_add_funds)

    val withdrawButtonControl: KNode = actionButtonControl(CoreResR.string.tangempay_card_details_withdraw)

    fun transactionRowWithText(text: String): KNode = child {
        hasText(text)
        useUnmergedTree = true
    }

    fun transactionRowWithAmount(name: String, amount: String): KNode = child {
        hasText(name)
        hasText(amount)
    }

    private val paymentAccountContent: KNode = child {
        hasTestTag(TangemPayTestTags.PAYMENT_ACCOUNT_CONTENT)
        useUnmergedTree = true
    }

    val historyErrorBlock: KNode = child {
        hasTestTag(EmptyTransactionBlockTestTags.BLOCK)
        useUnmergedTree = true
    }

    val historyErrorText: KNode = child {
        hasTestTag(EmptyTransactionBlockTestTags.TEXT)
        useUnmergedTree = true
    }

    val emptyHistoryIcon: KNode = child {
        hasTestTag(EmptyTransactionBlockTestTags.ICON)
        useUnmergedTree = true
    }

    val emptyHistoryText: KNode = child {
        hasTestTag(EmptyTransactionBlockTestTags.TEXT)
        hasText(getResourceString(CoreResR.string.transaction_history_empty_transactions))
        useUnmergedTree = true
    }

    val reloadHistoryButton: KNode = child {
        hasTestTag(TangemPayTestTags.TRANSACTION_HISTORY_RELOAD_BUTTON)
        useUnmergedTree = true
    }

    val issuingCardBanner: KNode = child {
        hasText(
            text = getResourceString(CoreResR.string.tangempay_issuing_new_digital_card_title),
            substring = true,
        )
        useUnmergedTree = true
    }

    val issuingCardBannerDescription: KNode = child {
        hasText(
            text = getResourceString(CoreResR.string.tangempay_reissue_card_in_progress_description),
            substring = true,
        )
        useUnmergedTree = true
    }

    val reissueInProgressBanner: KNode = child {
        hasText(
            text = getResourceString(CoreResR.string.tangempay_reissue_card_in_progress),
            substring = true,
        )
        useUnmergedTree = true
    }

    val deliveryBannerTitle: KNode = deliveryBannerText(
        tag = NotificationTestTags.TITLE,
        textRes = CoreResR.string.tangempay_card_delivery_banner_title,
    )

    val deliveryBannerMessage: KNode = deliveryBannerText(
        tag = NotificationTestTags.MESSAGE,
        textRes = CoreResR.string.tangempay_card_delivery_banner_description,
    )

    val deliveryBannerActivateButton: KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.DELIVERY_BANNER))
        hasText(getResourceString(CoreResR.string.tangempay_card_details_activate))
        useUnmergedTree = true
    }

    private fun deliveryBannerText(tag: String, @StringRes textRes: Int): KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.DELIVERY_BANNER))
        hasTestTag(tag)
        hasText(getResourceString(textRes))
        useUnmergedTree = true
    }

    private fun bannerTitle(@StringRes textRes: Int): KNode = child {
        hasTestTag(NotificationTestTags.TITLE)
        hasText(getResourceString(textRes))
        useUnmergedTree = true
    }

    private fun bannerMessage(@StringRes textRes: Int): KNode = child {
        hasTestTag(NotificationTestTags.MESSAGE)
        hasText(getResourceString(textRes))
        useUnmergedTree = true
    }

    private fun actionButtonControl(@StringRes titleRes: Int): KNode = child {
        hasAnyAncestor(withTestTag(BaseActionButtonsBlockTestTags.ACTION_BUTTON))
        hasAnySibling(withText(getResourceString(titleRes)))
        useUnmergedTree = true
    }

    // History is the tail of the payment-account lazy list, so its rows start off the fold.
    @OptIn(ExperimentalTestApi::class)
    fun scrollToTransactionWithText(text: String) = paymentAccountContent {
        performScrollToNode(withText(text))
    }

    // The empty and the failed-to-load history states share this block, so both are reached with it.
    @OptIn(ExperimentalTestApi::class)
    fun scrollToHistoryStateBlock() = paymentAccountContent {
        performScrollToNode(withTestTag(EmptyTransactionBlockTestTags.BLOCK))
    }
}

internal fun BaseTestCase.onTangemPayMainScreen(function: TangemPayMainPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)