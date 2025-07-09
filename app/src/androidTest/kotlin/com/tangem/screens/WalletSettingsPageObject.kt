package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.WalletSettingsScreenTestTags
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class WalletSettingsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<WalletSettingsPageObject>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(WalletSettingsScreenTestTags.SCREEN_CONTAINER) }
    ) {
    private val walletSettingsItem: KNode = child {
        hasTestTag(WalletSettingsScreenTestTags.SCREEN_ITEM)
    }

    val linkMoreCardsButton: KNode = walletSettingsItem.child {
        hasText(getResourceString(R.string.details_row_title_create_backup))
    }
    val cardSettingsButton: KNode = walletSettingsItem.child {
        hasText(getResourceString(R.string.card_settings_title))
    }
    val referralProgramButton: KNode = walletSettingsItem.child {
        hasText(getResourceString(R.string.details_referral_title))
    }
    val forgetWalletButton: KNode = walletSettingsItem.child {
        hasText(getResourceString(R.string.settings_forget_wallet))
    }
}

internal fun BaseTestCase.onWalletSettingsScreen(function: WalletSettingsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)