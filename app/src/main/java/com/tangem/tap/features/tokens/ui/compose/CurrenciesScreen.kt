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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.tap.common.compose.Keyboard
import com.tangem.tap.common.compose.keyboardAsState
import com.tangem.tap.common.extensions.pixelsToDp
import com.tangem.tap.domain.TapWorkarounds.useOldStyleDerivation
import com.tangem.tap.domain.tokens.Currency
import com.tangem.tap.features.tokens.redux.ContractAddress
import com.tangem.tap.features.tokens.redux.TokenWithBlockchain
import com.tangem.tap.features.tokens.redux.TokensState
import com.tangem.tap.store
import com.tangem.wallet.R

@Composable
fun CurrenciesScreen(
    tokensState: MutableState<TokensState> = mutableStateOf(store.state.tokensState),
    searchInput: MutableState<String>,
    onSaveChanges: (List<TokenWithBlockchain>, List<Blockchain>) -> Unit,
    onNetworkItemClicked: (ContractAddress) -> Unit
) {

    val addedTokensState = remember { mutableStateOf(tokensState.value.addedTokens) }
    val addedBlockchainsState = remember { mutableStateOf(tokensState.value.addedBlockchains) }

    val isKeyboardOpen by keyboardAsState()

    val onAddCurrencyToggleClick = { currency: Currency, token: TokenWithBlockchain? ->
        if (token != null) {
            val mutableList = addedTokensState.value.toMutableList()
            if (mutableList.contains(token)) {
                mutableList.remove(token)
            } else {
                mutableList.add(token)
            }
            addedTokensState.value = mutableList
        } else {
            val blockchain = Blockchain.fromNetworkId(currency.id)
            val mutableList = addedBlockchainsState.value.toMutableList()
            if (mutableList.contains(blockchain)) {
                mutableList.remove(blockchain)
            } else {
                blockchain?.let { mutableList.add(blockchain) }
            }
            addedBlockchainsState.value = mutableList
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
            visible = tokensState.value.currencies.isEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ){
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
            visible = tokensState.value.currencies.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(1000)),
            exit = fadeOut(animationSpec = tween(1000))
        ) {
            Column {
                if (tokensState.value.scanResponse?.card?.useOldStyleDerivation == true) CurrenciesWarning()
                ListOfCurrencies(
                    currencies = tokensState.value.currencies,
                    nonRemovableTokens = tokensState.value.nonRemovableTokens,
                    nonRemovableBlockchains = tokensState.value.nonRemovableBlockchains,
                    addedTokens = addedTokensState.value,
                    addedBlockchains = addedBlockchainsState.value,
                    searchInput = searchInput.value,
                    allowToAdd = tokensState.value.allowToAdd,
                    onAddCurrencyToggled = onAddCurrencyToggleClick,
                    onNetworkItemClicked = onNetworkItemClicked
                )
            }
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
        backgroundColor = Color(0xFF1ACE80),
        contentColor = Color.White,
        modifier = Modifier.padding(bottom = padding.dp)
    )
}