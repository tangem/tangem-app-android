package com.tangem.feature.swap.domain

import java.util.Collections.synchronizedSet

class AllowPermissionsHandlerImpl : AllowPermissionsHandler {

    // todo maybe need to save in store
    private val allowPermissionsInProgress = synchronizedSet(mutableSetOf<String>())

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