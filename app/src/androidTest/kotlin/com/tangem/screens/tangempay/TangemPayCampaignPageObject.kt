package com.tangem.screens.tangempay

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.TopNavigationTestTags
import io.github.kakaocup.compose.intercept.operation.ComposeOperationType
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TangemPayCampaignPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayCampaignPageObject>(semanticsProvider = semanticsProvider) {

    val landingTitle: KNode = textNode(CoreResR.string.tangempay_campaign_landing_title)

    val privacyBlockTitle: KNode = textNode(CoreResR.string.tangempay_onboarding_security_title)

    val privacyBlockDescription: KNode = textNode(CoreResR.string.tangempay_onboarding_security_description)

    val cashbackBlockTitle: KNode = textNode(CoreResR.string.tangempay_campaign_cashback_title)

    val cashbackBlockDescription: KNode = textNode(CoreResR.string.tangempay_campaign_cashback_description)

    val spendBlockTitle: KNode = textNode(CoreResR.string.tangempay_onboarding_pay_title)

    val spendBlockDescription: KNode = textNode(CoreResR.string.tangempay_campaign_spend_description)

    val legalCaption: KNode = child {
        hasText(getResourceString(CoreResR.string.common_privacy_policy), substring = true)
        useUnmergedTree = true
    }

    val getCardButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(CoreResR.string.tangempay_onboarding_get_card_button_text))
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasTestTag(TopNavigationTestTags.CLOSE_BUTTON)
        useUnmergedTree = true
    }

    val issueCardTitle: KNode = textNode(CoreResR.string.tangempay_campaign_issue_card_title)

    val issueFeeTitle: KNode = textNode(CoreResR.string.tangempay_order_type_issue_fee)

    fun issueFeeValue(fee: String): KNode = child {
        hasText(fee)
        useUnmergedTree = true
    }

    val issueTimeTitle: KNode = textNode(CoreResR.string.tangempay_campaign_issue_time)

    val issueTimeInstantValue: KNode = textNode(CoreResR.string.tangempay_order_type_delivery_time_instant)

    val issueButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(CoreResR.string.tangempay_campaign_issue_button))
        useUnmergedTree = true
    }

    fun assertBlocksAreInOrder() {
        val blockTops = listOf(privacyBlockTitle, cashbackBlockTitle, spendBlockTitle).map { it.topInRoot() }
        if (blockTops != blockTops.sorted()) throw AssertionError("Landing blocks are out of order: $blockTops")
    }

    private fun KNode.topInRoot(): Float {
        var top = 0f
        delegate.perform(BlockAction.READ_POSITION) { top = fetchSemanticsNode().positionInRoot.y }
        return top
    }

    private fun textNode(@StringRes textRes: Int): KNode = child {
        hasText(getResourceString(textRes))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayCampaignScreen(function: TangemPayCampaignPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)

private enum class BlockAction : ComposeOperationType { READ_POSITION }