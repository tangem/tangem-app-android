package com.tangem.core.ui.components.dropdownmenu

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.PopUpMenuTestTags

@Suppress("ComposableEventParameterNaming")
@Composable
fun TangemDropdownItem(item: TangemDropdownMenuItem, dismissParent: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .clickable {
                dismissParent()
                item.onClick()
            }
            .padding(vertical = TangemTheme.dimens.spacing8, horizontal = TangemTheme.dimens.spacing16)
            .testTag(PopUpMenuTestTags.BUTTON),
        text = item.title.resolveReference(),
        style = TangemTheme.typography.button.copy(color = item.textColorProvider()),
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TokenDetailsAppBarDropdownItem() {
    TangemThemePreview {
        TangemDropdownItem(
            modifier = Modifier.background(TangemTheme.colors.background.primary),
            dismissParent = {},
            item = TangemDropdownMenuItem(
                title = TextReference.Res(id = R.string.token_details_hide_token),
                textColorProvider = { TangemTheme.colors.text.warning },
                onClick = { },
            ),
        )
    }
}