package com.tangem.tap.features.tokens.ui.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FabPosition
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.tap.common.compose.Keyboard
import com.tangem.tap.common.compose.keyboardAsState
import com.tangem.tap.common.extensions.pixelsToDp
import com.tangem.tap.domain.tokens.Currency
import com.tangem.tap.domain.tokens.fromNetworkId
import com.tangem.tap.features.tokens.redux.TokensState
import com.tangem.tap.store
import com.tangem.wallet.R

@Composable
fun CurrenciesScreen(
    tokensState: MutableState<TokensState> = mutableStateOf(store.state.tokensState),
    searchInput: MutableState<String>,
    onSaveChanges: (List<Token>, List<Blockchain>) -> Unit
) {

    val addedTokensState = remember { mutableStateOf(tokensState.value.addedTokens) }
    val addedBlockchainsState = remember { mutableStateOf(tokensState.value.addedBlockchains) }

    val isKeyboardOpen by keyboardAsState()

    val onAddCurrencyToggleClick = { currency: Currency, contract: Token? ->
        if (contract != null) {
            val mutableList = addedTokensState.value.toMutableList()
            if (mutableList.contains(contract)) {
                mutableList.remove(contract)
            } else {
                mutableList.add(contract)
            }
            addedTokensState.value = mutableList
        } else {
            val blockchain = Blockchain.fromNetworkId(currency.id)
            val mutableList = addedBlockchainsState.value.toMutableList()
            if (mutableList.contains(blockchain)) {
                mutableList.remove(blockchain)
            } else {
                mutableList.add(blockchain)
            }
            addedBlockchainsState.value = mutableList
        }
    }

    Scaffold(
//        topBar =,
        floatingActionButton = {
            if (tokensState.value.allowToAdd) SaveChangesButton(isKeyboardOpen) {
                onSaveChanges(addedTokensState.value, addedBlockchainsState.value)
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) {

        ListOfCurrencies(
            currencies = tokensState.value.currencies,
            nonRemovableTokens = tokensState.value.nonRemovableTokens,
            nonRemovableBlockchains = tokensState.value.nonRemovableBlockchains,
            addedTokens = addedTokensState.value,
            addedBlockchains = addedBlockchainsState.value,
            searchInput = searchInput.value,
            allowToAdd = tokensState.value.allowToAdd,
            onAddCurrencyToggled = onAddCurrencyToggleClick
        )
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