package com.tangem.data.account.producer

import com.tangem.data.account.converter.AccountListConverter
import com.tangem.data.account.store.AccountsResponseStore
import com.tangem.data.account.store.AccountsResponseStoreFactory
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.models.wallet.requireColdWallet
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Factory that creates a flow of [AccountList] for a specific [UserWallet]
 *
 * @property accountsResponseStoreFactory factory to create [AccountsResponseStore]
 * @property accountListConverterFactory  factory to create [AccountListConverter]
 * @property cardCryptoCurrencyFactory    factory to create supported crypto currencies for a card
 *
[REDACTED_AUTHOR]
 */
internal class WalletAccountListFlowFactory @Inject constructor(
    private val accountsResponseStoreFactory: AccountsResponseStoreFactory,
    private val accountListConverterFactory: AccountListConverter.Factory,
    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
) {

    fun create(userWallet: UserWallet): Flow<AccountList> {
        return if (userWallet.isMultiCurrency) {
            createForMultiWallet(userWallet)
        } else {
            flowOf(createForSingleWallet(userWallet))
        }
    }

    private fun createForMultiWallet(userWallet: UserWallet): Flow<AccountList> {
        val converter by lazy { accountListConverterFactory.create(userWallet) }

        return accountsResponseStoreFactory.create(userWallet.walletId).data
            .filterNotNull()
            .distinctUntilChanged()
            .map(converter::convert)
    }

    private fun createForSingleWallet(userWallet: UserWallet): AccountList {
        val isSingleWalletWithToken = userWallet.requireColdWallet().cardTypesResolver.isSingleWalletWithToken()

        val currencies = if (isSingleWalletWithToken) {
            cardCryptoCurrencyFactory.createCurrenciesForSingleCurrencyCardWithToken(userWallet = userWallet).toSet()
        } else {
            cardCryptoCurrencyFactory.createPrimaryCurrencyForSingleCurrencyCard(userWallet = userWallet).let(::setOf)
        }

        return AccountList.empty(userWallet = userWallet, cryptoCurrencies = currencies)
    }
}