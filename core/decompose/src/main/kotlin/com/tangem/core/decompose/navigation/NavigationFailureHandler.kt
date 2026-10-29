package com.tangem.core.decompose.navigation

/** Reports a root navigation operation that failed while its caller passed no completion callback. */
fun interface NavigationFailureHandler {

    fun onNavigationFailed(errorMessage: String)

    companion object {
        val None = NavigationFailureHandler { }
    }
}