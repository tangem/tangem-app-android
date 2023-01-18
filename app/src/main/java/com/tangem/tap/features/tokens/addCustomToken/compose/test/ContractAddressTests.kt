package com.tangem.tap.features.tokens.addCustomToken.compose.test

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.VoidCallback
import com.tangem.domain.common.form.Field
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenAction
import com.tangem.domain.redux.domainStore

/**
 * Created by Anton Zhilenkov on 29/05/2022.
 */
@Composable
fun ContractAddressTests(
    onItemClick: VoidCallback,
) {
    val casesInfo = listOf(
        "USDC on ETH" to "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",
        "BUSD on ETH" to "0x4fabb145d64652a948d72533023f6e7a623c7c53",
        "ETH on AVALANCHE" to "0xf20d962a6c8f70c731bd838a3a388d7d48fa6e15",
        "USDC on ETH (invalid - cut address)" to "0xa0b86991c6218b36c1d1",
        "Custom EVM" to "0x1111111111111111112111111111111111111113",
        "Supported by several networks" to "0xa1faa113cbe53436df28ff0aee54275c13b40975",
        "Invalid" to "!@#_  _-%%^&&*((){P P2iOWsdfFQLA",
    )
    CasesListContent(casesInfo, onItemClick)
}

@Composable
fun SolanaAddressTests(
    onItemClick: VoidCallback,
) {
    val casesInfo = listOf(
        "USDT (full)" to "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB",
        "USDT (valid - 2/3 of address)" to "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8Ben",
        "USDT (invalid - 1/3 of address)" to "Es9vMFrzaCERmJ",
        "ETH (full)" to "2FPyTwcZLUg1MDrwsyoP4D6s1tM7hAkHYRjkNb5w6Pxk",
    )
    CasesListContent(casesInfo, onItemClick)
}

@Composable
private fun CasesListContent(
    casesList: List<Pair<String, String>>,
    onItemClick: VoidCallback,
) {
    LazyColumn(
        content = {
            item {
                Row {
                    ResetContractAddressButton(onItemClick)
                    Text("", modifier = Modifier.weight(1f))
                    ResetAllFieldsButton(onItemClick)
                }
                Divider()
            }
            items(casesList.size) {
                val (info, address) = casesList[it]
                ContractAddressButton(info, address, onItemClick)
            }
        },
    )
}

@Composable
fun ResetAllFieldsButton(onItemClick: VoidCallback) {
    ActionButton(
        name = "Reset",
        onClick = {
            onItemClick()
            domainStore.dispatch(AddCustomTokenAction.OnTokenContractAddressChanged(Field.Data("", false)))
            domainStore.dispatch(AddCustomTokenAction.OnTokenNetworkChanged(Field.Data(Blockchain.Unknown, false)))
            domainStore.dispatch(AddCustomTokenAction.OnTokenNameChanged(Field.Data("", false)))
            domainStore.dispatch(AddCustomTokenAction.OnTokenSymbolChanged(Field.Data("", false)))
            domainStore.dispatch(AddCustomTokenAction.OnTokenDecimalsChanged(Field.Data("", false)))
            domainStore.dispatch(
                AddCustomTokenAction.OnTokenDerivationPathChanged(
                    Field.Data(
                        Blockchain.Unknown,
                        false,
                    ),
                ),
            )
        },
    )
}

@Composable
fun ResetContractAddressButton(
    onItemClick: VoidCallback,
) {
    ActionButton(
        name = "Set empty address",
        onClick = {
            onItemClick()
            domainStore.dispatch(AddCustomTokenAction.OnTokenContractAddressChanged(Field.Data("", false)))
        },
    )
}

@Composable
private fun ContractAddressButton(
    name: String,
    address: String,
    onItemClick: VoidCallback,
) {
    ActionButton(
        modifier = Modifier.fillMaxWidth(),
        name = name,
        onClick = {
            onItemClick()
            domainStore.dispatch(AddCustomTokenAction.OnTokenContractAddressChanged(Field.Data(address, false)))
        },
    )
}

@Composable
fun ActionButton(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        modifier = modifier.padding(horizontal = 8.dp),
        onClick = onClick,
    ) { Text(name, fontSize = 12.sp) }
}
