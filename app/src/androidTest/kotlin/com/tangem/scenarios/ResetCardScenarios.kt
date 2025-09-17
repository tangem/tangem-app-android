package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.screens.onResetCardScreen
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.checkResetCardScreen(withBackup: Boolean = false) {
    step("Assert title is displayed") {
        onResetCardScreen { title.assertIsDisplayed() }
    }
    step("Assert 'Attention' image is displayed") {
        onResetCardScreen { attentionImage.assertIsDisplayed() }
    }
    step("Assert 'Attention' subtitle is displayed") {
        onResetCardScreen { subtitle.assertIsDisplayed() }
    }
    step("Assert description is displayed") {
        onResetCardScreen { description.assertIsDisplayed() }
    }
    step("Assert 'Lost wallet' checkbox is displayed") {
        onResetCardScreen { lostWalletAccessCheckBox.assertIsDisplayed() }
    }
    if (withBackup) {
        step("Assert 'Lost password restore' checkbox is displayed") {
            onResetCardScreen { lostPasswordRestoreCheckBox.assertIsDisplayed() }
        }
    } else {
        step("Assert 'Lost password restore' checkbox doesn't exist") {
            onResetCardScreen { lostPasswordRestoreCheckBox.assertDoesNotExist() }
        }
    }
}

fun BaseTestCase.checkCheckBoxLogic(withBackup: Boolean = false) {
    if (withBackup) {
        step("Click on 'Lost wallet' checkbox") {
            onResetCardScreen { lostWalletAccessCheckBox.performClick() }
        }
        step("Assert 'Lost wallet' checkbox is enabled") {
            onResetCardScreen { lostWalletAccessCheckBox.assertIsEnabled() }
        }
        step("Assert 'Reset the card' button is disabled") {
            onResetCardScreen { resetCardButton.assertIsNotEnabled() }
        }
        step("Click on 'Lost password restore' checkbox") {
            onResetCardScreen { lostPasswordRestoreCheckBox.performClick() }
        }
        step("Assert 'Lost password restore' checkbox is enabled") {
            onResetCardScreen { lostPasswordRestoreCheckBox.assertIsEnabled() }
        }
        step("Assert 'Reset the card' button is enabled") {
            onResetCardScreen { resetCardButton.assertIsEnabled() }
        }
        step("Click on 'Lost password restore' checkbox") {
            onResetCardScreen { lostPasswordRestoreCheckBox.performClick() }
        }
        step("Assert 'Reset the card' button is disabled") {
            onResetCardScreen { resetCardButton.assertIsNotEnabled() }
        }
        step("Click on 'Lost wallet' checkbox") {
            onResetCardScreen { lostWalletAccessCheckBox.performClick() }
        }
        step("Click on 'Lost password restore' checkbox") {
            onResetCardScreen { lostPasswordRestoreCheckBox.performClick() }
        }
        step("Assert 'Reset the card' button is disabled") {
            onResetCardScreen { resetCardButton.assertIsNotEnabled() }
        }
    } else {
        step("Click on 'Lost wallet' checkbox") {
            onResetCardScreen { lostWalletAccessCheckBox.performClick() }
        }
        step("Assert 'Lost wallet' checkbox is enabled") {
            onResetCardScreen { lostWalletAccessCheckBox.assertIsEnabled() }
        }
        step("Assert 'Reset the card' button is enabled") {
            onResetCardScreen { resetCardButton.assertIsEnabled() }
        }
    }
}
