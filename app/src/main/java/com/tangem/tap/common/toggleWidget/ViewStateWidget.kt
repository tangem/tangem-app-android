package com.tangem.tap.common.toggleWidget

import android.view.View


/**
[REDACTED_AUTHOR]
 */
interface WidgetState

interface ViewStateWidget {
    val mainView: View
    fun changeState(state: WidgetState)
}

sealed class ProgressState : WidgetState {
    object None : ProgressState()
    data class Progress(val progress: Int = 0) : ProgressState()
}