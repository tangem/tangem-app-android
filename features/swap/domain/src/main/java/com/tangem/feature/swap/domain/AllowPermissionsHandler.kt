package com.tangem.feature.swap.domain

import java.math.BigDecimal

/**
 * Tracks tokens whose approve transaction has been sent but whose on-chain allowance has not yet
 * reflected it. While a token is "in progress" the swap flow shows the approval-in-progress state
 * instead of offering another approve.
 */
interface AllowPermissionsHandler {

    /**
     * Marks [tokenAddress] as having a pending approve transaction. [approvedAmount] is the amount
     * the approval was given for — used to detect that the transaction has landed even when the
     * entered swap amount has grown past it since (`null` when the amount is unknown).
     */
    fun addAddressToInProgress(tokenAddress: String, approvedAmount: BigDecimal?)

    fun removeAddressFromProgress(tokenAddress: String)

    fun isAddressAllowanceInProgress(tokenAddress: String): Boolean

    /** The amount the pending approval was given for, or `null` if unknown or not in progress. */
    fun getApprovedAmount(tokenAddress: String): BigDecimal?
}