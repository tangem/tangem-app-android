package com.tangem.data.moneyaccount.converter

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.spend.datasource.visa.entity.MoneyAccountStatusValueDM
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MoneyAccountStatusValueDMConverter @Inject constructor(
    private val tangemPayCurrencyFactory: TangemPayCurrencyFactory,
) {

    fun convert(value: MoneyAccountStatusValue): MoneyAccountStatusValueDM? {
        return when (value) {
            is MoneyAccountStatusValue.Empty -> MoneyAccountStatusValueDM.Empty()
            is MoneyAccountStatusValue.NotCreated -> MoneyAccountStatusValueDM.NotCreated()
            is MoneyAccountStatusValue.Onboarding.CountryNotSupported -> {
                MoneyAccountStatusValueDM.CountryNotSupported()
            }
            is MoneyAccountStatusValue.Onboarding.Issuing -> MoneyAccountStatusValueDM.Issuing(
                step = value.step,
                customerId = value.customerId,
                balance = value.balance?.toDM(),
            )
            is MoneyAccountStatusValue.Onboarding.UnderReview -> MoneyAccountStatusValueDM.UnderReview(
                kycStatus = value.kycStatus,
                customerId = value.customerId,
                balance = value.balance?.toDM(),
            )
            is MoneyAccountStatusValue.Onboarding.VerificationDeclined -> {
                MoneyAccountStatusValueDM.VerificationDeclined(
                    customerId = value.customerId,
                    balance = value.balance?.toDM(),
                )
            }
            is MoneyAccountStatusValue.Onboarding.Activating -> MoneyAccountStatusValueDM.Activating(
                customerId = value.customerId,
                balance = value.balance?.toDM(),
            )
            is MoneyAccountStatusValue.Account.Active -> MoneyAccountStatusValueDM.ActiveAccount(
                customerId = value.customerId,
                productInstanceId = value.productInstanceId,
                balance = value.balance?.toDM(),
            )
            is MoneyAccountStatusValue.Account.Deactivated -> MoneyAccountStatusValueDM.Deactivated(
                customerId = value.customerId,
                productInstanceId = value.productInstanceId,
                balance = value.balance?.toDM(),
            )
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
            is MoneyAccountStatusValueDM.CountryNotSupported -> MoneyAccountStatusValue.Onboarding.CountryNotSupported
            is MoneyAccountStatusValueDM.Issuing -> MoneyAccountStatusValue.Onboarding.Issuing(
                source = StatusSource.CACHE,
                balance = value.balance?.toDomain(userWalletId),
                customerId = value.customerId,
                step = value.step,
            )
            is MoneyAccountStatusValueDM.UnderReview -> MoneyAccountStatusValue.Onboarding.UnderReview(
                source = StatusSource.CACHE,
                balance = value.balance?.toDomain(userWalletId),
                kycStatus = value.kycStatus,
                customerId = value.customerId,
            )
            is MoneyAccountStatusValueDM.VerificationDeclined -> {
                MoneyAccountStatusValue.Onboarding.VerificationDeclined(
                    source = StatusSource.CACHE,
                    balance = value.balance?.toDomain(userWalletId),
                    customerId = value.customerId,
                )
            }
            is MoneyAccountStatusValueDM.Activating -> MoneyAccountStatusValue.Onboarding.Activating(
                source = StatusSource.CACHE,
                balance = value.balance?.toDomain(userWalletId),
                customerId = value.customerId,
            )
            is MoneyAccountStatusValueDM.ActiveAccount -> MoneyAccountStatusValue.Account.Active(
                source = StatusSource.CACHE,
                balance = value.balance?.toDomain(userWalletId),
                customerId = value.customerId,
                productInstanceId = value.productInstanceId,
            )
            is MoneyAccountStatusValueDM.Deactivated -> MoneyAccountStatusValue.Account.Deactivated(
                source = StatusSource.CACHE,
                balance = value.balance?.toDomain(userWalletId),
                customerId = value.customerId,
                productInstanceId = value.productInstanceId,
            )
            null -> MoneyAccountStatusValue.Error.Unavailable
        }
    }

    private fun MoneyAccountStatusValue.Balance.toDM(): MoneyAccountStatusValueDM.BalanceDM {
        return MoneyAccountStatusValueDM.BalanceDM(
            cryptoAmount = cryptoAmount,
            depositAddress = depositAddress,
            currencyCode = currencyCode,
            fiatRate = fiatRate,
        )
    }

    private fun MoneyAccountStatusValueDM.BalanceDM.toDomain(
        userWalletId: UserWalletId,
    ): MoneyAccountStatusValue.Balance {
        return MoneyAccountStatusValue.Balance(
            cryptoAmount = cryptoAmount,
            cryptoCurrency = tangemPayCurrencyFactory.createMoneyAccountToken(userWalletId),
            depositAddress = depositAddress,
            currencyCode = currencyCode,
            fiatRate = fiatRate,
        )
    }
}