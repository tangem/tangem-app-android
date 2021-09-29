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