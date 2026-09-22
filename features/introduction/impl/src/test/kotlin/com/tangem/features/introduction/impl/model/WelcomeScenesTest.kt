package com.tangem.features.introduction.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.features.introduction.impl.ui.WelcomeTextTimeline
import org.junit.jupiter.api.Test

internal class WelcomeScenesTest {

    @Test
    fun `GIVEN the welcome scenes WHEN counted THEN the timeline animates every one of them`() {
        assertThat(welcomeScenes).hasSize(WelcomeTextTimeline.sceneCount)
    }
}