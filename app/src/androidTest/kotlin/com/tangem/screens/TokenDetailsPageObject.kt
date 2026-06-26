package com.tangem.screens

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseActionButtonsBlockTestTags
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.NotificationTestTags
import com.tangem.core.ui.test.TokenDetailsScreenTestTags
import com.tangem.features.tokendetails.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText
import androidx.compose.ui.test.hasAnyDescendant as withAnyDescendant

class TokenDetailsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TokenDetailsPageObject>(semanticsProvider = semanticsProvider) {

    val screenContainer: KNode = child {
        hasTestTag(TokenDetailsScreenTestTags.SCREEN_CONTAINER)
    }

    val availableStakingBlock: KNode = child {
        hasTestTag(TokenDetailsScreenTestTags.STAKING_AVAILABLE_BLOCK)
        useUnmergedTree = true
    }

    val stakingBlock: KNode = child {
        hasTestTag(TokenDetailsScreenTestTags.STAKING_BLOCK)
        useUnmergedTree = true
    }

    fun availableStakingBlockText(apy: String): KNode = child {
        hasText(getResourceString(R.string.token_details_earn_staking_subtitle, apy))
        useUnmergedTree = true
    }

    val stakeButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_stake))
        useUnmergedTree = true
    }

    val stakingFiatAmount: KNode = child {
        hasTestTag(TokenDetailsScreenTestTags.STAKING_FIAT_AMOUNT)
        useUnmergedTree = true
    }

    val stakingTokenAmount: KNode = child {
        hasTestTag(TokenDetailsScreenTestTags.STAKING_TOKEN_AMOUNT)
        useUnmergedTree = true
    }

    val stakingTitle: KNode = child {
        hasText(getResourceString(R.string.common_staking))
    }

    val stakingEnabledTitle: KNode = child {
        hasText(getResourceString(R.string.staking_enabled))
    }

    val title: KNode = child {
        hasTestTag(TokenDetailsScreenTestTags.TOKEN_TITLE)
    }

    val fiatBalance: KNode = child {
        hasAnyAncestor(withTestTag(TokenDetailsScreenTestTags.BALANCE_FIAT))
        addSemanticsMatcher(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
        useUnmergedTree = true
    }

    val addFundsButton: KNode = child {
        hasTestTag(BaseActionButtonsBlockTestTags.ACTION_BUTTON)
        hasAnyDescendant(withText(getResourceString(R.string.tangempay_card_details_add_funds)))
        useUnmergedTree = true
    }

    val swapButton: KNode = child {
        hasTestTag(BaseActionButtonsBlockTestTags.ACTION_BUTTON)
        hasAnyDescendant(withText(getResourceString(R.string.common_swap)))
        useUnmergedTree = true
    }

    val transferButton: KNode = child {
        hasTestTag(BaseActionButtonsBlockTestTags.ACTION_BUTTON)
        hasAnyDescendant(withText(getResourceString(R.string.common_transfer)))
        useUnmergedTree = true
    }

    fun networkFeeNotificationIcon(feeCurrencyName: String): KNode = child {
        hasAnySibling(withText(getResourceString(R.string.warning_send_blocked_funds_for_fee_title, feeCurrencyName)))
        hasTestTag(NotificationTestTags.ICON)
        useUnmergedTree = true
    }

    fun networkFeeNotificationTitle(feeCurrencyName: String): KNode = child {
        hasTestTag(NotificationTestTags.TITLE)
        hasText(getResourceString(R.string.warning_send_blocked_funds_for_fee_title, feeCurrencyName))
        useUnmergedTree = true
    }

    val topUpYourWalletNotificationTitle: KNode = child {
        hasTestTag(NotificationTestTags.TITLE)
        hasText(getResourceString(R.string.warning_no_account_title))
        useUnmergedTree = true
    }

    val topUpYourWalletNotificationIcon: KNode = child {
        hasAnySibling(withText(getResourceString(R.string.warning_no_account_title)))
        hasTestTag(NotificationTestTags.ICON)
        useUnmergedTree = true
    }

    fun tokenTitle(name: String): KNode {
        val titleText = withText(text = name, substring = true)
        return child {
            hasTestTag(TokenDetailsScreenTestTags.TOKEN_TITLE)
            addSemanticsMatcher(titleText or withAnyDescendant(titleText))
            useUnmergedTree = true
        }
    }

    fun networkFeeNotificationMessage(
        currencyName: String,
        networkName: String,
        feeCurrencyName: String,
        feeCurrencySymbol: String,
    ): KNode = child {
        hasTestTag(NotificationTestTags.MESSAGE)
        hasText(
            getResourceString(
                R.string.warning_send_blocked_funds_for_fee_message,
                currencyName,
                networkName,
                currencyName,
                feeCurrencyName,
                feeCurrencySymbol
            )
        )
        useUnmergedTree = true
    }

    fun topUpYourWalletNotificationMessage(
        networkName: String,
        amount: String,
        currencySymbol: String,
    ): KNode = child {
        hasTestTag(NotificationTestTags.MESSAGE)
        hasText(
            getResourceString(
                R.string.no_account_generic,
                networkName,
                amount,
                currencySymbol,
            )
        )
        useUnmergedTree = true
    }

    fun goToBuyCurrencyButton(feeCurrencySymbol: String): KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_buy_currency, feeCurrencySymbol))
        useUnmergedTree = true
    }

    fun expressStatusItem(title: String): KNode = child {
        hasTestTag(TokenDetailsScreenTestTags.EXPRESS_STATUS_ITEM)
        hasAnyDescendant(
            withTestTag(TokenDetailsScreenTestTags.EXPRESS_STATUS_ITEM_TITLE) and withText(title)
        )
        hasAnyDescendant(withTestTag(TokenDetailsScreenTestTags.EXPRESS_STATUS_ITEM_FROM_ICON))
        hasAnyDescendant(withTestTag(TokenDetailsScreenTestTags.EXPRESS_STATUS_ITEM_FROM_AMOUNT))
        hasAnyDescendant(withTestTag(TokenDetailsScreenTestTags.EXPRESS_STATUS_ITEM_SWAP_ICON))
        hasAnyDescendant(withTestTag(TokenDetailsScreenTestTags.EXPRESS_STATUS_ITEM_TO_ICON))
        hasAnyDescendant(withTestTag(TokenDetailsScreenTestTags.EXPRESS_STATUS_ITEM_TO_AMOUNT))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTokenDetailsScreen(function: TokenDetailsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)