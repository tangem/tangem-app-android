package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.ReferralProgramScreenTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import com.tangem.feature.referral.presentation.R as ReferralPresentationR
import androidx.compose.ui.test.hasText as withText

class ReferralProgramPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ReferralProgramPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        hasText(getResourceString(R.string.details_referral_title))
        useUnmergedTree = true
    }

    val referTitle: KNode = child {
        hasText(getResourceString(R.string.referral_title))
        useUnmergedTree = true
    }

    val image: KNode = child {
        hasTestTag(ReferralProgramScreenTestTags.IMAGE)
        useUnmergedTree = true
    }

    val infoForYouText: KNode = child {
        hasTestTag(ReferralProgramScreenTestTags.INFO_FOR_YOU_TEXT)
        useUnmergedTree = true
    }

    val infoForYouBlock: KNode = child {
        hasTestTag(ReferralProgramScreenTestTags.CONDITION_BLOCK)
        hasAnyDescendant(
            withText(
                getResourceString(ReferralPresentationR.string.referral_point_currencies_title),
                substring = true
            )
        )
        useUnmergedTree = true
    }

    val infoForYourFriendText: KNode = child {
        hasTestTag(ReferralProgramScreenTestTags.INFO_FOR_YOUR_FRIEND_TEXT)
        useUnmergedTree = true
    }

    val infoForYourFriendBlock: KNode = child {
        hasTestTag(ReferralProgramScreenTestTags.CONDITION_BLOCK)
        hasAnyDescendant(
            withText(
                getResourceString(ReferralPresentationR.string.referral_point_discount_title),
                substring = true
            )
        )
        useUnmergedTree = true
    }

    val agreementText: KNode = child {
        hasText(getResourceString(
            ReferralPresentationR.string.referral_tos_not_enroled_prefix) + " " +
            getResourceString(ReferralPresentationR.string.common_terms_and_conditions ) + " " +
            getResourceString(ReferralPresentationR.string.referral_tos_suffix),
            substring = true
        )
        useUnmergedTree = true
    }

    val participateButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onReferralProgramScreen(function: ReferralProgramPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)