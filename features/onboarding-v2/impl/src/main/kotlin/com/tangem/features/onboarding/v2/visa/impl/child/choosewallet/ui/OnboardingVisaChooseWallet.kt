package com.tangem.features.onboarding.v2.visa.impl.child.choosewallet.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.components.rows.RowContentContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.selectedBorder
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.visa.impl.child.choosewallet.OnboardingVisaChooseWalletComponent
import com.tangem.features.onboarding.v2.visa.impl.child.choosewallet.ui.state.OnboardingVisaChooseWalletUM
import com.tangem.features.onboarding.v2.visa.impl.child.choosewallet.ui.state.SelectableChainRowUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun OnboardingVisaChooseWallet(state: OnboardingVisaChooseWalletUM, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(top = 12.dp)
                .padding(horizontal = 16.dp),
        ) {
            Notification(
                config = NotificationConfig(
                    subtitle = resourceReference(R.string.visa_onboarding_approve_wallet_selector_notification_message),
                    iconResId = R.drawable.ic_alert_circle_24,
                ),
                iconTint = TangemTheme.colors.icon.accent,
            )

            Text(
                modifier = Modifier
                    .padding(
                        top = 12.dp,
                        bottom = 8.dp,
                        start = 12.dp,
                        end = 12.dp,
                    ),
                text = stringResourceSafe(R.string.visa_onboarding_wallet_list_header),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )

            SelectableChainRows(
                items = state.options,
                selected = state.selectedOption,
                onClick = state.onOptionSelected,
            )
        }

        PrimaryButton(
            modifier = Modifier
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            text = stringResourceSafe(R.string.common_continue),
            onClick = state.onContinueClick,
        )
    }
}

@Composable
private fun SelectableChainRows(
    items: ImmutableList<SelectableChainRowUM>,
    selected: SelectableChainRowUM?,
    onClick: (SelectableChainRowUM) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        items.forEach { item ->
            SelectableChainRow(
                state = item,
                selected = item == selected,
                onClick = { onClick(item) },
            )
        }
    }
}

@Composable
private fun SelectableChainRow(
    state: SelectableChainRowUM,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RowContentContainer(
        modifier = modifier
            .heightIn(min = 48.dp)
            .selectedBorder(selected)
            .clickable(onClick = onClick)
            .padding(12.dp),
        icon = {
            Icon(
                imageVector = ImageVector.vectorResource(id = state.icon),
                tint = if (selected) TangemTheme.colors.icon.primary1 else TangemTheme.colors.icon.informative,
                contentDescription = null,
            )
        },
        text = {
            Text(
                text = state.text.resolveReference(),
                style = TangemTheme.typography.button,
                color = if (selected) TangemTheme.colors.text.primary1 else TangemTheme.colors.text.secondary,
            )
        },
        action = {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreview {
        OnboardingVisaChooseWallet(
            state = OnboardingVisaChooseWalletUM(
                options = persistentListOf(
                    SelectableChainRowUM(
                        event = OnboardingVisaChooseWalletComponent.Params.Event.TangemWallet,
                        icon = R.drawable.ic_tangem_24,
                        text = TextReference.Str("Tangem Wallet"),
                    ),
                    SelectableChainRowUM(
                        event = OnboardingVisaChooseWalletComponent.Params.Event.OtherWallet,
                        icon = R.drawable.ic_wallet_filled_24,
                        text = TextReference.Str("Other Wallet"),
                    ),
                ),
                selectedOption = SelectableChainRowUM(
                    event = OnboardingVisaChooseWalletComponent.Params.Event.TangemWallet,
                    icon = R.drawable.ic_tangem_24,
                    text = TextReference.Str("Tangem Wallet"),
                ),
            ),
        )
    }
}