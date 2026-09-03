package com.tangem.domain.account.models

import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.DerivationIndex
import kotlinx.serialization.Serializable

/**
 * Represents an archived crypto portfolio account
 *
 * @property accountId       the unique identifier of the archived account
 * @property name            the name of the archived account
 * @property icon            the icon representing the archived account
 * @property derivationIndex the wallet's own derivation index for a [Type.CRYPTO] account, or the joint account's
 * owner key index for a [Type.JOINT] one — the backend reuses the same field for both, and the two index spaces
 * are independent (see `OwnerKeyIndex`). Recovering a joint account does not need it to be exact: a subsequent
 * refresh of the joint account list replaces it with the real owner key index
 * @property tokensCount     the number of tokens in the archived account
 * @property networksCount   the number of networks associated with the archived account
 * @property type            whether this is a personal crypto portfolio or a joint account
 *
[REDACTED_AUTHOR]
 */
@Serializable
data class ArchivedAccount(
    val accountId: AccountId,
    val name: AccountName,
    val icon: CryptoPortfolioIcon,
    val derivationIndex: DerivationIndex,
    val tokensCount: Int,
    val networksCount: Int,
    val type: Type,
) {

    /** The kind of account this archived row is for */
    @Serializable
    enum class Type {
        CRYPTO,
        JOINT,
    }
}