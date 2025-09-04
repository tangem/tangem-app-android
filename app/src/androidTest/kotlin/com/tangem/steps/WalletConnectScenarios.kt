package com.tangem.steps

import com.tangem.common.BaseTestCase
import com.tangem.screens.onWalletConnectBottomSheet
import com.tangem.screens.onWalletConnectDetailsBottomSheet
import com.tangem.screens.onWalletConnectScreen
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.checkWalletConnectBottomSheet() {
    step("Assert 'Wallet Connect' bottom sheet title is displayed") {
        onWalletConnectBottomSheet { title.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet app icon is displayed") {
        onWalletConnectBottomSheet { appIcon.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet app name is displayed") {
        onWalletConnectBottomSheet { appName.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet approve icon is displayed") {
        onWalletConnectBottomSheet { approveIcon.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet app URL is displayed") {
        onWalletConnectBottomSheet { appUrl.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet connection request icon is displayed") {
        onWalletConnectBottomSheet { connectionRequestIcon.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet connection request text is displayed") {
        onWalletConnectBottomSheet { connectionRequestText.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet connection request chevron is displayed") {
        onWalletConnectBottomSheet { connectionRequestChevron.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet wallet icon is displayed") {
        onWalletConnectBottomSheet { walletIcon.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet wallet title is displayed") {
        onWalletConnectBottomSheet { walletNameTitle.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet wallet name is displayed") {
        onWalletConnectBottomSheet { walletName.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet networks icon is displayed") {
        onWalletConnectBottomSheet { networksIcon.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet networks title is displayed") {
        onWalletConnectBottomSheet { networksTitle.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet right networks icons is displayed") {
        onWalletConnectBottomSheet { networksIcons.assertIsDisplayed() }
    }
    step("'Wallet Connect' bottom sheet networks selector icon is displayed") {
        onWalletConnectBottomSheet { networksSelectorIcon.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet 'Cancel' button is displayed") {
        onWalletConnectBottomSheet { cancelButton.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet 'Connect' button is displayed") {
        onWalletConnectBottomSheet { connectButton.assertIsDisplayed() }
    }
}

fun BaseTestCase.checkWalletConnectScreen() {
    step("Assert 'Wallet Connect' title is displayed") {
        onWalletConnectScreen { title.assertIsDisplayed() }
    }
    step("Assert 'More' button is displayed") {
        onWalletConnectScreen { moreButton.assertIsDisplayed() }
    }
    step("Assert wallet name is displayed") {
        onWalletConnectScreen { walletName.assertIsDisplayed() }
    }
    step("Assert app icon is displayed") {
        onWalletConnectScreen { appIcon.assertIsDisplayed() }
    }
    step("Assert app name is displayed") {
        onWalletConnectScreen { appName.assertIsDisplayed() }
    }
    step("Assert approve icon is displayed") {
        onWalletConnectScreen { approveIcon.assertIsDisplayed() }
    }
    step("Assert app URL is displayed") {
        onWalletConnectScreen { appUrl.assertIsDisplayed() }
    }
    step("Assert 'New Connection' button is displayed") {
        onWalletConnectScreen { newConnectionButton.assertIsDisplayed() }
    }
}

fun BaseTestCase.checkWalletConnectDetailsBottomSheet(dAppName: String) {
    step("Assert connection details title is displayed") {
        onWalletConnectDetailsBottomSheet { title.assertIsDisplayed() }
    }
    step("Assert date is displayed") {
        onWalletConnectDetailsBottomSheet { date.assertIsDisplayed() }
    }
    step("Assert 'Close' button is displayed") {
        onWalletConnectDetailsBottomSheet { closeButton.assertIsDisplayed() }
    }
    step("Assert app icon is displayed") {
        onWalletConnectDetailsBottomSheet { appIcon.assertIsDisplayed() }
    }
    step("Assert app name is displayed") {
        onWalletConnectDetailsBottomSheet { appName.assertIsDisplayed() }
    }
    step("Assert approve icon is displayed") {
        onWalletConnectDetailsBottomSheet { approveIcon.assertIsDisplayed() }
    }
    step("Assert app URL is displayed") {
        onWalletConnectDetailsBottomSheet { appUrl.assertIsDisplayed() }
    }
    step("Assert wallet icon is displayed") {
        onWalletConnectDetailsBottomSheet { walletIcon.assertIsDisplayed() }
    }
    step("Assert wallet title is displayed") {
        onWalletConnectDetailsBottomSheet { walletTitle.assertIsDisplayed() }
    }
    step("Assert wallet name is displayed") {
        onWalletConnectDetailsBottomSheet { walletName.assertIsDisplayed() }
    }
    step("Assert 'Connected networks' title is displayed") {
        onWalletConnectDetailsBottomSheet { connectedNetworksTitle.assertIsDisplayed() }
    }
    step("Assert connected network item is displayed") {
        onWalletConnectDetailsBottomSheet { connectedNetworkItem.assertIsDisplayed() }
    }
    step("Assert connected network icon is displayed") {
        onWalletConnectDetailsBottomSheet { connectedNetworkIcon.assertIsDisplayed() }
    }
    step("Assert connected dApp name: '$dAppName'") {
        onWalletConnectDetailsBottomSheet { connectedNetworkName.assertTextContains(dAppName) }
    }
    step("Assert connected network symbol is displayed") {
        onWalletConnectDetailsBottomSheet { connectedNetworkSymbol.assertIsDisplayed() }
    }
    step("Assert 'Disconnect button' is displayed") {
        onWalletConnectDetailsBottomSheet { disconnectButton.assertIsDisplayed() }
    }
}