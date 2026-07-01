package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.performTextInputInChunks
import com.tangem.screens.accounts.onAccountDetailsScreen
import com.tangem.screens.onAddCustomTokenScreen
import com.tangem.screens.onDetailsScreen
import com.tangem.screens.onDialog
import com.tangem.screens.onManageTokensScreen
import com.tangem.screens.onWalletSettingsScreen
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.Allure.step
import com.tangem.core.res.R as CoreResR

private fun mainAccountName(): String = getResourceString(CoreResR.string.account_main_account_title)

// flakySafely is unavailable in BaseTestCase extensions — wait until the assertion/action stops throwing.
private fun BaseTestCase.awaitSuccess(block: () -> Unit) {
    composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT) { runCatching(block).isSuccess }
}

fun BaseTestCase.openManageTokens(accountName: String = mainAccountName()) {
    openWalletSettingsScreen()
    openAccountDetails(accountName)
    step("Click on 'Manage tokens' button") {
        onAccountDetailsScreen { manageTokensButton.clickWithAssertion() }
    }
}

fun BaseTestCase.openAddCustomToken(accountName: String = mainAccountName()) {
    openManageTokens(accountName)
    step("Click on 'Add custom token' button") {
        onManageTokensScreen { addCustomTokenButton.clickWithAssertion() }
    }
}

fun BaseTestCase.addCustomTokenWithCustomDerivation(network: String, contract: String, derivationPath: String) {
    step("Click on network: '$network'") {
        awaitSuccess { onAddCustomTokenScreen { scrollToNetwork(network) } }
        onAddCustomTokenScreen { networkRow(network).performClick() }
    }
    step("Enter contract address: '$contract'") {
        awaitSuccess { onAddCustomTokenScreen { contractAddressField.assertExists() } }
        onAddCustomTokenScreen { contractAddressField.performTextInputInChunks(contract) }
    }
    step("Click on 'Derivation path' field") {
        awaitSuccess { onAddCustomTokenScreen { derivationSelectorField.assertExists() } }
        onAddCustomTokenScreen { derivationSelectorField.performClick() }
    }
    step("Click on 'Custom derivation' button") {
        awaitSuccess { onAddCustomTokenScreen { customDerivationButton.assertIsDisplayed() } }
        onAddCustomTokenScreen { customDerivationButton.performClick() }
    }
    step("Enter custom derivation path: '$derivationPath'") {
        awaitSuccess { onDialog { inputField.assertIsDisplayed() } }
        onDialog { inputField.performTextInputInChunks(derivationPath) }
        awaitSuccess { onDialog { okButton.assertIsEnabled() } }
        onDialog { okButton.performClick() }
    }
    step("Click on 'Add token' button") {
        awaitSuccess { onAddCustomTokenScreen { addTokenButton.assertIsEnabled() } }
        onAddCustomTokenScreen { addTokenButton.performClick() }
    }
    navigateBackToMainFromManageTokens()
}

fun BaseTestCase.navigateBackToMainFromManageTokens() {
    step("Click on 'Manage tokens' screen 'Back' button") {
        waitForIdle()
        onManageTokensScreen { topAppBarBackButton.performClick() }
    }
    step("Click on 'Account details' screen 'Back' button") {
        waitForIdle()
        onAccountDetailsScreen { topAppBarBackButton.performClick() }
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