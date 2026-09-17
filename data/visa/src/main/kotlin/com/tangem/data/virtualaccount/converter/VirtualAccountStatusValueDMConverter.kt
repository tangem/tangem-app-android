package com.tangem.data.virtualaccount.converter

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.VirtualAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.spend.datasource.visa.entity.VirtualAccountStatusValueDM
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class VirtualAccountStatusValueDMConverter @Inject constructor(
    private val tangemPayCurrencyFactory: TangemPayCurrencyFactory,
) {

    fun convert(value: VirtualAccountStatusValue): VirtualAccountStatusValueDM? {
        return when (value) {
            is VirtualAccountStatusValue.Empty -> VirtualAccountStatusValueDM.Empty()
            is VirtualAccountStatusValue.NotCreated -> VirtualAccountStatusValueDM.NotCreated()
            is VirtualAccountStatusValue.Issuance.CountryNotSupported -> {
                VirtualAccountStatusValueDM.CountryNotSupported()
            }
            is VirtualAccountStatusValue.Issuance.Issuing -> VirtualAccountStatusValueDM.Issuing(
                step = value.step,
                customerId = value.customerId,
                balance = value.balance?.toDM(),
            )
            is VirtualAccountStatusValue.Issuance.UnderReview -> VirtualAccountStatusValueDM.UnderReview(
                kycStatus = value.kycStatus,
                customerId = value.customerId,
                balance = value.balance?.toDM(),
            )
            is VirtualAccountStatusValue.Issuance.VerificationDeclined -> {
                VirtualAccountStatusValueDM.VerificationDeclined(
                    customerId = value.customerId,
                    balance = value.balance?.toDM(),
                )
            }
            is VirtualAccountStatusValue.Issuance.Activating -> VirtualAccountStatusValueDM.Activating(
                customerId = value.customerId,
                balance = value.balance?.toDM(),
            )
            is VirtualAccountStatusValue.Issued.Active -> VirtualAccountStatusValueDM.ActiveAccount(
                customerId = value.customerId,
                productInstanceId = value.productInstanceId,
                balance = value.balance?.toDM(),
            )
            is VirtualAccountStatusValue.Issued.Deactivated -> VirtualAccountStatusValueDM.Deactivated(
                customerId = value.customerId,
                productInstanceId = value.productInstanceId,
                balance = value.balance?.toDM(),
            )
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
            is VirtualAccountStatusValueDM.CountryNotSupported -> VirtualAccountStatusValue.Issuance.CountryNotSupported
            is VirtualAccountStatusValueDM.Issuing -> VirtualAccountStatusValue.Issuance.Issuing(
                source = StatusSource.CACHE,
                balance = value.balance?.toDomain(userWalletId),
                customerId = value.customerId,
                step = value.step,
            )
            is VirtualAccountStatusValueDM.UnderReview -> VirtualAccountStatusValue.Issuance.UnderReview(
                source = StatusSource.CACHE,
                balance = value.balance?.toDomain(userWalletId),
                kycStatus = value.kycStatus,
                customerId = value.customerId,
            )
            is VirtualAccountStatusValueDM.VerificationDeclined -> {
                VirtualAccountStatusValue.Issuance.VerificationDeclined(
                    source = StatusSource.CACHE,
                    balance = value.balance?.toDomain(userWalletId),
                    customerId = value.customerId,
                )
            }
            is VirtualAccountStatusValueDM.Activating -> VirtualAccountStatusValue.Issuance.Activating(
                source = StatusSource.CACHE,
                balance = value.balance?.toDomain(userWalletId),
                customerId = value.customerId,
            )
            is VirtualAccountStatusValueDM.ActiveAccount -> VirtualAccountStatusValue.Issued.Active(
                source = StatusSource.CACHE,
                balance = value.balance?.toDomain(userWalletId),
                customerId = value.customerId,
                productInstanceId = value.productInstanceId,
            )
            is VirtualAccountStatusValueDM.Deactivated -> VirtualAccountStatusValue.Issued.Deactivated(
                source = StatusSource.CACHE,
                balance = value.balance?.toDomain(userWalletId),
                customerId = value.customerId,
                productInstanceId = value.productInstanceId,
            )
            null -> VirtualAccountStatusValue.Error.Unavailable
        }
    }

    private fun VirtualAccountStatusValue.Balance.toDM(): VirtualAccountStatusValueDM.BalanceDM {
        return VirtualAccountStatusValueDM.BalanceDM(
            cryptoAmount = cryptoAmount,
            depositAddress = depositAddress,
            currencyCode = currencyCode,
            fiatRate = fiatRate,
        )
    }

    private fun VirtualAccountStatusValueDM.BalanceDM.toDomain(
        userWalletId: UserWalletId,
    ): VirtualAccountStatusValue.Balance {
        return VirtualAccountStatusValue.Balance(
            cryptoAmount = cryptoAmount,
            cryptoCurrency = tangemPayCurrencyFactory.createVirtualAccountToken(userWalletId),
            depositAddress = depositAddress,
            currencyCode = currencyCode,
            fiatRate = fiatRate,
        )
    }
}