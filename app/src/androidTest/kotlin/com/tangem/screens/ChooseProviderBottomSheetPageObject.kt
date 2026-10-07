package com.tangem.screens

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import com.tangem.core.ui.test.SwapTokenScreenTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

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

    fun filterSegment(title: String): KNode = child {
        addSemanticsMatcher(SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected))
        hasAnyDescendant(withText(title))
        useUnmergedTree = true
    }

    /** Type label ("CEX", "DEX", "DEX/Bridge") of a provider row in the list. */
    fun providerTypeLabel(type: String): KNode = child {
        hasTestTag(SwapTokenScreenTestTags.PROVIDER_TYPE)
        hasText(type)
        useUnmergedTree = true
    }

    fun providerItem(name: String): KNode = child {
        hasText(name, substring = true)
        useUnmergedTree = true
    }

    // Provider rows have no testTag — the clickable row is the card holding both the name and the badge.
    fun unavailableProviderItem(name: String): KNode = child {
        hasClickAction()
        hasAnyDescendant(withText(name))
        hasAnyDescendant(withText(getResourceString(R.string.common_not_available)))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onChooseProviderBottomSheet(function: ChooseProviderBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)