package com.tangem.features.addressbook.list.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_left_20
import com.tangem.core.ui.res.generated.icons.ic_sign_plus_20

@Composable
internal fun AddressBookEmptyScreen(
    onAddContactClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TangemTopBar(
            modifier = Modifier.statusBarsPadding(),
            title = resourceReference(R.string.address_book_title),
            startContent = {
                TangemButton(
                    iconStart = TangemIconUM.Icon(imageVector = Icons.ic_chevron_left_20),
                    onClick = onBackClick,
                    size = TangemButton.Size.X11,
                    variant = TangemButton.Variant.Material,
                )
            },
        )
        NoContactInfo(onAddClick = onAddContactClick)
    }
}

@Composable
private fun ColumnScope.NoContactInfo(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ContactImage()
        Text(
            modifier = Modifier.padding(top = 32.dp),
            text = stringResourceSafe(R.string.address_book_no_contacts),
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.heading.small,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = stringResourceSafe(R.string.address_book_no_contacts_description),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.subheading.medium,
            textAlign = TextAlign.Center,
        )
        TangemButton(
            modifier = Modifier.padding(top = 40.dp),
            text = resourceReference(R.string.address_book_add_address),
            onClick = onAddClick,
            iconEnd = TangemIconUM.Icon(imageVector = Icons.ic_sign_plus_20),
        )
    }
}

@Composable
private fun ContactImage() {
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(
                color = TangemTheme.colors3.bg.status.infoSubtle,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = ImageVector.vectorResource(R.drawable.ic_address_book_24),
            contentDescription = stringResourceSafe(R.string.address_book_no_contacts),
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360)
private fun Preview_AddressBookEmptyScreen() {
    TangemThemePreviewRedesign {
        AddressBookEmptyScreen(
            onAddContactClick = {},
            onBackClick = {},
        )
    }
}