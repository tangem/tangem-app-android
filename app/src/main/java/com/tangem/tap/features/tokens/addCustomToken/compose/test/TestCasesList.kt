package com.tangem.tap.features.tokens.addCustomToken.compose.test

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.tangem.common.extensions.VoidCallback
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R

/**
 * Created by Anton Zhilenkov on 12/04/2022.
 */
@Composable
fun TestCasesList(onItemClick: (TestCase) -> Unit) {
    if (!BuildConfig.TEST_ACTION_ENABLED) return

    Surface(color = colorResource(id = R.color.lightGray5)) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            listOf(TestCase.ContractAddress, TestCase.SolanaTokens)
                .map { case -> TestCaseListItem(testCase = case, onItemClick = { onItemClick(case) }) }
        }
    }
}

@Composable
fun TestCaseListItem(testCase: TestCase, onItemClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = testCase.description,
        )
        Button(
            onClick = onItemClick,
        ) { Text("Start") }
    }
}

enum class TestCase(val description: String, val content: @Composable (VoidCallback) -> Unit) {
    ContractAddress("Test contract address field", { ContractAddressTests(it) }),
    Auto("Test contract address field", { ContractAddressTests(it) }),
    SolanaTokens("Test Solana contract addresses", { SolanaAddressTests(it) }),
    ;
}
