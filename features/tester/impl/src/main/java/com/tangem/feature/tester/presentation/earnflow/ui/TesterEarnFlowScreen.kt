package com.tangem.feature.tester.presentation.earnflow.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.ds2.toggle.TangemSwitch
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.earnflow.state.TesterEarnFlowUM
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TesterEarnFlowScreen(state: TesterEarnFlowUM) {
    val launch = state.launch
    if (launch != null) {
        TesterEarnFlowHost(params = launch, onClose = state.onFlowClose, modifier = Modifier.fillMaxSize())
        return
    }

    Scaffold(
        topBar = {
            AppBarWithBackButton(
                onBackClick = state.onBackClick,
                text = stringResourceSafe(id = R.string.earn_flow),
                modifier = Modifier.statusBarsPadding(),
            )
        },
        containerColor = TangemTheme.colors3.bg.primary,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Caption(text = state.target)
            Options(title = "Fake product", options = state.presets)
            Options(title = "Action", options = state.actions)
            state.switches.forEach { switch -> SwitchRow(switch) }
            PrimaryButton(
                text = "Open flow",
                onClick = state.onOpenClick,
                enabled = state.isOpenEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Options(title: String, options: ImmutableList<TesterEarnFlowUM.OptionUM>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Caption(text = title)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val modifier = Modifier.weight(1f)
                if (option.isSelected) {
                    PrimaryButton(text = option.title, onClick = option.onClick, modifier = modifier)
                } else {
                    SecondaryButton(text = option.title, onClick = option.onClick, modifier = modifier)
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(switch: TesterEarnFlowUM.SwitchUM) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = switch.title,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors3.text.primary,
            modifier = Modifier.weight(1f),
        )
        TangemSwitch(checked = switch.isChecked, onCheckedChange = switch.onCheckedChange)
    }
}

@Composable
private fun Caption(text: String) {
    Text(text = text, style = TangemTheme.typography.caption1, color = TangemTheme.colors3.text.secondary)
}