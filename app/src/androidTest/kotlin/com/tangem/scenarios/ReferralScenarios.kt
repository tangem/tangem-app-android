package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.screens.onDetailsScreen
import com.tangem.screens.onMainScreenTopBar
import com.tangem.screens.onReferralProgramScreen
import com.tangem.screens.onWalletSettingsScreen
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.referralTakeParticipate() {
    step("Open 'Details' screen") {
        onMainScreenTopBar { moreButton.clickWithAssertion() }
    }
    step("Open 'Wallet settings' screen") {
        onDetailsScreen { walletNameButton.clickWithAssertion() }
    }
    step("Open 'Referral program' screen") {
        onWalletSettingsScreen { referralProgramButton.clickWithAssertion() }
    }
    step("Tap 'Participate'") {
        onReferralProgramScreen { participateButton.clickWithAssertion() }
    }
    step("Verify referral code is displayed") {
        onReferralProgramScreen { personalCodeCard.assertIsDisplayed() }
    }
}