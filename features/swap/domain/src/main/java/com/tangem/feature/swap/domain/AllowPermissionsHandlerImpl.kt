package com.tangem.feature.swap.domain

import java.math.BigDecimal
import java.util.Collections.synchronizedMap

class AllowPermissionsHandlerImpl : AllowPermissionsHandler {

    // todo maybe need to save in store
    private val allowPermissionsInProgress = synchronizedMap(mutableMapOf<String, BigDecimal?>())

    override fun addAddressToInProgress(tokenAddress: String, approvedAmount: BigDecimal?) {
        allowPermissionsInProgress[tokenAddress] = approvedAmount
    }

    override fun removeAddressFromProgress(tokenAddress: String) {
        allowPermissionsInProgress.remove(tokenAddress)
    }

    override fun isAddressAllowanceInProgress(tokenAddress: String): Boolean {
        return allowPermissionsInProgress.containsKey(tokenAddress)
    }

    override fun getApprovedAmount(tokenAddress: String): BigDecimal? {
        return allowPermissionsInProgress[tokenAddress]
    }
}