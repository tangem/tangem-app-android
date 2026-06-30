package com.tangem.features.addressbook.editcontact.ui

import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.common.ui.account.AccountIcon
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.common.ui.account.getUiColor
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.components.fields.AutoSizeTextField
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_sign_plus_20
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.addressbook.editcontact.ui.state.EditContactUM
import com.tangem.features.addressbook.editcontact.ui.state.ValidatedAddress
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun EditContactContent(state: EditContactUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = TangemTheme.colors3.bg.primary)
            .imePadding()
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

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val minContentHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(min = minContentHeight)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ContactSummary(state = state)
                        ContactColor(colors = state.colors)
                        BlockCard(
                            shape = RoundedCornerShape(24.dp),
                            colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors3.bg.secondary),
                        ) {
                            ContactAddresses(addresses = state.addresses)
                            AddAddressRow(isEnabled = state.isAddAddressEnabled, onClick = state.onAddAddressClick)
                        }
                        WalletBlock(walletBlock = state.walletBlock)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    SaveButton(saveButton = state.saveButton)
                }
            }
        }
    }
}

@Composable
private fun SaveButton(saveButton: TangemButtonUM) {
    TangemButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        text = saveButton.text,
        onClick = saveButton.onClick,
        isEnabled = saveButton.isEnabled,
        isLoading = saveButton.isLoading,
        size = TangemButton.Size.X12,
    )
}

@Composable
private fun ContactAddresses(addresses: ImmutableList<ValidatedAddress>) {
    addresses.fastForEach { entry ->
        AddressRow(entry = entry)
    }
}

@Composable
private fun AddressRow(entry: ValidatedAddress) {
    TangemRow(
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = {
            TangemIcon(
                tangemIconUM = TangemIconUM.Ident(text = entry.address),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
            )
        },
        titleSlot = {
            TangemRowText(
                text = stringReference(entry.address),
                role = TangemRowTextRole.Title,
                overflow = TextOverflow.MiddleEllipsis,
            )
        },
        subtitleSlot = {
            TangemRowText(
                text = pluralReference(
                    id = R.plurals.common_networks_count,
                    count = entry.networkIds.size,
                    formatArgs = wrappedList(entry.networkIds.size),
                ),
                role = TangemRowTextRole.Subtitle,
            )
        },
    )
}

@Composable
private fun AddAddressRow(isEnabled: Boolean, onClick: () -> Unit) {
    TangemRow(
        verticalAlignment = TangemRowVerticalAlignment.Center,
        onClick = onClick,
        startSlot = {
            TangemIcon(
                tangemIconUM = TangemIconUM.Icon(
                    imageVector = Icons.ic_sign_plus_20,
                    tintReference = {
                        if (isEnabled) {
                            TangemTheme.colors3.icon.brand
                        } else {
                            TangemTheme.colors3.icon.tertiary
                        }
                    },
                ),
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isEnabled) {
                            TangemTheme.colors3.bg.status.infoSubtle
                        } else {
                            TangemTheme.colors3.bg.opaque.secondary
                        },
                        shape = RoundedCornerShape(10.dp),
                    )
                    .padding(8.dp),
            )
        },
        titleSlot = {
            if (isEnabled) {
                TangemRowText(
                    text = TextReference.Res(R.string.address_book_add_address),
                    role = TangemRowTextRole.Title,
                )
            } else {
                Text(
                    text = stringResourceSafe(R.string.address_book_add_address),
                    color = TangemTheme.colors3.text.tertiary,
                    style = TangemTheme.typography3.body.medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        subtitleSlot = {
            if (isEnabled) {
                TangemRowText(
                    text = TextReference.Res(R.string.address_book_add_address_description),
                    role = TangemRowTextRole.Subtitle,
                )
            } else {
                Text(
                    text = stringResourceSafe(R.string.address_book_add_address_description),
                    color = TangemTheme.colors3.text.tertiary,
                    style = TangemTheme.typography3.caption.medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    )
}

@Composable
private fun WalletBlock(walletBlock: EditContactUM.WalletBlockUM, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .fillMaxWidth()
            .background(TangemTheme.colors3.bg.secondary),
    ) {
        TangemRow(
            onClick = if (walletBlock.isChangeable) walletBlock.onClick else null,
            verticalAlignment = TangemRowVerticalAlignment.Center,
            titleSlot = {
                TangemRowText(
                    text = stringResourceSafe(R.string.address_book_save_to_wallet_title),
                    role = TangemRowTextRole.Title,
                )
            },
            endSlot = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = walletBlock.walletName,
                        style = TangemTheme.typography3.body.medium,
                        color = TangemTheme.colors3.text.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = TangemTheme.typography3.caption.medium.fontSize,
                            maxFontSize = TangemTheme.typography3.body.medium.fontSize,
                        ),
                    )
                    if (walletBlock.isChangeable) {
                        WalletChevronIcon()
                    }
                }
            },
        )
    }
}

@Composable
private fun WalletChevronIcon() {
    Icon(
        modifier = Modifier
            .padding(start = 4.dp)
            .size(20.dp),
        tint = TangemTheme.colors3.icon.secondary,
        imageVector = ImageVector.vectorResource(id = R.drawable.ic_select_18_24),
        contentDescription = null,
    )
}

@Composable
private fun ContactSummary(state: EditContactUM) {
    val avatarName = state.name.ifBlank { state.namePlaceholder.resolveReference() }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .fillMaxWidth()
            .background(TangemTheme.colors3.bg.secondary)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpacerH(20.dp)

        AccountIcon(
            name = stringReference(avatarName),
            icon = state.portfolioIcon,
            size = AccountIconSize.RedesignLarge,
        )

        SpacerH(28.dp)

        Text(
            text = stringResourceSafe(R.string.address_book_contact_name),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )

        SpacerH(4.dp)

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

        if (state.nameError != null) {
            Text(
                modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center,
                text = state.nameError.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.status.error,
            )
        }

        SpacerH(8.dp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Suppress("MagicNumber")
@Composable
private fun ContactColor(colors: EditContactUM.Colors) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .fillMaxWidth()
            .background(TangemTheme.colors3.bg.secondary)
            .padding(16.dp),
    ) {
        FlowRow(
            maxItemsInEachRow = 6,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(18.dp),
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
                                .size(48.dp)
                                .border(2.dp, color.getUiColor(), shape = CircleShape),
                        )
                        Box(
                            modifier = Modifier
                                .size(38.dp)
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
                nameError = null,
                portfolioIcon = AccountIconUM.CryptoPortfolio(
                    value = CryptoPortfolioIcon.Icon.Letter,
                    color = colors.first(),
                ),
                colors = EditContactUM.Colors(
                    selected = colors.first(),
                    list = colors,
                    onColorSelect = {},
                ),
                addresses = persistentListOf(
                    ValidatedAddress(
                        address = "0x1234567890abcdef1234567890abcdef12345678",
                        networkIds = persistentListOf("ethereum", "bsc", "polygon"),
                    ),
                ),
                walletBlock = EditContactUM.WalletBlockUM(
                    walletName = "Main Wallet",
                    isChangeable = true,
                    onClick = {},
                ),
                isAddAddressEnabled = true,
                saveButton = TangemButtonUM(
                    text = TextReference.Res(R.string.common_save),
                    type = TangemButtonType.Primary,
                    isEnabled = true,
                    onClick = {},
                ),
                onNameChange = {},
                onCloseClick = {},
                onAddAddressClick = {},
            ),
        )
    }
}