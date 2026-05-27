package com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.buttons.actions.RoundedActionButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState

private const val ORGANIZE_BUTTON_CONTENT_TYPE = "OrganizeTokensButton"

/**
 * Organize tokens button
 *
 * @param onClick  callback is invoked when button is clicked
 * @param modifier modifier
 *
[REDACTED_AUTHOR]
 */
internal fun LazyListScope.organizeTokensButton(
    config: WalletTokensListState.OrganizeTokensButtonConfig,
    modifier: Modifier = Modifier,
) {
    item(key = ORGANIZE_BUTTON_CONTENT_TYPE, contentType = ORGANIZE_BUTTON_CONTENT_TYPE) {
        val testTag = if (config.textRes == R.string.main_add_and_manage_tokens) {
            MainScreenTestTags.ADD_AND_MANAGE_BUTTON
        } else {
            MainScreenTestTags.ORGANIZE_TOKENS_BUTTON
        }
        RoundedActionButton(
            modifier = modifier.testTag(testTag),
            config = ActionButtonConfig(
                text = resourceReference(id = config.textRes),
                iconResId = config.iconRes,
                onClick = config.onClick,
                isEnabled = config.isEnabled,
            ),
        )
    }
}