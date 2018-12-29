package com.tangem.domain.wallet.ducatus;

import android.net.Uri;
import android.text.InputFilter;
import android.util.Log;

import com.tangem.data.network.ElectrumRequest;
import com.tangem.data.network.ServerApiInsight;
import com.tangem.data.network.model.InsightResponse;
import com.tangem.domain.wallet.BTCUtils;
import com.tangem.domain.wallet.BalanceValidator;
import com.tangem.domain.wallet.Base58;
import com.tangem.domain.wallet.CoinData;
import com.tangem.domain.wallet.TangemContext;
import com.tangem.domain.wallet.Transaction;
import com.tangem.domain.wallet.UnspentOutputInfo;
import com.tangem.domain.wallet.btc.BtcData;
import com.tangem.domain.wallet.btc.BtcEngine;
import com.tangem.tangemcard.data.TangemCard;
import com.tangem.tangemcard.reader.CardProtocol;
import com.tangem.tangemcard.tasks.SignTask;
import com.tangem.tangemcard.util.Util;
import com.tangem.util.CryptoUtil;
import com.tangem.util.DecimalDigitsInputFilter;
import com.tangem.util.DerEncodingUtil;
import com.tangem.wallet.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DucatusEngine extends BtcEngine {
    private static final String TAG = DucatusEngine.class.getSimpleName();
    public BtcData coinData = null;

    public DucatusEngine(TangemContext context) throws Exception {
        super(context);
        if (context.getCoinData() == null) {
            coinData = new BtcData();
            context.setCoinData(coinData);
        } else if (context.getCoinData() instanceof BtcData) {
            coinData = (BtcData) context.getCoinData();
        } else {
            throw new Exception("Invalid type of Blockchain data for LtcEngine");
        }
    }

    public DucatusEngine() {
        super();
    }

    private static int getDecimals() {
        return 8;
    }


    private void checkBlockchainDataExists() throws Exception {
        if (coinData == null) throw new Exception("No blockchain data");
    }

    @Override
    public boolean awaitingConfirmation() {
        if (coinData == null) return false;
        return coinData.getBalanceUnconfirmed() != 0;
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
        return "LTC";
    }

    @Override
    public String getOfflineBalanceHTML() {
        InternalAmount offlineInternalAmount = convertToInternalAmount(ctx.getCard().getOfflineBalance());
        Amount offlineAmount = convertToAmount(offlineInternalAmount);
        return offlineAmount.toDescriptionString(getDecimals());
    }

    @Override
    public boolean isBalanceNotZero() {
        if (coinData == null) return false;
        if (coinData.getBalanceInInternalUnits() == null) return false;
        return coinData.getBalanceInInternalUnits().notZero();
    }

    @Override
    public boolean hasBalanceInfo() {
        if (coinData == null) return false;
        return coinData.hasBalanceInfo();
    }


    @Override
    public boolean isExtractPossible() {
        if (!hasBalanceInfo()) {
            ctx.setMessage(R.string.cannot_obtain_data_from_blockchain);
        } else if (!isBalanceNotZero()) {
            ctx.setMessage(R.string.wallet_empty);
        } else if (awaitingConfirmation()) {
            ctx.setMessage(R.string.please_wait_while_previous);
        } else if (coinData.getUnspentTransactions().size() == 0) {
            ctx.setMessage(R.string.please_wait_for_confirmation);
        } else {
            return true;
        }
        return false;
    }

    @Override
    public String getFeeCurrency() {
        return "LTC";
    }

    @Override
    public boolean validateAddress(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }

        if (address.length() < 25) {
            return false;
        }

        if (address.length() > 35) {
            return false;
        }

        if (!address.startsWith("L") && !address.startsWith("M")) {
            return false;
        }

        byte[] decAddress = Base58.decodeBase58(address);

        if (decAddress == null || decAddress.length == 0) {
            return false;
        }

        byte[] rip = new byte[21];
        for (int i = 0; i < 21; ++i) {
            rip[i] = decAddress[i];
        }

        byte[] kcv = CryptoUtil.doubleSha256(rip);

        for (int i = 0; i < 4; ++i) {
            if (kcv[i] != decAddress[21 + i])
                return false;
        }

        return true;
    }


    @Override
    public boolean isNeedCheckNode() {
        return true;
    }

    @Override
    public Uri getShareWalletUriExplorer() {
        return Uri.parse("https://live.blockcypher.com/ltc/address/" + ctx.getCoinData().getWallet());
    }

    @Override
    public Uri getShareWalletUri() {
        if (ctx.getCard().getDenomination() != null) {
            return Uri.parse("litecoin:" + ctx.getCoinData().getWallet() + "?amount=" + convertToAmount(convertToInternalAmount(ctx.getCard().getDenomination())).toValueString(8));
        } else {
            return Uri.parse("litecoin:" + ctx.getCoinData().getWallet());
        }
    }

    @Override
    public InputFilter[] getAmountInputFilters() {
        return new InputFilter[]{new DecimalDigitsInputFilter(getDecimals())};
    }

    @Override
    public boolean checkNewTransactionAmount(Amount amount) {
        if (coinData == null) return false;
        if (amount.compareTo(convertToAmount(coinData.getBalanceInInternalUnits())) > 0) {
            return false;
        }
        return true;
    }

    @Override
    public boolean checkNewTransactionAmountAndFee(Amount amountValue, Amount feeValue, Boolean isIncludeFee) {
        InternalAmount fee;
        InternalAmount amount;

        try {
            checkBlockchainDataExists();
            amount = convertToInternalAmount(amountValue);
            fee = convertToInternalAmount(feeValue);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        if (fee == null || amount == null)
            return false;

        if (fee.isZero() || amount.isZero())
            return false;

        if (isIncludeFee && (amount.compareTo(coinData.getBalanceInInternalUnits()) > 0 || amount.compareTo(fee) < 0))
            return false;

        if (!isIncludeFee && amount.add(fee).compareTo(coinData.getBalanceInInternalUnits()) > 0)
            return false;

        return true;
    }

    @Override
    public boolean validateBalance(BalanceValidator balanceValidator) {
        if (((ctx.getCard().getOfflineBalance() == null) && !ctx.getCoinData().isBalanceReceived()) || (!ctx.getCoinData().isBalanceReceived() && (ctx.getCard().getRemainingSignatures() != ctx.getCard().getMaxSignatures()))) {
            balanceValidator.setScore(0);
            balanceValidator.setFirstLine("Unknown balance");
            balanceValidator.setSecondLine("Balance cannot be verified. Swipe down to refresh.");
            return false;
        }

        // Workaround before new back-end
//        if (card.getRemainingSignatures() == card.getMaxSignatures()) {
//            firstLine = "Verified balance";
//            secondLine = "Balance confirmed in blockchain. ";
//            secondLine += "Verified note identity. ";
//            return;
//        }

        if (coinData.getBalanceUnconfirmed() != 0) {
            balanceValidator.setScore(0);
            balanceValidator.setFirstLine("Transaction in progress");
            balanceValidator.setSecondLine("Wait for confirmation in blockchain");
            return false;
        }

        if (coinData.isBalanceReceived() && coinData.isBalanceEqual()) {
            balanceValidator.setScore(100);
            balanceValidator.setFirstLine("Verified balance");
            balanceValidator.setSecondLine("Balance confirmed in blockchain");
            if (coinData.getBalanceInInternalUnits().isZero()) {
                balanceValidator.setFirstLine("Empty wallet");
                balanceValidator.setSecondLine("");
            }
        }

        // rule 4 TODO: need to check SignedHashed against number of outputs in blockchain
//        if((card.getRemainingSignatures() != card.getMaxSignatures()) && card.getBalance() != 0)
//        {
//            score = 80;
//            firstLine = "Unguaranteed balance";
//            secondLine = "Potential unsent transaction. Redeem immediately if accept. ";
//            return;
//        }

        if ((ctx.getCard().getOfflineBalance() != null) && !coinData.isBalanceReceived() && (ctx.getCard().getRemainingSignatures() == ctx.getCard().getMaxSignatures()) && coinData.getBalanceInInternalUnits().notZero()) {
            balanceValidator.setScore(80);
            balanceValidator.setFirstLine("Verified offline balance");
            balanceValidator.setSecondLine("Can't obtain balance from blockchain. Restore internet connection to be more confident. ");
        }

//            if(card.getFailedBalanceRequestCounter()!=0) {
//                score -= 5 * card.getFailedBalanceRequestCounter();
//                secondLine += "Not all nodes have returned balance. Swipe down or tap again. ";
//                if(score <= 0)
//                    return;
//            }

        //
//            if(card.isBalanceReceived() && !card.isBalanceEqual()) {
//                score = 0;
//                firstLine = "Disputed balance";
//                secondLine += " Cannot obtain trusted balance at the moment. Try to tap and check this banknote later.";
//                return;
//            }

        return true;
    }

    @Override
    public Amount getBalance() {
        if (!hasBalanceInfo()) return null;
        return convertToAmount(coinData.getBalanceInInternalUnits());
    }

    @Override
    public String evaluateFeeEquivalent(String fee) {
        if (!coinData.getAmountEquivalentDescriptionAvailable()) return "";
        try {
            Amount feeAmount = new Amount(fee, getFeeCurrency());
            return feeAmount.toEquivalentString(coinData.getRate());
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String getBalanceEquivalent() {
        if (coinData == null || !coinData.getAmountEquivalentDescriptionAvailable()) return "";
        Amount balance = getBalance();
        if (balance == null) return "";
        return balance.toEquivalentString(coinData.getRate());
    }

    @Override
    public String calculateAddress(byte[] pkUncompressed) throws NoSuchProviderException, NoSuchAlgorithmException {
        byte netSelectionByte = (byte) 0x30;

        byte hash1[] = Util.calculateSHA256(pkUncompressed);
        byte hash2[] = Util.calculateRIPEMD160(hash1);

        ByteBuffer BB = ByteBuffer.allocate(hash2.length + 1);

        BB.put(netSelectionByte);
        BB.put(hash2);

        byte hash3[] = Util.calculateSHA256(BB.array());
        byte hash4[] = Util.calculateSHA256(hash3);

        BB = ByteBuffer.allocate(hash2.length + 5);
        BB.put(netSelectionByte); //BB.put((byte) 0x6f);
        BB.put(hash2);
        BB.put(hash4[0]);
        BB.put(hash4[1]);
        BB.put(hash4[2]);
        BB.put(hash4[3]);

        return org.bitcoinj.core.Base58.encode(BB.array());
    }

    @Override
    public Amount convertToAmount(InternalAmount internalAmount) {
        BigDecimal d = internalAmount.divide(new BigDecimal("100000000"));
        return new Amount(d, getBalanceCurrency());
    }

    @Override
    public Amount convertToAmount(String strAmount, String currency) {
        return new Amount(strAmount, currency);
    }

    @Override
    public InternalAmount convertToInternalAmount(Amount amount) {
        BigDecimal d = amount.multiply(new BigDecimal("100000000"));
        return new InternalAmount(d, "Satoshi");
    }

    @Override
    public InternalAmount convertToInternalAmount(byte[] bytes) {
        if (bytes == null) return null;
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) reversed[i] = bytes[bytes.length - i - 1];
        return new InternalAmount(Util.byteArrayToLong(reversed), "Satoshi");
    }

    @Override
    public byte[] convertToByteArray(InternalAmount internalAmount) {
        byte[] bytes = Util.longToByteArray(internalAmount.longValueExact());
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) reversed[i] = bytes[bytes.length - i - 1];
        return reversed;
    }

    @Override
    public CoinData createCoinData() {
        return new BtcData();
    }

    @Override
    public String getUnspentInputsDescription() {
        return coinData.getUnspentInputsDescription();
    }

    @Override
    public SignTask.PaymentToSign constructPayment(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) throws Exception {
        final ArrayList<UnspentOutputInfo> unspentOutputs;
        checkBlockchainDataExists();

        String myAddress = ctx.getCoinData().getWallet();
        byte[] pbKey = ctx.getCard().getWalletPublicKey();

        // Build script for our address
        List<BtcData.UnspentTransaction> rawTxList = coinData.getUnspentTransactions();
        byte[] outputScriptWeAreAbleToSpend = Transaction.Script.buildOutput(myAddress).bytes;

        // Collect unspent
        unspentOutputs = BTCUtils.getOutputs(rawTxList, outputScriptWeAreAbleToSpend);

        long fullAmount = 0;
        for (int i = 0; i < unspentOutputs.size(); ++i) {
            fullAmount += unspentOutputs.get(i).value;
        }

        long fees = convertToInternalAmount(feeValue).longValueExact();
        long amount = convertToInternalAmount(amountValue).longValueExact();
        long change = fullAmount - amount;
        if (IncFee) {
            amount = amount - fees;
        } else {
            change = change - fees;
        }

        final long amountFinal=amount;
        final long changeFinal=change;

        if (amount + fees > fullAmount) {
            throw new CardProtocol.TangemException_WrongAmount(String.format("Balance (%d) < change (%d) + amount (%d)", fullAmount, change, amount));
        }

        final byte[][] txForSign = new byte[unspentOutputs.size()][];
        final byte[][] bodyDoubleHash = new byte[unspentOutputs.size()][];
        final byte[][] bodyHash= new byte[unspentOutputs.size()][];

        for (int i = 0; i < unspentOutputs.size(); ++i) {
            txForSign[i] = BTCUtils.buildTXForSign(myAddress, targetAddress, myAddress, unspentOutputs, i, amount, change);
            bodyHash[i] = Util.calculateSHA256(txForSign[i]);
            bodyDoubleHash[i] = Util.calculateSHA256(bodyHash[i]);
        }

        return new SignTask.PaymentToSign() {

            @Override
            public boolean isSigningMethodSupported(TangemCard.SigningMethod signingMethod) {
                return signingMethod==TangemCard.SigningMethod.Sign_Hash || signingMethod==TangemCard.SigningMethod.Sign_Raw;
            }

            @Override
            public byte[][] getHashesToSign() throws Exception {
                byte[][] dataForSign=new byte[unspentOutputs.size()][];
                if (txForSign.length > 10) throw new Exception("To much hashes in one transaction!");
                for (int i = 0; i < unspentOutputs.size(); ++i) {
                    dataForSign[i] = bodyDoubleHash[i];
                }
                return dataForSign;
            }

            @Override
            public byte[] getRawDataToSign() throws Exception {
                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                for (int i = 0; i < txForSign.length; i++) {
                    if (i != 0 && txForSign[0].length != txForSign[i].length)
                        throw new Exception("Hashes length must be identical!");
                    bs.write(txForSign[i]);
                }

                return bs.toByteArray();
            }

            @Override
            public String getHashAlgToSign() {
                return "sha-256x2";
            }

            @Override
            public byte[] getIssuerTransactionSignature(byte[] dataToSignByIssuer) throws Exception {
                throw new Exception("Issuer validation not supported!");
            }

            @Override
            public byte[] onSignCompleted(byte[] signFromCard) throws Exception {
                for (int i = 0; i < unspentOutputs.size(); ++i) {
                    BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, i * 64, 32 + i * 64));
                    BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, 32 + i * 64, 64 + i * 64));
                    s = CryptoUtil.toCanonicalised(s);

                    unspentOutputs.get(i).scriptForBuild = DerEncodingUtil.packSignDer(r, s, pbKey);
                }

                byte[] txForSend=BTCUtils.buildTXForSend(targetAddress, myAddress, unspentOutputs, amountFinal, changeFinal);
                notifyOnNeedSendPayment(txForSend);
                return txForSend;
            }
        };
    }

    @Override
    public void requestBalanceAndUnspentTransactions(BlockchainRequestsCallbacks blockchainRequestsCallbacks) {
        final ServerApiInsight serverApiInsight = new ServerApiInsight();

        ServerApiInsight.InsightBodyListener insightBodyListenerListener = new ServerApiInsight.InsightBodyListener() {
            public void onSuccess(String method, InsightResponse insightResponse) {
                switch (method) {
                    case ServerApiInsight.INSIGHT_ADDRESS: {
                        try {
                            String walletAddress = insightResponse.getAddrStr();
                            if (!walletAddress.equals(coinData.getWallet())) {
                                // todo - check
                                throw new Exception("Invalid wallet address in answer!");
                            }
                            coinData.setBalanceReceived(true);
                            coinData.setBalanceConfirmed(insightResponse.getBalanceSat());
                            coinData.setBalanceUnconfirmed(insightResponse.getUnconfirmedBalanceSat());
                            //                        Log.i("$TAG eth_get_balance", balanceCap)
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "FAIL INSIGHT_ADDRESS Exception");
                        }
                    }
                    break;

                    case ServerApiInsight.INFURA_ETH_GET_TRANSACTION_COUNT: {
                        String nonce = insightResponse.getResult();
                        nonce = nonce.substring(2);
                        BigInteger count = new BigInteger(nonce, 16);
                        coinData.setConfirmedTXCount(count);


//                        Log.i("$TAG eth_getTransCount", nonce)
                    }
                    break;

                    case ServerApiInsight.INFURA_ETH_GET_PENDING_COUNT: {
                        String pending = insightResponse.getResult();
                        pending = pending.substring(2);
                        BigInteger count = new BigInteger(pending, 16);
                        coinData.setUnconfirmedTXCount(count);

//                        Log.i("$TAG eth_getPendingTxCount", pending)
                    }
                    break;
                }

                if (serverApiInfura.isRequestsSequenceCompleted()) {
                    blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                } else {
                    blockchainRequestsCallbacks.onProgress();
                }
            }

            @Override
            public void onSuccess(ElectrumRequest electrumRequest) {
                Log.i(TAG, "onSuccess: "+electrumRequest.getMethod());
                if (electrumRequest.isMethod(ElectrumRequest.METHOD_GetBalance)) {
                    try {
                        String walletAddress = electrumRequest.getParams().getString(0);
                        if (!walletAddress.equals(coinData.getWallet())) {
                            // todo - check
                            throw new Exception("Invalid wallet address in answer!");
                        }
                        Long confBalance = electrumRequest.getResult().getLong("confirmed");
                        Long unconfirmedBalance = electrumRequest.getResult().getLong("unconfirmed");
                        coinData.setBalanceReceived(true);
                        coinData.setBalanceConfirmed(confBalance);
                        coinData.setBalanceUnconfirmed(unconfirmedBalance);
                        coinData.setValidationNodeDescription(serverApiInsight.lastNode);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e(TAG, "FAIL METHOD_GetBalance JSONException");
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "FAIL METHOD_GetBalance Exception");
                    }
                } else if (electrumRequest.isMethod(ElectrumRequest.METHOD_ListUnspent)) {
                    try {
                        String walletAddress = electrumRequest.getParams().getString(0);
                        JSONArray jsUnspentArray = electrumRequest.getResultArray();
                        try {
                            coinData.getUnspentTransactions().clear();
                            for (int i = 0; i < jsUnspentArray.length(); i++) {
                                JSONObject jsUnspent = jsUnspentArray.getJSONObject(i);
                                BtcData.UnspentTransaction trUnspent = new BtcData.UnspentTransaction();
                                trUnspent.txID = jsUnspent.getString("tx_hash");
                                trUnspent.Amount = jsUnspent.getInt("value");
                                trUnspent.Height = jsUnspent.getInt("height");
                                coinData.getUnspentTransactions().add(trUnspent);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "FAIL METHOD_ListUnspent JSONException");
                        }

                        for (int i = 0; i < jsUnspentArray.length(); i++) {
                            JSONObject jsUnspent = jsUnspentArray.getJSONObject(i);
                            Integer height = jsUnspent.getInt("height");
                            String hash = jsUnspent.getString("tx_hash");
                            if (height != -1) {
                                if (blockchainRequestsCallbacks.allowAdvance()) {
                                    serverApiInsight.electrumRequestData(ctx, ElectrumRequest.getTransaction(walletAddress, hash));
                                } else {
                                    ctx.setError("Terminated by user");
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (electrumRequest.isMethod(ElectrumRequest.METHOD_GetTransaction)) {
                    try {
                        String txHash = electrumRequest.txHash;
                        String raw = electrumRequest.getResultString();
                        for (BtcData.UnspentTransaction tx : coinData.getUnspentTransactions()) {
                            if (tx.txID.equals(txHash))
                                tx.Raw = raw;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                if (serverApiInsight.isRequestsSequenceCompleted()) {
                    blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                }else{
                    blockchainRequestsCallbacks.onProgress();
                }
            }

            @Override
            public void onFail(String method, String message) {
                Log.i(TAG, "onFail: "+electrumRequest.getMethod()+" "+electrumRequest.getError());
                ctx.setError(electrumRequest.getError());
                if (serverApiInsight.isRequestsSequenceCompleted()) {
                    blockchainRequestsCallbacks.onComplete(false);//serverApiInsight// .isErrorOccurred(), serverApiInsight
                    //.getError());
                }else{
                    blockchainRequestsCallbacks.onProgress();
                }
            }
        };

        serverApiInsight.setElectrumRequestData(electrumListener);

        serverApiInsight.electrumRequestData(ctx, ElectrumRequest.checkBalance(coinData.getWallet()));
        serverApiInsight.electrumRequestData(ctx, ElectrumRequest.listUnspent(coinData.getWallet()));
    }

    private final static BigDecimal relayFee = new BigDecimal(0.00001);

    @Override
    public void requestFee(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) throws Exception {
        final int calcSize = calculateEstimatedTransactionSize(targetAddress, amount.toValueString());
        Log.e(TAG, String.format("Estimated tx size %d", calcSize));
        coinData.minFee=null;
        coinData.maxFee=null;
        coinData.normalFee=null;

        final ServerApiInsight serverApiInsight = new ServerApiInsight();

        final ServerApiInsight.InsightBodyListener electrumListener  = new ServerApiInsight.InsightBodyListener () {
            @Override
            public void onSuccess(ElectrumRequest electrumRequest) {
                BigDecimal fee;
                if (electrumRequest.isMethod(ElectrumRequest.METHOD_GetFee)) {
                    try {
                        fee = new BigDecimal(electrumRequest.getResultString()); //fee per KB

                        if (fee.equals(BigDecimal.ZERO)) {
                            serverApiInsight.electrumRequestData(ctx, ElectrumRequest.getFee());
                        }

//                        if (calcSize != 0) {
                        fee = fee.multiply(new BigDecimal(calcSize)).divide(new BigDecimal(1024)); // (per KB -> per byte)*size
//                        } else {
//                            serverApiInsight
//.electrumRequestData(ctx, ElectrumRequest.getFee());
//                        }

                        //compare fee to usual relay fee
                        if (fee.compareTo(relayFee) < 0) {
                            fee = relayFee;
                        }
                        fee = fee.setScale(8, RoundingMode.DOWN);

                        Amount feeAmount = new Amount(fee,  ctx.getBlockchain().getCurrency());
                        coinData.minFee = feeAmount;
                        coinData.normalFee = feeAmount;
                        coinData.maxFee = feeAmount;
//                        if (coinData.minFee != null && coinData.normalFee != null && coinData.maxFee != null) {
                        blockchainRequestsCallbacks.onComplete(true);
//                        } else {
//                            blockchainRequestsCallbacks.onProgress();
//                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFail(ElectrumRequest electrumRequest) {
                ctx.setError(electrumRequest.getError());
                blockchainRequestsCallbacks.onComplete(false);
            }
        };
        serverApiInsight.setElectrumRequestData(electrumListener);

        serverApiInsight.electrumRequestData(ctx, ElectrumRequest.getFee());
    }
}