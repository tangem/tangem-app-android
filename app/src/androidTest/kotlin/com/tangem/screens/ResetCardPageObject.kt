package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.ResetCardScreenTestTags
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class ResetCardPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ResetCardPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(ResetCardScreenTestTags.TITLE)
        hasText(getResourceString(R.string.card_settings_reset_card_to_factory))
        useUnmergedTree = true
    }

    val attentionImage: KNode = child {
        hasTestTag(ResetCardScreenTestTags.ATTENTION_IMAGE)
        useUnmergedTree = true
    }

    val subtitle: KNode = child {
        hasTestTag(ResetCardScreenTestTags.SUBTITLE)
        hasText(getResourceString(R.string.common_attention))
        useUnmergedTree = true
    }

    val description: KNode = child {
        hasTestTag(ResetCardScreenTestTags.DESCRIPTION)
        useUnmergedTree = true
    }

    val lostWalletAccessCheckBox: KNode = child {
        hasTestTag(ResetCardScreenTestTags.CHECKBOX)
        hasAnySibling(
            withTestTag(ResetCardScreenTestTags.CHECKBOX_TEXT) and
            withText(getResourceString(R.string.reset_card_to_factory_condition_1))
        )
        useUnmergedTree = true
    }

    val lostPasswordRestoreCheckBox: KNode = child {
        hasTestTag(ResetCardScreenTestTags.CHECKBOX)
        hasAnySibling(
            withTestTag(ResetCardScreenTestTags.CHECKBOX_TEXT) and
                withText(getResourceString(R.string.reset_card_to_factory_condition_2))
        )
        useUnmergedTree = true
    }

    val resetCardButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasText(getResourceString(R.string.reset_card_to_factory_button_title))
    }
}

internal fun BaseTestCase.onResetCardScreen(function: ResetCardPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)