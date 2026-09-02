package com.tangem.features.collectibles.impl.main.model.converter

import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.collectibles.impl.main.model.CollectiblesPack
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal class CollectiblesPacksConverter(
    private val onPackClick: (String) -> Unit,
) : Converter<List<CollectiblesPack>, ImmutableList<CollectiblesMainUM.PackUM>> {

    override fun convert(value: List<CollectiblesPack>): ImmutableList<CollectiblesMainUM.PackUM> {
        return value
            .map { pack ->
                CollectiblesMainUM.PackUM(
                    id = pack.id,
                    imageUrl = pack.imageUrl,
                    title = stringReference(pack.title),
                    price = stringReference(pack.price),
                    badge = pack.buyback?.let(::stringReference),
                    onClick = { onPackClick(pack.id) },
                )
            }
            .toImmutableList()
    }
}