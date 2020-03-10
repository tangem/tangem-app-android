package com.tangem.tangemtest.commons

import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import com.tangem.tangemtest.R
import com.tangem.tangemtest.card_use_cases.actions.Action

/**
[REDACTED_AUTHOR]
 */

fun getDefaultNavigationOptions(): NavOptions {
    return navOptions {
        anim {
            enter = R.anim.slide_in_right
            exit = R.anim.slide_out_left
            popEnter = R.anim.slide_in_left
            popExit = R.anim.slide_out_right
        }
    }
}

data class NavigateAction(val action: Action, val destinationId: Int)