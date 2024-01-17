package com.tangem.tap.features.signin.redux

import com.tangem.core.analytics.models.Basic
import org.rekotlin.Action

/**
 * @author Andrew Khokhlov on 21/03/2023
 */
sealed interface SignInAction : Action {

    data class SetSignInType(val type: Basic.SignedIn.SignInType) : SignInAction
}
