package com.tangem.domain

enum class BitcoinNode(val host: String, val port: Int) {
    n13("btc.cihar.com", 50001),
    n14("electrum.vom-stausee.de", 50001),
    n15("helicarrier.bauerj.eu", 50001),
    n16("kirsche.emzy.de", 50001),
    n17("spv.48.org", 50003),
    n18("vps.hsmiths.com", 50001),
}