package com.tangem.feature.learn2earn.data

import android.content.Context
import com.tangem.feature.learn2earn.data.api.Learn2earnPreferenceStorage
import hu.autsoft.krate.SimpleKrate
import hu.autsoft.krate.booleanPref
import hu.autsoft.krate.default.withDefault
import hu.autsoft.krate.stringPref

/**
[REDACTED_AUTHOR]
 */
internal class DefaultPreferenceStorage(
    context: Context,
) : SimpleKrate(context = context, name = "Lear2earnPromotion"), Learn2earnPreferenceStorage {

    override var promoCode: String? by stringPref().withDefault(null)

    override var promotionInfo: String? by stringPref().withDefault(null)

    override var alreadyReceivedAward: Boolean by booleanPref().withDefault(false)
}