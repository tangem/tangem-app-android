package com.tangem.feature.swap.domain

class AllowPermissionsHandlerImpl : AllowPermissionsHandler {
// [REDACTED_TODO_COMMENT]
    private val allowPermissionsInProgress = mutableSetOf<String>()

    override fun addAddressToInProgress(tokenAddress: String) {
        allowPermissionsInProgress.add(tokenAddress)
    }

    override fun removeAddressFromProgress(tokenAddress: String) {
        allowPermissionsInProgress.remove(tokenAddress)
    }

    override fun isAddressAllowanceInProgress(tokenAddress: String): Boolean {
        return allowPermissionsInProgress.contains(tokenAddress)
    }
}
