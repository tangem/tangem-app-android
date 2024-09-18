package com.tangem.features.staking.impl.presentation.state.converters

import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.PendingTransaction
import com.tangem.domain.staking.model.stakekit.BalanceType
import com.tangem.domain.staking.model.stakekit.BalanceType.Companion.isClickable
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf

internal class PendingTransactionItemConverter(
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val validator: Yield.Validator?,
) : Converter<PendingTransaction, BalanceState?> {

    override fun convert(value: PendingTransaction): BalanceState? {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val appCurrency = appCurrencyProvider()
        val cryptoCurrency = cryptoCurrencyStatus.currency

        val cryptoAmount = value.amount ?: return null
        val fiatAmount = cryptoCurrencyStatus.value.fiatRate?.times(cryptoAmount)

        val balanceType = value.type ?: return null
        val title = balanceType.getTitle(validator?.name)
        return title?.let {
            BalanceState(
                id = value.id,
                validator = validator,
                title = title,
                subtitle = TextReference.EMPTY,
                type = balanceType,
                cryptoValue = cryptoAmount.parseBigDecimal(cryptoCurrency.decimals),
                cryptoDecimal = cryptoAmount,
                cryptoAmount = stringReference(
                    BigDecimalFormatter.formatCryptoAmount(
                        cryptoAmount = cryptoAmount,
                        cryptoCurrency = cryptoCurrency,
                    ),
                ),
                fiatAmount = stringReference(
                    BigDecimalFormatter.formatFiatAmount(
                        fiatAmount = fiatAmount,
                        fiatCurrencyCode = appCurrency.code,
                        fiatCurrencySymbol = appCurrency.symbol,
                    ),
                ),
                rawCurrencyId = value.rawCurrencyId,
                pendingActions = persistentListOf(),
                isClickable = false,
            )
        }
    }

    private fun BalanceType.getTitle(validatorName: String?) = when (this) {
        BalanceType.PREPARING,
        BalanceType.STAKED,
        -> validatorName?.let { stringReference(it) }
        BalanceType.UNSTAKED -> resourceReference(R.string.staking_unstaked)
        BalanceType.UNSTAKING -> resourceReference(R.string.staking_unstaking)
        BalanceType.LOCKED -> resourceReference(R.string.staking_locked)
        BalanceType.AVAILABLE,
        BalanceType.REWARDS,
        BalanceType.UNLOCKING,
        BalanceType.UNKNOWN,
        -> null
    }
}
