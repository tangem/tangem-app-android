package com.tangem.tap.features.tokens.addCustomToken.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.VoidCallback
import com.tangem.common.services.Result
import com.tangem.domain.common.form.Field
import com.tangem.domain.features.addCustomToken.TangemTechServiceManager
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenAction
import com.tangem.domain.redux.domainStore
import com.tangem.network.api.tangemTech.TangemTechService
import com.tangem.wallet.BuildConfig
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
@Composable
fun AddCustomTokenDebugActions() {
    if (!BuildConfig.DEBUG) return

    Column() {
        // deep test
//        ActionRow("Invalid field value") { InvalidFieldValue() }
//        ActionRow("Active(true)") { ActiveTrue() }
//        ActionRow("Active(false) && decimalCount != null") { ActiveFalseDecimals() }
//        ActionRow("In several networks") { InSeveralNetworks() }
//        ActionRow("Address not found") { UnknownContracts() }

        // test
        ActionRow("All in one") { AllInOne() }

        // Any action
//        ActionRow("CustomActions -  find tokens active=false, decimals != null") { CustomActions() }
    }
}

@Composable
private fun AllInOne() {
    // validation error
    ContractAddressButton(
        name = "invalid",
        address = "unk"
    )
    // active = true
    ContractAddressButton(
        name = "true",
        address = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
    )
    // active = false, decimalCount != null
    ContractAddressButton(
        name = "false",
        address = "0x2147efff675e4a4ee1c2f918d181cdbd7a8e208f"
    )
    // more than one network
    ContractAddressButton(
        name = ">1 network",
        address = "0xa1faa113cbe53436df28ff0aee54275c13b40975"
    )
    // unknown
    ContractAddressButton(
        name = "unk",
        address = "0x1111111111111111112111111111111111111113"
    )
}

@Composable
private fun InvalidFieldValue() {
    ContractAddressButton(
        name = "unk",
        address = "unk"
    )
    ContractAddressButton(
        name = "someText",
        address = "someText"
    )
    ContractAddressButton(
        name = "alskdml...fa s",
        address = "alskdmlasd asdln alsdknflasd flasd fa s"
    )
}

@Composable
private fun ActiveTrue() {
    ContractAddressButton(
        name = "USDC- Ethereum",
        address = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
    )
    ContractAddressButton(
        name = "USDT- Avalanche",
        address = "0xc7198437980c041c805a1edcba50c1ce5db95118"
    )
    ContractAddressButton(
        name = "VID- Fantom",
        address = "0x922d641a426dcffaef11680e5358f34d97d112e1"
    )
}

@Composable
private fun ActiveFalseDecimals() {
    ContractAddressButton(
        name = "ALPHA- avalanche",
        address = "0x2147efff675e4a4ee1c2f918d181cdbd7a8e208f"
    )
    ContractAddressButton(
        name = "BETA- avalanche",
        address = "0x511d35c52a3c244e7b8bd92c0c297755fbd89212"
    )
}

@Composable
private fun InSeveralNetworks() {
    ContractAddressButton(
        name = "ALPHA- Eth,Bsc",
        address = "0xa1faa113cbe53436df28ff0aee54275c13b40975"
    )
    ContractAddressButton(
        name = "BETA- Eth,Bsc",
        address = "0xbe1a001fe942f96eea22ba08783140b9dcc09d28"
    )
}

@Composable
private fun UnknownContracts() {
    ContractAddressButton(
        name = "0x11...113",
        address = "0x1111111111111111112111111111111111111113"
    )
    ContractAddressButton(
        name = "0xc7...111",
        address = "0xc7198437980c041c805a1edcba50c1ce5db95111"
    )
}

@Composable
private fun CustomActions() {

    CustomActionButton(
        name = "Find tokens in several networks",
        action = {
            val manager = TangemTechServiceManager(TangemTechService())
            val currencies = manager.tokens()
            val asdfsd = mutableMapOf<String, MutableList<Any>>()
            val contractAddresses = currencies.mapNotNull { currency ->
                currency.contracts?.map { it.address }
            }.flatten()
            contractAddresses.take(500).forEachIndexed() { index, address ->
                when (val result = manager.checkAddress(address)) {
                    is Result.Success -> {
                        val contractList = mutableListOf<Any>()
                        result.data.forEach { token ->
                            token.contracts.forEach { contract ->
                                if (!contract.active && contract.decimalCount != null) {
                                    contractList.add(contract)
                                }
                            }
                        }
                        if (contractList.isNotEmpty()) {
                            val list = asdfsd[address] ?: mutableListOf()
                            list.addAll(contractList)
                            asdfsd[address] = list
                        }
                        Timber.e("Success. handle $index item from size ${contractAddresses.size}. Result = ${asdfsd.size}")
                    }
                    is Result.Failure -> {}
                }
            }
            val result = asdfsd.filter { it.value.size > 1 }
            if (result.isEmpty()) return@CustomActionButton
        }
    )
}

@Composable
private fun ActionRow(
    name: String,
    content: @Composable () -> Unit
) {
    Column() {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = name,
            fontSize = 14.sp
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) { content() }
    }

}

@Composable
private fun ActionButton(
    name: String,
    onClick: VoidCallback,
) {
    Button(
        modifier = Modifier.padding(horizontal = 4.dp),
        onClick = onClick
    ) { Text(name, fontSize = 8.sp) }
}

@Composable
private fun ContractAddressButton(
    name: String,
    address: String,
) {
    ActionButton(name = name) {
        resetTokenValues()
        domainStore.dispatch(AddCustomTokenAction.OnTokenContractAddressChanged(Field.Data(address, false)))
    }
}

@Composable
private fun CustomActionButton(
    name: String,
    action: suspend () -> Unit
) {
    val startValue = 0
    val anyValue = remember { mutableStateOf(startValue) }
    LaunchedEffect(key1 = anyValue.value, block = {
        if (anyValue.value != startValue) {
            action()
        }
    })

    ActionButton(name) { anyValue.value = anyValue.value + 1 }
}

private fun resetTokenValues() {
    domainStore.dispatch(AddCustomTokenAction.OnTokenNetworkChanged(Field.Data(Blockchain.Unknown, false)))
//    domainStore.dispatch(AddCustomTokenAction.OnTokenNameChanged(Field.Data("", false)))
//    domainStore.dispatch(AddCustomTokenAction.OnTokenSymbolChanged(Field.Data("", false)))
//    domainStore.dispatch(AddCustomTokenAction.OnTokenDecimalsChanged(Field.Data("", false)))
}