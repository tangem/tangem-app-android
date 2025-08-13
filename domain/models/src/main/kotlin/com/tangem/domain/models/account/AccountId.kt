package com.tangem.domain.models.account

import com.tangem.common.extensions.toByteArray
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.extensions.toHexString
import kotlinx.serialization.Serializable
import java.security.MessageDigest

/**
 * Represents a unique identifier for an account
 *
 * @property value        a unique string value that distinguishes this account
 * @property userWalletId the identifier of the user wallet associated with the account
 */
@Serializable
data class AccountId private constructor(
    val value: String,
    val userWalletId: UserWalletId,
) {

    companion object {

        private val sha256Digest: MessageDigest by lazy { MessageDigest.getInstance("SHA-256") }

        /**
         * Creates a unique account identifier for a crypto portfolio
         *
         * @param userWalletId    the identifier of the user wallet
         * @param derivationIndex the derivation index used to generate the identifier
         */
        fun forCryptoPortfolio(userWalletId: UserWalletId, derivationIndex: DerivationIndex): AccountId {
            val input = userWalletId.value + derivationIndex.value.toByteArray()
            val value = sha256Digest.digest(input).toHexString()

            return AccountId(value = value, userWalletId = userWalletId)
        }
    }
}