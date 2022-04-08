package com.tangem.tap.features.tokens.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.tap.common.extensions.getGreyedOutIconRes
import com.tangem.tap.common.extensions.getNetworkName
import com.tangem.tap.common.extensions.getRoundIconRes
import com.tangem.tap.domain.tokens.Contract
import com.tangem.tap.domain.tokens.Currency

@Composable
fun NetworkItem(
    currency: Currency, contract: Contract?,
    blockchain: Blockchain, allowToAdd: Boolean,
    added: Boolean, canBeRemoved: Boolean,
    onAddCurrencyToggled: (Currency, Token?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()

    ) {
        SubcomposeAsyncImage(
            model = if (added) blockchain.getRoundIconRes() else blockchain.getGreyedOutIconRes(),
            contentDescription = blockchain.fullName,
            loading = { CurrencyPlaceholderIcon(blockchain.id) },
            error = { CurrencyPlaceholderIcon(blockchain.id) },
            modifier = Modifier
                .clip(CircleShape)
                .padding(start = 8.dp, top = 16.dp, bottom = 16.dp, end = 6.dp)
                .size(20.dp)
                .align(Alignment.CenterVertically)
        )
        Row(modifier = Modifier
            .fillMaxHeight()
            .weight(1f)
            .align(Alignment.CenterVertically)) {
            Text(
                text = blockchain.name.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (added) Color.Black else Color(0xFF848488),
            )
            Spacer(modifier = Modifier.size(3.dp))
            Text(
                text = if (contract != null) blockchain.getNetworkName().uppercase() else "MAIN",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = if (contract != null) Color(0xFF8E8E93) else Color(0xFF1ACE80),
            )
        }

        val token = if (contract != null) {
            Token(
                name = currency.name, symbol = currency.symbol, contractAddress = contract.address,
                decimals = contract.decimalCount, blockchain = contract.blockchain
            )
        } else {
            null
        }
        if (allowToAdd) Switch(
            checked = added,
            enabled = canBeRemoved,
            onCheckedChange = { onAddCurrencyToggled(currency, token) },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF1ACE80)
            )
        )
    }
}