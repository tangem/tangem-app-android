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
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme

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
        TangemTopAppBar(
            modifier = Modifier.statusBarsPadding(),
            title = resourceReference(R.string.address_book_title),
            startButton = TopAppBarButtonUM.Back(
                onBackClicked = onBackClick,
            ),
        )
        NoContactInfo()
        PrimaryButtonIconEnd(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            text = stringResourceSafe(R.string.address_book_add_contact),
            iconResId = R.drawable.ic_plus_24,
            onClick = onAddContactClick,
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
    AddressBookEmptyScreen(onAddContactClick = {}, onBackClick = {})
}