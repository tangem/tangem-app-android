package com.tangem.features.onboarding.v2.note.impl.child.create.ui.state

import com.tangem.core.ui.components.artwork.ArtworkUM

data class OnboardingNoteCreateWalletUM(
    val artwork: ArtworkUM? = null,
    val createWalletInProgress: Boolean = false,
    val onCreateClick: () -> Unit = {},
)