package com.tangem.features.collectibles.impl.main.model.converter

import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.collectibles.impl.main.model.CollectiblesMainStubData
import com.tangem.features.collectibles.impl.main.model.CollectiblesPack
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM
import com.tangem.utils.converter.Converter

/** Narrows the catalogue down to the selected category and the query, and shapes what is left into the grid. */
internal class CollectiblesContentConverter(
    private val categoryId: String,
    private val query: String,
    private val onPackClick: (String) -> Unit,
) : Converter<List<CollectiblesPack>, CollectiblesMainUM.ContentUM> {

    private val packsConverter by lazy(LazyThreadSafetyMode.NONE) {
        CollectiblesPacksConverter(onPackClick = onPackClick)
    }

    override fun convert(value: List<CollectiblesPack>): CollectiblesMainUM.ContentUM {
        val packs = value.filter { pack ->
            val isCategoryMatched = categoryId == CollectiblesMainStubData.ALL_CATEGORY_ID ||
                pack.categoryId == categoryId
            isCategoryMatched && (query.isBlank() || pack.title.contains(query, ignoreCase = true))
        }

        if (packs.isEmpty()) return CollectiblesMainUM.ContentUM.Empty(message = stringReference(EMPTY_MESSAGE))

        return CollectiblesMainUM.ContentUM.Content(packs = packsConverter.convert(packs))
    }

    private companion object {

        const val EMPTY_MESSAGE = "No results. Try to change your wording\nor explore from the list"
    }
}