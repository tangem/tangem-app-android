package com.tangem.common.extensions

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performClick
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.AppSettingsScreenTestTags
import com.tangem.core.ui.test.TokenDetailsTopBarTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import com.tangem.core.ui.test.TopNavigationTestTags

fun BaseTestCase.tapBackButton() {
    waitForIdle()
    composeTestRule.onNode(
        hasTestTag(TopNavigationTestTags.BACK_BUTTON)
            or hasTestTag(TopAppBarTestTags.CLOSE_BUTTON)
            or hasTestTag(TokenDetailsTopBarTestTags.BACK_BUTTON)
            or hasTestTag(AppSettingsScreenTestTags.BACK_BUTTON),
        useUnmergedTree = true,
    ).performClick()
}