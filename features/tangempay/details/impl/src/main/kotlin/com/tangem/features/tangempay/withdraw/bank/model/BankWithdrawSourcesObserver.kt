package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.bankofframp.BankOfframpOptions
import com.tangem.features.tangempay.common.balanceOrNull
import com.tangem.features.tangempay.common.networksOrNull
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
import com.tangem.utils.extensions.isPositive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class BankWithdrawSourcesObserver @Inject constructor(
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
    private val moneyAccountStatusSupplier: MoneyAccountStatusSupplier,
) {

    fun observe(account: BankWithdrawSourceAccount, offramp: BankOfframpOptions): Flow<BankWithdrawOptions?> =
        when (account) {
            is BankWithdrawSourceAccount.Payment -> observePaymentAccount(account.userWalletId, offramp)
            is BankWithdrawSourceAccount.Money -> observeMoneyAccount(account.userWalletId, offramp)
        }

    private fun observePaymentAccount(
        userWalletId: UserWalletId,
        offramp: BankOfframpOptions,
    ): Flow<BankWithdrawOptions?> = paymentAccountStatusSupplier(userWalletId)
        .filterNot { status -> status.value is PaymentAccountStatusValue.Loading }
        .map { status ->
            val networks = status.networksOrNull() ?: return@map null
            BankWithdrawOptionsFactory.create(
                destinations = offramp.destinations,
                sources = networks.filterIsInstance<PaymentNetworkStatus.Available>().flatMap { network ->
                    network.cryptoCurrencyStatuses.mapNotNull { currencyStatus ->
                        currencyStatus.toWithdrawSource(network.network, network.chainId, offramp)
                    }
                },
                balanceCurrencyCode = status.balanceOrNull()?.fiatBalance?.currency,
            )
        }

    private fun observeMoneyAccount(
        userWalletId: UserWalletId,
        offramp: BankOfframpOptions,
    ): Flow<BankWithdrawOptions?> = moneyAccountStatusSupplier(userWalletId)
        .filterNot { status -> status.value is MoneyAccountStatusValue.Loading }
        .map { status ->
            val active = status.value as? MoneyAccountStatusValue.Account.Active ?: return@map null
            BankWithdrawOptionsFactory.create(
                destinations = offramp.destinations,
                sources = listOfNotNull(
                    active.cryptoCurrencyStatus?.toWithdrawSource(
                        network = active.cryptoCurrency.network,
                        chainId = TangemPayCurrencyFactory.TOKEN_CHAIN_ID,
                        offramp = offramp,
                    ),
                ),
                balanceCurrencyCode = active.balance?.currencyCode,
            )
        }

    private fun CryptoCurrencyStatus.toWithdrawSource(
        network: Network,
        chainId: Long,
        offramp: BankOfframpOptions,
    ): BankWithdrawSource? {
        val token = currency as? CryptoCurrency.Token ?: return null
        val fiatBalance = value.fiatAmount?.takeIf { it.isPositive() } ?: return null
        return BankWithdrawSource(
            network = network,
            chainId = chainId,
            token = token,
            fiatBalance = fiatBalance,
            isAvailable = offramp.sources.any { source ->
                source.chainId == chainId &&
                    source.tokens.any { it.contractAddress.equals(token.contractAddress, ignoreCase = true) }
            },
        )
    }
}