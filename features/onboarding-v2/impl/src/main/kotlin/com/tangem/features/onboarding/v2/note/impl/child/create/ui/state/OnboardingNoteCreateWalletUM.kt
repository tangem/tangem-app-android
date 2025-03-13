package com.tangem.features.onboarding.v2.note.impl.child.create.ui.state

data class OnboardingNoteCreateWalletUM(
    val artworkUrl: String? = null,
    val createWalletInProgress: Boolean = false,
    val onCreateClick: () -> Unit = {},
)