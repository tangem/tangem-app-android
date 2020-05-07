package com.tangem.devkit.ucase

import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import com.tangem.devkit.R

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