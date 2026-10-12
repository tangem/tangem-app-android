package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.WarningBottomSheetTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

/** Message bottom sheets raised by the tariff-plan flows: the two cards-limit popups and 'Stay on plan'. */
class TangemPayTariffMessageSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayTariffMessageSheetPageObject>(semanticsProvider = semanticsProvider) {

    val cardsLimitForPlanTitle: KNode = title(
        getResourceString(CoreResR.string.tangempay_maximum_cards_issued_for_plan_title),
    )

    val cardsLimitForPlanDescription: KNode = message(
        getResourceString(CoreResR.string.tangempay_maximum_cards_issued_for_plan_description),
    )

    val maximumCardsTitle: KNode = title(getResourceString(CoreResR.string.tangempay_maximum_cards_issued_title))

    val maximumCardsDescription: KNode = message(
        getResourceString(CoreResR.string.tangempay_maximum_cards_issued_description),
    )

    fun stayOnPlanTitle(planName: String): KNode = title(
        getResourceString(CoreResR.string.tangempay_current_plan_stay_sheet_title, planName),
    )

    fun stayOnPlanDescription(targetPlanName: String): KNode = message(
        getResourceString(CoreResR.string.tangempay_current_plan_stay_sheet_body, targetPlanName),
    )

    val primaryButton: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.BUTTON_PRIMARY)
        useUnmergedTree = true
    }

    val cancelButton: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.BUTTON_SECONDARY)
        hasAnyDescendant(withText(getResourceString(CoreResR.string.common_cancel)))
        useUnmergedTree = true
    }

    private fun title(text: String): KNode = child {
        hasTestTag(WarningBottomSheetTestTags.TITLE)
        hasText(text)
        useUnmergedTree = true
    }

    private fun message(text: String): KNode = child {
        hasTestTag(WarningBottomSheetTestTags.MESSAGE)
        hasText(text)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayTariffMessageSheet(
    function: TangemPayTariffMessageSheetPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)