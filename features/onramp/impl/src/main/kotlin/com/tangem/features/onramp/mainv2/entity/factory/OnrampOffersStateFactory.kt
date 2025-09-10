package com.tangem.features.onramp.mainv2.entity.factory

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.onramp.model.*
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.mainv2.entity.*
import com.tangem.utils.Provider
import com.tangem.utils.StringsSigns.MINUS
import kotlinx.collections.immutable.toPersistentList

internal class OnrampOffersStateFactory(
    private val currentStateProvider: Provider<OnrampV2MainComponentUM>,
    private val onrampIntents: OnrampV2Intents,
) {

    fun getOnShowOffersState(offers: List<OnrampOffersBlock>): OnrampV2MainComponentUM {
        val currentState = currentStateProvider.invoke()
        return when (currentState) {
            is OnrampV2MainComponentUM.Content -> {
                currentState.copy(
                    offersBlockState = mapOnrampOffersBlockToUM(offers),
                )
            }
            is OnrampV2MainComponentUM.InitialLoading -> {
                currentState
            }
        }
    }

    private fun mapOnrampOffersBlockToUM(offersBlocks: List<OnrampOffersBlock>): OnrampOffersBlockUM.Content {
        val allOffersUM = mutableListOf<OnrampOfferUM>()
        offersBlocks.map { block ->
            block.offers.forEach { offer ->
                when (val currentQuote = offer.quote) {
                    is OnrampQuote.AmountError,
                    is OnrampQuote.Error,
                    -> Unit
                    is OnrampQuote.Data -> {
                        allOffersUM.add(
                            OnrampOfferUM(
                                category = mapOfferCategoryDTOtoUM(block.category),
                                advantages = mapOfferAdvantagesDTOtoUM(offer.advantages),
                                paymentMethod = currentQuote.paymentMethod,
                                providerId = currentQuote.provider.id,
                                providerName = currentQuote.provider.info.name,
                                rate = currentQuote.toAmount.value.format {
                                    crypto(
                                        symbol = currentQuote.toAmount.symbol,
                                        decimals = currentQuote.toAmount.decimals,
                                    )
                                },
                                diff = offer.rateDif?.let { diff ->
                                    stringReference("$MINUS${diff.format { percent() }}")
                                },
                                onBuyClicked = {
                                    onrampIntents.onBuyClick(
                                        quote = OnrampProviderWithQuote.Data(
                                            provider = offer.quote.provider,
                                            paymentMethod = currentQuote.paymentMethod,
                                            toAmount = currentQuote.toAmount,
                                            fromAmount = currentQuote.fromAmount,
                                        ),
                                        onrampOfferAdvantagesUM = mapOfferAdvantagesDTOtoUM(offer.advantages),
                                    )
                                },
                            ),
                        )
                    }
                }
            }
        }

        return OnrampOffersBlockUM.Content(
            isBlockVisible = false,
            recentOffer = allOffersUM.firstOrNull { it.category == OnrampOfferCategoryUM.RecentlyUsed },
            recommended = allOffersUM.filter { it.category == OnrampOfferCategoryUM.Recommended }.toPersistentList(),
            onrampAllOffersButtonConfig = if (offersBlocks.any { it.hasMoreOffers }) {
                OnrampAllOffersButtonConfig(
                    title = TextReference.Res(R.string.onramp_all_offers_button_title),
                    onClick = { onrampIntents.openProviders() },
                )
            } else {
                null
            },
        )
    }

    private fun mapOfferCategoryDTOtoUM(category: OnrampOfferCategory): OnrampOfferCategoryUM {
        return when (category) {
            OnrampOfferCategory.Recent -> OnrampOfferCategoryUM.RecentlyUsed
            OnrampOfferCategory.Recommended -> OnrampOfferCategoryUM.Recommended
        }
    }

    private fun mapOfferAdvantagesDTOtoUM(advantages: OnrampOfferAdvantages): OnrampOfferAdvantagesUM {
        return when (advantages) {
            OnrampOfferAdvantages.Default -> OnrampOfferAdvantagesUM.Default
            OnrampOfferAdvantages.BestRate -> OnrampOfferAdvantagesUM.BestRate
            OnrampOfferAdvantages.Fastest -> OnrampOfferAdvantagesUM.Fastest
        }
    }
}