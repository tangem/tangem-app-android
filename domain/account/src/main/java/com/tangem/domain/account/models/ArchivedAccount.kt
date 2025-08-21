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
 * @property derivationIndex the derivation index for the archived account
 * @property tokensCount     the number of tokens in the archived account
 * @property networksCount   the number of networks associated with the archived account
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
)