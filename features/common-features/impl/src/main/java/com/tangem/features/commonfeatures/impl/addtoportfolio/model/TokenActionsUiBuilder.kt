package com.tangem.features.commonfeatures.impl.addtoportfolio.model

import androidx.compose.ui.text.SpanStyle
import com.tangem.common.getTotalCryptoAmount
import com.tangem.common.getTotalFiatAmount
import com.tangem.common.ui.account.*
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.markets.action.CryptoCurrencyData
import com.tangem.common.ui.markets.action.QuickActionsConverter.quickActions
import com.tangem.common.ui.markets.action.TokenActionsHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.DesignFeatureToggles
import com.tangem.core.ui.R
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.ds.badge.TangemBadgeIconPosition
import com.tangem.core.ui.ds.badge.TangemBadgeShape
import com.tangem.core.ui.ds.badge.TangemBadgeSize
import com.tangem.core.ui.ds.badge.TangemBadgeUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.commonfeatures.impl.addtoportfolio.TokenActionsComponent
import com.tangem.features.commonfeatures.impl.addtoportfolio.ui.state.TokenActionsUM
import java.math.BigDecimal
import javax.inject.Inject

@ModelScoped
internal class TokenActionsUiBuilder @Inject constructor(
    paramsContainer: ParamsContainer,
    private val designFeatureToggles: DesignFeatureToggles,
) {
    private val params = paramsContainer.require<TokenActionsComponent.Params>()

    fun build(
        cryptoCurrencyData: CryptoCurrencyData,
        tokenActionsHandler: TokenActionsHandler,
        appCurrency: AppCurrency,
        isBalanceHidden: Boolean,
    ): TokenActionsUM {
        return if (designFeatureToggles.isRedesignEnabled) {
            buildV2(
                cryptoCurrencyData = cryptoCurrencyData,
                tokenActionsHandler = tokenActionsHandler,
                appCurrency = appCurrency,
                isBalanceHidden = isBalanceHidden,
            )
        } else {
            buildV1(
                cryptoCurrencyData = cryptoCurrencyData,
                tokenActionsHandler = tokenActionsHandler,
            )
        }
    }

    private fun buildV1(
        cryptoCurrencyData: CryptoCurrencyData,
        tokenActionsHandler: TokenActionsHandler,
    ): TokenActionsUM {
        val status = cryptoCurrencyData.status
        val tokenUM = TokenItemState.Content(
            id = status.currency.id.value,
            iconState = CryptoCurrencyToIconStateConverter().convert(status.currency),
            titleState = TokenItemState.TitleState.Content(stringReference(status.currency.name)),
            fiatAmountState = null,
            subtitle2State = null,
            subtitleState = TokenItemState.SubtitleState.TextContent(stringReference(status.currency.symbol)),
            onItemClick = null,
            onItemLongClick = null,
        )
        return TokenActionsUM(
            token = tokenUM,
            quickActions = quickActions(
                cryptoData = cryptoCurrencyData,
                tokenActionsHandler = tokenActionsHandler,
                isRedesignEnabled = false,
            ),
            onLaterClick = {
                params.callbacks.onLaterClick()
            },
        )
    }

    private fun buildV2(
        cryptoCurrencyData: CryptoCurrencyData,
        tokenActionsHandler: TokenActionsHandler,
        appCurrency: AppCurrency,
        isBalanceHidden: Boolean,
    ): TokenActionsUM {
        val status = cryptoCurrencyData.status
        val tokenUM = TokenItemState.Content(
            id = status.currency.id.value,
            iconState = CryptoCurrencyToIconStateConverter().convert(status.currency),
            titleState = TokenItemState.TitleState.Content(stringReference(status.currency.name)),
            fiatAmountState = createFiatAmountState(status, appCurrency),
            subtitle2State = createSubtitle2State(status),
            subtitleState = TokenItemState.SubtitleState.TextContent(stringReference(status.currency.symbol)),
            onItemClick = null,
            onItemLongClick = null,
        )
        return TokenActionsUM(
            token = tokenUM,
            quickActions = quickActions(
                cryptoData = cryptoCurrencyData,
                tokenActionsHandler = tokenActionsHandler,
                isRedesignEnabled = true,
            ),
            onLaterClick = {
                params.callbacks.onLaterClick()
            },
            isBalancesHidden = isBalanceHidden,
            portfolioBadge = createPortfolioBadge(cryptoCurrencyData),
        )
    }

    private fun createPortfolioBadge(cryptoCurrencyData: CryptoCurrencyData): TangemBadgeUM {
        val icon: AccountIconUM?
        val name = if (cryptoCurrencyData.isAccountMode) {
            icon = CryptoPortfolioIconConverter.convert(cryptoCurrencyData.account.account.icon)
            cryptoCurrencyData
                .account
                .account
                .accountName
                .toUM()
                .value
        } else {
            icon = null
            stringReference(cryptoCurrencyData.userWallet.name)
        }
        return TangemBadgeUM(
            text = name,
            tangemIconUM = if (icon == null) {
                TangemIconUM.Icon(
                    iconRes = R.drawable.ic_key_card_20,
                    tintReference = { TangemTheme.colors2.graphic.neutral.tertiary },
                )
            } else {
                TangemIconUM.Icon(
                    iconRes = icon.value.getResId(),
                    tintReference = { icon.color.getUiColor() },
                )
            },
            size = TangemBadgeSize.X6,
            shape = TangemBadgeShape.Rounded,
            iconPosition = if (cryptoCurrencyData.isAccountMode) {
                TangemBadgeIconPosition.Start
            } else {
                TangemBadgeIconPosition.End
            },
            shouldRespectIconTint = cryptoCurrencyData.isAccountMode,
        )
    }

    private fun createSubtitle2State(status: CryptoCurrencyStatus): TokenItemState.Subtitle2State? {
        return when (status.value) {
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.NoAccount,
            -> {
                TokenItemState.Subtitle2State.TextContent(
                    text = status.getTotalCryptoAmount().format {
                        crypto(status.currency)
                    },
                )
            }
            is CryptoCurrencyStatus.Loading,
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.Unreachable,
            is CryptoCurrencyStatus.NoAmount,
            -> TokenItemState.Subtitle2State.TextContent(
                text = BigDecimal.ZERO.format {
                    crypto(status.currency)
                },
            )
        }
    }

    private fun createFiatAmountState(
        status: CryptoCurrencyStatus,
        appCurrency: AppCurrency,
    ): TokenItemState.FiatAmountState? {
        return when (status.value) {
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.NoAccount,
            -> {
                TokenItemState.FiatAmountState.AnnotatedContent(
                    text = status.getTotalFiatAmount().formatStyled {
                        fiat(
                            fiatCurrencyCode = appCurrency.code,
                            fiatCurrencySymbol = appCurrency.symbol,
                            spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
                        ).price()
                    },
                )
            }
            is CryptoCurrencyStatus.Unreachable,
            is CryptoCurrencyStatus.NoAmount,
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.Loading,
            -> TokenItemState.FiatAmountState.AnnotatedContent(
                text = BigDecimal.ZERO.formatStyled {
                    fiat(
                        fiatCurrencyCode = appCurrency.code,
                        fiatCurrencySymbol = appCurrency.symbol,
                        spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
                    ).price()
                },
            )
        }
    }
}