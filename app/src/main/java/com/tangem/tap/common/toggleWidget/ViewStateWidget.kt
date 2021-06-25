package com.tangem.tap.common.toggleWidget


/**
[REDACTED_AUTHOR]
 */
interface WidgetState

interface ViewStateWidget {
    fun changeState(state: WidgetState)
}

sealed class ProgressState : WidgetState {
    object None : ProgressState()
    data class Progress(val progress: Int = 0) : ProgressState()
}