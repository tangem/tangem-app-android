package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.ResidenceSettingsScreenTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import com.tangem.features.onramp.impl.R as OnrampImplR

class ResidenceSettingsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ResidenceSettingsPageObject>(semanticsProvider = semanticsProvider) {

    val topBarTitle: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        hasText(getResourceString(R.string.onramp_settings_title))
        useUnmergedTree = true
    }

    val topBarCloseButton: KNode = child {
        hasTestTag(TopAppBarTestTags.CLOSE_BUTTON)
        useUnmergedTree = true
    }

    val residenceButton: KNode = child {
        hasText(getResourceString(OnrampImplR.string.onramp_settings_residence))
        useUnmergedTree = true
    }

    val countryName: KNode = child {
        hasTestTag(ResidenceSettingsScreenTestTags.COUNTRY_NAME)
        useUnmergedTree = true
    }

    val residenceSettingsDescription: KNode = child {
        hasText(getResourceString(OnrampImplR.string.onramp_settings_residence_description))
        useUnmergedTree = true
    }

}

internal fun BaseTestCase.onResidenceSettingsScreen(function: ResidenceSettingsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)