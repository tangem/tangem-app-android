package com.tangem.data.account.converter

import arrow.core.getOrElse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWalletId

internal fun String.toAccountId(userWalletId: UserWalletId): AccountId {
    return AccountId.forCryptoPortfolio(value = this, userWalletId = userWalletId).getOrElse {
        error("Unable to create AccountId from value: $this. Cause: $it")
    }
}

internal fun String.toAccountName(): AccountName {
    return AccountName(value = this).getOrElse {
        error("Unable to create AccountName from value: $this. Cause: $it")
    }
}

internal fun WalletAccountDTO.toIcon(): CryptoPortfolioIcon {
    return CryptoPortfolioIconConverter.convert(
        value = CryptoPortfolioIconConverter.DataModel(icon = icon, color = iconColor),
    )
}

internal fun Int.toDerivationIndex(): DerivationIndex {
    return DerivationIndex(value = this).getOrElse {
        error("Unable to create DerivationIndex from value: $this. Cause: $it")
    }
}