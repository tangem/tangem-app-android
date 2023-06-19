package com.tangem.feature.learn2earn.data.api

/**
 * @author Anton Zhilenkov on 07.06.2023.
 */
interface Learn2earnPreferenceStorage {
    var promoCode: String?
    var promotionInfo: String?
    var alreadyReceivedAward: Boolean
}
