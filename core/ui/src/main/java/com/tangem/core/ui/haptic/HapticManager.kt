package com.tangem.core.ui.haptic

import androidx.compose.runtime.Stable

@Stable
interface HapticManager {

    fun vibrateShort()

    fun vibrateLong()
}
