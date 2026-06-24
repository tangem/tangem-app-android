package com.tangem.features.addressbook.addaddress.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.addressbook.addaddress.ui.state.AddAddressUM
import com.tangem.features.addressbook.addaddress.ui.state.AddAddressUM.ChosenNetworkStateUM.Result.NetworkUM
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private const val MAX_VISIBLE_NETWORKS = 3

// Horizontal advance per icon. Smaller than the icon size so icons overlap; the bg-colored ring on
// the icon drawn on top carves the crescent cut-out from the icon below.
private val NetworkIconStep = 18.dp

@Composable
internal fun NetworkBlock(
    onNetworkSelectClick: () -> Unit,
    chosenNetworkStateUM: AddAddressUM.ChosenNetworkStateUM,
    modifier: Modifier = Modifier,
) {
    TangemRow(
        verticalAlignment = TangemRowVerticalAlignment.Center,
        modifier = modifier,
        titleSlot = {
            Text(
                text = stringResourceSafe(R.string.common_network),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
            )
        },
        endSlot = {
            SelectNetworkButton(
                onNetworkSelectClick = onNetworkSelectClick,
                chosenNetworkStateUM = chosenNetworkStateUM,
            )
        },
    )
}

@Composable
private fun SelectNetworkButton(
    onNetworkSelectClick: () -> Unit,
    chosenNetworkStateUM: AddAddressUM.ChosenNetworkStateUM,
) {
    Row(
        modifier = Modifier.clickableSingle(
            onClick = onNetworkSelectClick,
            enabled = chosenNetworkStateUM !is AddAddressUM.ChosenNetworkStateUM.Loading,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (chosenNetworkStateUM) {
            is AddAddressUM.ChosenNetworkStateUM.Result -> NetworkIconsResolver(chosenNetworkStateUM.networkUMList)
            AddAddressUM.ChosenNetworkStateUM.Loading -> TangemLoader(size = TangemLoaderSize.X20)
            AddAddressUM.ChosenNetworkStateUM.Empty -> {
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResourceSafe(R.string.address_book_select_network),
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.secondary,
                )
                SpacerW(4.dp)
                ChevronIcon()
            }
        }
    }
}

@Composable
private fun NetworkIconsResolver(networks: ImmutableList<NetworkUM>) {
    when (networks.size) {
        0 -> Unit
        1 -> {
            val network = networks.first()
            Image(
                painter = painterResource(id = network.iconResId),
                contentDescription = null,
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = network.networkName,
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
            )
            ChevronIcon()
        }
        // 3 and any larger count share the same rendering: up to MAX_VISIBLE_NETWORKS overlapping
        // icons, plus a "+N" badge that appears only when there are more than that.
        else -> {
            OverlappingNetworkIcons(networks)
            ChevronIcon()
        }
    }
}

@Composable
private fun OverlappingNetworkIcons(networks: ImmutableList<NetworkUM>) {
    val visible = networks.take(MAX_VISIBLE_NETWORKS)
    val remaining = networks.size - visible.size

    Box(modifier = Modifier.wrapContentWidth()) {
        visible.fastForEachIndexed { index, network ->
            Image(
                painter = painterResource(id = network.iconResId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(start = NetworkIconStep * index)
                    .networkIconRing()
                    .size(24.dp),
            )
        }
        if (remaining > 0) {
            Box(
                modifier = Modifier
                    .padding(start = NetworkIconStep * visible.size)
                    .networkIconRing()
                    .background(color = TangemTheme.colors3.bg.tertiary)
                    .heightIn(min = 24.dp)
                    .padding(vertical = 2.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${StringsSigns.PLUS}$remaining",
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.secondary,
                )
            }
        }
    }
}

// bg-colored ring + clip applied to every overlapping element so the one drawn on top carves a
// crescent out of the one below it. The ring color must match the surface the icons sit on.
@Composable
private fun Modifier.networkIconRing(): Modifier = this
    .border(width = 2.dp, color = TangemTheme.colors3.bg.secondary, shape = CircleShape)
    .padding(2.dp)
    .clip(CircleShape)

@Composable
private fun ChevronIcon() {
    Icon(
        modifier = Modifier
            .padding(start = 8.dp)
            .size(20.dp),
        tint = TangemTheme.colors3.icon.secondary,
        imageVector = ImageVector.vectorResource(id = R.drawable.ic_select_18_24),
        contentDescription = null,
    )
}

@Preview(showBackground = true)
@Composable
private fun Preview_NetworkBlock() {
    TangemThemePreviewRedesign {
        Column {
            NetworkBlock(
                onNetworkSelectClick = {},
                chosenNetworkStateUM = AddAddressUM.ChosenNetworkStateUM.Result(
                    networkUMList = persistentListOf(
                        NetworkUM(networkName = "Ethereum", iconResId = R.drawable.img_eth_22),
                    ),
                ),
            )
            SpacerH12()
            NetworkBlock(
                onNetworkSelectClick = {},
                chosenNetworkStateUM = AddAddressUM.ChosenNetworkStateUM.Result(
                    networkUMList = persistentListOf(
                        NetworkUM(networkName = "Ethereum", iconResId = R.drawable.img_eth_22),
                        NetworkUM(networkName = "BSC", iconResId = R.drawable.img_bsc_22),
                        NetworkUM(networkName = "Polygon", iconResId = R.drawable.img_polygon_22),
                    ),
                ),
            )
            SpacerH12()
            NetworkBlock(
                onNetworkSelectClick = {},
                chosenNetworkStateUM = AddAddressUM.ChosenNetworkStateUM.Result(
                    networkUMList = List(15) {
                        NetworkUM(networkName = "Network", iconResId = R.drawable.img_eth_22)
                    }.toImmutableList(),
                ),
            )
            SpacerH12()
            NetworkBlock(chosenNetworkStateUM = AddAddressUM.ChosenNetworkStateUM.Loading, onNetworkSelectClick = {})
            SpacerH12()
            NetworkBlock(chosenNetworkStateUM = AddAddressUM.ChosenNetworkStateUM.Empty, onNetworkSelectClick = {})
        }
    }
}