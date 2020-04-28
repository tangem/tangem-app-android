package com.tangem.blockchain.blockchains.bitcoincash.cashaddr;


/**
 * Copyright (c) 2018 Tobias Brandt
 * 
 * Distributed under the MIT software license, see the accompanying file LICENSE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
public enum BitcoinCashAddressType {

	P2PKH((byte) 0), P2SH((byte) 8);

	private final byte versionByte;

	BitcoinCashAddressType(byte versionByte) {
		this.versionByte = versionByte;
	}

	public byte getVersionByte() {
		return versionByte;
	}
}