package com.tangem.blockchain.blockchains.bitcoincash.cashaddr;
 
// Helper class for CashAddr

public class BitcoinCashAddressDecodedParts {

	String prefix;

	BitcoinCashAddressType addressType;

	byte[] hash;

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public BitcoinCashAddressType getAddressType() {
		return addressType;
	}

	public void setAddressType(BitcoinCashAddressType addressType) {
		this.addressType = addressType;
	}

	public byte[] getHash() {
		return hash;
	}

	public void setHash(byte[] hash) {
		this.hash = hash;
	}

}