package com.tangem.tap

import android.app.Application
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.appReducer
import org.rekotlin.Store

val store = Store(
        reducer = ::appReducer,
        middleware = AppState.getMiddleware(),
        state = AppState()
)

class TapApplication : Application()