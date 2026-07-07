package com.tangem.data.virtualaccount.converter

import com.tangem.datasource.local.visa.entity.VirtualAccountStatusValueDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.VirtualAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayCurrencyFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Two-way converter between [VirtualAccountStatusValue] and [VirtualAccountStatusValueDM].
 *
 * [convert] maps domain → data model. Returns null for transient statuses that should not be persisted
 * (Loading, ExposedDevice, Unavailable, NotSynced).
 *
 * [convertBack] maps data model → domain. All restored statuses have [StatusSource.CACHE] as source.
 */
@Singleton
internal class VirtualAccountStatusValueDMConverter @Inject constructor(
    private val tangemPayCurrencyFactory: TangemPayCurrencyFactory,
) {

    fun convert(value: VirtualAccountStatusValue): VirtualAccountStatusValueDM? {
        return when (value) {
            is VirtualAccountStatusValue.Empty -> VirtualAccountStatusValueDM.Empty()
            is VirtualAccountStatusValue.NotCreated -> VirtualAccountStatusValueDM.NotCreated()
            is VirtualAccountStatusValue.UnderReview -> VirtualAccountStatusValueDM.UnderReview(
                kycStatus = value.kycStatus,
                customerId = value.customerId,
            )
            is VirtualAccountStatusValue.Provisioning -> VirtualAccountStatusValueDM.Provisioning()
            is VirtualAccountStatusValue.CountryNotSupported -> VirtualAccountStatusValueDM.CountryNotSupported()
            is VirtualAccountStatusValue.Active -> VirtualAccountStatusValueDM.ActiveAccount(
                customerId = value.customerId,
                currencyCode = value.currencyCode,
                depositAddress = value.depositAddress,
                fiatBalance = value.fiatBalance.toDM(),
                cryptoBalance = value.cryptoBalance.toDM(),
                fiatRate = value.fiatRate,
                availableForWithdrawal = value.availableForWithdrawal,
            )
            // Transient statuses are not persisted
            is VirtualAccountStatusValue.Loading,
            is VirtualAccountStatusValue.Error.ExposedDevice,
            is VirtualAccountStatusValue.Error.Unavailable,
            is VirtualAccountStatusValue.Error.NotSynced,
            -> null
        }
    }

    fun convertBack(userWalletId: UserWalletId, value: VirtualAccountStatusValueDM?): VirtualAccountStatusValue {
        return when (value) {
            is VirtualAccountStatusValueDM.Empty -> VirtualAccountStatusValue.Empty
            is VirtualAccountStatusValueDM.NotCreated -> VirtualAccountStatusValue.NotCreated
            is VirtualAccountStatusValueDM.Provisioning -> VirtualAccountStatusValue.Provisioning(
                source = StatusSource.CACHE,
            )
            is VirtualAccountStatusValueDM.CountryNotSupported -> VirtualAccountStatusValue.CountryNotSupported
            is VirtualAccountStatusValueDM.UnderReview -> VirtualAccountStatusValue.UnderReview(
                source = StatusSource.CACHE,
                kycStatus = value.kycStatus,
                customerId = value.customerId,
            )
            is VirtualAccountStatusValueDM.ActiveAccount -> VirtualAccountStatusValue.Active(
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
            null -> VirtualAccountStatusValue.Error.Unavailable
        }
    }

    private fun VirtualAccountStatusValue.FiatBalance.toDM(): VirtualAccountStatusValueDM.FiatBalanceDM {
        return VirtualAccountStatusValueDM.FiatBalanceDM(
            availableBalance = availableBalance,
            currency = currency,
        )
    }

    private fun VirtualAccountStatusValue.CryptoBalance.toDM(): VirtualAccountStatusValueDM.CryptoBalanceDM {
        return VirtualAccountStatusValueDM.CryptoBalanceDM(
            id = id,
            chainId = chainId,
            depositAddress = depositAddress,
            tokenContractAddress = tokenContractAddress,
            balance = balance,
        )
    }

    private fun VirtualAccountStatusValueDM.FiatBalanceDM.toDomain(): VirtualAccountStatusValue.FiatBalance {
        return VirtualAccountStatusValue.FiatBalance(
            availableBalance = availableBalance,
            currency = currency,
        )
    }

    private fun VirtualAccountStatusValueDM.CryptoBalanceDM.toDomain(): VirtualAccountStatusValue.CryptoBalance {
        return VirtualAccountStatusValue.CryptoBalance(
            id = id,
            chainId = chainId,
            depositAddress = depositAddress,
            tokenContractAddress = tokenContractAddress,
            balance = balance,
        )
    }
}