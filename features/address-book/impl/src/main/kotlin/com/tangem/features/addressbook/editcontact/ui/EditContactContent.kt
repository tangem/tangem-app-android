package com.tangem.features.addressbook.editcontact.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.common.ui.account.AccountIcon
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.common.ui.account.getUiColor
import com.tangem.core.ui.R
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.fields.AutoSizeTextField
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.addressbook.editcontact.contract.EditContactUM
import com.tangem.features.addressbook.editcontact.contract.ValidatedAddress
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun EditContactContent(state: EditContactUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = TangemTheme.colors3.bg.primary)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TangemTopBar(
            modifier = Modifier.statusBarsPadding(),
            title = state.title,
            endContent = {
                TangemButton(
                    iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_close_24),
                    onClick = state.onCloseClick,
                    size = TangemButton.Size.X11,
                    variant = TangemButton.Variant.Material,
                )
            },
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ContactSummary(state = state)
            ContactColor(colors = state.colors)
            ContactAddresses(addresses = state.addresses)
            AddAddressRow(onClick = state.onAddAddressClick)
        }
    }
}

@Composable
private fun ContactAddresses(addresses: ImmutableList<ValidatedAddress>) {
    if (addresses.isEmpty()) return
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth()
            .background(TangemTheme.colors3.bg.secondary),
    ) {
        addresses.fastForEach { entry ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = entry.network.name,
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.tertiary,
                )
                Text(
                    text = entry.address,
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.primary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun AddAddressRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth()
            .background(TangemTheme.colors3.bg.secondary)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(TangemTheme.colors3.bg.status.infoSubtle),
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = ImageVector.vectorResource(R.drawable.ic_plus_24),
                tint = TangemTheme.colors3.text.status.info,
                contentDescription = null,
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResourceSafe(R.string.address_book_add_address),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                text = stringResourceSafe(R.string.address_book_add_address_description),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.tertiary,
            )
        }
    }
}

@Composable
private fun ContactSummary(state: EditContactUM) {
    val avatarName = state.name.ifBlank { state.namePlaceholder.resolveReference() }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth()
            .background(TangemTheme.colors3.bg.secondary),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        AccountIcon(
            name = stringReference(avatarName),
            icon = state.portfolioIcon,
            size = AccountIconSize.Large,
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResourceSafe(R.string.address_book_contact_name),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.tertiary,
        )
        Spacer(modifier = Modifier.height(2.dp))

        AutoSizeTextField(
            value = state.name,
            onValueChange = state.onNameChange,
            centered = true,
            singleLine = true,
            placeholder = state.namePlaceholder,
            textStyle = TangemTheme.typography3.heading.medium,
            color = TangemTheme.colors3.text.primary,
            placeholderColor = TangemTheme.colors3.text.tertiary,
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Suppress("MagicNumber")
@Composable
private fun ContactColor(colors: EditContactUM.Colors) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth()
            .background(TangemTheme.colors3.bg.secondary),
    ) {
        FlowRow(
            maxItemsInEachRow = 6,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        ) {
            colors.list.fastForEach { color ->
                val isSelected = color == colors.selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = { colors.onColorSelect(color) })
                        .size(48.dp),
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(47.dp)
                                .border(2.dp, color.getUiColor(), shape = CircleShape),
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(color = color.getUiColor(), shape = CircleShape),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(color = color.getUiColor(), shape = CircleShape),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_EditContactContent() {
    val colors = CryptoPortfolioIcon.Color.entries.toImmutableList()
    TangemThemePreviewRedesign {
        EditContactContent(
            state = EditContactUM(
                title = stringReference("New contact"),
                name = "",
                namePlaceholder = stringReference("New contact"),
                portfolioIcon = AccountIconUM.CryptoPortfolio(
                    value = CryptoPortfolioIcon.Icon.Letter,
                    color = colors.first(),
                ),
                colors = EditContactUM.Colors(
                    selected = colors.first(),
                    list = colors,
                    onColorSelect = {},
                ),
                addresses = persistentListOf(),
                onNameChange = {},
                onCloseClick = {},
                onAddAddressClick = {},
            ),
        )
    }
}