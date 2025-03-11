package com.tangem.features.onboarding.v2

import com.tangem.core.ui.extensions.TextReference
import kotlinx.coroutines.flow.StateFlow

interface TitleProvider {
    val currentTitle: StateFlow<TextReference>
    fun changeTitle(text: TextReference)
}