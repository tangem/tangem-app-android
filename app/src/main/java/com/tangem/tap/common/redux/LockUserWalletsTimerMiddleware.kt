package com.tangem.tap.common.redux

import com.tangem.tap.lockUserWalletsTimer
import org.rekotlin.Middleware

class LockUserWalletsTimerMiddleware {
    val middleware: Middleware<AppState> = { _, _ ->
        { nextDispatch ->
            { action ->
                lockUserWalletsTimer?.restart()
                nextDispatch(action)
            }
        }
    }
}
