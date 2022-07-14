package com.tangem.tap.features.tokens.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.TapWorkarounds.useOldStyleDerivation
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.tap.common.compose.Keyboard
import com.tangem.tap.common.compose.extensions.addAndNotify
import com.tangem.tap.common.compose.extensions.removeAndNotify
import com.tangem.tap.common.compose.keyboardAsState
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

@Composable
fun CurrenciesScreen(
    tokensState: MutableState<TokensState> = mutableStateOf(store.state.tokensState),
    onSaveChanges: (List<TokenWithBlockchain>, List<Blockchain>) -> Unit,
    onNetworkItemClicked: (ContractAddress) -> Unit,
    onLoadMore: () -> Unit
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

    Scaffold(
        floatingActionButton = {
            if (tokensState.value.allowToAdd) SaveChangesButton(isKeyboardOpen) {
                onSaveChanges(addedTokensState.value, addedBlockchainsState.value)
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) {

        AnimatedVisibility(
            visible = tokensState.value.loadCoinsState == LoadCoinsState.LOADING,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF1ACE80)
                )
            }
        }
        AnimatedVisibility(
            visible = tokensState.value.loadCoinsState == LoadCoinsState.LOADED,
            enter = fadeIn(animationSpec = tween(1000)),
            exit = fadeOut(animationSpec = tween(1000))
        ) {
            Column {
                val showHeader = tokensState.value.scanResponse?.card?.useOldStyleDerivation == true
                ListOfCurrencies(
                    header = { if (showHeader) CurrenciesWarning() },
                    currencies = tokensState.value.currencies,
                    addedTokens = addedTokensState.value,
                    addedBlockchains = addedBlockchainsState.value,
                    allowToAdd = tokensState.value.allowToAdd,
                    onAddCurrencyToggled = { currency, token ->
                        onAddCurrencyToggleClick(currency, token)
                    },
                    onNetworkItemClicked = onNetworkItemClicked,
                    onLoadMore = onLoadMore
                )
            }
        }

    }
}

private fun toggleBlockchain(
    blockchain: Blockchain,
    blockchainsAddedOnMainScreen: List<Blockchain>,
    addedBlockchainsState: MutableState<List<Blockchain>>,
    addedTokens: List<TokenWithBlockchain>
) {
    val isTryingToRemove = addedBlockchainsState.value.contains(blockchain)
    val isAddedOnMainScreen = blockchainsAddedOnMainScreen.contains(blockchain)
    val isTokenWithSameBlockchainFound = addedTokens.any { it.blockchain == blockchain }

    if (isTryingToRemove) {
        if (isTokenWithSameBlockchainFound) {
            store.dispatchDialogShow(WalletDialog.TokensAreLinkedDialog(
                currencyTitle = blockchain.name,
                currencySymbol = blockchain.currency
            ))
        } else {
            if (isAddedOnMainScreen) {
                store.dispatchDialogShow(WalletDialog.RemoveWalletDialog(
                    currencyTitle = blockchain.name,
                    onOk = { addedBlockchainsState.removeAndNotify(blockchain) }
                ))
            } else {
                addedBlockchainsState.removeAndNotify(blockchain)
            }
        }
    } else {
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

    if (isTryingToRemove) {
        if (isAddedOnMainScreen) {
            store.dispatchDialogShow(WalletDialog.RemoveWalletDialog(
                currencyTitle = token.token.name,
                onOk = { addedTokensState.removeAndNotify(token) }
            ))
        } else {
            addedTokensState.removeAndNotify(token)
        }
    } else {
        if (isUnsupportedToken) {
            store.dispatchDialogShow(AppDialog.SimpleOkDialogRes(
                headerId = R.string.common_warning,
                messageId = R.string.alert_manage_tokens_unsupported_message,
            ))
        } else {
            addedTokensState.addAndNotify(token)
        }
    }
}

@Composable
fun SaveChangesButton(keyboardState: Keyboard, onSaveChanges: () -> Unit) {

    val padding = if (keyboardState is Keyboard.Opened) {
        LocalContext.current.pixelsToDp(keyboardState.height)
    } else {
        0
    }

    ExtendedFloatingActionButton(
        text = {
            Text(
                text = stringResource(id = R.string.common_save_changes),
            )
        },
        onClick = onSaveChanges,
        backgroundColor = colorResource(id = R.color.accent),
        contentColor = Color.White,
        modifier = Modifier.padding(bottom = padding.dp)
    )
}