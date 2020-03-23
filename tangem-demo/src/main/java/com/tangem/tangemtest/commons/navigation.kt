package com.tangem.tangemtest.commons

import androidx.annotation.StringRes
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import com.tangem.tangemtest.R

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

data class NavigateOptions(val name: ActionType, val destinationId: Int)

enum class ActionType(@StringRes val resName: Int) {
    Scan(R.string.action_card_scan),
    Sign(R.string.action_card_sign),
    CreateWallet(R.string.action_wallet_create),
    PurgeWallet(R.string.action_wallet_purge),
    ReadIssuerData(R.string.action_issuer_read_data),
    WriteIssuerData(R.string.action_issuer_write_data),
    ReadIssuerExData(R.string.action_issuer_read_ex_data),
    WriteIssuerExData(R.string.action_issuer_write_ex_data),
    ReadUserData(R.string.action_user_read_data),
    WriteUserData(R.string.action_user_write_data),
    Personalize(R.string.personalize),
    Unknown(R.string.unknown),
}