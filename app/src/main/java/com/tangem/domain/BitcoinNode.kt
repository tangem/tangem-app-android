package com.tangem.domain

enum class BitcoinNode(val host: String, val port: Int) {
	n1("174.138.11.174", 50001),
	n2("btc.cihar.com", 50001),
	n3("electrum.leblancnet.us", 50001),
	n4("electrum.qtornado.com", 50001),
	n5("electrum.vom-stausee.de", 50001),
	n6("helicarrier.bauerj.eu", 50001),
	n7("kirsche.emzy.de", 50001),
	n8("ndnd.selfhost.eu", 50001),
	n9("spv.48.org", 50003),
	n10("such.ninja", 50001),
	n11("tardis.bauerj.eu", 50001),
	n12("vps.hsmiths.com", 50001)
}