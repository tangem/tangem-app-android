package com.tangem.data.moneyaccount.converter

import com.tangem.spend.datasource.visa.entity.MoneyAccountStatusValueDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayCurrencyFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Two-way converter between [MoneyAccountStatusValue] and [MoneyAccountStatusValueDM].
 *
 * [convert] maps domain → data model. Returns null for transient statuses that should not be persisted
 * (Loading, ExposedDevice, Unavailable, NotSynced).
 *
 * [convertBack] maps data model → domain. All restored statuses have [StatusSource.CACHE] as source.
 */
@Singleton
internal class MoneyAccountStatusValueDMConverter @Inject constructor(
    private val tangemPayCurrencyFactory: TangemPayCurrencyFactory,
) {

    fun convert(value: MoneyAccountStatusValue): MoneyAccountStatusValueDM? {
        return when (value) {
            is MoneyAccountStatusValue.Empty -> MoneyAccountStatusValueDM.Empty()
            is MoneyAccountStatusValue.NotCreated -> MoneyAccountStatusValueDM.NotCreated()
            is MoneyAccountStatusValue.UnderReview -> MoneyAccountStatusValueDM.UnderReview(
                kycStatus = value.kycStatus,
                customerId = value.customerId,
            )
            is MoneyAccountStatusValue.Provisioning -> MoneyAccountStatusValueDM.Provisioning()
            is MoneyAccountStatusValue.CountryNotSupported -> MoneyAccountStatusValueDM.CountryNotSupported()
            is MoneyAccountStatusValue.Active -> MoneyAccountStatusValueDM.ActiveAccount(
                customerId = value.customerId,
                currencyCode = value.currencyCode,
                depositAddress = value.depositAddress,
                fiatBalance = value.fiatBalance.toDM(),
                cryptoBalance = value.cryptoBalance.toDM(),
                fiatRate = value.fiatRate,
                availableForWithdrawal = value.availableForWithdrawal,
            )
            // Transient statuses are not persisted
            is MoneyAccountStatusValue.Loading,
            is MoneyAccountStatusValue.Error.ExposedDevice,
            is MoneyAccountStatusValue.Error.Unavailable,
            is MoneyAccountStatusValue.Error.NotSynced,
            -> null
        }
    }

    fun convertBack(userWalletId: UserWalletId, value: MoneyAccountStatusValueDM?): MoneyAccountStatusValue {
        return when (value) {
            is MoneyAccountStatusValueDM.Empty -> MoneyAccountStatusValue.Empty
            is MoneyAccountStatusValueDM.NotCreated -> MoneyAccountStatusValue.NotCreated
            is MoneyAccountStatusValueDM.Provisioning -> MoneyAccountStatusValue.Provisioning(
                source = StatusSource.CACHE,
            )
            is MoneyAccountStatusValueDM.CountryNotSupported -> MoneyAccountStatusValue.CountryNotSupported
            is MoneyAccountStatusValueDM.UnderReview -> MoneyAccountStatusValue.UnderReview(
                source = StatusSource.CACHE,
                kycStatus = value.kycStatus,
                customerId = value.customerId,
            )
            is MoneyAccountStatusValueDM.ActiveAccount -> MoneyAccountStatusValue.Active(
                source = StatusSource.CACHE,
                customerId = value.customerId,
                currencyCode = value.currencyCode,
                depositAddress = value.depositAddress,
                fiatBalance = value.fiatBalance.toDomain(),
                cryptoBalance = value.cryptoBalance.toDomain(),
                availableForWithdrawal = value.availableForWithdrawal,
                cryptoCurrency = tangemPayCurrencyFactory.create(userWalletId),
                fiatRate = value.fiatRate,
            )
            null -> MoneyAccountStatusValue.Error.Unavailable
        }
    }

    private fun MoneyAccountStatusValue.FiatBalance.toDM(): MoneyAccountStatusValueDM.FiatBalanceDM {
        return MoneyAccountStatusValueDM.FiatBalanceDM(
            availableBalance = availableBalance,
            currency = currency,
        )
    }

    private fun MoneyAccountStatusValue.CryptoBalance.toDM(): MoneyAccountStatusValueDM.CryptoBalanceDM {
        return MoneyAccountStatusValueDM.CryptoBalanceDM(
            id = id,
            chainId = chainId,
            depositAddress = depositAddress,
            tokenContractAddress = tokenContractAddress,
            balance = balance,
        )
    }

    private fun MoneyAccountStatusValueDM.FiatBalanceDM.toDomain(): MoneyAccountStatusValue.FiatBalance {
        return MoneyAccountStatusValue.FiatBalance(
            availableBalance = availableBalance,
            currency = currency,
        )
    }

    private fun MoneyAccountStatusValueDM.CryptoBalanceDM.toDomain(): MoneyAccountStatusValue.CryptoBalance {
        return MoneyAccountStatusValue.CryptoBalance(
            id = id,
            chainId = chainId,
            depositAddress = depositAddress,
            tokenContractAddress = tokenContractAddress,
            balance = balance,
        )
    }
}