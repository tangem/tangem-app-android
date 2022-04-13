package com.tangem.tap.features.tokens.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.common.extensions.getGreyedOutIconRes
import com.tangem.tap.common.extensions.getRoundIconRes
import com.tangem.tap.domain.tokens.Currency
import com.tangem.tap.domain.tokens.fromNetworkId
import com.tangem.tap.features.tokens.redux.TokenWithBlockchain
import com.tangem.wallet.R

@Composable
fun CollapsedCurrencyItem(
    currency: Currency,
    addedTokens: List<TokenWithBlockchain>,
    addedBlockchains: List<Blockchain>,
    onCurrencyClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onCurrencyClick(currency.id) })
    ) {
        val blockchain = Blockchain.fromNetworkId(currency.id)
        val iconRes = currency.iconUrl

        SubcomposeAsyncImage(
            model = iconRes,
            contentDescription = currency.id,
            loading = { CurrencyPlaceholderIcon(currency.id) },
            error = { CurrencyPlaceholderIcon(currency.id) },
            modifier = Modifier
                .clip(CircleShape)
                .padding(16.dp)
                .size(46.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = currency.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF1C1C1E),
            )
            Spacer(modifier = Modifier.size(6.dp))
            Row {
                if (!currency.contracts.isNullOrEmpty()) {
                    currency.contracts.map { contract ->
                        if (contract.address == currency.symbol) {
                            BlockchainNetworkItem(
                                blockchain = Blockchain.fromNetworkId(contract.networkId),
                                addedBlockchains = addedBlockchains
                            )
                        } else {
                            val added =
                                addedTokens.map { it.token.contractAddress }
                                    .contains(contract.address)
                            val icon = if (added) {
                                contract.blockchain.getRoundIconRes()
                            } else {
                                contract.blockchain.getGreyedOutIconRes()
                            }
                            Image(
                                painter = painterResource(id = icon),
                                contentDescription = null,
                                Modifier
                                    .size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(5.dp))
                        }

                    }
                } else {
                    BlockchainNetworkItem(
                        blockchain = blockchain,
                        addedBlockchains = addedBlockchains
                    )
                }
            }
        }
        Image(
            painter = painterResource(id = R.drawable.ic_arrow_collapsed),
            contentDescription = null,
            Modifier
                .padding(20.dp)
                .align(Alignment.CenterVertically)

        )
    }
}

@Composable
fun BlockchainNetworkItem(
    blockchain: Blockchain?,
    addedBlockchains: List<Blockchain>
) {
    val added = addedBlockchains.contains(blockchain)
    val icon =
        if (added) blockchain?.getRoundIconRes() else blockchain?.getGreyedOutIconRes()
    if (icon != null) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = null,
            Modifier
                .size(20.dp)
        )
        Spacer(modifier = Modifier.size(5.dp))
    }
}