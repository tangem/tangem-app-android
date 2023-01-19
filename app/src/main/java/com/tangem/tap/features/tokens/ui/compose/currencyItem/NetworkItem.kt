package com.tangem.tap.features.tokens.ui.compose.currencyItem

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.tap.common.extensions.fullNameWithoutTestnet
import com.tangem.tap.common.extensions.getGreyedOutIconRes
import com.tangem.tap.common.extensions.getNetworkName
import com.tangem.tap.domain.tokens.Contract
import com.tangem.tap.domain.tokens.Currency
import com.tangem.tap.features.tokens.redux.ContractAddress
import com.tangem.tap.features.tokens.redux.TokenWithBlockchain
import com.tangem.tap.features.tokens.ui.compose.CurrencyPlaceholderIcon

@Suppress("LongParameterList", "LongMethod", "MagicNumber")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NetworkItem(
    currency: Currency,
    contract: Contract,
    blockchain: Blockchain,
    allowToAdd: Boolean,
    added: Boolean,
    index: Int,
    size: Int,
    onAddCurrencyToggle: (Currency, TokenWithBlockchain?) -> Unit,
    onNetworkItemClick: (ContractAddress) -> Unit,
) {
    val rowHeight = 53.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(rowHeight)
            .combinedClickable(
                enabled = allowToAdd,
                onLongClick = {
                    contract.address?.let { onNetworkItemClick(it) }
                },
                onClick = {},
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ),
    ) {
        Box(
            Modifier
                .width(78.dp)
                .padding(start = 38.dp),
        ) {
            CurrencyItemArrow(
                rowHeight = rowHeight,
                isLastArrow = index == size - 1,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterVertically),
        ) {
            SubcomposeAsyncImage(
                model = if (added) getActiveIconRes(blockchain.id) else blockchain.getGreyedOutIconRes(),
                contentDescription = blockchain.fullName,
                loading = { CurrencyPlaceholderIcon(blockchain.id) },
                error = { CurrencyPlaceholderIcon(blockchain.id) },
                modifier = Modifier
                    .size(20.dp),
            )
            if (contract.address == null) {
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
        Spacer(Modifier.width(6.dp))
        Text(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            text = prepareNetworkNameSpannableText(
                blockchain = blockchain,
                contractAddress = contract.address,
            ),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = if (added) Color.Black else Color(0xFF848488),
        )

        if (allowToAdd) {
            val token = if (contract.address != null) {
                Token(
                    id = currency.id,
                    name = currency.name,
                    symbol = currency.symbol,
                    contractAddress = contract.address,
                    decimals = contract.decimalCount!!,
                )
            } else {
                null
            }
            val tokenWithBlockchain = token?.let { TokenWithBlockchain(it, contract.blockchain) }

            val currencyToSave = if (contract.address == null) {
                currency.copy(id = contract.networkId)
            } else {
                currency
            }

            Switch(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 16.dp, end = 16.dp),
                checked = added,
                onCheckedChange = { onAddCurrencyToggle(currencyToSave, tokenWithBlockchain) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF1ACE80),
                ),
            )
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun prepareNetworkNameSpannableText(
    blockchain: Blockchain,
    contractAddress: String?,
): AnnotatedString {
    val blockchainName = blockchain.fullNameWithoutTestnet.uppercase()
    val additionalText =
        if (contractAddress == null) "MAIN" else blockchain.getNetworkName().uppercase()

    val text = "$blockchainName $additionalText"

    val startOfAdditionalText =
        if (additionalText.isNotBlank()) text.indexOf(additionalText) else text.length

    val spanStyles = listOf(
        AnnotatedString.Range(
            SpanStyle(
                fontWeight = FontWeight.Normal,
                color = if (contractAddress != null) Color(0xFF8E8E93) else Color(0xFF1ACE80),
            ),
            start = startOfAdditionalText,
            end = text.length,
        ),
    )
    return AnnotatedString(text = text, spanStyles = spanStyles)
}
