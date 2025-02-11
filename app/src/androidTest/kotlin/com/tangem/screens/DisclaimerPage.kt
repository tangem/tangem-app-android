package com.tangem.screens

import androidx.compose.ui.test.hasTestTag
import com.atiurin.ultron.page.Page
import com.tangem.core.ui.test.TestTags

object DisclaimerPage : Page<DisclaimerPage>() {
    val acceptButton = hasTestTag(TestTags.DISCLAIMER_SCREEN_ACCEPT_BUTTON)
}