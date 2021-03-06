package com.tangem.tap.features.tokens.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.tap.common.extensions.getGreyedOutIconRes
import com.tangem.tap.common.extensions.getRoundIconRes
import com.tangem.tap.domain.tokens.Currency
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

        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(currency.iconUrl)
                .crossfade(true)
                .build(),
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
                text = currency.fullName,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF1C1C1E),
            )
            Spacer(modifier = Modifier.size(6.dp))
            Row {
                if (!currency.contracts.isNullOrEmpty()) {
                    currency.contracts.map { contract ->
                        if (contract.address == null) {
                            BlockchainNetworkItem(
                                blockchain = contract.blockchain,
                                isMainNetwork = true,
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
                        isMainNetwork = false,
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
    isMainNetwork: Boolean,
    addedBlockchains: List<Blockchain>
) {
    val added = addedBlockchains.contains(blockchain)
    val icon =
        if (added) blockchain?.getRoundIconRes() else blockchain?.getGreyedOutIconRes()
    if (icon != null) {
        Box(Modifier
            .size(20.dp)) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = null,
                Modifier
                    .size(20.dp)
            )
            if (isMainNetwork) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1ACE80))
                    )
                }
            }
        }
        Spacer(modifier = Modifier.size(5.dp))
    }
}