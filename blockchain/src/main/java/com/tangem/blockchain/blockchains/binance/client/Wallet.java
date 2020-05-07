package com.tangem.blockchain.blockchains.binance.client;

import com.tangem.blockchain.blockchains.binance.client.domain.Account;
import com.tangem.blockchain.blockchains.binance.client.domain.AccountSequence;
import com.tangem.blockchain.blockchains.binance.client.domain.Infos;
import com.tangem.blockchain.blockchains.binance.client.encoding.Crypto;
import com.tangem.blockchain.blockchains.binance.client.encoding.message.MessageType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.bitcoinj.core.ECKey;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Wallet {
    private final static Map<BinanceDexEnvironment, String> CHAIN_IDS = new HashMap<>();
    private String privateKey;
    private String address;
    private ECKey ecKey;
    private byte[] addressBytes;
    private byte[] pubKeyForSign;
    private Integer accountNumber;
    private Long sequence = null;
    private BinanceDexEnvironment env;

    private String chainId;

    public Wallet(String privateKey, BinanceDexEnvironment env) {
        if (!StringUtils.isEmpty(privateKey)) {
            this.privateKey = privateKey;
            this.env = env;
            this.ecKey = ECKey.fromPrivate(new BigInteger(privateKey, 16));
            this.address = Crypto.getAddressFromECKey(this.ecKey, env.getHrp());
            this.addressBytes = Crypto.decodeAddress(this.address);
            byte[] pubKey = ecKey.getPubKeyPoint().getEncoded(true);
            byte[] pubKeyPrefix = MessageType.PubKey.getTypePrefixBytes();
            this.pubKeyForSign = new byte[pubKey.length + pubKeyPrefix.length + 1];
            System.arraycopy(pubKeyPrefix, 0, this.pubKeyForSign, 0, pubKeyPrefix.length);
            pubKeyForSign[pubKeyPrefix.length] = (byte) 33;
            System.arraycopy(pubKey, 0, this.pubKeyForSign, pubKeyPrefix.length + 1, pubKey.length);
        } else {
            throw new IllegalArgumentException("Private key cannot be empty.");
        }
    }

    public static Wallet createRandomWallet(BinanceDexEnvironment env) throws IOException {
        return createWalletFromMnemonicCode(Crypto.generateMnemonicCode(), env);
    }

    public static Wallet createWalletFromMnemonicCode(List<String> words, BinanceDexEnvironment env) throws IOException {
        String privateKey = Crypto.getPrivateKeyFromMnemonicCode(words);
        return new Wallet(privateKey, env);
    }

    public synchronized void initAccount(BinanceDexApiRestClient client) {
        Account account = client.getAccount(this.address);
        if (account != null) {
            this.accountNumber = account.getAccountNumber();
            this.sequence = account.getSequence();
        } else {
            throw new IllegalStateException("Cannot get account information for address " + this.address);
        }
    }

    public synchronized void reloadAccountSequence(BinanceDexApiRestClient client) {
        AccountSequence accountSequence = client.getAccountSequence(this.address);
        this.sequence = accountSequence.getSequence();
    }

    public synchronized void increaseAccountSequence() {
        if (this.sequence != null)
            this.sequence++;
    }

    public synchronized void decreaseAccountSequence() {
        if (this.sequence != null)
            this.sequence--;
    }

    public synchronized long getSequence() {
        if (sequence == null)
            throw new IllegalStateException("Account sequence is not initialized.");
        return sequence;
    }

    public synchronized void setAccountNumber(Integer accountNumber) {
        this.accountNumber = accountNumber;
    }

    public synchronized void setSequence(Long sequence) {
        this.sequence = sequence;
    }

    public synchronized void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public synchronized void invalidAccountSequence() {
        this.sequence = null;
    }

    public synchronized void ensureWalletIsReady(BinanceDexApiRestClient client) {
        if (accountNumber == null) {
            initAccount(client);
        } else if (sequence == null) {
            reloadAccountSequence(client);
        }

        if (chainId == null) {
            chainId = CHAIN_IDS.get(chainId);
            if (chainId == null) {
                initChainId(client);
            }
        }
    }

    public synchronized void initChainId(BinanceDexApiRestClient client) {
        Infos info = client.getNodeInfo();
        chainId = info.getNodeInfo().getNetwork();
        CHAIN_IDS.put(env, chainId);
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getAddress() {
        return address;
    }

    public ECKey getEcKey() {
        return ecKey;
    }

    public byte[] getPubKeyForSign() {
        return pubKeyForSign;
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public String getChainId() {
        return chainId;
    }

    public byte[] getAddressBytes() {
        return addressBytes;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, BinanceDexConstants.BINANCE_DEX_TO_STRING_STYLE)
                .append("addressBytes", addressBytes)
                .append("address", address)
                .append("ecKey", ecKey)
                .append("pubKeyForSign", pubKeyForSign)
                .append("accountNumber", accountNumber)
                .append("sequence", sequence)
                .append("chainId", chainId)
                .toString();
    }
}
