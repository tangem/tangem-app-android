package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.core.ui.test.TestTags
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class WalletSettingsTestScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<WalletSettingsTestScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(TestTags.WALLET_SETTINGS_SCREEN) }
    ) {
    private val walletSettingsItem: KNode = child {
        hasTestTag(TestTags.WALLET_SETTINGS_SCREEN_ITEM)
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