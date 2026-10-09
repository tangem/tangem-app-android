package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.SelectSwapProviderBottomSheetTestTags
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasAnyDescendant as withAnyDescendant
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class SelectSwapProviderBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SelectSwapProviderBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    fun providerItem(name: String, type: String, isBestRate: Boolean): KNode = child {
        hasTestTag(SelectSwapProviderBottomSheetTestTags.PROVIDER_ITEM)
        hasAnyDescendant(withText(name))
        hasAnyDescendant(withText(type))
        if (isBestRate) {
            hasAnyDescendant(withTestTag(SelectSwapProviderBottomSheetTestTags.BEST_RATE_LABEL))
        }
        useUnmergedTree = true
    }

    fun fcaWarningBadge(providerName: String): KNode = child {
        hasText(getResourceString(R.string.express_provider_fca_warning_list))
        hasAnyAncestor(
            withTestTag(SelectSwapProviderBottomSheetTestTags.PROVIDER_ITEM) and withAnyDescendant(withText(providerName)),
        )
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onSelectSwapProviderBottomSheet(function: SelectSwapProviderBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)