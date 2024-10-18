package com.tangem.feature.wallet.presentation.wallet.domain

import androidx.annotation.DrawableRes
import com.tangem.feature.wallet.impl.R

/**
 * Wallet2 cobrand image.
 * To integrate a new cobrand, just implement new enum object and that all.
 *
 * @property cards2ResId image resource id for wallet set with 2 cards
 * @property cards3ResId image resource id for wallet set with 3 cards
 * @property batchIds    set of unique batch ids for this cobrand
 *
* [REDACTED_AUTHOR]
 */
internal enum class Wallet2CobrandImage(
    @DrawableRes val cards2ResId: Int,
    @DrawableRes val cards3ResId: Int,
    val batchIds: Set<String>,
) {

    Avrora(
        cards2ResId = R.drawable.ill_avrora_card2_120_106,
        cards3ResId = R.drawable.ill_avrora_card3_120_106,
        batchIds = setOf("AF18"),
    ),

    BabyDoge(
        cards2ResId = R.drawable.ill_baby_doge_card2_120_106,
        cards3ResId = R.drawable.ill_baby_doge_card3_120_106,
        batchIds = setOf("AF51"),
    ),

    Bad(
        cards2ResId = R.drawable.ill_bad_card2_120_106,
        cards3ResId = R.drawable.ill_bad_card3_120_106,
        batchIds = setOf("AF09"),
    ),

    BitcoinGold(
        cards2ResId = R.drawable.ill_bitcoingold_card2_120_106,
        cards3ResId = R.drawable.ill_bitcoingold_card3_120_106,
        batchIds = setOf("AF71"),
    ),

    BitcoinPizzaDay(
        cards2ResId = R.drawable.ill_pizza_day_card2_120_106,
        cards3ResId = R.drawable.ill_pizza_day_card3_120_106,
        batchIds = setOf("AF33"),
    ),

    CoinMetrica(
        cards2ResId = R.drawable.ill_coin_metrica_card2_120_106,
        cards3ResId = R.drawable.ill_coin_metrica_card3_120_106,
        batchIds = setOf("AF27"),
    ),

    COQ(
        cards2ResId = R.drawable.ill_coq_card2_120_106,
        cards3ResId = R.drawable.ill_coq_card3_120_106,
        batchIds = setOf("AF28"),
    ),

    CryptoSeth(
        cards2ResId = R.drawable.ill_crypto_seth_card2_120_106,
        cards3ResId = R.drawable.ill_crypto_seth_card3_120_106,
        batchIds = setOf("AF32"),
    ),

    Grim(
        cards2ResId = R.drawable.ill_grim_card2_120_106,
        cards3ResId = R.drawable.ill_grim_card3_120_106,
        batchIds = setOf("AF13"),
    ),

    Jr(
        cards2ResId = R.drawable.ill_jr_card2_120_106,
        cards3ResId = R.drawable.ill_jr_card3_120_106,
        batchIds = setOf("AF14"),
    ),

    Kaspa(
        cards2ResId = R.drawable.ill_kaspa_card2_120_106,
        cards3ResId = R.drawable.ill_kaspa_card3_120_106,
        batchIds = setOf("AF08"),
    ),

    Kaspa2(
        cards2ResId = R.drawable.ill_kaspa2_card2_120_106,
        cards3ResId = R.drawable.ill_kaspa2_card3_120_106,
        batchIds = setOf("AF25"),
    ),

    KaspaReseller(
        cards2ResId = R.drawable.ill_kaspa_reseller_card2_120_106,
        cards3ResId = R.drawable.ill_kaspa_reseller_card3_120_106,
        batchIds = setOf("AF31"),
    ),

    Kaspa3(
        cards2ResId = R.drawable.ill_kaspa3_card2_120_106,
        cards3ResId = R.drawable.ill_kaspa3_card3_120_106,
        batchIds = setOf("AF73"),
    ),

    KishuInu(
        cards2ResId = R.drawable.ill_kishu_inu_card2_120_106,
        cards3ResId = R.drawable.ill_kishu_inu_card3_120_106,
        batchIds = setOf("AF52"),
    ),

    NewWorldElite(
        cards2ResId = R.drawable.ill_nwe_card2_120_106,
        cards3ResId = R.drawable.ill_nwe_card3_120_106,
        batchIds = setOf("AF26"),
    ),

    // for multicolored cards use image of 3 cards in all cases
    Pastel(
        cards2ResId = R.drawable.ill_pastel_cards3_120_106,
        cards3ResId = R.drawable.ill_pastel_cards3_120_106,
        batchIds = setOf("AF43", "AF44", "AF45"),
    ),

    RedPanda(
        cards2ResId = R.drawable.ill_red_panda_card2_120_106,
        cards3ResId = R.drawable.ill_red_panda_card3_120_106,
        batchIds = setOf("AF34"),
    ),

    SatoshiFriends(
        cards2ResId = R.drawable.ill_satoshi_card2_120_106,
        cards3ResId = R.drawable.ill_satoshi_card3_120_106,
        batchIds = setOf("AF19"),
    ),

    Trillant(
        cards2ResId = R.drawable.ill_trillant_card2_120_106,
        cards3ResId = R.drawable.ill_trillant_card3_120_106,
        batchIds = setOf("AF16"),
    ),

    Tron(
        cards2ResId = R.drawable.ill_tron_card2_120_106,
        cards3ResId = R.drawable.ill_tron_card3_120_106,
        batchIds = setOf("AF07"),
    ),

    VeChain(
        cards2ResId = R.drawable.ill_vechain_card2_120_106,
        cards3ResId = R.drawable.ill_vechain_card3_120_106,
        batchIds = setOf("AF29"),
    ),

    // for multicolored cards use image of 3 cards in all cases
    Vivid(
        cards2ResId = R.drawable.ill_vivid_cards3_120_106,
        cards3ResId = R.drawable.ill_vivid_cards3_120_106,
        batchIds = setOf("AF40", "AF41", "AF42"),
    ),

    VoltInu(
        cards2ResId = R.drawable.ill_volt_inu_card2_120_106,
        cards3ResId = R.drawable.ill_volt_inu_card3_120_106,
        batchIds = setOf("AF35"),
    ),

    WhiteTangem(
        cards2ResId = R.drawable.ill_white_card2_120_106,
        cards3ResId = R.drawable.ill_white_card3_120_106,
        batchIds = setOf("AF15"),
    ),
}
