package com.tangem.tap.features.signin.redux

import com.tangem.core.analytics.models.Basic
import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
sealed interface SignInAction : Action {

    data class SetSignInType(val type: Basic.SignedIn.SignInType) : SignInAction
}