package com.tangem.tap.features.tokens.ui.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import com.tangem.tap.common.extensions.fullNameWithoutTestnet
import com.tangem.tap.common.extensions.getGreyedOutIconRes
import com.tangem.tap.common.extensions.getNetworkName
import com.tangem.tap.common.extensions.getRoundIconRes
import com.tangem.tap.domain.tokens.Contract
import com.tangem.tap.domain.tokens.Currency
import com.tangem.tap.features.tokens.redux.ContractAddress
import com.tangem.tap.features.tokens.redux.TokenWithBlockchain

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NetworkItem(
    currency: Currency, contract: Contract,
    blockchain: Blockchain, allowToAdd: Boolean,
    added: Boolean, canBeRemoved: Boolean,
    onAddCurrencyToggled: (Currency, TokenWithBlockchain?) -> Unit,
    onNetworkItemClicked: (ContractAddress) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = allowToAdd,
                onLongClick = {
                    contract.address?.let { onNetworkItemClicked(it) }
                },
                onClick = {},
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(start = 8.dp, top = 16.dp, bottom = 16.dp, end = 6.dp)
        ) {
            SubcomposeAsyncImage(
                model = if (added) blockchain.getRoundIconRes() else blockchain.getGreyedOutIconRes(),
                contentDescription = blockchain.fullName,
                loading = { CurrencyPlaceholderIcon(blockchain.id) },
                error = { CurrencyPlaceholderIcon(blockchain.id) },
                modifier = Modifier
                    .size(20.dp)
            )
            if (contract.address == null) {
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
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = prepareNetworkNameSpannableText(
                    blockchain = blockchain,
                    contractAddress = contract.address
                ),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = if (added) Color.Black else Color(0xFF848488),
            )
        }

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
            val tokenWithBlockchain =
                token?.let { TokenWithBlockchain(token, contract.blockchain) }

            val currencyToSave = if (contract.address == null) {
                currency.copy(id = contract.networkId)
            } else {
                currency
            }

            Switch(
                checked = added,
                enabled = canBeRemoved,
                onCheckedChange = { onAddCurrencyToggled(currencyToSave, tokenWithBlockchain) },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF1ACE80)
                )
            )
        }
    }
}


@Composable
fun prepareNetworkNameSpannableText(
    blockchain: Blockchain,
    contractAddress: String?
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
                color = if (contractAddress != null) Color(0xFF8E8E93) else Color(0xFF1ACE80)
            ),
            start = startOfAdditionalText,
            end = text.length
        )
    )
    return AnnotatedString(text = text, spanStyles = spanStyles)
}