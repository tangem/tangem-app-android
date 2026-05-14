package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import androidx.compose.ui.text.SpanStyle
import com.tangem.common.getTotalWithRewardsStakingBalance
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.defaultAmount
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenBalanceTypeUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.isNullOrZero
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

/**
 * Maps [CryptoCurrencyStatus] into [TokenDetailsBalanceBlockUM] and sets it on the state.
 *
 * Produces [TokenDetailsBalanceBlockUM.Content] for loaded states,
 * [TokenDetailsBalanceBlockUM.Loading] for loading,
 * [TokenDetailsBalanceBlockUM.Error] for unreachable/no-amount/missed-derivation.
 */
internal class SetBalanceTransformer(
    private val status: CryptoCurrencyStatus,
    private val appCurrency: AppCurrency,
    private val onToggleBalanceType: () -> Unit,
) : Transformer<TokenDetailsUM> {

    override fun transform(prevState: TokenDetailsUM): TokenDetailsUM {
        val prev = prevState.balanceBlockUM
        val balanceBlockUM = when (status.value) {
            is CryptoCurrencyStatus.Loading -> TokenDetailsBalanceBlockUM.Loading(
                addFundsButton = prev.addFundsButton,
                swapButton = prev.swapButton,
                transferButton = prev.transferButton,
                tokenBalanceTypeUM = prev.tokenBalanceTypeUM,
                currencyIconState = prev.currencyIconState,
            )
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.NoAccount,
            is CryptoCurrencyStatus.Custom,
            -> buildLoadedContent(prev)
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.Unreachable,
            is CryptoCurrencyStatus.NoAmount,
            -> TokenDetailsBalanceBlockUM.Error(
                addFundsButton = prev.addFundsButton,
                swapButton = prev.swapButton,
                transferButton = prev.transferButton,
                tokenBalanceTypeUM = prev.tokenBalanceTypeUM,
                currencyIconState = prev.currencyIconState,
            )
        }
        return prevState.copy(balanceBlockUM = balanceBlockUM)
    }

    private fun buildLoadedContent(prev: TokenDetailsBalanceBlockUM): TokenDetailsBalanceBlockUM.Content {
        val stakingCryptoAmount =
            (status.value.stakingBalance as? StakingBalance.Data)?.getTotalWithRewardsStakingBalance(
                status.currency.network.rawId,
            )
        val stakingFiatAmount = stakingCryptoAmount?.let { status.value.fiatRate?.multiply(it) }
        val hasStaking = !stakingCryptoAmount.isNullOrZero()

        val prevType = prev.tokenBalanceTypeUM
        val tokenBalanceTypeUM = if (hasStaking) {
            TokenBalanceTypeUM.Multiple(
                type = (prevType as? TokenBalanceTypeUM.Multiple)?.type ?: TokenBalanceTypeUM.Type.ALL,
                availableTypes = persistentListOf(TokenBalanceTypeUM.Type.ALL, TokenBalanceTypeUM.Type.AVAILABLE),
                onSelect = onToggleBalanceType,
            )
        } else {
            TokenBalanceTypeUM.Single
        }

        val totalCryptoAmount = computeTotal(status.value.amount, stakingCryptoAmount)

        return TokenDetailsBalanceBlockUM.Content(
            addFundsButton = prev.addFundsButton,
            swapButton = prev.swapButton,
            transferButton = prev.transferButton,
            currencyIconState = prev.currencyIconState,
            tokenBalanceTypeUM = tokenBalanceTypeUM,
            displayFiatBalanceAll = formatFiatStyled(
                fiatAmount = computeTotal(status.value.fiatAmount, stakingFiatAmount),
            ),
            displayCryptoBalanceAll = formatCrypto(amount = totalCryptoAmount),
            displayFiatBalanceAvailable = if (hasStaking) {
                formatFiatStyled(fiatAmount = status.value.fiatAmount)
            } else {
                null
            },
            displayCryptoBalanceAvailable = if (hasStaking) {
                formatCrypto(amount = status.value.amount)
            } else {
                null
            },
            isBalanceFlickering = status.value.sources.total == StatusSource.CACHE,
            isBalanceZero = totalCryptoAmount.isNullOrZero(),
        )
    }

    private fun computeTotal(base: BigDecimal?, staking: BigDecimal?): BigDecimal? {
        if (base == null) return null
        return if (staking != null) base + staking else base
    }

    private fun formatFiatStyled(fiatAmount: BigDecimal?): TextReference {
        if (fiatAmount == null) return stringReference(DASH_SIGN)
        return fiatAmount.formatStyled {
            fiat(
                fiatCurrencyCode = appCurrency.code,
                fiatCurrencySymbol = appCurrency.symbol,
                spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
            ).defaultAmount(
                spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
            )
        }
    }

    private fun formatCrypto(amount: BigDecimal?): TextReference {
        if (amount == null) return stringReference(DASH_SIGN)
        return stringReference(
            amount.format { crypto(status.currency).defaultAmount() },
        )
    }
}