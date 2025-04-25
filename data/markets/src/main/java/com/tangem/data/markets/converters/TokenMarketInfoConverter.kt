package com.tangem.data.markets.converters

import android.net.Uri
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.datasource.api.markets.models.response.TokenMarketInfoResponse
import com.tangem.domain.markets.BuildConfig
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenQuotes
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class TokenMarketInfoConverter(
    private val excludedBlockchains: ExcludedBlockchains,
) : Converter<TokenMarketInfoResponse, TokenMarketInfo> {

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
                securityData = securityData?.convert(),
                links = links?.convert(),
                pricePerformance = pricePerformance?.convert(),
                exchangesAmount = exchangesAmount,
            )
        }
    }

    private fun TokenMarketInfoResponse.getQuotes(): TokenQuotes {
        return TokenQuotes(
            currentPrice = currentPrice,
            h24ChangePercent = priceChangePercentage?.getPercentageByInterval { day },
            weekChangePercent = priceChangePercentage?.getPercentageByInterval { week },
            monthChangePercent = priceChangePercentage?.getPercentageByInterval { month },
            m3ChangePercent = priceChangePercentage?.getPercentageByInterval { threeMonths },
            m6ChangePercent = priceChangePercentage?.getPercentageByInterval { sixMonths },
            yearChangePercent = priceChangePercentage?.getPercentageByInterval { year },
            allTimeChangePercent = priceChangePercentage?.getPercentageByInterval { allTime },
        )
    }

    private fun TokenMarketInfoResponse.PriceChangePercentage.getPercentageByInterval(
        intervalProvider: TokenMarketInfoResponse.PriceChangePercentage.() -> BigDecimal?,
    ): BigDecimal? {
        return (intervalProvider() ?: allTime)?.movePointLeft(2)
    }

    @JvmName("convertNetwork")
    private fun List<TokenMarketInfoResponse.Network>.convert(): List<TokenMarketInfo.Network> {
        return mapNotNull { network ->
            val blockchain = Blockchain.fromNetworkId(network.networkId)
            when {
                network.contractAddress.isNullOrEmpty() -> {
                    TokenMarketInfo.Network(
                        networkId = network.networkId,
                        exchangeable = network.exchangeable,
                        contractAddress = network.contractAddress,
                        decimalCount = network.decimalCount,
                    )
                }
                blockchain != null && blockchain !in excludedBlockchains &&
                    blockchain.canHandleTokens() -> {
                    TokenMarketInfo.Network(
                        networkId = network.networkId,
                        exchangeable = network.exchangeable,
                        contractAddress = blockchain.reformatContractAddress(network.contractAddress),
                        decimalCount = network.decimalCount,
                    )
                }
                else -> null
            }
        }
    }

    @JvmName("convertInsight")
    private fun TokenMarketInfoResponse.Insights.convert(): TokenMarketInfo.Insights {
        return TokenMarketInfo.Insights(
            holdersChange = holdersChange?.convert(),
            liquidityChange = liquidityChange?.convert(),
            buyPressureChange = buyPressureChange?.convert(),
            experiencedBuyerChange = experiencedBuyerChange?.convert(),
            sourceNetworks = sourceNetworks.orEmpty().map { TokenMarketInfo.Insights.SourceNetwork(it.id, it.name) },
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
            maxSupply = maxSupply,
            fullyDilutedValuation = fullyDilutedValuation,
        )
    }

    private fun TokenMarketInfoResponse.SecurityData.convert(): TokenMarketInfo.SecurityData {
        return TokenMarketInfo.SecurityData(
            totalSecurityScore = totalSecurityScore,
            securityScoreProviderData = providerData.map { it.convert() },
        )
    }

    private fun TokenMarketInfoResponse.ProviderData.convert(): TokenMarketInfo.SecurityScoreProvider {
        return TokenMarketInfo.SecurityScoreProvider(
            providerId = providerId,
            providerName = providerName,
            urlData = link?.convertToUrlData(),
            securityScore = securityScore,
            lastAuditDate = lastAuditDate,
            iconUrl = "${getImageHost()}large/$providerId.png",
        )
    }

    private fun String.convertToUrlData(): TokenMarketInfo.SecurityScoreProvider.UrlData {
        return TokenMarketInfo.SecurityScoreProvider.UrlData(
            fullUrl = this,
            rootHost = this.extractMainDomainOrNull(),
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

    private fun String.extractMainDomainOrNull(): String? {
        return runCatching {
            val uri = Uri.parse(this)
            val host = uri.host ?: return null

            val parts = host.split(".")

            if (parts.size >= 2) {
                parts.takeLast(2).joinToString(".")
            } else {
                host
            }
        }.getOrNull()
    }

    private fun getImageHost(): String {
        return if (BuildConfig.TESTER_MENU_ENABLED) {
            DEV_IMAGE_HOST
        } else {
            PROD_IMAGE_HOST
        }
    }

    private companion object {

        const val PROD_IMAGE_HOST = "https://s3.eu-central-1.amazonaws.com/tangem.api/security_provider/"
        const val DEV_IMAGE_HOST = "https://s3.eu-central-1.amazonaws.com/tangem.api.dev/security_provider/"
    }
}