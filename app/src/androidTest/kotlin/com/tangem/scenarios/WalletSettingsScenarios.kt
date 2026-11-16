package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.screens.onDialog
import com.tangem.screens.onForgetWalletScreen
import com.tangem.screens.onIntroductionScreen
import com.tangem.screens.onWalletSettingsScreen
import com.tangem.screens.onWarningBottomSheet
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.forgetLastHotWallet() {
    openWalletSettingsScreen()
    step("Click on 'Forget wallet' button") {
        onWalletSettingsScreen {
            scrollToForgetWallet()
            forgetWalletButton.performClick()
        }
    }
    step("Click on 'Forget wallet' button in the warning sheet") {
        awaitSuccess { onWarningBottomSheet { forgetWalletButton.assertIsDisplayed() } }
        onWarningBottomSheet { forgetWalletButton.performClick() }
    }
    step("Click on the acknowledgement checkbox") {
        awaitSuccess { onForgetWalletScreen { acknowledgeCheckbox.assertIsDisplayed() } }
        onForgetWalletScreen { acknowledgeCheckbox.performClick() }
    }
    step("Click on 'Forget wallet' button") {
        onForgetWalletScreen {
            forgetWalletButton.assertIsEnabled()
            forgetWalletButton.performClick()
        }
    }
    step("Click on 'Forget' dialog button") {
        onDialog { forgetButton.clickWithAssertion() }
    }
    step("Assert 'Welcome' screen is displayed") {
        awaitSuccess { onIntroductionScreen { iHaveWalletButton.assertIsDisplayed() } }
    }
}