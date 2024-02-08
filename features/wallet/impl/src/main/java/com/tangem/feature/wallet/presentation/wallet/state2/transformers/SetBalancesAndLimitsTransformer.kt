package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.visa.model.VisaCurrency
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.getCardsCount
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletAdditionalInfo
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state2.model.BalancesAndLimitsBlockState
import com.tangem.feature.wallet.presentation.wallet.state2.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import org.joda.time.DateTime
import org.joda.time.Days

internal class SetBalancesAndLimitsTransformer(
    private val userWallet: UserWallet,
    private val maybeVisaCurrency: Either<Throwable, VisaCurrency>,
    private val clickIntents: WalletClickIntentsV2,
) : WalletStateTransformer(userWallet.walletId) {

    override fun transform(prevState: WalletState): WalletState {
        return prevState.transformWhenInState<WalletState.Visa.Content> { state ->
            val visaCurrency = maybeVisaCurrency.getOrElse {
                return state.copy(
                    walletCardState = getErrorWalletCardState(state.walletCardState),
                    depositButtonState = state.depositButtonState.copy(isEnabled = false),
                    balancesAndLimitBlockState = BalancesAndLimitsBlockState.Error,
                )
            }

            state.copy(
                walletCardState = getContentWalletCardState(state.walletCardState, visaCurrency),
                depositButtonState = state.depositButtonState.copy(isEnabled = true),
                balancesAndLimitBlockState = getContentBlockState(visaCurrency),
            )
        }
    }

    private fun getContentBlockState(visaCurrency: VisaCurrency) = BalancesAndLimitsBlockState.Content(
        availableBalance = BigDecimalFormatter.formatCryptoAmount(
            visaCurrency.limits.remainingOtp,
            visaCurrency.symbol,
            visaCurrency.decimals,
        ),
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
                balance = BigDecimalFormatter.formatCryptoAmount(
                    visaCurrency.balances.available,
                    visaCurrency.symbol,
                    visaCurrency.decimals,
                ),
                cardCount = userWallet.getCardsCount(),
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