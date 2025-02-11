package com.tangem.screens

import androidx.compose.ui.test.hasTestTag
import com.atiurin.ultron.page.Page
import com.tangem.core.ui.test.TestTags

object StoriesPage : Page<StoriesPage>() {
    val scanButton = hasTestTag(TestTags.STORIES_SCREEN_SCAN_BUTTON)
    val orderButton = hasTestTag(TestTags.STORIES_SCREEN_ORDER_BUTTON)
}