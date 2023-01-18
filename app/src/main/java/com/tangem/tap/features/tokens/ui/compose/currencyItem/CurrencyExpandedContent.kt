package com.tangem.tap.features.tokens.ui.compose.currencyItem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.domain.tokens.Contract
import com.tangem.tap.domain.tokens.Currency
import com.tangem.tap.features.tokens.redux.ContractAddress
import com.tangem.tap.features.tokens.redux.TokenWithBlockchain

@Suppress("LongParameterList")
@Composable
fun CurrencyExpandedContent(
    currency: Currency,
    addedTokens: List<TokenWithBlockchain>,
    addedBlockchains: List<Blockchain>,
    allowToAdd: Boolean,
    isExpanded: Boolean,
    onAddCurrencyToggle: (Currency, TokenWithBlockchain?) -> Unit,
    onNetworkItemClick: (ContractAddress) -> Unit,
) {
    AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        val blockchains = currency.contracts.map { it.blockchain }
        Column(modifier = Modifier.fillMaxWidth()) {
            blockchains.mapIndexed { index, blockchain ->
                val currencyContract = currency.contracts
                    .firstOrNull { it.blockchain == blockchain }
                    ?: return@mapIndexed

                val added = if (currencyContract.address != null) {
                    addedTokens.any(currencyContract::isInclude)
                } else {
                    addedBlockchains.contains(blockchain)
                }
                NetworkItem(
                    currency = currency,
                    contract = currencyContract,
                    blockchain = blockchain,
                    allowToAdd = allowToAdd,
                    added = added,
                    index = index,
                    size = blockchains.size,
                    onAddCurrencyToggle = onAddCurrencyToggle,
                    onNetworkItemClick = onNetworkItemClick,
                )
            }
        }
    }
}

private fun Contract.isInclude(addedToken: TokenWithBlockchain): Boolean =
    address == addedToken.token.contractAddress && blockchain == addedToken.blockchain
