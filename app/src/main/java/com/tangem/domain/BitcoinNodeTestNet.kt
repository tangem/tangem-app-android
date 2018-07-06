package com.tangem.domain

enum class BitcoinNodeTestNet(val host: String, val port: Int) {
    qtornado_com("testnet.qtornado.com", 51001),
    hsmiths_com("testnet.hsmiths.com", 53011),
    bauerj_eu("testnet1.bauerj.eu", 50001),
    arihanc_com("testnetnode.arihanc.com", 51001),
}