package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.WalletConnectBottomSheetTestTags
import com.tangem.core.ui.test.WalletConnectDetailsBottomSheetTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import com.tangem.features.walletconnect.impl.R as WalletConnectImplR

class WalletConnectDetailsBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<WalletConnectDetailsBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(WalletConnectDetailsBottomSheetTestTags.TITLE)
        hasText(getResourceString(WalletConnectImplR.string.wc_connected_app_title))
        useUnmergedTree = true
    }

    val date: KNode = child {
        hasTestTag(WalletConnectDetailsBottomSheetTestTags.DATE)
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasTestTag(WalletConnectDetailsBottomSheetTestTags.CLOSE_BUTTON)
        useUnmergedTree = true
    }

    val networksBlockTitle: KNode = child {
        hasText(getResourceString(WalletConnectImplR.string.wc_connected_networks))
        useUnmergedTree = true
    }

    val appIcon: KNode = child {
        hasTestTag(WalletConnectBottomSheetTestTags.APP_ICON)
        useUnmergedTree = true
    }

    val approveIcon: KNode = child {
        hasTestTag(WalletConnectBottomSheetTestTags.APPROVE_ICON)
        useUnmergedTree = true
    }

    val appName: KNode = child {
        hasTestTag(WalletConnectBottomSheetTestTags.APP_NAME)
        useUnmergedTree = true
    }

    val appUrl: KNode = child {
        hasTestTag(WalletConnectBottomSheetTestTags.APP_URL)
        useUnmergedTree = true
    }

    val walletIcon: KNode = child {
        hasTestTag(WalletConnectDetailsBottomSheetTestTags.WALLET_ICON)
        useUnmergedTree = true
    }

    val walletTitle: KNode = child {
        hasTestTag(WalletConnectDetailsBottomSheetTestTags.WALLET_TITLE)
        useUnmergedTree = true
    }

    val walletName: KNode = child {
        hasTestTag(WalletConnectDetailsBottomSheetTestTags.WALLET_NAME)
        useUnmergedTree = true
    }

    val connectedNetworksTitle: KNode = child {
        hasTestTag(WalletConnectDetailsBottomSheetTestTags.NETWORKS_TITLE)
        useUnmergedTree = true
    }

    val connectedNetworkItem: KNode = child {
        hasTestTag(WalletConnectDetailsBottomSheetTestTags.NETWORK_ITEM)
        useUnmergedTree = true
    }

    val connectedNetworkIcon: KNode = child {
        hasTestTag(WalletConnectDetailsBottomSheetTestTags.NETWORK_ICON)
        useUnmergedTree = true
    }

    val connectedNetworkName: KNode = child {
        hasTestTag(WalletConnectDetailsBottomSheetTestTags.NETWORK_NAME)
        useUnmergedTree = true
    }

    val connectedNetworkSymbol: KNode = child {
        hasTestTag(WalletConnectDetailsBottomSheetTestTags.NETWORK_SYMBOL)
        useUnmergedTree = true
    }

    val disconnectButton: KNode = child {
        hasText(getResourceString(WalletConnectImplR.string.common_disconnect))
        hasTestTag(BaseButtonTestTags.TEXT)
        useUnmergedTree = true
    }

}

internal fun BaseTestCase.onWalletConnectDetailsBottomSheet(function: WalletConnectDetailsBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)