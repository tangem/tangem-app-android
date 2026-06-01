package com.tangem.features.commonfeatures.impl.tokenactions.model

import androidx.compose.ui.text.SpanStyle
import com.tangem.common.getTotalCryptoAmount
import com.tangem.common.getTotalFiatAmount
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.getResId
import com.tangem.common.ui.account.getUiColor
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.markets.action.CryptoCurrencyData
import com.tangem.common.ui.markets.action.QuickActionsConverter.quickActions
import com.tangem.common.ui.markets.action.TokenActionsHandler
import com.tangem.features.commonfeatures.api.tokenactions.BottomAction
import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.DesignFeatureToggles
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.ds.badge.TangemBadgeIconPosition
import com.tangem.core.ui.ds.badge.TangemBadgeShape
import com.tangem.core.ui.ds.badge.TangemBadgeSize
import com.tangem.core.ui.ds.badge.TangemBadgeUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.commonfeatures.impl.R
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.features.commonfeatures.impl.tokenactions.TokenActionsComponent
import com.tangem.features.commonfeatures.impl.tokenactions.ui.state.PortfolioBadgeUM
import com.tangem.features.commonfeatures.impl.tokenactions.ui.state.TokenActionsUM
import java.math.BigDecimal
import javax.inject.Inject

@ModelScoped
internal class TokenActionsUiBuilder @Inject constructor(
    paramsContainer: ParamsContainer,
    private val designFeatureToggles: DesignFeatureToggles,
    private val getWalletIconUseCase: GetWalletIconUseCase,
    private val walletIconUMConverter: WalletIconUMConverter,
) {
    private val params = paramsContainer.require<TokenActionsComponent.Params>()

    fun build(
        cryptoCurrencyData: CryptoCurrencyData,
        tokenActionsHandler: TokenActionsHandler,
        appCurrency: AppCurrency,
        isBalanceHidden: Boolean,
        bottomAction: BottomAction,
    ): TokenActionsUM {
        return if (designFeatureToggles.isRedesignEnabled || params.isRedesignForced) {
            buildV2(
                cryptoCurrencyData = cryptoCurrencyData,
                tokenActionsHandler = tokenActionsHandler,
                appCurrency = appCurrency,
                isBalanceHidden = isBalanceHidden,
                bottomAction = bottomAction,
            )
        } else {
            buildV1(
                cryptoCurrencyData = cryptoCurrencyData,
                tokenActionsHandler = tokenActionsHandler,
                bottomAction = bottomAction,
            )
        }
    }

    private fun buildV1(
        cryptoCurrencyData: CryptoCurrencyData,
        tokenActionsHandler: TokenActionsHandler,
        bottomAction: BottomAction,
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
            bottomActionText = bottomActionText(bottomAction),
            onBottomActionClick = {
                params.callbacks.onBottomActionClick(bottomAction)
            },
        )
    }

    private fun buildV2(
        cryptoCurrencyData: CryptoCurrencyData,
        tokenActionsHandler: TokenActionsHandler,
        appCurrency: AppCurrency,
        isBalanceHidden: Boolean,
        bottomAction: BottomAction,
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
            bottomActionText = bottomActionText(bottomAction),
            onBottomActionClick = {
                params.callbacks.onBottomActionClick(bottomAction)
            },
            isBalancesHidden = isBalanceHidden,
            portfolioBadge = createPortfolioBadge(cryptoCurrencyData = cryptoCurrencyData),
            isCompact = params.isCompact,
        )
    }

    private fun bottomActionText(action: BottomAction): TextReference? {
        return when (action) {
            BottomAction.GoToToken -> resourceReference(R.string.common_go_to_token)
            BottomAction.None -> null
        }
    }

    private fun createPortfolioBadge(cryptoCurrencyData: CryptoCurrencyData): PortfolioBadgeUM {
        return if (cryptoCurrencyData.isAccountMode) {
            val icon = CryptoPortfolioIconConverter.convert(cryptoCurrencyData.account.account.icon)
            val name = cryptoCurrencyData
                .account
                .account
                .accountName
                .toUM()
                .value
            PortfolioBadgeUM.Account(
                badge = TangemBadgeUM(
                    text = name,
                    tangemIconUM = TangemIconUM.Icon(
                        iconRes = icon.value.getResId(),
                        tintReference = { icon.color.getUiColor() },
                    ),
                    size = TangemBadgeSize.X6,
                    shape = TangemBadgeShape.Rounded,
                    iconPosition = TangemBadgeIconPosition.Start,
                    shouldRespectIconTint = true,
                ),
            )
        } else {
            val userWallet = cryptoCurrencyData.userWallet
            PortfolioBadgeUM.Wallet(
                name = stringReference(userWallet.name),
                deviceIcon = walletIconUMConverter.convert(
                    getWalletIconUseCase(cryptoCurrencyData.userWallet),
                ),
            )
        }
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