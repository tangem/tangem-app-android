package com.tangem.features.earn.impl.widgets.calculator.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_sign_minus_20
import com.tangem.core.ui.res.generated.icons.ic_sign_plus_20
import com.tangem.features.earn.impl.widgets.calculator.ui.state.EarnStepperUM

@Composable
internal fun EarnStepper(state: EarnStepperUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.label.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
            Text(
                text = state.value.resolveReference(),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
            )
        }
        StepperButton(
            icon = Icons.ic_sign_minus_20,
            isEnabled = state.isMinusEnabled,
            description = state.minusDescription.resolveReference(),
            onClick = state.onMinus,
        )
        StepperButton(
            icon = Icons.ic_sign_plus_20,
            isEnabled = state.isPlusEnabled,
            description = state.plusDescription.resolveReference(),
            onClick = state.onPlus,
        )
    }
}

@Composable
private fun StepperButton(icon: ImageVector, isEnabled: Boolean, description: String, onClick: () -> Unit) {
    TangemButton(
        variant = TangemButton.Variant.Secondary,
        size = TangemButton.Size.X10,
        isEnabled = isEnabled,
        iconStart = TangemIconUM.Icon(icon),
        contentDescription = description,
        onClick = onClick,
    )
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnStepperPreview() {
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.background(TangemTheme.colors3.bg.primary).padding(16.dp)) {
            EarnStepper(state = previewStepper(isMinusEnabled = true, isPlusEnabled = true))
            EarnStepper(state = previewStepper(isMinusEnabled = false, isPlusEnabled = true))
        }
    }
}

private fun previewStepper(isMinusEnabled: Boolean, isPlusEnabled: Boolean) = EarnStepperUM(
    label = stringReference("Initial balance"),
    value = stringReference("$2,500"),
    isMinusEnabled = isMinusEnabled,
    isPlusEnabled = isPlusEnabled,
    minusDescription = stringReference("Decrease Initial balance"),
    plusDescription = stringReference("Increase Initial balance"),
    onMinus = {},
    onPlus = {},
)