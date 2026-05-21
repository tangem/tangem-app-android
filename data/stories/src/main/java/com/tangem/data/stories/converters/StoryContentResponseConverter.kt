package com.tangem.data.stories.converters

import com.tangem.datasource.api.stories.models.StoryContentResponse
import com.tangem.domain.stories.models.StoryContent
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