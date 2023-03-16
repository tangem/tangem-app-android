package com.tangem.tap.features.signin.redux

import com.tangem.tap.common.analytics.events.Basic

/**
 * @author Andrew Khokhlov on 21/03/2023
 */
data class SignInState(val type: Basic.SignedIn.SignInType? = null)
