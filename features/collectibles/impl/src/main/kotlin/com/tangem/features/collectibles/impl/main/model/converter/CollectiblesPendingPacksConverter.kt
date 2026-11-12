package com.tangem.features.collectibles.impl.main.model.converter

import com.tangem.core.res.R
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_clock_20
import com.tangem.core.ui.res.generated.icons.ic_warning_20
import com.tangem.domain.collectibles.models.CollectiblePendingPack
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM.PendingPackUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal class CollectiblesPendingPacksConverter(
    private val onOpenPackClick: (CollectiblePendingPack.ReadyToOpen) -> Unit,
    private val onGetAssistanceClick: () -> Unit,
    private val onCloseClick: (txId: String) -> Unit,
) : Converter<List<CollectiblePendingPack>, ImmutableList<PendingPackUM>> {

    override fun convert(value: List<CollectiblePendingPack>): ImmutableList<PendingPackUM> =
        value.map(::convertPack).toImmutableList()

    private fun convertPack(pack: CollectiblePendingPack): PendingPackUM = when (pack) {
        is CollectiblePendingPack.PaymentInProgress -> PendingPackUM(
            txId = pack.txId,
            title = resourceReference(R.string.collectibles_payment_in_progress_banner_title),
            description = resourceReference(R.string.collectibles_payment_in_progress_banner_subtitle),
            icon = Icons.ic_clock_20,
            onCloseClick = null,
            button = null,
        )
        is CollectiblePendingPack.ReadyToOpen -> PendingPackUM(
            txId = pack.txId,
            title = resourceReference(R.string.collectibles_pack_to_open_banner_title),
            description = resourceReference(R.string.collectibles_pack_to_open_banner_subtitle),
            icon = Icons.ic_warning_20,
            onCloseClick = null,
            button = TangemMessageBanner.Button(
                text = resourceReference(R.string.collectibles_pack_unopened_open_pack),
                onClick = { onOpenPackClick(pack) },
            ),
        )
        is CollectiblePendingPack.OpenFailed -> PendingPackUM(
            txId = pack.txId,
            title = resourceReference(R.string.collectibles_open_pack_failed_title),
            description = resourceReference(R.string.collectibles_open_pack_failed_subtitle),
            icon = null,
            onCloseClick = { onCloseClick(pack.txId) },
            button = TangemMessageBanner.Button(
                text = resourceReference(R.string.collectibles_main_menu_get_assistance),
                onClick = onGetAssistanceClick,
            ),
        )
        is CollectiblePendingPack.Refunding -> PendingPackUM(
            txId = pack.txId,
            title = resourceReference(R.string.collectibles_refund_on_the_way_title),
            description = resourceReference(R.string.collectibles_refund_on_the_way_subtitle),
            icon = null,
            onCloseClick = { onCloseClick(pack.txId) },
            button = null,
        )
    }
}