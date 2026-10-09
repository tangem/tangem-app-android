package com.tangem.screens.tangempay

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag

class TangemPayCardActivationPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayCardActivationPageObject>(semanticsProvider = semanticsProvider) {

    val topBarHeader: KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.CARD_ACTIVATION_TOP_BAR))
        hasText(getResourceString(CoreResR.string.tangempay_card_details_activate))
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.CARD_ACTIVATION_TOP_BAR))
        addSemanticsMatcher(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        useUnmergedTree = true
    }

    val lastDigitsInput: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_ACTIVATION_INPUT_FIELD)
        useUnmergedTree = true
    }

    val hint: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_ACTIVATION_HINT)
        useUnmergedTree = true
    }

    val continueButton: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_ACTIVATION_CONTINUE_BUTTON)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayCardActivationScreen(function: TangemPayCardActivationPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)