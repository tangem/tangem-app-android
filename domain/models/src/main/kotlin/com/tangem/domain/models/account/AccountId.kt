package com.tangem.domain.models.account

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
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

    sealed interface Error {

        val tag: String
            get() = this::class.simpleName ?: "AccountId.Error"

        data object Empty : Error {
            override fun toString(): String = "$tag: Account ID cannot be blank"
        }

        data object InvalidFormat : Error {
            override fun toString(): String = "$tag: Account ID must be a 64-character hexadecimal string"
        }
    }

    companion object {

        private val sha256Digest: MessageDigest by lazy { MessageDigest.getInstance("SHA-256") }
        private val hexRegex = Regex("^[a-fA-F0-9]{64}$")

        /**
         * Creates a unique account identifier for a crypto portfolio
         *
         * @param userWalletId the identifier of the user wallet
         * @param value        the unique string value representing the account
         *
         * @return an [Either] containing the [AccountId] on success, or an [Error] on failure
         */
        fun forCryptoPortfolio(userWalletId: UserWalletId, value: String): Either<Error, AccountId> = either {
            ensure(value.isNotBlank()) { Error.Empty }
            ensure(value.matches(hexRegex)) { Error.InvalidFormat }

            AccountId(value = value, userWalletId = userWalletId)
        }

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