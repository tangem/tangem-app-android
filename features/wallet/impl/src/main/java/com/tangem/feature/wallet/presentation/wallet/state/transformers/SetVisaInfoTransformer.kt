package com.tangem.feature.wallet.presentation.wallet.state.transformers

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent
import com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent.Companion.VISA_TYPE
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.common.util.getCardsCount
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.visa.exception.RefreshTokenExpiredException
import com.tangem.domain.visa.model.VisaCurrency
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.model.BalancesAndLimitsBlockState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAdditionalInfo
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletManageButton
import com.tangem.utils.extensions.isZero
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.joda.time.DateTime
import org.joda.time.Days

internal class SetVisaInfoTransformer(
    private val userWallet: UserWallet.Cold,
    private val maybeVisaCurrency: Either<Throwable, VisaCurrency>,
    private val clickIntents: WalletClickIntents,
) : TypedWalletStateTransformer<WalletState.Visa.Content>(
    userWalletId = userWallet.walletId,
    targetStateClass = WalletState.Visa.Content::class,
) {

    override fun transformTyped(prevState: WalletState.Visa.Content): WalletState {
        val visaCurrency = maybeVisaCurrency.getOrElse {
            if (it is RefreshTokenExpiredException) {
                return getRefreshTokenExpiredState(prevState)
            }

            return prevState.copy(
                buttons = createVisaButtonsDimmed(),
                walletCardState = getErrorWalletCardState(prevState.walletCardState),
                balancesAndLimitBlockState = BalancesAndLimitsBlockState.Error,
            )
        }

        return prevState.copy(
            buttons = createVisaButtons(visaCurrency = visaCurrency),
            walletCardState = getContentWalletCardState(prevState.walletCardState, visaCurrency),
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
                dropDownItems = dropDownItems,
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
                dropDownItems = dropDownItems,
                balance = visaCurrency.balances.available.format {
                    crypto(visaCurrency.symbol, visaCurrency.decimals)
                },
                cardCount = userWallet.getCardsCount(),
                isZeroBalance = visaCurrency.balances.available.isZero(),
                isBalanceFlickering = false,
            )
        }
    }

    private fun createAdditionalInfo(visaCurrency: VisaCurrency): WalletAdditionalInfo {
        val fiatAmount = visaCurrency.fiatRate?.let { visaCurrency.balances.available.multiply(it) }
            .format {
                fiat(
                    fiatCurrencyCode = visaCurrency.fiatCurrency.code,
                    fiatCurrencySymbol = visaCurrency.fiatCurrency.symbol,
                )
            }

        val infoContent = stringReference(
            value = buildString {
                append(fiatAmount)
                append(" • ")
                append(visaCurrency.networkName)
            },
        )

        return WalletAdditionalInfo(hideable = true, infoContent)
    }

    private fun getRefreshTokenExpiredState(prevState: WalletState.Visa.Content): WalletState {
        return WalletState.Visa.AccessTokenLocked(
            walletCardState = prevState.walletCardState,
            buttons = prevState.buttons,
            bottomSheetConfig = prevState.bottomSheetConfig,
            onExploreClick = clickIntents::onExploreClick,
            onUnlockVisaAccessNotificationClick = clickIntents::onUnlockVisaAccessClick,
        )
    }

    private fun createVisaButtonsDimmed(): PersistentList<WalletManageButton> {
        return persistentListOf(
            WalletManageButton.Receive(enabled = true, dimContent = true, onClick = {}, onLongClick = null),
            WalletManageButton.Buy(enabled = true, dimContent = true, onClick = {}),
        )
    }

    private fun createVisaButtons(visaCurrency: VisaCurrency): PersistentList<WalletManageButton> {
        // [Second Visa Iteration] Make VisaCurrency contain CryptoCurrencyStatus
        val cryptoCurrencyStatus = CryptoCurrencyStatus(
            currency = visaCurrency.cryptoCurrency,
            value = CryptoCurrencyStatus.Loaded(
                amount = visaCurrency.balances.available,
                fiatAmount = visaCurrency.balances.available.multiply(visaCurrency.fiatRate),
                fiatRate = visaCurrency.fiatRate,
                priceChange = visaCurrency.priceChange,
                yieldBalance = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = visaCurrency.paymentAccountAddress,
                sources = CryptoCurrencyStatus.Sources(),
            ),
        )

        return persistentListOf(
            WalletManageButton.Receive(
                enabled = true,
                dimContent = false,
                onClick = {
                    clickIntents.onReceiveClick(cryptoCurrencyStatus, MainScreenAnalyticsEvent.ButtonReceive)
                },
                onLongClick = {
                    clickIntents.onCopyAddressLongClick(cryptoCurrencyStatus)
                },
            ),
            WalletManageButton.Buy(
                enabled = true,
                dimContent = false,
                onClick = {
                    clickIntents.onMultiWalletBuyClick(userWalletId = userWallet.walletId, screenType = VISA_TYPE)
                },
            ),
        )
    }
}