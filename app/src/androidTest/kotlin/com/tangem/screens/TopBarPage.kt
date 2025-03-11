package com.tangem.screens

import androidx.compose.ui.test.hasTestTag
import com.atiurin.ultron.page.Page
import com.tangem.core.ui.test.TestTags

object TopBarPage : Page<TopBarPage>() {
    val moreButton = hasTestTag(TestTags.MAIN_SCREEN_MORE_BUTTON)
}