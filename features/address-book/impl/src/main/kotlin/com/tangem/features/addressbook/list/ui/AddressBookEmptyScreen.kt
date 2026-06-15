package com.tangem.features.addressbook.list.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.button.PrimaryTangemButton
import com.tangem.core.ui.ds.button.TangemButtonIconPosition
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun AddressBookEmptyScreen(
    tangemButtonUM: TangemButtonUM,
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
                    iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_back_24),
                    onClick = onBackClick,
                    size = TangemButton.Size.X11,
                    variant = TangemButton.Variant.Material,
                )
            },
        )
        NoContactInfo()
        PrimaryTangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            buttonUM = tangemButtonUM,
        )
    }
}

@Composable
private fun ColumnScope.NoContactInfo() {
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ContactImage()
        Text(
            modifier = Modifier.padding(top = TangemTheme.dimens.spacing24),
            text = stringResourceSafe(R.string.address_book_no_contacts),
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.heading.medium,
        )
        Text(
            modifier = Modifier.padding(top = TangemTheme.dimens.spacing8),
            text = stringResourceSafe(R.string.address_book_no_contacts_description),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.body.medium,
            textAlign = TextAlign.Center,
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
            painter = painterResource(R.drawable.ic_contact_20),
            contentDescription = stringResourceSafe(R.string.address_book_no_contacts),
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_AddressBookEmptyScreen() {
    AddressBookEmptyScreen(
        tangemButtonUM = TangemButtonUM(
            text = TextReference.Res(R.string.address_book_new_contact),
            tangemIconUM = TangemIconUM.Icon(iconRes = R.drawable.ic_plus_24),
            iconPosition = TangemButtonIconPosition.End,
            type = TangemButtonType.Secondary,
            onClick = {},
        ),
        onBackClick = {},
    )
}