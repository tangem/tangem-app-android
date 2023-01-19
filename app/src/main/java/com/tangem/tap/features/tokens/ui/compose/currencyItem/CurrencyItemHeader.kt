package com.tangem.tap.features.tokens.ui.compose.currencyItem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.tap.common.extensions.getGreyedOutIconRes
import com.tangem.tap.domain.tokens.Currency
import com.tangem.tap.features.tokens.redux.TokenWithBlockchain
import com.tangem.tap.features.tokens.ui.compose.CurrencyPlaceholderIcon
import com.tangem.tap.features.tokens.ui.compose.fullName
import com.tangem.wallet.R

@Suppress("LongMethod", "MagicNumber")
@Composable
fun CurrencyItemHeader(
    currency: Currency,
    addedTokens: List<TokenWithBlockchain>,
    addedBlockchains: List<Blockchain>,
    isExpanded: Boolean,
    onCurrencyClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCurrencyClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterVertically)
                .clip(RoundedCornerShape(6.dp)),
        ) {
            SubcomposeAsyncImage(
                modifier = Modifier
                    .size(46.dp),
                model = ImageRequest.Builder(LocalContext.current)
                    .data(currency.iconUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = currency.id,
                loading = { CurrencyPlaceholderIcon(currency.id) },
                error = { CurrencyPlaceholderIcon(currency.id) },
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
        ) {
            Text(
                text = currency.fullName,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF1C1C1E),
            )
            Spacer(modifier = Modifier.size(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
            ) {
                if (isExpanded) {
                    Text(
                        modifier = Modifier.align(Alignment.TopStart),
                        text = stringResource(id = R.string.currency_subtitle_expanded),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF8E8E93),
                    )
                } else {
                    NetworksRow(
                        currency = currency,
                        addedTokens = addedTokens,
                        addedBlockchains = addedBlockchains,
                    )
                }
            }
        }

        val expandCollapseArrow = if (isExpanded) R.drawable.ic_arrow_extended else R.drawable.ic_arrow_collapsed
        Image(
            painter = painterResource(id = expandCollapseArrow),
            contentDescription = null,
            Modifier
                .padding(20.dp)
                .align(Alignment.CenterVertically),
        )
    }
}

@Composable
private fun NetworksRow(
    currency: Currency,
    addedTokens: List<TokenWithBlockchain>,
    addedBlockchains: List<Blockchain>,
) {
    Row {
        if (currency.contracts.isNotEmpty()) {
            currency.contracts.map { contract ->
                if (contract.address == null) {
                    BlockchainNetworkItem(
                        blockchain = contract.blockchain,
                        isMainNetwork = true,
                        addedBlockchains = addedBlockchains,
                    )
                } else {
                    val isAdded = addedTokens.map { it.token.contractAddress }.contains(contract.address)
                    val iconResId = if (isAdded) {
                        getActiveIconRes(contract.blockchain.id)
                    } else {
                        contract.blockchain.getGreyedOutIconRes()
                    }
                    Image(
                        modifier = Modifier
                            .size(20.dp),
                        painter = painterResource(id = iconResId),
                        contentDescription = null,
                    )
                    Spacer(Modifier.size(5.dp))
                }
            }
        } else {
            BlockchainNetworkItem(
                blockchain = Blockchain.fromNetworkId(currency.id),
                isMainNetwork = false,
                addedBlockchains = addedBlockchains,
            )
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun BlockchainNetworkItem(
    blockchain: Blockchain?,
    isMainNetwork: Boolean,
    addedBlockchains: List<Blockchain>,
) {
    val added = addedBlockchains.contains(blockchain)
    val icon = if (added) blockchain?.let { getActiveIconRes(it.id) } else blockchain?.getGreyedOutIconRes()
    if (icon != null) {
        Box(
            Modifier
                .size(20.dp),
        ) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = null,
                Modifier
                    .size(20.dp),
            )
            if (isMainNetwork) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1ACE80)),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.size(5.dp))
    }
}
