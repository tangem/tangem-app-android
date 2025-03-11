package com.tangem.data.promo.converters

import com.tangem.datasource.api.promotion.models.StoryContentResponse
import com.tangem.domain.promo.models.StoryContent
import com.tangem.utils.converter.Converter

internal class StoryContentResponseConverter : Converter<StoryContentResponse, StoryContent> {
    override fun convert(value: StoryContentResponse): StoryContent {
        return StoryContent(
            imageHost = value.imageHost,
            story = StoryContent.Story(
                id = value.story.id,
                title = value.story.title,
                slides = value.story.slides?.map { slide ->
                    StoryContent.StorySlide(
                        id = slide.id,
                    )
                }.orEmpty(),
            ),
        )
    }
}