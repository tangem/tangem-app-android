package com.tangem.data.moneyaccount.converter

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.account.MoneyAccountStatusValue.Onboarding.Activating.Step as ActivatingStep
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.currency.MoneyAccountCurrencyFactory
import com.tangem.spend.datasource.tangempay.entity.MoneyAccountStatusValueDM
import com.tangem.spend.datasource.tangempay.entity.MoneyAccountStatusValueDM.Activating.StepDM as ActivatingStepDM
import com.tangem.utils.converter.Converter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MoneyAccountStatusValueDMConverter @Inject constructor(
    private val moneyAccountCurrencyFactory: MoneyAccountCurrencyFactory,
) : Converter<MoneyAccountStatusValue, MoneyAccountStatusValueDM?> {

    override fun convert(value: MoneyAccountStatusValue): MoneyAccountStatusValueDM? {
        return when (value) {
            is MoneyAccountStatusValue.Empty -> MoneyAccountStatusValueDM.Empty
            is MoneyAccountStatusValue.Onboarding.Activating -> MoneyAccountStatusValueDM.Activating(
                customerId = value.customerId,
                step = value.step.toDM(),
            )
            is MoneyAccountStatusValue.Onboarding.Failed -> MoneyAccountStatusValueDM.Failed(
                customerId = value.customerId,
            )
            is MoneyAccountStatusValue.Account.Active -> MoneyAccountStatusValueDM.ActiveAccount(
                customerId = value.customerId,
                productInstanceId = value.productInstanceId,
                balance = value.balance?.toDM(),
            )
            is MoneyAccountStatusValue.Error.ExposedDevice,
            is MoneyAccountStatusValue.Error.Unavailable,
            is MoneyAccountStatusValue.Error.NotSynced,
            -> null
        }
    }

    fun convertBack(userWalletId: UserWalletId, value: MoneyAccountStatusValueDM?): MoneyAccountStatusValue {
        return when (value) {
            is MoneyAccountStatusValueDM.Empty -> MoneyAccountStatusValue.Empty
            is MoneyAccountStatusValueDM.Activating -> MoneyAccountStatusValue.Onboarding.Activating(
                source = StatusSource.CACHE,
                customerId = value.customerId,
                step = value.step.toDomain(),
            )
            is MoneyAccountStatusValueDM.Failed -> MoneyAccountStatusValue.Onboarding.Failed(
                source = StatusSource.CACHE,
                customerId = value.customerId,
            )
            is MoneyAccountStatusValueDM.ActiveAccount -> value.productInstanceId?.let { productInstanceId ->
                MoneyAccountStatusValue.Account.Active(
                    source = StatusSource.CACHE,
                    cryptoCurrency = moneyAccountCurrencyFactory.create(userWalletId),
                    balance = value.balance?.toDomain(),
                    customerId = value.customerId,
                    productInstanceId = productInstanceId,
                    error = null,
                )
            } ?: MoneyAccountStatusValue.Error.Unavailable
            null -> MoneyAccountStatusValue.Error.Unavailable
        }
    }

    private fun ActivatingStep.toDM(): ActivatingStepDM {
        return when (this) {
            ActivatingStep.ISSUING -> ActivatingStepDM.ISSUING
            ActivatingStep.AWAITING_DEPOSIT -> ActivatingStepDM.AWAITING_DEPOSIT
        }
    }

    private fun ActivatingStepDM.toDomain(): ActivatingStep {
        return when (this) {
            ActivatingStepDM.ISSUING -> ActivatingStep.ISSUING
            ActivatingStepDM.AWAITING_DEPOSIT -> ActivatingStep.AWAITING_DEPOSIT
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

    private fun MoneyAccountStatusValueDM.BalanceDM.toDomain(): MoneyAccountStatusValue.Balance {
        return MoneyAccountStatusValue.Balance(
            cryptoAmount = cryptoAmount,
            depositAddress = depositAddress,
            currencyCode = currencyCode,
            fiatRate = fiatRate,
        )
    }
}