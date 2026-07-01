package com.tangem.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.performScrollToNode
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.AddCustomTokenScreenTestTags
import com.tangem.core.ui.test.BaseButtonTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import com.tangem.core.res.R as CoreResR

class AddCustomTokenPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<AddCustomTokenPageObject>(semanticsProvider = semanticsProvider) {

    private val selectorList: KNode = child {
        hasTestTag(AddCustomTokenScreenTestTags.SELECTOR_LIST)
    }

    fun networkRow(networkName: String): KNode = child {
        hasTestTag(AddCustomTokenScreenTestTags.networkRow(networkName))
    }

    @OptIn(ExperimentalTestApi::class)
    fun scrollToNetwork(networkName: String) = selectorList {
        performScrollToNode(withTestTag(AddCustomTokenScreenTestTags.networkRow(networkName)))
    }

    val contractAddressField: KNode = child {
        hasTestTag(AddCustomTokenScreenTestTags.CONTRACT_ADDRESS_FIELD)
    }

    val derivationSelectorField: KNode = child {
        hasTestTag(AddCustomTokenScreenTestTags.DERIVATION_SELECTOR_FIELD)
    }

    val customDerivationButton: KNode = child {
        hasTestTag(AddCustomTokenScreenTestTags.CUSTOM_DERIVATION_BUTTON)
    }

    val warningNotification: KNode = child {
        hasTestTag(AddCustomTokenScreenTestTags.WARNING_NOTIFICATION)
    }

    val addTokenButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasText(getResourceString(CoreResR.string.common_add_token))
    }
}

internal fun BaseTestCase.onAddCustomTokenScreen(function: AddCustomTokenPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)