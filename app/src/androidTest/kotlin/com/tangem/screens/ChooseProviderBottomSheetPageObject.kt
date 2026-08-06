package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class ChooseProviderBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ChooseProviderBottomSheetPageObject>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(BaseBottomSheetTestTags.CONTAINER) },
    ) {

    // ALL / CEX / DEX segmented filter — matched by its label (no dedicated testTag on segments).
    fun filterButton(title: String): KNode = child {
        hasText(title)
        useUnmergedTree = true
    }

    fun providerItem(name: String): KNode = child {
        hasText(name, substring = true)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onChooseProviderBottomSheet(function: ChooseProviderBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)