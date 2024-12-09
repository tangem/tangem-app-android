package com.tangem.feature.wallet.presentation.wallet.state.transformers

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.common.util.getCardsCount
import com.tangem.domain.visa.model.VisaCurrency
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.model.BalancesAndLimitsBlockState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAdditionalInfo
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import com.tangem.utils.extensions.isZero
import org.joda.time.DateTime
import org.joda.time.Days

internal class SetBalancesAndLimitsTransformer(
    private val userWallet: UserWallet,
    private val maybeVisaCurrency: Either<Throwable, VisaCurrency>,
    private val clickIntents: WalletClickIntents,
) : TypedWalletStateTransformer<WalletState.Visa.Content>(
    userWalletId = userWallet.walletId,
    targetStateClass = WalletState.Visa.Content::class,
) {

    override fun transformTyped(prevState: WalletState.Visa.Content): WalletState {
        val visaCurrency = maybeVisaCurrency.getOrElse {
            return prevState.copy(
                walletCardState = getErrorWalletCardState(prevState.walletCardState),
                depositButtonState = prevState.depositButtonState.copy(isEnabled = false),
                balancesAndLimitBlockState = BalancesAndLimitsBlockState.Error,
            )
        }

        return prevState.copy(
            walletCardState = getContentWalletCardState(prevState.walletCardState, visaCurrency),
            depositButtonState = prevState.depositButtonState.copy(isEnabled = true),
            balancesAndLimitBlockState = getContentBlockState(visaCurrency),
        )
    }

    private fun getContentBlockState(visaCurrency: VisaCurrency) = BalancesAndLimitsBlockState.Content(
        availableBalance = visaCurrency.limits.remainingOtp.format {
            crypto(visaCurrency.symbol, visaCurrency.decimals)
        },
        limitDays = Days.daysBetween(DateTime.now(), visaCurrency.limits.expirationDate).days.inc(),
        isEnabled = true,
        onClick = clickIntents::onBalancesAndLimitsClick,
    )

    private fun getErrorWalletCardState(prevState: WalletCardState): WalletCardState {
        return with(prevState) {
            WalletCardState.Error(
                id = id,
                title = title,
                imageResId = imageResId,
                onRenameClick = onRenameClick,
                onDeleteClick = onDeleteClick,
            )
        }
    }

    private fun getContentWalletCardState(prevState: WalletCardState, visaCurrency: VisaCurrency): WalletCardState {
        return with(prevState) {
            WalletCardState.Content(
                id = id,
                title = title,
                additionalInfo = createAdditionalInfo(visaCurrency),
                imageResId = imageResId,
                onRenameClick = onRenameClick,
                onDeleteClick = onDeleteClick,
                balance = visaCurrency.balances.available.format {
                    crypto(visaCurrency.symbol, visaCurrency.decimals)
                },
                cardCount = userWallet.getCardsCount(),
                isZeroBalance = visaCurrency.balances.available.isZero(),
            )
        }
    }

    private fun createAdditionalInfo(visaCurrency: VisaCurrency): WalletAdditionalInfo {
        val fiatAmount = BigDecimalFormatter.formatFiatAmount(
            fiatAmount = visaCurrency.fiatRate?.let { visaCurrency.balances.available.multiply(it) },
            fiatCurrencyCode = visaCurrency.fiatCurrency.code,
            fiatCurrencySymbol = visaCurrency.fiatCurrency.symbol,
        )
        val infoContent = stringReference(
            value = buildString {
                append(fiatAmount)
                append(" • ")
                append(visaCurrency.networkName)
            },
        )

        return WalletAdditionalInfo(hideable = true, infoContent)
    }
}