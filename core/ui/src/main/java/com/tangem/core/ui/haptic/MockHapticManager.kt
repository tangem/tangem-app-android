package com.tangem.core.ui.haptic

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Suppress("FunctionName")
fun MockHapticManager(mockHapticFeedback: HapticFeedback? = null): HapticManager =
    if (mockHapticFeedback == null) MockHapticManager else MockHapticManagerImpl(mockHapticFeedback)

val MockHapticManager: HapticManager = MockHapticManagerImpl()

private class MockHapticManagerImpl(
    private val mockHapticFeedback: HapticFeedback? = null,
) : HapticManager {

    override fun vibrateShort() {
        mockHapticFeedback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        /** Intentionally do nothing */
    }

    override fun vibrateMeduim() {
        mockHapticFeedback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        /** Intentionally do nothing */
    }

    override fun vibrateLong() {
        mockHapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
        /** Intentionally do nothing */
    }
}