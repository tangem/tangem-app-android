package com.tangem.domain.wallet.bch

enum class BitcoinCashNode(val host: String, val port: Int, val proto: String) {
    N_001("electrumx.hillsideinternet.com", 50002, "ssl"),
    N_002("bch0.kister.net", 50002, "ssl"),
    N_003("abc1.hsmiths.com", 60002, "ssl"),
    N_004("bch.curalle.ovh", 50002, "ssl"),
    N_005("207.180.215.112", 52002, "ssl"),
    N_006("bch.imaginary.cash", 50002, "ssl"),
    N_007("dedi.jochen-hoenicke.de", 51002, "ssl"),
    N_008("crypto.mldlabs.com", 50002, "ssl"),
    N_009("bch.electrumx.cash", 50002, "ssl"),
    N_010("electroncash.cascharia.com", 50002, "ssl"),
    N_011("bch.crypto.mldlabs.com", 50002, "ssl"),
    N_012("electron-cash.dragon.zone", 50002, "ssl"),
    N_013("electron.coinucopia.io", 50002, "ssl"),
    N_014("blackie.c3-soft.com", 50002, "ssl"),
    N_015("electroncash.ueo.ch", 51002, "ssl"),
    N_016("electrum.imaginary.cash", 50002, "ssl"),
    N_017("35.157.238.5", 51002, "ssl"),
    N_018("bitcoincash.quangld.com", 50002, "ssl"),
    N_019("bch.stitthappens.com", 50002, "ssl"),
    N_020("electroncash.dk", 50002, "ssl"),
}