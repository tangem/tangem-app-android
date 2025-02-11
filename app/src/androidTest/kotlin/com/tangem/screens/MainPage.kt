package com.tangem.screens

import androidx.compose.ui.test.hasTestTag
import com.atiurin.ultron.page.Page
import com.tangem.core.ui.test.TestTags

object MainPage : Page<MainPage>() {
    val container = hasTestTag(TestTags.MAIN_SCREEN)
}