package com.tangem.data.markets.converters

import com.tangem.datasource.api.markets.models.response.TokenMarketInfoResponse
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenQuotes
import com.tangem.utils.converter.Converter

internal object TokenMarketInfoConverter : Converter<TokenMarketInfoResponse, TokenMarketInfo> {

    override fun convert(value: TokenMarketInfoResponse): TokenMarketInfo {
        return with(value) {
            TokenMarketInfo(
                id = id,
                name = name,
                symbol = symbol,
                quotes = getQuotes(),
                networks = networks?.convert(),
                shortDescription = shortDescription,
                fullDescription = fullDescription,
                insights = insights?.convert(),
                metrics = metrics?.convert(),
                links = links?.convert(),
                pricePerformance = pricePerformance?.convert(),
            )
        }
    }

    private fun TokenMarketInfoResponse.getQuotes(): TokenQuotes {
        return TokenQuotes(
            currentPrice = currentPrice,
            h24ChangePercent = priceChangePercentage?.day?.movePointLeft(2),
            weekChangePercent = priceChangePercentage?.week?.movePointLeft(2),
            monthChangePercent = priceChangePercentage?.month?.movePointLeft(2),
            m3ChangePercent = priceChangePercentage?.threeMonths?.movePointLeft(2),
            m6ChangePercent = priceChangePercentage?.sixMonths?.movePointLeft(2),
            yearChangePercent = priceChangePercentage?.year?.movePointLeft(2),
            allTimeChangePercent = priceChangePercentage?.allTime?.movePointLeft(2),
        )
    }

    @JvmName("convertNetwork")
    private fun List<TokenMarketInfoResponse.Network>.convert(): List<TokenMarketInfo.Network> {
        return map {
            TokenMarketInfo.Network(
                networkId = it.networkId,
                exchangeable = it.exchangeable,
                contractAddress = it.contractAddress,
                decimalCount = it.decimalCount,
            )
        }
    }

    @JvmName("convertInsight")
    private fun TokenMarketInfoResponse.Insights.convert(): TokenMarketInfo.Insights {
        return TokenMarketInfo.Insights(
            holdersChange = holdersChange?.convert(),
            liquidityChange = liquidityChange?.convert(),
            buyPressureChange = buyPressureChange?.convert(),
            experiencedBuyerChange = experiencedBuyerChange?.convert(),
        )
    }

    private fun TokenMarketInfoResponse.Change.convert(): TokenMarketInfo.Change {
        return TokenMarketInfo.Change(
            day = day,
            week = week,
            month = month,
        )
    }

    private fun TokenMarketInfoResponse.Metrics.convert(): TokenMarketInfo.Metrics {
        return TokenMarketInfo.Metrics(
            marketRating = marketRating,
            circulatingSupply = circulatingSupply,
            marketCap = marketCap,
            volume24h = volume24h,
            totalSupply = totalSupply,
            fullyDilutedValuation = fullyDilutedValuation,
        )
    }

    private fun TokenMarketInfoResponse.Links.convert(): TokenMarketInfo.Links {
        return TokenMarketInfo.Links(
            officialLinks = officialLinks?.convert(),
            social = social?.convert(),
            repository = repository?.convert(),
            blockchainSite = blockchainSite?.convert(),
        )
    }

    @JvmName("convertLink")
    private fun List<TokenMarketInfoResponse.Link>.convert(): List<TokenMarketInfo.Link> {
        return map {
            TokenMarketInfo.Link(
                title = it.title,
                id = it.id,
                link = it.link,
            )
        }
    }

    private fun TokenMarketInfoResponse.PricePerformance.convert(): TokenMarketInfo.PricePerformance {
        return TokenMarketInfo.PricePerformance(
            day = day?.convert(),
            month = month?.convert(),
            allTime = allTime?.convert(),
        )
    }

    private fun TokenMarketInfoResponse.Range.convert(): TokenMarketInfo.Range {
        return TokenMarketInfo.Range(
            low = low,
            high = high,
        )
    }
}