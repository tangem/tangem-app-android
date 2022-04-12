package com.tangem.tap.features.tokens.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.domain.tokens.Currency
import com.tangem.tap.domain.tokens.fromNetworkId
import com.tangem.tap.features.tokens.redux.ContractAddress
import com.tangem.tap.features.tokens.redux.TokenWithBlockchain
import com.tangem.wallet.R

@Composable
fun ExpandedCurrencyItem(
    currency: Currency,
    nonRemovableTokens: List<ContractAddress>,
    nonRemovableBlockchains: List<Blockchain>,
    addedTokens: List<TokenWithBlockchain>,
    addedBlockchains: List<Blockchain>,
    allowToAdd: Boolean,
    onCurrencyClick: (String) -> Unit,
    onAddCurrencyToggled: (Currency, TokenWithBlockchain?) -> Unit,
    onNetworkItemClicked: (ContractAddress) -> Unit
) {
    val blockchain = Blockchain.fromNetworkId(currency.id)
    val iconRes = currency.iconUrl

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { onCurrencyClick(currency.id) })
        ) {
            SubcomposeAsyncImage(
                model = iconRes,
                contentDescription = currency.id,
                loading = { CurrencyPlaceholderIcon(currency.id) },
                error = { CurrencyPlaceholderIcon(currency.id) },
                modifier = Modifier
                    .clip(CircleShape)
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                    .size(46.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(top = 14.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = currency.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF1C1C1E),
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = stringResource(id = R.string.currency_subtitle_expanded),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF8E8E93),
                )
            }
            Image(
                painter = painterResource(id = R.drawable.ic_arrow_extended),
                contentDescription = null,
                Modifier
                    .padding(20.dp)
                    .align(Alignment.CenterVertically)
            )
        }

        val blockchains = currency.contracts?.map { it.blockchain } ?: listOfNotNull(
            Blockchain.fromNetworkId(currency.id)
        )

        Row {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
            ) {
                if (blockchains.size > 1) {
                    val dividerHeight = if (blockchains.size == 2) {
                        60
                    } else if (blockchains.size == 3) {
                        110
                    } else {
                        43 * blockchains.size
                    }
                    Divider(
                        color = Color(0xFFDEDEDE),
                        modifier = Modifier
                            .height(dividerHeight.dp)
                            .padding(start = 37.5.dp)
                            .width(1.dp)
                    )
                }

                Column {
                    blockchains.map {
                        Image(
                            painter = painterResource(id = R.drawable.ic_link),
                            contentDescription = null,
                            Modifier
                                .padding(
                                    start = 37.dp,
                                )
                        )
                        Spacer(modifier = Modifier.size(13.5.dp))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            ) {
                blockchains.map { blockchain ->
                    val contract = currency.contracts?.firstOrNull { it.blockchain == blockchain }
                    val added = if (contract != null && contract.address != currency.symbol) {
                        addedTokens.map { it.token.contractAddress }.contains(contract.address)
                    } else {
                        addedBlockchains.contains(blockchain)
                    }
                    val canBeRemoved = if (contract != null && contract.address != currency.symbol) {
                        !nonRemovableTokens.contains(contract.address)
                    } else {
                        !nonRemovableBlockchains.contains(blockchain)
                    }
                    NetworkItem(
                        currency = currency,
                        contract = contract,
                        blockchain = blockchain, allowToAdd = allowToAdd,
                        added = added,
                        canBeRemoved = canBeRemoved,
                        onAddCurrencyToggled = onAddCurrencyToggled,
                        onNetworkItemClicked = onNetworkItemClicked,
                    )
                }
            }
        }
    }
}