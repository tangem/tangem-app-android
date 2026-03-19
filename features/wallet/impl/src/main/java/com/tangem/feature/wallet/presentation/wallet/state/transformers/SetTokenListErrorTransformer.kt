package com.tangem.feature.wallet.presentation.wallet.state.transformers

import androidx.compose.ui.text.SpanStyle
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.card.common.util.getCardsCount
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.state.model.*
import com.tangem.feature.wallet.presentation.wallet.state.utils.disableButtons
import timber.log.Timber
import java.math.BigDecimal

internal class SetTokenListErrorTransformer(
    private val selectedWallet: UserWallet,
    private val error: TokenListError,
    private val appCurrency: AppCurrency,
    private val clickIntents: WalletClickIntents,
) : WalletStateTransformer(selectedWallet.walletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (error) {
            is TokenListError.EmptyTokens -> {
                when (prevState) {
                    is WalletState.MultiCurrency.Content -> {
                        prevState.copy(
                            walletCardState = prevState.walletCardState.toLoadedState(),
                            tokensListState = WalletTokensListState.Empty,
                            buttons = if (error == TokenListError.EmptyTokens) {
                                prevState.disableButtons()
                            } else {
                                prevState.buttons
                            },
                        )
                    }
                    is WalletState.MultiCurrency.Locked -> {
                        Timber.w("Impossible to load tokens list for locked wallet")
                        prevState
                    }
                    is WalletState.SingleCurrency -> {
                        Timber.w("Impossible to load tokens list for single-currency wallet")
                        prevState
                    }
                }
            }
            is TokenListError.DataError,
            is TokenListError.UnableToSortTokenList,
            -> prevState
        }
    }

    override fun transform(walletUM: WalletUM): WalletUM {
        return when (walletUM) {
            is WalletUM.Content -> {
                walletUM.copy(
                    walletsBalanceUM = walletUM.walletsBalanceUM.toLoadedState(),
                    tokensListUM = WalletTokensListUM.Empty(
                        onEmptyClick = {
                            clickIntents.onManageTokensClick(walletUM.walletsBalanceUM.id)
                        },
                    ),
                    buttons = walletUM.disableButtons(),
                )
            }
            is WalletUM.Locked -> {
                Timber.w("Impossible to load tokens list for locked wallet")
                walletUM
            }
        }
    }

    private fun WalletCardState.toLoadedState(): WalletCardState {
        return WalletCardState.Content(
            id = id,
            title = title,
            additionalInfo = WalletAdditionalInfoFactory.resolve(wallet = selectedWallet),
            imageResId = imageResId,
            dropDownItems = dropDownItems,
            balance = BigDecimal.ZERO.format {
                fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
            },
            cardCount = when (selectedWallet) {
                is UserWallet.Cold -> selectedWallet.getCardsCount()
                is UserWallet.Hot -> null
            },
            isZeroBalance = true,
            isBalanceFlickering = false,
        )
    }

    private fun WalletBalanceUM.toLoadedState(): WalletBalanceUM {
        return WalletBalanceUM.Content(
            id = id,
            name = name,
            deviceIcon = deviceIcon,
            balanceInAppBar = BigDecimal.ZERO.formatStyled {
                fiat(
                    fiatCurrencyCode = appCurrency.code,
                    fiatCurrencySymbol = appCurrency.symbol,
                    spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
                )
            },
            balance = BigDecimal.ZERO.formatStyled {
                fiat(
                    fiatCurrencyCode = appCurrency.code,
                    fiatCurrencySymbol = appCurrency.symbol,
                    spanStyleReference = { TangemTheme.typography2.headingRegular28.toSpanStyle() },
                )
            },
            isZeroBalance = true,
            isBalanceFlickering = false,
        )
    }
}