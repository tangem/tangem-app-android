package com.tangem.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.utils.LazyListItemNode
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.NotificationTestTags
import com.tangem.core.ui.test.TokenDetailsScreenTestTags
import com.tangem.core.ui.utils.LazyListItemPositionSemantics
import com.tangem.features.tokendetails.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.compose.node.element.lazylist.KLazyListNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

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

    val availableStakingBlockTitle: KNode = child {
        hasTestTag(TokenDetailsScreenTestTags.STAKING_SERVICE_TITLE)
        useUnmergedTree = true
    }

    val availableStakingBlockText: KNode = child {
        hasTestTag(TokenDetailsScreenTestTags.STAKING_SERVICE_TEXT)
        useUnmergedTree = true
    }

    val availableStakingBlockCurrencyIcon: KNode = child {
        hasTestTag(TokenDetailsScreenTestTags.STAKING_CURRENCY_ICON)
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

    val stakingDot: KNode = child {
        hasTestTag(TokenDetailsScreenTestTags.STAKING_DOT)
        useUnmergedTree = true
    }

    val stakingTokenAmount: KNode = child {
        hasTestTag(TokenDetailsScreenTestTags.STAKING_TOKEN_AMOUNT)
        useUnmergedTree = true
    }

    val stakingChevronIcon: KNode = child {
        hasTestTag(TokenDetailsScreenTestTags.STAKING_CHEVRON_ICON)
        useUnmergedTree = true
    }

    val stakingTitle: KNode = child {
        hasText(getResourceString(R.string.staking_native))
    }

    val title: KNode = child {
        hasTestTag(TokenDetailsScreenTestTags.TOKEN_TITLE)
    }

    private val horizontalActionChips = KLazyListNode(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(TokenDetailsScreenTestTags.HORIZONTAL_ACTION_CHIPS) },
        itemTypeBuilder = { itemType(::LazyListItemNode) },
        positionMatcher = { position ->
            SemanticsMatcher.expectValue(
                LazyListItemPositionSemantics,
                position
            )
        }
    )

    @OptIn(ExperimentalTestApi::class)
    val swapButton: LazyListItemNode = horizontalActionChips.childWith<LazyListItemNode> {
        hasTestTag(TokenDetailsScreenTestTags.ACTION_BUTTON)
        hasText(getResourceString(R.string.common_swap))
    }

    @OptIn(ExperimentalTestApi::class)
    val sellButton: LazyListItemNode = horizontalActionChips.childWith<LazyListItemNode> {
        hasTestTag(TokenDetailsScreenTestTags.ACTION_BUTTON)
        hasText(getResourceString(R.string.common_sell))
    }

    @OptIn(ExperimentalTestApi::class)
    val buyButton: LazyListItemNode = horizontalActionChips.childWith<LazyListItemNode> {
        hasTestTag(TokenDetailsScreenTestTags.ACTION_BUTTON)
        hasText(getResourceString(R.string.common_buy))
    }

    @OptIn(ExperimentalTestApi::class)
    val sendButton: LazyListItemNode = horizontalActionChips.childWith<LazyListItemNode> {
        hasTestTag(TokenDetailsScreenTestTags.ACTION_BUTTON)
        hasText(getResourceString(R.string.common_send))
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

    fun goToBuyCurrencyButton(feeCurrencySymbol: String): KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_buy_currency, feeCurrencySymbol))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTokenDetailsScreen(function: TokenDetailsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)