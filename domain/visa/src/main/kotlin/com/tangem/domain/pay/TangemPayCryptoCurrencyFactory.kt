package com.tangem.domain.pay

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet

interface TangemPayCryptoCurrencyFactory {

    fun create(userWallet: UserWallet, chainId: Int): Either<UniversalError, CryptoCurrency>
}