package com.tangem.features.details.utils

import androidx.compose.ui.text.intl.Locale
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.features.details.entity.DetailsFooterUM
import com.tangem.features.details.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

@ComponentScoped
internal class SocialsBuilder @Inject constructor(
    private val urlOpener: UrlOpener,
) {

    fun buildAll(): ImmutableList<DetailsFooterUM.Social> = Social.all.map { social ->
        DetailsFooterUM.Social(
            id = social.name,
            iconResId = social.iconResId,
            onClick = { openUrl(social) },
        )
    }.toImmutableList()

    private fun openUrl(social: Social) {
        val locale = Locale.current.region

        val url = if (locale == RUSSIA_LOCALE && social.urlRu != null) {
            social.urlRu
        } else {
            social.url
        }

        urlOpener.openUrl(url)
    }

    private enum class Social(
        val iconResId: Int,
        val url: String,
        val urlRu: String? = null,
    ) {
        X(
            iconResId = R.drawable.ic_twitter_24,
            url = "https://x.com/tangem",
        ),
        TELEGRAM(
            iconResId = R.drawable.ic_telegram_24,
            url = "https://t.me/tangem_chat",
            urlRu = "https://t.me/tangem_chat_ru",
        ),
        DISCORD(
            iconResId = R.drawable.ic_discord_24,
            url = "https://discord.gg/tangem",
        ),
        REDDIT(
            iconResId = R.drawable.ic_reddit_24,
            url = "https://www.reddit.com/r/Tangem/",
        ),
        INSTAGRAM(
            iconResId = R.drawable.ic_instagram_24,
            url = "https://www.instagram.com/tangemwallet",
        ),
        GIT_HUB(
            iconResId = R.drawable.ic_github_24,
            url = "https://github.com/tangem",
        ),
        FACEBOOK(
            iconResId = R.drawable.ic_facebook_24,
            url = "https://www.facebook.com/tangemwallet",
        ),
        LINKEDIN(
            iconResId = R.drawable.ic_linkedin_24,
            url = "https://www.linkedin.com/company/tangem",
        ),
        YOUTUBE(
            iconResId = R.drawable.ic_youtube_24,
            url = "https://youtube.com/@tangem_official",
        ),
        ;

        companion object {

            val all = values()
        }
    }

    private companion object {
        const val RUSSIA_LOCALE = "ru"
    }
}