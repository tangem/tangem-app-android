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

class TangemPayRegionUnavailableSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayRegionUnavailableSheetPageObject>(semanticsProvider = semanticsProvider) {

    val icon: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.ICON)
        useUnmergedTree = true
    }

    val title: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.TITLE)
        hasText(getResourceString(CoreResR.string.money_account_main_status_dialog_country_not_supported_title))
        useUnmergedTree = true
    }

    val message: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.MESSAGE)
        hasText(
            getResourceString(
                CoreResR.string.money_account_main_status_dialog_country_not_supported_description_template,
                getResourceString(CoreResR.string.common_contact_support),
            ),
        )
        useUnmergedTree = true
    }

    val selectAnotherRegionButton: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.BUTTON_SECONDARY)
        hasAnyDescendant(withText(getResourceString(CoreResR.string.tangempay_region_unavailable_secondary_button)))
        useUnmergedTree = true
    }

    val hideAccountButton: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.BUTTON_PRIMARY)
        hasAnyDescendant(withText(getResourceString(CoreResR.string.money_account_main_status_dialog_hide_account)))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayRegionUnavailableSheet(
    function: TangemPayRegionUnavailableSheetPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)