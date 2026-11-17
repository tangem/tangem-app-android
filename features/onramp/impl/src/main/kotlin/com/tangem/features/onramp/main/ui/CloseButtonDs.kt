package com.tangem.features.onramp.main.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R as CoreUiR
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cross_24
import com.tangem.core.ui.test.TopAppBarTestTags
import com.tangem.utils.annotations.RemoveWithToggle

@Composable
@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
internal fun CloseButtonDs(state: TopAppBarButtonUM, modifier: Modifier = Modifier) {
    val closeDescription = stringResourceSafe(CoreUiR.string.common_close)
    RoundGlassSurface(
        modifier = modifier
            .size(44.dp)
            .semantics {
                role = Role.Button
                contentDescription = closeDescription
            }
            .testTag(TopAppBarTestTags.CLOSE_BUTTON),
        enabled = state.isEnabled,
        onClick = state.onClicked,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = Icons.ic_cross_24,
            tint = TangemTheme.colors3.icon.primary,
            contentDescription = null,
        )
    }
}