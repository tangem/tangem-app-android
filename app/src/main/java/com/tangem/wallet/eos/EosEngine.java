package com.tangem.wallet.eos;

import android.net.Uri;
import android.text.InputFilter;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.tangem.App;
import com.tangem.Constant;
import com.tangem.data.Blockchain;
import com.tangem.data.network.ServerApiEos;
import com.tangem.tangem_card.data.TangemCard;
import com.tangem.tangem_card.reader.CardProtocol;
import com.tangem.tangem_card.tasks.SignTask;
import com.tangem.tangem_card.util.Util;
import com.tangem.util.CryptoUtil;
import com.tangem.util.DecimalDigitsInputFilter;
import com.tangem.wallet.BTCUtils;
import com.tangem.wallet.BalanceValidator;
import com.tangem.wallet.BuildConfig;
import com.tangem.wallet.CoinData;
import com.tangem.wallet.CoinEngine;
import com.tangem.wallet.R;
import com.tangem.wallet.TangemContext;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import io.jafka.jeos.EosApi;
import io.jafka.jeos.EosApiFactory;
import io.jafka.jeos.LocalApi;
import io.jafka.jeos.convert.Packer;
import io.jafka.jeos.core.common.SignArg;
import io.jafka.jeos.core.common.transaction.TransactionAction;
import io.jafka.jeos.core.common.transaction.TransactionAuthorization;
import io.jafka.jeos.core.request.chain.json2bin.TransferArg;
import io.jafka.jeos.core.response.chain.account.Account;
import io.jafka.jeos.core.response.chain.transaction.PushedTransaction;
import io.jafka.jeos.util.Base58;
import io.jafka.jeos.util.Raw;
import io.jafka.jeos.util.ecc.Ripemd160;
import io.reactivex.Observer;
import io.reactivex.observers.DefaultObserver;

public class EosEngine extends CoinEngine {

    private static final String TAG = EosEngine.class.getSimpleName();
    public EosData coinData = null;

    private final int signRetries = 10;

    public EosEngine(TangemContext ctx) throws Exception {
        super(ctx);
        if (ctx.getCoinData() == null) {
            coinData = new EosData();
            ctx.setCoinData(coinData);
        } else if (ctx.getCoinData() instanceof EosData) {
            coinData = (EosData) ctx.getCoinData();
        } else {
            throw new Exception("Invalid type of Blockchain data for " + this.getClass().getSimpleName());
        }
    }

    public EosEngine() {
        super();
    }

    private static int getDecimals() {
        return 4;
    }

    @Override
    public boolean awaitingConfirmation() {
        return App.pendingTransactionsStorage.hasTransactions(ctx.getCard());
    }

    @Override
    public Amount getBalance() {
        if (!hasBalanceInfo()) {
            return null;
        }
        return coinData.getBalance();
    }

    @Override
    public String getBalanceHTML() {
        Amount balance = getBalance();
        if (balance != null) {
            return balance.toDescriptionString(getDecimals());
        } else {
            return "";
        }
    }

    @Override
    public String getBalanceCurrency() {
        return Blockchain.Eos.getCurrency();
    }

    @Override
    public boolean isBalanceNotZero() {
        if (coinData == null) return false;
        if (coinData.getBalance() == null) return false;
        return coinData.getBalance().notZero();
    }

    @Override
    public String getFeeCurrency() {
        return Blockchain.Eos.getCurrency();
    }

    public boolean isNeedCheckNode() {
        return false;
    }


    @Override
    public CoinData createCoinData() {
        return new EosData();
    }

    @Override
    public String getUnspentInputsDescription() {
        return "";
    }

    public void defineWallet() throws CardProtocol.TangemException {
        try {
            String wallet = calculateAddress(ctx.getCard().getWalletPublicKeyRar());
            ctx.getCoinData().setWallet(wallet);
        } catch (Exception e) {
            ctx.getCoinData().setWallet("ERROR");
            throw new CardProtocol.TangemException("Can't define wallet address");
        }

    }

//    BigDecimal convertToEth(String value) {
//        BigInteger m = new BigInteger(value, 10);
//        BigDecimal n = new BigDecimal(m);
//        BigDecimal d = n.divide(new BigDecimal("1000000000000000000"));
//        d = d.setScale(8, RoundingMode.DOWN);
//        return d;
//    }

    @Override
    public boolean validateAddress(String address) {
        if (address.length() != 12) {
            return false;
        }

        if (!address.toLowerCase().equals(address)) {
            return false;
        }

        if (StringUtils.containsAny(address, "06789")) {
            return false;
        }

        return true;
    }

    @Override
    public String getBalanceEquivalent() {
        Amount balance = getBalance();
        if (balance == null) return "";
        return balance.toEquivalentString(coinData.getRate());
    }

    @Override
    public Amount convertToAmount(InternalAmount internalAmount) {
        return new Amount(internalAmount, getBalanceCurrency());
    }

    @Override
    public Amount convertToAmount(String strAmount, String currency) {
        return new Amount(strAmount, currency);
    }

    @Override
    public InternalAmount convertToInternalAmount(Amount amount) {
        return new InternalAmount(amount, getBalanceCurrency());
    }

    @Override
    public InternalAmount convertToInternalAmount(byte[] bytes) {
        //throw new Exception("Not implemented");
        return null;
    }


    @Override
    public byte[] convertToByteArray(InternalAmount amount) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public boolean hasBalanceInfo() {
        return coinData.getBalance() != null;
    }

    @Override
    public Uri getShareWalletUri() { //TODO: check
        return Uri.parse(ctx.getCoinData().getWallet());
    }

    @Override
    public Uri getWalletExplorerUri() {
        return Uri.parse("https://bloks.io/account/" + ctx.getCoinData().getWallet());
    }

    @Override
    public boolean isExtractPossible() {
        if (!hasBalanceInfo()) {
            ctx.setMessage(R.string.loaded_wallet_error_obtaining_blockchain_data);
        } else if (!isBalanceNotZero()) {
            ctx.setMessage(R.string.general_wallet_empty);
        } else if (awaitingConfirmation()) {
            ctx.setMessage(R.string.loaded_wallet_message_wait);
        } else {
            return true;
        }
        return false;
    }

    @Override
    public InputFilter[] getAmountInputFilters() {
        return new InputFilter[]{new DecimalDigitsInputFilter(getDecimals())};
    }

    @Override
    public boolean checkNewTransactionAmount(Amount amount) {
        if (BuildConfig.FLAVOR == Constant.FLAVOR_TANGEM_CARDANO) {
            return true;
        }
        Amount balance = getBalance();
        if (balance == null || amount.compareTo(balance) > 0) {
            return false;
        }
        return true;
    }

    @Override
    public boolean checkNewTransactionAmountAndFee(Amount amount, Amount fee, Boolean isFeeIncluded) {
        try {
            BigDecimal cardBalance = getBalance();

            if (isFeeIncluded && (amount.compareTo(cardBalance) > 0 || amount.compareTo(fee) < 0))
                return false;

            if (!isFeeIncluded && amount.add(fee).compareTo(cardBalance) > 0)
                return false;

        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override //TODO: check
    public boolean validateBalance(BalanceValidator balanceValidator) {
        if (getBalance() == null) {
            balanceValidator.setScore(0);
            balanceValidator.setFirstLine(R.string.balance_validator_first_line_unknown_balance);
            balanceValidator.setSecondLine(R.string.balance_validator_second_line_unverified_balance);
            return false;
        }

        if (coinData.isBalanceReceived()) {
            balanceValidator.setScore(100);
            balanceValidator.setFirstLine(R.string.balance_validator_first_line_verified_balance);
            balanceValidator.setSecondLine(R.string.balance_validator_second_line_confirmed_in_blockchain);
            if (getBalance().isZero()) {
                balanceValidator.setFirstLine(R.string.balance_validator_first_line_empty_wallet);
                balanceValidator.setSecondLine(R.string.empty_string);
            }
        }

        if ((ctx.getCard().getOfflineBalance() != null) && !coinData.isBalanceReceived() && (ctx.getCard().getRemainingSignatures() == ctx.getCard().getMaxSignatures()) && getBalance().notZero()) {
            balanceValidator.setScore(80);
            balanceValidator.setFirstLine(R.string.balance_validator_first_line_verified_offline);
            balanceValidator.setSecondLine(R.string.balance_validator_second_line_internet_to_verify_online);
        }

        return true;

    }

    @Override
    public String evaluateFeeEquivalent(String fee) {
        try {
            Amount feeValue = new Amount(fee, ctx.getBlockchain().getCurrency());
            return feeValue.toEquivalentString(coinData.getRate());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public String calculateAddress(byte[] pkCompressed) {
        String cid = Util.bytesToHex(ctx.getCard().getCID()).toLowerCase();
        String address = cid.substring(0, 4) + cid.substring(8, 16);
        address = address.replace("0", "o").replace("6", "b").replace("7", "t").replace("8", "s").replace("9", "g");
        return address;

//        String cid = Util.bytesToHex(ctx.getCard().getCID());
//        String address = "testem" + cid.substring(2, 4) + cid.substring(11, 15);
//        address = address.replace("0", "o").replace("6", "b").replace("7,", "t").replace("8", "s").replace("9", "g");
//        return address;

//        byte[] csum = Ripemd160.from(pkCompressed).bytes();
//        csum = script.copy(csum, 0, 4);
//        byte[] addy = script.concat(pkCompressed, csum);
//        StringBuffer bf = new StringBuffer("EOS");
//        bf.append(Base58.encode(addy));
//        return bf.toString() + " " + address;
    }

    public String calculateEosPubKey(byte[] pkCompressed) {
        byte[] csum = Ripemd160.from(pkCompressed).bytes();
        csum = Raw.copy(csum, 0, 4);
        byte[] addy = Raw.concat(pkCompressed, csum);
        StringBuffer bf = new StringBuffer("EOS");
        bf.append(Base58.encode(addy));
        return bf.toString();
    }

    // reference - https://gist.github.com/adyliu/492503b94d0306371298f24e15481da4
    @Override
    public SignTask.TransactionToSign constructTransaction(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) throws JsonProcessingException {

        // get the current state of blockchain
        EosApi eosApi = EosApiFactory.create("https://api.eosdetroit.io:443");
        SignArg arg = eosApi.getSignArg(120);
        System.out.println(eosApi.getObjectMapper().writeValueAsString(arg));


        // --- prepare transaction for sign as in LocalApiImpl
        String quantity = amountValue.setScale(4).toString();
        String memo = "";

        // ① pack transfer data
        TransferArg transferArg = new TransferArg(coinData.getWallet(), targetAddress, quantity, memo);
        String transferData = Packer.packTransfer(transferArg);
        //

        // ③ create the authorization
        List<TransactionAuthorization> authorizations = Arrays.asList(new TransactionAuthorization(coinData.getWallet(), "active"));

        // ④ build the all actions
        List<TransactionAction> actions = Arrays.asList(//
                new TransactionAction("eosio.token", "transfer", authorizations, transferData)//
        );

        long expMillis = System.currentTimeMillis() + (arg.getExpiredSecond() * 1000);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.5", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String stringTime = format.format(new Date(expMillis));

        // ⑤ build the packed transaction
        EosPackedTransaction packedTransaction = new EosPackedTransaction();
        packedTransaction.setExpiration(stringTime);
        packedTransaction.setRefBlockNum(arg.getLastIrreversibleBlockNum());
        packedTransaction.setRefBlockPrefix(arg.getRefBlockPrefix());

        packedTransaction.setMaxNetUsageWords(0);
        packedTransaction.setMaxCpuUsageMs(0);
        packedTransaction.setDelaySec(0);
        packedTransaction.setActions(actions);

        Raw raw = EosPacker.packPackedTransaction(arg.getChainId(), packedTransaction);
        raw.pack(ByteBuffer.allocate(33).array()); //black magic
        Sha256Hash hashForSign = Sha256Hash.of(raw.bytes());

        return new SignTask.TransactionToSign() {
            @Override
            public boolean isSigningMethodSupported(TangemCard.SigningMethod signingMethod) {
                return signingMethod == TangemCard.SigningMethod.Sign_Hash;
            }

            @Override
            public byte[][] getHashesToSign() throws NoSuchAlgorithmException {
                byte[][] hashesForSign = new byte[signRetries][];
                for (int i = 0; i < signRetries; i++) {
                    hashesForSign[i] = hashForSign.getBytes();
                }
                return hashesForSign;
            }

            @Override
            public byte[] getRawDataToSign() throws Exception {
                throw new Exception("Signing of raw transaction not supported for " + this.getClass().getSimpleName());
            }

            @Override
            public String getHashAlgToSign() throws Exception {
                throw new Exception("Signing of raw transaction not supported for " + this.getClass().getSimpleName());
            }

            @Override
            public byte[] getIssuerTransactionSignature(byte[] dataToSignByIssuer) throws Exception {
                throw new Exception("Transaction validation by issuer not supported in this version");
            }

            @Override
            public byte[] onSignCompleted(byte[] signFromCard) throws Exception {
                BigInteger r = null, s = null;
                for (int i = 0; i < signRetries; ++i) {
                    r = new BigInteger(1, Arrays.copyOfRange(signFromCard, i * 64, 32 + i * 64));

                    if (r.toByteArray().length == 33) {
                        Log.e(TAG, "33 bytes R: " + Util.bytesToHex(r.toByteArray()));
                    } else {
                        s = new BigInteger(1, Arrays.copyOfRange(signFromCard, 32 + i * 64, 64 + i * 64));
                        s = CryptoUtil.toCanonicalised(s);
                        break;
                    }
                }
                if (s == null) {
                    throw new Exception("All signatures not canonical");
                }

                boolean f = ECKey.verify(Util.calculateSHA256(raw.bytes()), new ECKey.ECDSASignature(r, s), ctx.getCard().getWalletPublicKey());

                if (!f) {
                    Log.e(this.getClass().getSimpleName() + "-CHECK", "sign Failed.");
                }

                ECKey.ECDSASignature ecdsaSig = new ECKey.ECDSASignature(r, s);
                int v = BruteRecoveryID(ecdsaSig, hashForSign, ctx.getCard().getWalletPublicKeyRar());

                v += 4; // compressed
                v += 27; // compact // 24 or 27 :( forcing odd-y 2nd key candidate)

                byte[] rbytes = Utils.bigIntegerToBytes(r, 32);
                byte[] sbytes = Utils.bigIntegerToBytes(s, 32);

                //TODO: not every signature works for EOS, if r.toByteArray length is 33, even if first 0x00 byte is cut,
                //TODO: signature would be counted non canonical because first bit is not zero. Need to check it on card or there will be multiple security delays
//                if (r.toByteArray().length == 33) {
//                    Log.e(TAG, "33 bytes R: " + Util.bytesToHex(rbytes));
//                    throw new Exception("33 byte R");
//                }

                byte[] pub_buf = new byte[65];
                pub_buf[0] = (byte) v;

                System.arraycopy(rbytes, 0, pub_buf, 1, rbytes.length);
                System.arraycopy(sbytes, 0, pub_buf, rbytes.length + 1, sbytes.length);

                byte[] checksum = Ripemd160.from(Raw.concat(pub_buf, "K1".getBytes())).bytes();

                byte[] signatureBytes = Raw.concat(pub_buf, Raw.copy(checksum, 0, 4));
                Log.e(TAG, "Signature hex " + Util.byteArrayToHexString(signatureBytes));

                String signatureString = "SIG_K1_" + Base58.encode(signatureBytes);
                Log.e(TAG, "1st sig" + signatureString);

//                byte[] sigDer = DerEncodingUtil.DerEncoding(newR, s);
//                String eosPubKey = calculateEosPubKey(ctx.getCard().getWalletPublicKeyRar());
//                String pemPubKey = EOSFormatter.convertEOSPublicKeyToPEMFormat(eosPubKey);
//                String convertedSignature = EOSFormatter.convertDERSignatureToEOSFormat(sigDer, raw.bytes(), pemPubKey);
//                Log.e(TAG, "2nd sig" + convertedSignature);
//                String convertedBase = convertedSignature.substring(7);
//                byte[] convertedBytes = Base58.decode(convertedBase);

                EosPushTransactionRequest req = new EosPushTransactionRequest();
                req.setTransaction(packedTransaction);
                req.setSignatures(Arrays.asList(signatureString));

                //serialize
                String reqString = new Gson().toJson(req);
                byte[] txForSend = SerializationUtils.serialize(reqString);

                notifyOnNeedSendTransaction(txForSend);
                return txForSend;
            }
        };
    }

    private int BruteRecoveryID(ECKey.ECDSASignature sig, Sha256Hash messageHash, byte[] thisKey) {
        Log.e("EOS_KZ", BTCUtils.toHex(thisKey));
        int recId = -1;
        for (int i = 0; i < 4; i++) {
            ECKey k = ECKey.recoverFromSignature(i, sig, messageHash, true);

            if (k == null)
                continue;
            byte[] recK = k.getPubKey();
            Log.e("EOS_k " + i, BTCUtils.toHex(recK));
            if (Arrays.equals(recK, thisKey)) {
                recId = i;
                break;
            }
        }
        return recId;
    }

    @Override
    public void requestBalanceAndUnspentTransactions(BlockchainRequestsCallbacks blockchainRequestsCallbacks) {
        Observer<Account> accountObserver = new DefaultObserver<Account>() {
            @Override
            public void onNext(Account account) {
                if (account.getCoreLiquidBalance() != null) {
                    String[] balanceStrings = account.getCoreLiquidBalance().split(" ");
                    coinData.setBalanceReceived(true);
                    coinData.setBalance(new Amount(balanceStrings[0], balanceStrings[1]));
                } else {
                    coinData.setBalanceReceived(true);
                    coinData.setBalance(new Amount(0L, getBalanceCurrency()));
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "requestBalanceAndUnspentTransactions error" + e.getMessage());
                ctx.setError(e.getMessage());
                blockchainRequestsCallbacks.onComplete(false);
            }

            @Override
            public void onComplete() {
                blockchainRequestsCallbacks.onComplete(true);
            }
        };
        ServerApiEos.getBalance(coinData.getWallet(), accountObserver);
    }

    @Override
    public void requestFee(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) {
        // no fee in EOS
        coinData.minFee = coinData.normalFee = coinData.maxFee = new Amount(0L, "EOS");
        blockchainRequestsCallbacks.onComplete(true);
    }

    @Override
    public void requestSendTransaction(BlockchainRequestsCallbacks blockchainRequestsCallbacks, byte[] txForSend) throws IOException, ClassNotFoundException {
        // deserialize
        String reqString = SerializationUtils.deserialize(txForSend);
        EosPushTransactionRequest req = new Gson().fromJson(reqString, EosPushTransactionRequest.class);

        Observer<PushedTransaction> sendObserver = new DefaultObserver<PushedTransaction>() {
            @Override
            public void onNext(PushedTransaction pushedTransaction) {
                if (pushedTransaction.getProcessed().getReceipt().getStatus().equals("executed")) {
                    ctx.setError(null);
                } else {
                    ctx.setError("Error sending transaction. Transaction rejected");
                    try {
                        LocalApi localApi = EosApiFactory.createLocalApi();
                        Log.e(TAG, localApi.getObjectMapper().writeValueAsString(pushedTransaction));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "requestSendTransaction error" + e.getMessage());
                ctx.setError(e.getMessage());
                LocalApi localApi = EosApiFactory.createLocalApi();
                try {
                    Log.e(TAG, localApi.getObjectMapper().writeValueAsString(req));
                } catch (JsonProcessingException e1) {
                    e1.printStackTrace();
                }
                blockchainRequestsCallbacks.onComplete(false);
            }

            @Override
            public void onComplete() {
                if (!ctx.hasError()) {
                    blockchainRequestsCallbacks.onComplete(true);
                } else {
                    blockchainRequestsCallbacks.onComplete(false);
                }
            }
        };

        ServerApiEos.sendTransaction(req, sendObserver);
    }

    public int pendingTransactionTimeoutInSeconds() {
        return 10;
    }

    @Override
    public boolean needMultipleLinesForBalance() {
        return true;
    }

    @Override
    public boolean allowSelectFeeLevel() {
        return false;
    }

    @Override
    public boolean allowSelectFeeInclusion() {
        return false;
    }

}
