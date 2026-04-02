package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.TopAppBarTestTags
import com.tangem.core.ui.test.WalletSettingsScreenTestTags
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

class WalletSettingsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<WalletSettingsPageObject>(semanticsProvider = semanticsProvider) {

    val topAppBarBackButton: KNode = child {
        hasTestTag(TopAppBarTestTags.CLOSE_BUTTON)
    }

    private val walletSettingsItem: KNode = child {
        hasTestTag(WalletSettingsScreenTestTags.SCREEN_ITEM)
    }

    val linkMoreCardsButton: KNode = walletSettingsItem.child {
        hasText(getResourceString(R.string.details_row_title_create_backup))
    }

    val deviceSettingsButton: KNode = walletSettingsItem.child {
        hasText(getResourceString(R.string.card_settings_title))
    }

    val referralProgramButton: KNode = walletSettingsItem.child {
        hasText(getResourceString(R.string.details_referral_title))
    }

    val forgetWalletButton: KNode = walletSettingsItem.child {
        hasText(getResourceString(R.string.settings_forget_wallet))
    }

    fun accountItem(accountName: String): KNode = walletSettingsItem.child {
        hasTestTag(WalletSettingsScreenTestTags.USER_ACCOUNT_ITEM)
        hasAnyDescendant(withText(accountName))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onWalletSettingsScreen(function: WalletSettingsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)