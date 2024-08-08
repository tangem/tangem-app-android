package com.tangem.features.markets.details.impl.model.converters

import androidx.compose.runtime.Stable
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.markets.details.impl.ui.state.LinksUM
import com.tangem.features.markets.impl.R
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

@Stable
internal class LinksConverter(
    private val onLinkClick: (LinksUM.Link) -> Unit,
) : Converter<TokenMarketInfo.Links, LinksUM> {

    override fun convert(value: TokenMarketInfo.Links): LinksUM {
        return LinksUM(
            officialLinks = value.officialLinks?.map { it.convert() }.orEmpty().toImmutableList(),
            social = value.social?.map { it.convert() }.orEmpty().toImmutableList(),
            repository = value.repository?.map { it.convert() }.orEmpty().toImmutableList(),
            blockchainSite = value.blockchainSite?.map { it.convert() }.orEmpty().toImmutableList(),
            onLinkClick = onLinkClick,
        )
    }

    private fun TokenMarketInfo.Link.convert(): LinksUM.Link {
        return LinksUM.Link(
            title = stringReference(title),
            iconRes = getIconById(id),
            url = link,
        )
    }

    private fun getIconById(id: String?): Int {
        return when (id) {
            "linkedin" -> R.drawable.ic_linkedin_24
            "discord" -> R.drawable.ic_discord_24
            "youtube" -> R.drawable.ic_youtube_24
            "telegram" -> R.drawable.ic_telegram_24
            "github" -> R.drawable.ic_github_24
            "twitter" -> R.drawable.ic_twitter_24
            "facebook" -> R.drawable.ic_facebook_24
            "reddit" -> R.drawable.ic_reddit_24
            "instagram" -> R.drawable.ic_instagram_24
            "whitepaper" -> R.drawable.ic_doc_24
            else -> R.drawable.ic_arrow_top_right_24
        }
    }
}
