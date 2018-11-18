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

//	n16("VPS.hsmiths.com", 50002, "ssl"),
//	n17("agapc7dhcqfjqpjb.onion", 50002, "ssl"),
//	n18("alviss.coinjoined.com", 50002, "ssl"),
//	n19("arihancckjge66iv.onion", 50002, "ssl"),
//	n20("aspinall.io", 50002, "ssl"),
//	n21("bauerjda5hnedjam.onion", 50002, "ssl"),
//	n22("bauerjhejlv6di7s.onion", 50002, "ssl"),
//	n23("bauerjhejlv6di7s.onion", 50002, "ssl"),
//	n24("btc.asis.io", 50002, "ssl"),
//	n25("btc.cihar.com", 50002, "ssl"),
//	n26("btc.ex.laodc.com", 443, "ssl"),
//	n27("btc.gravitech.net", 50002, "ssl"),
//	n28("btc.pr0xima.de", 50002, "ssl"),
//	n29("btc.smsys.me", 995, "ssl"),

}