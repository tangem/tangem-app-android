@file:Suppress("MagicNumber")

package com.tangem.tap.features.tokens.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.FabPosition
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.analytics.Analytics
import com.tangem.core.ui.components.Keyboard
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.components.keyboardAsState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.common.TapWorkarounds.useOldStyleDerivation
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.ManageTokens
import com.tangem.tap.common.compose.extensions.addAndNotify
import com.tangem.tap.common.compose.extensions.removeAndNotify
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.pixelsToDp
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.domain.tokens.Currency
import com.tangem.tap.features.tokens.redux.ContractAddress
import com.tangem.tap.features.tokens.redux.LoadCoinsState
import com.tangem.tap.features.tokens.redux.TokenWithBlockchain
import com.tangem.tap.features.tokens.redux.TokensState
import com.tangem.tap.features.wallet.redux.models.WalletDialog
import com.tangem.tap.store
import com.tangem.wallet.R

@Suppress("LongMethod")
@Composable
fun CurrenciesScreen(
    tokensState: MutableState<TokensState>,
    onSaveChanges: (List<TokenWithBlockchain>, List<Blockchain>) -> Unit,
    onNetworkItemClick: (ContractAddress) -> Unit,
    onLoadMore: () -> Unit,
) {
    val tokensAddedOnMainScreen = remember { tokensState.value.addedTokens }
    val blockchainsAddedOnMainScreen = remember { tokensState.value.addedBlockchains }

    val addedTokensState = remember { mutableStateOf(tokensState.value.addedTokens) }
    val addedBlockchainsState = remember { mutableStateOf(tokensState.value.addedBlockchains) }

    val isKeyboardOpen by keyboardAsState()

    val onAddCurrencyToggleClick = { currency: Currency, token: TokenWithBlockchain? ->
        val blockchain = Blockchain.fromNetworkId(currency.id)
        if (blockchain != null && token == null) {
            toggleBlockchain(
                blockchain,
                blockchainsAddedOnMainScreen,
                addedBlockchainsState,
                addedTokensState.value,
            )
        } else if (token != null) {
            toggleToken(
                token,
                tokensAddedOnMainScreen,
                addedTokensState,
                tokensState.value,
            )
        }
    }

    val statusBarColor = colorResource(id = R.color.backgroundLightGray)
    SystemBarsEffect {
        setSystemBarsColor(color = statusBarColor)
    }

    Scaffold(
        floatingActionButton = {
            if (tokensState.value.allowToAdd) {
                SaveChangesButton(isKeyboardOpen) {
                    onSaveChanges(addedTokensState.value, addedBlockchainsState.value)
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) {
        AnimatedVisibility(
            visible = tokensState.value.loadCoinsState == LoadCoinsState.LOADING,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF1ACE80))
            }
        }
        AnimatedVisibility(
            visible = tokensState.value.loadCoinsState == LoadCoinsState.LOADED,
            enter = fadeIn(animationSpec = tween(1000)),
            exit = fadeOut(animationSpec = tween(1000)),
        ) {
            Column {
                val showHeader = tokensState.value.scanResponse?.card?.useOldStyleDerivation == true
                ListOfCurrencies(
                    header = { if (showHeader) CurrenciesWarning() },
                    currencies = tokensState.value.currencies,
                    addedTokens = addedTokensState.value,
                    addedBlockchains = addedBlockchainsState.value,
                    allowToAdd = tokensState.value.allowToAdd,
                    onAddCurrencyToggle = { currency, token ->
                        onAddCurrencyToggleClick(currency, token)
                    },
                    onNetworkItemClick = onNetworkItemClick,
                    onLoadMore = onLoadMore,
                )
            }
        }
    }
}

private fun toggleBlockchain(
    blockchain: Blockchain,
    blockchainsAddedOnMainScreen: List<Blockchain>,
    addedBlockchainsState: MutableState<List<Blockchain>>,
    addedTokens: List<TokenWithBlockchain>,
) {
    val isTryingToRemove = addedBlockchainsState.value.contains(blockchain)
    val isAddedOnMainScreen = blockchainsAddedOnMainScreen.contains(blockchain)
    val isTokenWithSameBlockchainFound = addedTokens.any { it.blockchain == blockchain }
    val analyticsCurrencyTypeParam = AnalyticsParam.CurrencyType.Blockchain(blockchain)

    if (isTryingToRemove) {
        if (isTokenWithSameBlockchainFound) {
            store.dispatchDialogShow(
                WalletDialog.TokensAreLinkedDialog(
                    currencyTitle = blockchain.name,
                    currencySymbol = blockchain.currency,
                ),
            )
        } else if (isAddedOnMainScreen) {
            store.dispatchDialogShow(
                WalletDialog.RemoveWalletDialog(
                    currencyTitle = blockchain.name,
                    onOk = {
                        analyticsCurrencyTypeParam.sendOn()
                        addedBlockchainsState.removeAndNotify(blockchain)
                    },
                ),
            )
        } else {
            analyticsCurrencyTypeParam.sendOff()
            addedBlockchainsState.removeAndNotify(blockchain)
        }
    } else {
        analyticsCurrencyTypeParam.sendOn()
        addedBlockchainsState.addAndNotify(blockchain)
    }
}

private fun toggleToken(
    token: TokenWithBlockchain,
    tokensAddedOnMainScreen: List<TokenWithBlockchain>,
    addedTokensState: MutableState<List<TokenWithBlockchain>>,
    tokensState: TokensState,
) {
    val isTryingToRemove = addedTokensState.value.contains(token)
    val isAddedOnMainScreen = tokensAddedOnMainScreen.contains(token)
    val isUnsupportedToken = !tokensState.canHandleToken(token)
    val analyticsCurrencyTypeParam = AnalyticsParam.CurrencyType.Token(token.token)

    if (isTryingToRemove) {
        if (isAddedOnMainScreen) {
            store.dispatchDialogShow(
                WalletDialog.RemoveWalletDialog(
                    currencyTitle = token.token.name,
                    onOk = {
                        analyticsCurrencyTypeParam.sendOff()
                        addedTokensState.removeAndNotify(token)
                    },
                ),
            )
        } else {
            analyticsCurrencyTypeParam.sendOff()
            addedTokensState.removeAndNotify(token)
        }
    } else {
        if (isUnsupportedToken) {
            store.dispatchDialogShow(
                AppDialog.SimpleOkDialogRes(
                    headerId = R.string.common_warning,
                    messageId = R.string.alert_manage_tokens_unsupported_message,
                ),
            )
        } else {
            analyticsCurrencyTypeParam.sendOn()
            addedTokensState.addAndNotify(token)
        }
    }
}

private fun AnalyticsParam.CurrencyType.sendOn() {
    Analytics.send(ManageTokens.TokenSwitcherChanged(this, AnalyticsParam.OnOffState.On))
}

private fun AnalyticsParam.CurrencyType.sendOff() {
    Analytics.send(ManageTokens.TokenSwitcherChanged(this, AnalyticsParam.OnOffState.Off))
}

@Composable
fun SaveChangesButton(keyboardState: Keyboard, onSaveChanges: () -> Unit) {
    val padding = if (keyboardState is Keyboard.Opened) {
        LocalContext.current.pixelsToDp(keyboardState.height)
    } else {
        0
    }

    PrimaryButton(
        modifier = Modifier
            .padding(bottom = padding.dp)
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .fillMaxWidth(),
        text = stringResource(id = R.string.common_save_changes),
        onClick = onSaveChanges,
    )
}
