package com.tangem.features.feed.model.news.list.loader

import com.tangem.common.ui.R
import com.tangem.core.ui.components.chip.entity.ChipUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.news.ArticleCategory
import com.tangem.domain.news.usecase.GetNewsCategoriesUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

internal class NewsCategoriesLoader(
    private val getNewsCategoriesUseCase: GetNewsCategoriesUseCase,
    private val defaultAllNewsCategoryId: Int,
    private val onCategoryClick: (Int) -> Unit,
) {

    suspend fun load(): ImmutableList<ChipUM> {
        val allCategoriesChip = createAllNewsChip()
        val categoriesResult = getNewsCategoriesUseCase.invoke()

        return categoriesResult.fold(
            ifLeft = { persistentListOf(allCategoriesChip) },
            ifRight = { categories ->
                val categoryChips = categories.map { articleCategory ->
                    createCategoryChip(articleCategory)
                }
                (listOf(allCategoriesChip) + categoryChips).toPersistentList()
            },
        )
    }

    private fun createAllNewsChip(): ChipUM {
        return ChipUM(
            id = defaultAllNewsCategoryId,
            text = TextReference.Res(R.string.news_all_news),
            isSelected = true,
            onClick = { onCategoryClick(defaultAllNewsCategoryId) },
        )
    }

    private fun createCategoryChip(articleCategory: ArticleCategory): ChipUM {
        return ChipUM(
            id = articleCategory.id,
            text = TextReference.Str(articleCategory.name),
            isSelected = false,
            onClick = { onCategoryClick(articleCategory.id) },
        )
    }
}