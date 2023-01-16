package com.tangem.tap.common.toggleWidget

import android.view.View

/**
 * Created by Anton Zhilenkov on 07/07/2020.
 */
interface WidgetState

interface ViewStateWidget {
    val mainView: View
    fun changeState(state: WidgetState)
}
