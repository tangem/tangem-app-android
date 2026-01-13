package com.tangem.features.hotwallet.forgetwallet.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.hotwallet.forgetwallet.entity.ForgetWalletUM

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ForgetWalletContent(state: ForgetWalletUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        TangemTopAppBar(
            modifier = Modifier.statusBarsPadding(),
            startButton = TopAppBarButtonUM.Back(state.onBackClick),
            title = TextReference.EMPTY,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Icon(
                painter = painterResource(R.drawable.ic_attention_72),
                contentDescription = null,
                tint = TangemTheme.colors.icon.warning,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResourceSafe(R.string.common_attention),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResourceSafe(R.string.hw_remove_wallet_attention_description),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Spacer(modifier = Modifier.height(48.dp))
        CheckboxItem(
            modifier = Modifier.padding(horizontal = 16.dp),
            checked = state.isCheckboxChecked,
            onCheckedChange = state.onCheckboxClick,
            text = stringResourceSafe(R.string.hw_remove_wallet_warning_access),
        )
        Spacer(modifier = Modifier.height(32.dp))
        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            text = stringResourceSafe(R.string.hw_remove_wallet_action_forget_title),
            onClick = state.onForgetWalletClick,
            enabled = state.isForgetButtonEnabled,
        )
    }
}

@Composable
private fun CheckboxItem(checked: Boolean, onCheckedChange: () -> Unit, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        IconToggleButton(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
        ) {
            AnimatedContent(
                targetState = checked,
                label = "Update checked state",
            ) { isChecked ->
                Icon(
                    painter = painterResource(
                        if (isChecked) {
                            R.drawable.ic_accepted_20
                        } else {
                            R.drawable.ic_unticked_20
                        },
                    ),
                    contentDescription = null,
                    tint = if (isChecked) {
                        TangemTheme.colors.control.checked
                    } else {
                        TangemTheme.colors.icon.secondary
                    },
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewForgetWalletContent() {
    TangemThemePreview {
        ForgetWalletContent(
            state = ForgetWalletUM(
                onBackClick = {},
                isCheckboxChecked = false,
                onCheckboxClick = {},
                onForgetWalletClick = {},
                isForgetButtonEnabled = false,
            ),
        )
    }
}