package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.core.ui.test.TestTags
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class DetailsTestScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<DetailsTestScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(TestTags.DETAILS_SCREEN) }
    ) {

    val walletConnectButton: KNode = child {
        hasTestTag(TestTags.DETAILS_SCREEN_ITEM)
        hasText(getResourceString(R.string.wallet_connect_title))
    }

    private val walletBlock: KNode = child {
        hasTestTag(TestTags.DETAILS_SCREEN_ITEM)
    }

    val walletNameButton: KNode = walletBlock.child {
        hasClickAction()
        hasPosition(0)
    }

    val scanCardButton: KNode = walletBlock.child {
        hasText(getResourceString(R.string.scan_card_settings_button))
    }

    val buyTangemButton: KNode = child {
        hasTestTag(TestTags.DETAILS_SCREEN_ITEM)
        hasText(getResourceString(R.string.details_buy_wallet))
    }

    val appSettingsButton: KNode = child {
        hasTestTag(TestTags.DETAILS_SCREEN_ITEM)
        hasText(getResourceString(R.string.app_settings_title))
    }
    val contactSupportButton: KNode = child {
        hasTestTag(TestTags.DETAILS_SCREEN_ITEM)
        hasText(getResourceString(R.string.details_row_title_contact_to_support))
    }
    val toSButton: KNode = child {
        hasTestTag(TestTags.DETAILS_SCREEN_ITEM)
        hasText(getResourceString(R.string.disclaimer_title))
    }
}