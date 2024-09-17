package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.core.ui.test.TestTags
import io.github.kakaocup.compose.node.element.ComposeScreen

class MainTestScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MainTestScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(TestTags.MAIN_SCREEN) }
    )