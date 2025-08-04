package com.tangem.domain.models.account

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

/**
 * Represents a unique identifier for an account
 *
 * @property value        a unique string value that distinguishes this account
 * @property userWalletId the identifier of the user wallet associated with the account
 */
@Serializable
data class AccountId(
    val value: String,
    val userWalletId: UserWalletId,
)