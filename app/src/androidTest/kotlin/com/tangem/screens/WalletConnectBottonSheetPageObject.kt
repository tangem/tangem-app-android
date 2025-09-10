package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.WalletConnectBottomSheetTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import com.tangem.features.walletconnect.impl.R as WalletConnectImplR

class WalletConnectBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<WalletConnectBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(WalletConnectImplR.string.wc_wallet_connect))
        hasTestTag(WalletConnectBottomSheetTestTags.TITLE)
        useUnmergedTree = true
    }

    val appIcon: KNode = child {
        hasTestTag(WalletConnectBottomSheetTestTags.APP_ICON)
        useUnmergedTree = true
    }

    val appName: KNode = child {
        hasTestTag(WalletConnectBottomSheetTestTags.APP_NAME)
        useUnmergedTree = true
    }

    val approveIcon: KNode = child {
        hasTestTag(WalletConnectBottomSheetTestTags.APPROVE_ICON)
        useUnmergedTree = true
    }

    val appUrl: KNode = child {
        hasTestTag(WalletConnectBottomSheetTestTags.APP_URL)
        useUnmergedTree = true
    }

    val connectionRequestIcon: KNode = child {
        hasTestTag(WalletConnectBottomSheetTestTags.CONNECTION_REQUEST_ICON)
        useUnmergedTree = true
    }

    val connectionRequestText: KNode = child {
        hasTestTag(WalletConnectBottomSheetTestTags.CONNECTION_REQUEST_TEXT)
        useUnmergedTree = true
    }

    val connectionRequestChevron: KNode = child {
        hasTestTag(WalletConnectBottomSheetTestTags.CONNECTION_REQUEST_CHEVRON)
        useUnmergedTree = true
    }

    val walletIcon: KNode = child {
        hasTestTag(WalletConnectBottomSheetTestTags.WALLET_ICON)
        useUnmergedTree = true
    }

    val walletNameTitle: KNode = child {
        hasTestTag(WalletConnectBottomSheetTestTags.WALLET_NAME_TITLE)
        useUnmergedTree = true
    }

    val walletName: KNode = child {
        hasTestTag(WalletConnectBottomSheetTestTags.WALLET_NAME)
        useUnmergedTree = true
    }

    val networksIcon: KNode = child {
        hasTestTag(WalletConnectBottomSheetTestTags.NETWORKS_ICON)
        useUnmergedTree = true
    }

    val networksTitle: KNode = child {
        hasTestTag(WalletConnectBottomSheetTestTags.NETWORKS_TITLE)
        useUnmergedTree = true
    }

    val networksIcons: KNode = child {
        hasTestTag(WalletConnectBottomSheetTestTags.NETWORKS_ICONS)
        useUnmergedTree = true
    }

    val networksSelectorIcon: KNode = child {
        hasTestTag(WalletConnectBottomSheetTestTags.NETWORKS_SELECTOR_ICON)
        useUnmergedTree = true
    }

    val cancelButton: KNode = child {
        hasText(getResourceString(R.string.common_cancel))
        hasTestTag(BaseButtonTestTags.TEXT)
        useUnmergedTree = true
    }

    val connectButton: KNode = child {
        hasText(getResourceString(R.string.wc_common_connect))
        hasTestTag(BaseButtonTestTags.TEXT)
        useUnmergedTree = true
    }

}

internal fun BaseTestCase.onWalletConnectBottomSheet(function: WalletConnectBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)