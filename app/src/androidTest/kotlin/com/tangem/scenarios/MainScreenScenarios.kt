package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.SwipeDirection
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.swipeVertical
import com.tangem.screens.onAddAndManageBottomSheet
import com.tangem.screens.onMainScreen
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.openOrganizeTokensScreen() {
    step("Swipe to 'Add & Manage' button") {
        swipeVertical(SwipeDirection.UP)
        swipeVertical(SwipeDirection.UP)
    }
    step("Click on 'Add & Manage' button") {
        onMainScreen { addAndManageButton().clickWithAssertion() }
    }
    step("Click on 'Organize tokens' button in bottom sheet") {
        onAddAndManageBottomSheet { organizeTokensButton.clickWithAssertion() }
    }
}