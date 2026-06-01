package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.DetailsScreenTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

class DetailsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<DetailsPageObject>(semanticsProvider = semanticsProvider) {

    val topAppBarBackButton: KNode = child {
        hasTestTag(TopAppBarTestTags.CLOSE_BUTTON)
    }

    val walletConnectButton: KNode = child {
        hasTestTag(DetailsScreenTestTags.SCREEN_ITEM)
        hasText(getResourceString(R.string.wallet_connect_title))
    }

    // Match the wallet row by its own tag (not position-0 clickable, which races with the async-loading
    // "Add Wallet" button and otherwise triggers a re-scan → "already saved" dialog).
    val walletNameButton: KNode = child {
        hasTestTag(DetailsScreenTestTags.USER_WALLET_ITEM)
        useUnmergedTree = true
    }

    val buyTangemButton: KNode = child {
        hasTestTag(DetailsScreenTestTags.SCREEN_ITEM)
        hasText(getResourceString(R.string.details_buy_wallet))
    }

    val appSettingsButton: KNode = child {
        hasTestTag(DetailsScreenTestTags.SCREEN_ITEM)
        hasText(getResourceString(R.string.app_settings_title))
    }

    val contactSupportButton: KNode = child {
        hasTestTag(DetailsScreenTestTags.SCREEN_ITEM)
        hasText(getResourceString(R.string.common_contact_support))
    }
    val toSButton: KNode = child {
        hasTestTag(DetailsScreenTestTags.SCREEN_ITEM)
        hasText(getResourceString(R.string.disclaimer_title))
    }

    val versionName: KNode = child {
        hasTestTag(DetailsScreenTestTags.VERSION_NAME)
        useUnmergedTree = true
    }

    fun walletNameValue(name: String): KNode = child {
        hasTestTag(DetailsScreenTestTags.SCREEN_ITEM)
        hasAnyDescendant(withText(name))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onDetailsScreen(function: DetailsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)