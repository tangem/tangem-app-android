package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.screens.onDetailsScreen
import com.tangem.screens.onMainScreen
import com.tangem.screens.onNFTAssetDetailsScreen
import com.tangem.screens.onNFTCollectionsScreen
import com.tangem.screens.onSendAddressScreen
import com.tangem.screens.onWalletSettingsScreen
import io.qameta.allure.kotlin.Allure.step

/** Turns the NFT block on from wallet settings and walks back to Main. */
fun BaseTestCase.enableNFTAndReturnToMain() {
    openWalletSettingsScreen()
    step("Click on 'NFT' switch") {
        // The row is only composed once scrolled to, so waiting for the switch first waits for nothing.
        awaitSuccess {
            onWalletSettingsScreen {
                scrollToNFT()
                nftSwitch.assertExists()
            }
        }
        onWalletSettingsScreen {
            // Enabling only: a caller arriving with NFT already on fails here rather than having this
            // helper switch it off, which would also hide state leaking in from an earlier test.
            nftSwitch.assertIsOff()
            nftSwitch.performClick()
        }
    }
    step("Assert 'NFT' switch is on") {
        awaitSuccess { onWalletSettingsScreen { nftSwitch.assertIsOn() } }
    }
    step("Click on 'Wallet settings' screen 'Back' button") {
        waitForIdle()
        onWalletSettingsScreen { topAppBarBackButton.performClick() }
    }
    step("Click on 'Details' screen 'Back' button") {
        waitForIdle()
        onDetailsScreen { topAppBarBackButton.performClick() }
    }
}

/** From Main, opens the 'NFT collections' screen via its block. */
fun BaseTestCase.openNFTCollectionsScreen() {
    step("Click on 'NFT collections' block") {
        awaitSuccess { onMainScreen { nftBlock.assertExists() } }
        onMainScreen {
            scrollToNFTBlock()
            nftBlock.performClick()
        }
    }
    step("Assert 'Receive NFT' button is displayed") {
        awaitSuccess { onNFTCollectionsScreen { receiveButton.assertIsDisplayed() } }
    }
}

/** From the NFT collections screen, expands [collectionName] and opens the [assetName] details. */
fun BaseTestCase.openNFTAssetDetails(collectionName: String, assetName: String) {
    step("Click on '$collectionName' collection") {
        awaitSuccess { onNFTCollectionsScreen { collectionItem(collectionName).assertExists() } }
        onNFTCollectionsScreen {
            scrollToText(collectionName)
            collectionItem(collectionName).performClick()
        }
    }
    step("Click on '$assetName' asset") {
        awaitSuccess { onNFTCollectionsScreen { assetItem(assetName).assertExists() } }
        onNFTCollectionsScreen {
            scrollToText(assetName)
            assetItem(assetName).performClick()
        }
    }
    step("Assert 'NFT asset details' screen is displayed") {
        awaitSuccess { onNFTAssetDetailsScreen { topAppBarTitle(assetName).assertIsDisplayed() } }
    }
}

/** From the NFT asset details screen, opens the send 'Address' step. */
fun BaseTestCase.openNFTSendAddressScreen() {
    step("Click on 'Send' button") {
        awaitSuccess { onNFTAssetDetailsScreen { sendButton.assertIsDisplayed() } }
        onNFTAssetDetailsScreen { sendButton.performClick() }
    }
    step("Assert 'Address' screen is displayed") {
        awaitSuccess { onSendAddressScreen { container.assertIsDisplayed() } }
    }
}

/** From the NFT collections screen, opens the account picker behind 'Receive NFT'. */
fun BaseTestCase.openNFTReceivePortfolioSelector() {
    step("Click on 'Receive NFT' button") {
        onNFTCollectionsScreen { receiveButton.clickWithAssertion() }
    }
}