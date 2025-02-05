package com.tangem.domain.promo.models

data class StoryContent(
    val imageHost: String,
    val story: Story,
) {
    data class Story(
        val id: String,
        val title: String?,
        val slides: List<StorySlide>,
    )

    data class StorySlide(
        val id: String,
    )

    fun getImageUrls(): List<String> {
        return story.slides.map { slide -> imageHost + slide.id + WEBP_EXTENSION }
    }

    private companion object {
        const val WEBP_EXTENSION = ".webp"
    }
}

enum class StoryContentIds(val id: String) {
    STORY_FIRST_TIME_SWAP("first-time-swap"),
}