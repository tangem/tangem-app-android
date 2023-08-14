package com.tangem.feature.onboarding.presentation.wallet2.viewmodel

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
* [REDACTED_AUTHOR]
 */
class SeedPhraseRouter(
    private val onBack: () -> Unit,
    private val onOpenChat: () -> Unit,
    private val onOpenUriClick: (Uri) -> Unit,
) {

    private val _currentScreen = MutableStateFlow(SeedPhraseScreen.Intro)

    val currentScreen: StateFlow<SeedPhraseScreen>
        get() = _currentScreen

    fun navigateBack() {
        _currentScreen.value = when (_currentScreen.value) {
            SeedPhraseScreen.CheckSeedPhrase -> SeedPhraseScreen.YourSeedPhrase
            SeedPhraseScreen.Intro,
            SeedPhraseScreen.AboutSeedPhrase,
            SeedPhraseScreen.YourSeedPhrase,
            SeedPhraseScreen.ImportSeedPhrase,
            -> {
                onBack.invoke()
                return
            }
        }
    }

    fun openScreen(screen: SeedPhraseScreen) {
        _currentScreen.value = screen
    }

    fun openChat() {
        onOpenChat.invoke()
    }

    fun openUri(uri: Uri) {
        onOpenUriClick.invoke(uri)
    }
}

enum class SeedPhraseScreen {
    Intro, AboutSeedPhrase, YourSeedPhrase, CheckSeedPhrase, ImportSeedPhrase
}
