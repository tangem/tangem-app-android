package com.tangem.feature.swap.domain

interface AllowPermissionsHandler {

    fun addAddressToInProgress(tokenAddress: String)
    fun removeAddressFromProgress(tokenAddress: String)
    fun isAddressAllowanceInProgress(tokenAddress: String): Boolean
}
