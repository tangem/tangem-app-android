package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.child.wallet.model.intents.MoneyAccountIntents
import com.tangem.features.moneyaccount.main.entity.MoneyAccountMainUM
import com.tangem.utils.converter.Converter

internal class MoneyAccountMainBlockConverter(
    private val moneyAccountIntents: MoneyAccountIntents,
) : Converter<AccountStatus.Money, MoneyAccountMainUM> {

    override fun convert(value: AccountStatus.Money): MoneyAccountMainUM {
        val userWalletId = value.account.userWalletId
        return when (val statusValue = value.value) {
            is MoneyAccountStatusValue.Empty,
            is MoneyAccountStatusValue.Error.ExposedDevice,
            -> MoneyAccountMainUM.Empty
            is MoneyAccountStatusValue.Onboarding.Activating -> MoneyAccountMainUM.Onboarding(
                subtitle = resourceReference(statusValue.step.subtitleRes()),
                onClick = moneyAccountIntents::onMoneyAccountActivatingClicked,
            )
            is MoneyAccountStatusValue.Onboarding.Failed -> MoneyAccountMainUM.Failed(
                subtitle = resourceReference(R.string.money_account_main_status_failed),
                onClick = {
                    moneyAccountIntents.onMoneyAccountFailedClicked(
                        userWalletId = userWalletId,
                        customerId = statusValue.customerId,
                    )
                },
            )
            is MoneyAccountStatusValue.Account.Active -> statusValue.toUM(userWalletId)
            is MoneyAccountStatusValue.Error.Unavailable,
            is MoneyAccountStatusValue.Error.NotSynced,
            -> MoneyAccountMainUM.Unavailable
        }
    }

    private fun MoneyAccountStatusValue.Onboarding.Activating.Step.subtitleRes(): Int = when (this) {
        MoneyAccountStatusValue.Onboarding.Activating.Step.ISSUING -> R.string.money_account_main_status_activating
        MoneyAccountStatusValue.Onboarding.Activating.Step.AWAITING_DEPOSIT -> {
            R.string.money_account_main_status_awaiting_deposit
        }
    }

    private fun MoneyAccountStatusValue.Account.toUM(userWalletId: UserWalletId): MoneyAccountMainUM {
        return MoneyAccountMainUM.Content(
            subtitle = resourceReference(R.string.money_account_main_status_active_v2),
            balance = balance?.toUM(source),
            onClick = { moneyAccountIntents.onMoneyAccountClicked(userWalletId) },
        )
    }

    private fun MoneyAccountStatusValue.Balance.toUM(source: StatusSource): MoneyAccountMainUM.BalanceUM {
        val currency = getJavaCurrencyByCode(currencyCode)
        return MoneyAccountMainUM.BalanceUM(
            amount = stringReference(
                cryptoAmount.format {
                    fiat(
                        fiatCurrencyCode = currency.currencyCode,
                        fiatCurrencySymbol = currency.symbol,
                    )
                },
            ),
            currency = stringReference(currencyCode),
            isFlickering = source == StatusSource.CACHE,
            isFromCache = source == StatusSource.ONLY_CACHE,
        )
    }
}