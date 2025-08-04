package com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.buttons.actions.RoundedActionButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.feature.wallet.impl.R

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
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    item(key = ORGANIZE_BUTTON_CONTENT_TYPE, contentType = ORGANIZE_BUTTON_CONTENT_TYPE) {
        RoundedActionButton(
            modifier = modifier.testTag(MainScreenTestTags.ORGANIZE_TOKENS_BUTTON),
            config = ActionButtonConfig(
                text = resourceReference(id = R.string.organize_tokens_title),
                iconResId = R.drawable.ic_filter_24,
                onClick = onClick,
                enabled = isEnabled,
            ),
        )
    }
}