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
[REDACTED_AUTHOR]
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

    BitcoinPizza2(
        cards2ResId = R.drawable.ill_pizza_day2_card2_120_106,
        cards3ResId = R.drawable.ill_pizza_day2_card3_120_106,
        batchIds = setOf("AF990019"),
    ),

    BTC365(
        cards2ResId = R.drawable.ill_btc365_card2_120_106,
        cards3ResId = R.drawable.ill_btc365_card3_120_106,
        batchIds = setOf("AF97"),
    ),

    CashClubGold(
        cards2ResId = R.drawable.ill_cashclubgold_card2_120_106,
        cards3ResId = R.drawable.ill_cashclubgold_card3_120_106,
        batchIds = setOf("BB000004"),
    ),

    Changenow(
        cards2ResId = R.drawable.ill_changenow_card2_120_106,
        cards3ResId = R.drawable.ill_changenow_card3_120_106,
        batchIds = setOf("BB000013"),
    ),

    Chilliz(
        cards2ResId = R.drawable.ill_chilliz_card2_120_106,
        cards3ResId = R.drawable.ill_chilliz_card3_120_106,
        batchIds = setOf("BB000016"),
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

    CryptoCasey(
        cards2ResId = R.drawable.ill_crypto_casey_card2_120_106,
        cards3ResId = R.drawable.ill_crypto_casey_card3_120_106,
        batchIds = setOf("AF21", "AF22", "AF23"),
    ),

    CryptoOrg(
        cards2ResId = R.drawable.ill_crypto_org_card2_120_106,
        cards3ResId = R.drawable.ill_crypto_org_card3_120_106,
        batchIds = setOf("AF57"),
    ),

    CryptoSeth(
        cards2ResId = R.drawable.ill_crypto_seth_card2_120_106,
        cards3ResId = R.drawable.ill_crypto_seth_card3_120_106,
        batchIds = setOf("AF32"),
    ),

    GetsMine(
        cards2ResId = R.drawable.ill_gets_mine_card2_120_106,
        cards3ResId = R.drawable.ill_gets_mine_card3_120_106,
        batchIds = setOf("BB000008"),
    ),

    Grim(
        cards2ResId = R.drawable.ill_grim_card2_120_106,
        cards3ResId = R.drawable.ill_grim_card3_120_106,
        batchIds = setOf("AF13"),
    ),

    Hodl(
        cards2ResId = R.drawable.ill_hodl_card2_120_106,
        cards3ResId = R.drawable.ill_hodl_card3_120_106,
        batchIds = setOf("BB000009"),
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
        batchIds = setOf("AF25", "AF61", "AF72"),
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

    Kasper(
        cards2ResId = R.drawable.ill_kasper_card2_120_106,
        cards3ResId = R.drawable.ill_kasper_card3_120_106,
        batchIds = setOf("AF96"),
    ),

    Kaspy(
        cards2ResId = R.drawable.ill_kaspy_card2_120_106,
        cards3ResId = R.drawable.ill_kaspy_card3_120_106,
        batchIds = setOf("AF95"),
    ),

    KishuInu(
        cards2ResId = R.drawable.ill_kishu_inu_card2_120_106,
        cards3ResId = R.drawable.ill_kishu_inu_card3_120_106,
        batchIds = setOf("AF52"),
    ),

    Kango(
        cards2ResId = R.drawable.ill_kango_card2_120_106,
        cards3ResId = R.drawable.ill_kango_card3_120_106,
        batchIds = setOf("BB000006"),
    ),

    Konan(
        cards2ResId = R.drawable.ill_konan_card2_120_106,
        cards3ResId = R.drawable.ill_konan_card3_120_106,
        batchIds = setOf("AF93"),
    ),

    Kroak(
        cards2ResId = R.drawable.ill_kroak_card2_120_106,
        cards3ResId = R.drawable.ill_kroak_card3_120_106,
        batchIds = setOf("BB000011"),
    ),

    Neiro(
        cards2ResId = R.drawable.ill_neiro_card2_120_106,
        cards3ResId = R.drawable.ill_neiro_card3_120_106,
        batchIds = setOf("AF98"),
    ),

    NewWorldElite(
        cards2ResId = R.drawable.ill_nwe_card2_120_106,
        cards3ResId = R.drawable.ill_nwe_card3_120_106,
        batchIds = setOf("AF26"),
    ),

    PassimPay(
        cards2ResId = R.drawable.ill_passimpay_cards2_120_106,
        cards3ResId = R.drawable.ill_passimpay_cards3_120_106,
        batchIds = setOf("BB000007"),
    ),

    // for multicolored cards use image of 3 cards in all cases
    Pastel(
        cards2ResId = R.drawable.ill_pastel_cards3_120_106,
        cards3ResId = R.drawable.ill_pastel_cards3_120_106,
        batchIds = setOf("AF43", "AF44", "AF45", "AF78", "AF79", "AF80"),
    ),

    RamenCat(
        cards2ResId = R.drawable.ill_ramen_cat_cards2_120_106,
        cards3ResId = R.drawable.ill_ramen_cat_cards3_120_106,
        batchIds = setOf("AF990006", "AF990007", "AF990008"),
    ),

    RedPanda(
        cards2ResId = R.drawable.ill_red_panda_card2_120_106,
        cards3ResId = R.drawable.ill_red_panda_card3_120_106,
        batchIds = setOf("AF34"),
    ),

    Rizo(
        cards2ResId = R.drawable.ill_rizo_card2_120_106,
        cards3ResId = R.drawable.ill_rizo_card3_120_106,
        batchIds = setOf("BB000012"),
    ),

    Sakura(
        cards2ResId = R.drawable.ill_sakura_card2_120_106,
        cards3ResId = R.drawable.ill_sakura_card3_120_106,
        batchIds = setOf("AF990029", "AF990030", "AF990031"),
    ),

    SatoshiFriends(
        cards2ResId = R.drawable.ill_satoshi_card2_120_106,
        cards3ResId = R.drawable.ill_satoshi_card3_120_106,
        batchIds = setOf("AF19"),
    ),

    SinCity(
        cards2ResId = R.drawable.ill_sincity_card2_120_106,
        cards3ResId = R.drawable.ill_sincity_card3_120_106,
        batchIds = setOf("BB000010"),
    ),

    SpringBloom(
        cards2ResId = R.drawable.ill_spring_bloom_card2_120_106,
        cards3ResId = R.drawable.ill_spring_bloom_card3_120_106,
        batchIds = setOf("AF990001", "AF990002", "AF990004"),
    ),

    StealthCard(
        cards2ResId = R.drawable.ill_stealth_cards2_120_106,
        cards3ResId = R.drawable.ill_stealth_cards3_120_106,
        batchIds = setOf("AF60", "AF74", "AF88"),
    ),

    SunDrop(
        cards2ResId = R.drawable.ill_sundrop_cards2_120_106,
        cards3ResId = R.drawable.ill_sundrop_cards3_120_106,
        batchIds = setOf("AF990005", "AF990003"),
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

    USA(
        cards2ResId = R.drawable.ill_usa_card2_120_106,
        cards3ResId = R.drawable.ill_usa_card3_120_106,
        batchIds = setOf("AF91"),
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
        batchIds = setOf("AF40", "AF41", "AF42", "AF75", "AF76", "AF77"),
    ),

    Vnish(
        cards2ResId = R.drawable.ill_vnish_card2_120_106,
        cards3ResId = R.drawable.ill_vnish_card3_120_106,
        batchIds = setOf("BB000005"),
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

    WildGoat(
        cards2ResId = R.drawable.ill_wildgoat_card2_120_106,
        cards3ResId = R.drawable.ill_wildgoat_card3_120_106,
        batchIds = setOf("BB000001"),
    ),

    Winter(
        cards2ResId = R.drawable.ill_winter_card2_120_106,
        cards3ResId = R.drawable.ill_winter_card3_120_106,
        batchIds = setOf("AF85", "AF86", "AF87", "AF990013", "AF990012", "AF990011"),
    ),

    LockedMoney(
        cards2ResId = R.drawable.ill_locked_money_card2_120_106,
        cards3ResId = R.drawable.ill_locked_money_card3_120_106,
        batchIds = setOf("AF63"),
    ),

    Ghoad(
        cards2ResId = R.drawable.ill_ghoad_card2_120_106,
        cards3ResId = R.drawable.ill_ghoad_card3_120_106,
        batchIds = setOf("AF89"),
    ),
}