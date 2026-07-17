package com.tangem.features.addressbook.list.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_24

/**
 * Shown when a stored address book uses a contract version newer than this build supports: the book can be
 * neither read nor edited, so the user is asked to update the app. No contacts, no add button.
 */
@Composable
internal fun AddressBookIncompatibleScreen(
    onUpdateClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TangemTopNavigation(
            title = resourceReference(R.string.address_book_title),
            contentAlign = TangemTopNavigation.ContentAlign.Center,
            blurBackground = false,
            onBack = onBackClick,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(color = TangemTheme.colors3.bg.status.infoSubtle, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.ic_info_24,
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.status.info,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                modifier = Modifier.padding(top = 32.dp),
                text = stringResourceSafe(R.string.force_update_warning_title),
                color = TangemTheme.colors3.text.primary,
                style = TangemTheme.typography3.heading.small,
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = stringResourceSafe(R.string.force_update_warning_message),
                color = TangemTheme.colors3.text.secondary,
                style = TangemTheme.typography3.subheading.medium,
                textAlign = TextAlign.Center,
            )
        }
        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            size = TangemButton.Size.X12,
            onClick = onUpdateClick,
            text = resourceReference(R.string.force_update_required_action),
        )
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360)
private fun Preview_AddressBookIncompatibleScreen() {
    TangemThemePreviewRedesign {
        AddressBookIncompatibleScreen(onBackClick = {}, onUpdateClick = {})
    }
}