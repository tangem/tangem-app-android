package com.tangem.features.onboarding.v2.note.impl.route

import kotlinx.serialization.Serializable

@Serializable
internal sealed class OnboardingNoteRoute {

    @Serializable
    data object CreateWallet : OnboardingNoteRoute()

    @Serializable
    data object Done : OnboardingNoteRoute()
}

internal const val ONBOARDING_NOTE_STEPS_COUNT = 2