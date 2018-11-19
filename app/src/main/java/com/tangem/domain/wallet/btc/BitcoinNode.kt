package com.tangem.domain.wallet.btc

enum class BitcoinNode(val host: String, val port: Int, val proto: String) {
	n1("electrum.anduck.net", 50001, "tcp"),
	n2("electrum-server.ninja", 50001, "tcp"),
	n3("btc.cihar.com", 50001, "tcp"),
	n4("vps.hsmiths.com", 50001, "tcp"),
	n5("electrum.hsmiths.com", 50001, "tcp"),
	n6("electrum.vom-stausee.de", 50001, "tcp"),
	n7("node.ispol.sk", 50001, "tcp"),
	n8("electrum2.eff.ro", 50001, "tcp"),
	n9("electrumx.nmdps.net", 50001, "tcp"),
	n10("kirsche.emzy.de", 50001, "tcp"),
	n11("electrum.petrkr.net", 50001, "tcp"),
	n12("electrum.dk", 50001, "tcp"),

//	n13("electrum.anduck.net", 50012, "ssl"),
//	n14("electrum.eff.ro", 50002, "ssl"),
//	n15("vps.hsmiths.com", 50002, "ssl"),


}