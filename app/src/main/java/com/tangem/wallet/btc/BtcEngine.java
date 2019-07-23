package com.tangem.wallet.btc;

import android.net.Uri;
import android.text.InputFilter;
import android.util.Log;

import com.tangem.App;
import com.tangem.card_common.data.TangemCard;
import com.tangem.card_common.reader.CardProtocol;
import com.tangem.card_common.tasks.SignTask;
import com.tangem.card_common.util.Util;
import com.tangem.data.Blockchain;
import com.tangem.data.local.PendingTransactionsStorage;
import com.tangem.data.network.ElectrumRequest;
import com.tangem.data.network.Server;
import com.tangem.data.network.ServerApiCommon;
import com.tangem.data.network.ServerApiElectrum;
import com.tangem.data.network.ServerApiSoChain;
import com.tangem.data.network.model.SoChain;
import com.tangem.util.CryptoUtil;
import com.tangem.util.DecimalDigitsInputFilter;
import com.tangem.util.DerEncodingUtil;
import com.tangem.wallet.BTCUtils;
import com.tangem.wallet.BalanceValidator;
import com.tangem.wallet.Base58;
import com.tangem.wallet.CoinData;
import com.tangem.wallet.CoinEngine;
import com.tangem.wallet.R;
import com.tangem.wallet.TangemContext;
import com.tangem.wallet.Transaction;
import com.tangem.wallet.UnspentOutputInfo;

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

public class BtcEngine extends CoinEngine {

    private static final String TAG = BtcEngine.class.getSimpleName();

    private static boolean useElectrum = false;

    public BtcData coinData = null;

    public BtcEngine(TangemContext context) throws Exception {
        super(context);
        if (context.getCoinData() == null) {
            coinData = new BtcData();
            context.setCoinData(coinData);
        } else if (context.getCoinData() instanceof BtcData) {
            coinData = (BtcData) context.getCoinData();
        } else {
            throw new Exception("Invalid type of Blockchain data for BtcEngine");
        }
    }

    public BtcEngine() {
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
        return "BTC";
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
        return "BTC";
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

        if (!address.startsWith("1") && !address.startsWith("2") && !address.startsWith("3") && !address.startsWith("n") && !address.startsWith("m")) {
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

        if (ctx.getBlockchain() != Blockchain.BitcoinTestNet && ctx.getBlockchain() != Blockchain.Bitcoin) {
            return false;
        }

        if (ctx.getBlockchain() == Blockchain.BitcoinTestNet && (address.startsWith("1") || address.startsWith("3"))) {
            return false;
        }

        return true;
    }


    @Override
    public boolean isNeedCheckNode() {
        return true;
    }

    @Override
    public Uri getWalletExplorerUri() {
        return Uri.parse((ctx.getBlockchain() == Blockchain.Bitcoin ? "https://www.blockchain.com/btc/address/" : "https://live.blockcypher.com/btc-testnet/address/") + ctx.getCoinData().getWallet());
    }

    @Override
    public Uri getShareWalletUri() {
        if (ctx.getCard().getDenomination() != null && !ctx.getCard().getDenominationText().equals("0.00")) {
            return Uri.parse("bitcoin:" + ctx.getCoinData().getWallet() + "?amount=" + convertToAmount(convertToInternalAmount(ctx.getCard().getDenomination())).toValueString(8));
        } else {
            return Uri.parse("bitcoin:" + ctx.getCoinData().getWallet());
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
        try {
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
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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
        byte netSelectionByte;
        switch (ctx.getBlockchain()) {
            case Bitcoin:
                netSelectionByte = (byte) 0x00; //0 - MainNet 0x6f - TestNet
                break;
            default:
                netSelectionByte = (byte) 0x6f; //0 - MainNet 0x6f - TestNet
                break;
        }

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
        BigDecimal d = internalAmount.divide(new BigDecimal("100000000")).setScale(8, RoundingMode.DOWN);
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
    public SignTask.TransactionToSign constructTransaction(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) throws Exception {
        final ArrayList<UnspentOutputInfo> unspentOutputs;
        checkBlockchainDataExists();

        String myAddress = ctx.getCoinData().getWallet();
        byte[] pbKey = ctx.getCard().getWalletPublicKey();

//        /blockcypher/
//        for (BtcData.UnspentTransaction utxo : coinData.getUnspentTransactions()) {
//            unspentOutputs.add(new UnspentOutputInfo(BTCUtils.fromHex(utxo.txID), new Transaction.Script(BTCUtils.fromHex(utxo.Raw)), utxo.Amount, utxo.Height, -1, utxo.txID, null));
//        }

        if (useElectrum) {
            // Build script for our address
            List<BtcData.UnspentTransaction> rawTxList = coinData.getUnspentTransactions();
            byte[] outputScriptWeAreAbleToSpend = Transaction.Script.buildOutput(myAddress).bytes;

            // Collect unspent
            unspentOutputs = BTCUtils.getOutputs(rawTxList, outputScriptWeAreAbleToSpend);
        } else {
            unspentOutputs = new ArrayList<>();
            for (BtcData.UnspentTransaction utxo : coinData.getUnspentTransactions()) {
                unspentOutputs.add(new UnspentOutputInfo(BTCUtils.fromHex(utxo.txID), new Transaction.Script(BTCUtils.fromHex(utxo.Raw)), utxo.Amount, utxo.Height, -1, utxo.txID, null));
            }
        }
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

        final long amountFinal = amount;
        final long changeFinal = change;

        if (amount + fees > fullAmount) {
            throw new CardProtocol.TangemException_WrongAmount(String.format("Balance (%d) < change (%d) + amount (%d)", fullAmount, change, amount));
        }

        final byte[][] txForSign = new byte[unspentOutputs.size()][];
        final byte[][] bodyDoubleHash = new byte[unspentOutputs.size()][];
        final byte[][] bodyHash = new byte[unspentOutputs.size()][];

        for (int i = 0; i < unspentOutputs.size(); ++i) {
            txForSign[i] = BTCUtils.buildTXForSign(myAddress, targetAddress, myAddress, unspentOutputs, i, amount, change);
            bodyHash[i] = Util.calculateSHA256(txForSign[i]);
            bodyDoubleHash[i] = Util.calculateSHA256(bodyHash[i]);
        }

        return new SignTask.TransactionToSign() {

            @Override
            public boolean isSigningMethodSupported(TangemCard.SigningMethod signingMethod) {
                return signingMethod == TangemCard.SigningMethod.Sign_Hash || signingMethod == TangemCard.SigningMethod.Sign_Raw;
            }

            @Override
            public byte[][] getHashesToSign() throws Exception {
                byte[][] dataForSign = new byte[unspentOutputs.size()][];
                if (txForSign.length > 10)
                    throw new Exception("To much hashes in one transaction!");
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

                byte[] txForSend = BTCUtils.buildTXForSend(targetAddress, myAddress, unspentOutputs, amountFinal, changeFinal);
                notifyOnNeedSendTransaction(txForSend);
                return txForSend;
            }
        };
    }

    @Override
    public void requestBalanceAndUnspentTransactions(BlockchainRequestsCallbacks blockchainRequestsCallbacks) throws Exception {
        if (useElectrum) {
            final ServerApiElectrum serverApiElectrum = new ServerApiElectrum();

            ServerApiElectrum.ResponseListener electrumListener = new ServerApiElectrum.ResponseListener() {
                @Override
                public void onSuccess(ElectrumRequest electrumRequest) {
                    Log.i(TAG, "onSuccess: " + electrumRequest.getMethod());
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
                            coinData.setValidationNodeDescription(serverApiElectrum.getValidationNodeDescription());
                            checkPending(blockchainRequestsCallbacks);
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
                                    trUnspent.Amount = jsUnspent.getLong("value");
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
                                        serverApiElectrum.requestData(ctx, ElectrumRequest.getTransaction(walletAddress, hash));
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

                    if (serverApiElectrum.isRequestsSequenceCompleted()) {
                        blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                    } else {
                        blockchainRequestsCallbacks.onProgress();
                    }
                }

                @Override
                public void onFail(ElectrumRequest electrumRequest) {
                    Log.i(TAG, "onFail: " + electrumRequest.getMethod() + " " + electrumRequest.getError());
                    ctx.setError(electrumRequest.getError());
//                ctx.setError(R.string.cannot_obtain_data_from_blockchain);
                    if (serverApiElectrum.isRequestsSequenceCompleted()) {
                        blockchainRequestsCallbacks.onComplete(false);//serverApiElectrum.isErrorOccurred(), serverApiElectrum.getError());
                    } else {
                        blockchainRequestsCallbacks.onProgress();
                    }
                }
            };

            serverApiElectrum.setResponseListener(electrumListener);

            serverApiElectrum.requestData(ctx, ElectrumRequest.checkBalance(coinData.getWallet()));
            serverApiElectrum.requestData(ctx, ElectrumRequest.listUnspent(coinData.getWallet()));

        } else {
            final ServerApiSoChain serverApi = new ServerApiSoChain();

            ServerApiSoChain.AddressInfoListener addressInfoListener = new ServerApiSoChain.AddressInfoListener() {
                @Override
                public void onSuccess(SoChain.Response.AddressBalance response) {
                    try {
                        String walletAddress = response.getData().getAddress();
                        if (!walletAddress.equals(coinData.getWallet())) {
                            // todo - check
                            throw new Exception("Invalid wallet address in answer!");
                        }
                        Long confBalance = convertToInternalAmount(convertToAmount(response.getData().getConfirmed_balance(), getBalanceCurrency())).longValueExact();
                        Long unconfirmedBalance = convertToInternalAmount(convertToAmount(response.getData().getUnconfirmed_balance(), getBalanceCurrency())).longValueExact();
                        coinData.setBalanceReceived(true);
                        coinData.setBalanceConfirmed(confBalance);
                        coinData.setBalanceUnconfirmed(unconfirmedBalance);
                        coinData.setValidationNodeDescription(Server.ApiSoChain.URL);
                        checkPending(blockchainRequestsCallbacks);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e(TAG, "FAIL METHOD_GetBalance JSONException");
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "FAIL METHOD_GetBalance Exception");
                    }
                    if (serverApi.isRequestsSequenceCompleted()) {
                        blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                    } else {
                        blockchainRequestsCallbacks.onProgress();
                    }

                }

                @Override
                public void onSuccess(SoChain.Response.TxUnspent response) {
                    String walletAddress = response.getData().getAddress();
                    try {
                        if (!walletAddress.equals(coinData.getWallet())) {
                            // todo - check
                            throw new Exception("Invalid wallet address in answer!");
                        }
                        coinData.getUnspentTransactions().clear();
                        if (response.getData().getTxs() != null)
                            for (SoChain.Response.TxUnspent.Data.Tx tx : response.getData().getTxs()) {
                                BtcData.UnspentTransaction trUnspent = new BtcData.UnspentTransaction();
                                trUnspent.txID = tx.getTxid();
                                trUnspent.Amount = convertToInternalAmount(convertToAmount(tx.getValue(), getBalanceCurrency())).longValueExact();
                                trUnspent.Height = tx.getOutput_no();
                                trUnspent.Raw = tx.getScript_hex();
                                coinData.getUnspentTransactions().add(trUnspent);

//                                    if (blockchainRequestsCallbacks.allowAdvance()) {
//                                        //serverApi.requestData(ctx, ElectrumRequest.getTransaction(walletAddress, hash));
//                                    } else {
//                                        ctx.setError("Terminated by user");
//                                    }
                            }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "FAIL METHOD_ListUnspent JSONException");
                    }

                    if (serverApi.isRequestsSequenceCompleted()) {
                        blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                    } else {
                        blockchainRequestsCallbacks.onProgress();
                    }

                }

//
//                } else if (electrumRequest.isMethod(ElectrumRequest.METHOD_GetTransaction)) {
//                    try {
//                        String txHash = electrumRequest.txHash;
//                        String raw = electrumRequest.getResultString();
//                        for (BtcData.UnspentTransaction tx : coinData.getUnspentTransactions()) {
//                            if (tx.txID.equals(txHash))
//                                tx.Raw = raw;
//                        }
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                }

//            }


                @Override
                public void onFail(String message) {
                    Log.i(TAG, "onFail: " + message);
                    ctx.setError(message);
//                ctx.setError(R.string.cannot_obtain_data_from_blockchain);
                    if (serverApi.isRequestsSequenceCompleted()) {
                        blockchainRequestsCallbacks.onComplete(false);//serverApiElectrum.isErrorOccurred(), serverApiElectrum.getError());
                    } else {
                        blockchainRequestsCallbacks.onProgress();
                    }
                }
            };


            serverApi.setAddressInfoListener(addressInfoListener);

            serverApi.requestAddressBalance(ctx.getBlockchain(), coinData.getWallet());
            serverApi.requestUnspentTx(ctx.getBlockchain(), coinData.getWallet());
        }
    }

    private void checkPending(BlockchainRequestsCallbacks blockchainRequestsCallbacks) throws Exception {
        if (App.pendingTransactionsStorage.hasTransactions(ctx.getCard())) {

            if (useElectrum) {
                ServerApiElectrum serverApiElectrum = new ServerApiElectrum();

                ServerApiElectrum.ResponseListener electrumListener = new ServerApiElectrum.ResponseListener() {
                    @Override
                    public void onSuccess(ElectrumRequest electrumRequest) {
                        Log.i(TAG, "onSuccess: " + electrumRequest.getMethod());
                        try {
                            if (electrumRequest.getResultString() != null) {
                                App.pendingTransactionsStorage.removeTransaction(ctx.getCard(), electrumRequest.txHash); //TODO Need remove transaction by RAW HEX
                            }

                        } catch (Exception e) {
                            Log.e(TAG, "onFail: " + electrumRequest.getMethod() + " " + electrumRequest.getError());
                        }
                        if (serverApiElectrum.isRequestsSequenceCompleted()) {
                            blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                        } else {
                            blockchainRequestsCallbacks.onProgress();
                        }
                    }

                    @Override
                    public void onFail(ElectrumRequest electrumRequest) {
                        Log.i(TAG, "onFail: " + electrumRequest.getMethod() + " " + electrumRequest.getError());
//                ctx.setError(R.string.cannot_obtain_data_from_blockchain);
                        if (serverApiElectrum.isRequestsSequenceCompleted()) {
                            blockchainRequestsCallbacks.onComplete(false);//serverApiElectrum.isErrorOccurred(), serverApiElectrum.getError());
                        } else {
                            blockchainRequestsCallbacks.onProgress();
                        }
                    }
                };

                serverApiElectrum.setResponseListener(electrumListener);
                for (PendingTransactionsStorage.TransactionInfo pendingTx : App.pendingTransactionsStorage.getTransactions(ctx.getCard()).getTransactions()) {
                    String txHash = BTCUtils.toHex(CryptoUtil.doubleSha256(BTCUtils.fromHex(pendingTx.getTx())));
                    serverApiElectrum.requestData(ctx, ElectrumRequest.getTransaction(ctx.getCoinData().getWallet(), txHash));
                }
            } else {
                ServerApiSoChain serverApi = new ServerApiSoChain();

                ServerApiSoChain.TransactionInfoListener listener = new ServerApiSoChain.TransactionInfoListener() {
                    @Override
                    public void onSuccess(SoChain.Response.GetTx response) {
                        Log.i(TAG, "onSuccess: GetTx");
                        try {
                            if (response.getData() != null && response.getData().getTx_hex() != null && response.getData().getTxid() != null) {
                                App.pendingTransactionsStorage.removeTransaction(ctx.getCard(), response.getData().getTx_hex());
                            }

                        } catch (Exception e) {
                            Log.e(TAG, "onFail: GetTx" + response);
                        }
                        if (serverApi.isRequestsSequenceCompleted()) {
                            blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                        } else {
                            blockchainRequestsCallbacks.onProgress();
                        }
                    }

                    @Override
                    public void onFail(String message) {
                        Log.i(TAG, "onFail: GetTx " + message);
                        if (serverApi.isRequestsSequenceCompleted()) {
                            blockchainRequestsCallbacks.onComplete(false);//serverApiElectrum.isErrorOccurred(), serverApiElectrum.getError());
                        } else {
                            blockchainRequestsCallbacks.onProgress();
                        }
                    }
                };

                serverApi.setTransactionInfoListener(listener);
                for (PendingTransactionsStorage.TransactionInfo pendingTx : App.pendingTransactionsStorage.getTransactions(ctx.getCard()).getTransactions()) {
                    String txId = BTCUtils.toHex(BTCUtils.reverse(CryptoUtil.doubleSha256(BTCUtils.fromHex(pendingTx.getTx()))));
                    serverApi.requestTransactionInfo(ctx.getBlockchain(), txId);
                }
            }
        }
    }

//        /blockcypher/
//        @Override
//        public void requestBalanceAndUnspentTransactions(BlockchainRequestsCallbacks blockchainRequestsCallbacks) {
//            final ServerApiBlockcypher serverApiBlockcypher = new ServerApiBlockcypher();
//
//            ServerApiBlockcypher.ResponseListener blockcypherListener = new ServerApiBlockcypher.ResponseListener() {
//                @Override
//                public void onSuccess(String method, BlockcypherResponse blockcypherResponse) {
//                    Log.i(TAG, "onSuccess: " + method);
//                    try {
//                        String walletAddress = blockcypherResponse.getAddress();
//                        if (!walletAddress.equals(coinData.getWallet())) {
//                            // todo - check
//                            throw new Exception("Invalid wallet address in answer!");
//                        }
//                        Long confBalance = blockcypherResponse.getBalance();
//                        Long unconfirmedBalance = blockcypherResponse.getUnconfirmed_balance();
//                        coinData.setBalanceReceived(true);
//                        coinData.setBalanceConfirmed(confBalance);
//                        coinData.setBalanceUnconfirmed(unconfirmedBalance);
//                        coinData.setValidationNodeDescription("blockcypher.com");
//                        //checkPending(blockchainRequestsCallbacks); TODO: rework checkPending
//
//                        coinData.getUnspentTransactions().clear();
//                        for (BlockcypherTxref txref : blockcypherResponse.getTxrefs()) {
//                            BtcData.UnspentTransaction trUnspent = new BtcData.UnspentTransaction();
//                            trUnspent.txID = txref.getTx_hash();
//                            trUnspent.Amount = txref.getValue();
//                            trUnspent.Height = txref.getTx_output_n(); //TODO: height -> outIndex
//                            trUnspent.Raw = txref.getScript(); //TODO: raw -> script
//                            coinData.getUnspentTransactions().add(trUnspent);
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        Log.e(TAG, "FAIL BLOCKCYPHER_ADDRESS Exception");
//                    }
//
//                    if (serverApiBlockcypher.isRequestsSequenceCompleted()) {
//                        blockchainRequestsCallbacks.onComplete(!ctx.hasError());
//                    } else {
//                        blockchainRequestsCallbacks.onProgress();
//                    }
//                }
//
//                public void onSuccess(String method, BlockcypherFee blockcypherFee) {
//                    Log.e(TAG, "Wrong response type for requestBalanceAndUnspentTransactions");
//                    ctx.setError("Wrong response type for requestBalanceAndUnspentTransactions");
//                    blockchainRequestsCallbacks.onComplete(false);
//                }
//
//                @Override
//                public void onFail(String method, String message) {
//                    Log.i(TAG, "onFail: " + method + " " + message);
//                    ctx.setError(message);
//                    blockchainRequestsCallbacks.onComplete(false);
//                }
//            };
//
//            serverApiBlockcypher.setResponseListener(blockcypherListener);
//
//            serverApiBlockcypher.requestData(ctx.getBlockchain().getID(), ServerApiBlockcypher.BLOCKCYPHER_ADDRESS, ctx.getCoinData().getWallet(), "");
//        }


    protected Integer calculateEstimatedTransactionSize(String outputAddress, String outAmount) {
        //todo - правильней было бы использовать constructTransaction
        try {
//            String myAddress = coinData.getWallet();
//            byte[] pbKey = ctx.getCard().getWalletPublicKey();
//            byte[] pbComprKey = ctx.getCard().getWalletPublicKeyRar();
//
//            // build script for our address
//            List<BtcData.UnspentTransaction> rawTxList = coinData.getUnspentTransactions();
//            byte[] outputScriptWeAreAbleToSpend = Transaction.Script.buildOutput(myAddress).bytes;
//
//            // collect unspent
//            ArrayList<UnspentOutputInfo> unspentOutputs = BTCUtils.getOutputs(rawTxList, outputScriptWeAreAbleToSpend);
//
//            Long fullAmount = 0L;
//            for (int i = 0; i < unspentOutputs.size(); i++) {
//                fullAmount += unspentOutputs.get(i).value;
//            }
//
//            // get first unspent
////        val outPut = unspentOutputs[0]
////        val outPutIndex = outPut.outputIndex
//
//            // get prev TX id;
////        val prevTXID = rawTxList[0].txID//"f67b838d6e2c0c587f476f583843e93ff20368eaf96a798bdc25e01f53f8f5d2";
//
//            Long fees = FormatUtil.ConvertStringToLong("0.00");
//            Long amount = FormatUtil.ConvertStringToLong(outAmount);
//            amount -= fees;
//
//            Long change = fullAmount - fees - amount;
//
//            if (amount + fees > fullAmount) {
//                throw new Exception(String.format("Balance (%d) < amount (%d) + (%d)", fullAmount, change, amount));
//            }
//
//            byte[][] hashesForSign = new byte[unspentOutputs.size()][];
//
//            for (int i = 0; i < unspentOutputs.size(); i++) {
//                byte[] newTX = BTCUtils.buildTXForSign(myAddress, outputAddress, myAddress, unspentOutputs, i, amount, change);
//                byte[] hashData = Util.calculateSHA256(newTX);
//                byte[] doubleHashData = Util.calculateSHA256(hashData);
////            Log.e("TX_BODY_1", BTCUtils.toHex(newTX))
////            Log.e("TX_HASH_1", BTCUtils.toHex(hashData))
////            Log.e("TX_HASH_2", BTCUtils.toHex(doubleHashData))
//
////            unspentOutputs[i].bodyDoubleHash = doubleHashData
////            unspentOutputs[i].bodyHash = hashData
//                hashesForSign[i] = doubleHashData;
//            }
//
//            byte[] signFromCard = new byte[64 * unspentOutputs.size()];
//
//            for (int i = 0; i < unspentOutputs.size(); i++) {
//                BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, i * 64, 32 + i * 64));
//                BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, 32 + i * 64, 64 + i * 64));
//                byte[] encodingSign = DerEncodingUtil.packSignDer(r, s, pbKey);
//                unspentOutputs.get(i).scriptForBuild = encodingSign;
//            }
//
//            byte[] realTX = BTCUtils.buildTXForSend(outputAddress, myAddress, unspentOutputs, amount, change);

            SignTask.TransactionToSign ps = constructTransaction(new Amount(outAmount, getBalanceCurrency()), new Amount("0.00000001", getFeeCurrency()), true, outputAddress);
            OnNeedSendTransaction onNeedSendTransactionBackup = onNeedSendTransaction;
            onNeedSendTransaction = (tx) -> {
            }; // empty function to bypass exception

            byte[][] hashesToSign = ps.getHashesToSign();
            byte[] signFromCard = new byte[64 * hashesToSign.length];
            Arrays.fill(signFromCard, (byte) 0x01);
            byte[] txForSend = ps.onSignCompleted(signFromCard);
            onNeedSendTransaction = onNeedSendTransactionBackup;
            Log.e(TAG, "txForSend.length=" + String.valueOf(txForSend.length));
            return txForSend.length + 1;

//            Log.e(TAG,"txForSend.length="+String.valueOf(txForSend.length)+" realTX.length="+String.valueOf(realTX.length));
//
//            return realTX.length;

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Can't calculate transaction size -> use default!");
            return 256;
        }
    }

    @Override
    public void requestFee(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) throws Exception {
        final int calcSize = calculateEstimatedTransactionSize(targetAddress, amount.toValueString());
        Log.e(TAG, String.format("Estimated tx size %d", calcSize));
        coinData.minFee = null;
        coinData.maxFee = null;
        coinData.normalFee = null;

        if (useElectrum) {

            final ServerApiElectrum serverApiElectrum = new ServerApiElectrum();

            final ServerApiElectrum.ResponseListener electrumListener = new ServerApiElectrum.ResponseListener() {
                @Override
                public void onSuccess(ElectrumRequest electrumRequest) {
                    BigDecimal kbFee;
                    if (electrumRequest.isMethod(ElectrumRequest.METHOD_GetFee)) {
                        try {
                            kbFee = new BigDecimal(electrumRequest.getResultString()); //fee per KB

                            if (kbFee.equals(BigDecimal.ZERO)) {
                                serverApiElectrum.requestData(ctx, ElectrumRequest.getFee());
                            }

                            BigDecimal minByteFee = kbFee.divide(new BigDecimal(1024)); // per KB -> per byte
                            BigDecimal normalByteFee = minByteFee.add(new BigDecimal(0.00000010));
                            BigDecimal maxByteFee = minByteFee.add(new BigDecimal(0.00000025));

                            BigDecimal minFee = minByteFee.multiply(new BigDecimal(calcSize)).setScale(8, RoundingMode.DOWN);
                            BigDecimal normalFee = normalByteFee.multiply(new BigDecimal(calcSize)).setScale(8, RoundingMode.DOWN);
                            BigDecimal maxFee = maxByteFee.multiply(new BigDecimal(calcSize)).setScale(8, RoundingMode.DOWN);

                            CoinEngine.Amount minAmount = new CoinEngine.Amount(minFee, ctx.getBlockchain().getCurrency());
                            CoinEngine.Amount normalAmount = new CoinEngine.Amount(normalFee, ctx.getBlockchain().getCurrency());
                            CoinEngine.Amount maxAmount = new CoinEngine.Amount(maxFee, ctx.getBlockchain().getCurrency());

                            coinData.minFee = minAmount;
                            coinData.normalFee = normalAmount;
                            coinData.maxFee = maxAmount;
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
            serverApiElectrum.setResponseListener(electrumListener);

            serverApiElectrum.requestData(ctx, ElectrumRequest.getFee());
        } else {

            final ServerApiCommon serverApiCommon = new ServerApiCommon();

            final ServerApiCommon.EstimatedFeeListener estimatedFeeListener = new ServerApiCommon.EstimatedFeeListener() {
                @Override
                public void onSuccess(int blockCount, String estimateFeeResponse) {
                    BigDecimal fee = new BigDecimal(estimateFeeResponse); // BTC per 1 kb

                    if (fee.equals(BigDecimal.ZERO)) {
                        if (blockchainRequestsCallbacks.allowAdvance()) {
                            serverApiCommon.requestBtcEstimatedFee(blockCount);
                        }
                        return;
                    }

                    if (calcSize != 0) {
                        fee = fee.multiply(new BigDecimal(calcSize)).divide(new BigDecimal(1024), BigDecimal.ROUND_DOWN); // per Kb -> per byte
                    } else {
                        if (blockchainRequestsCallbacks.allowAdvance()) {
                            serverApiCommon.requestBtcEstimatedFee(blockCount);
                        }
                        return;
                    }

                    fee = fee.setScale(8, RoundingMode.DOWN);

                    switch (blockCount) {
                        case ServerApiCommon.ESTIMATE_FEE_MINIMAL:
                            coinData.minFee = new CoinEngine.Amount(fee, getFeeCurrency());
                            break;
                        case ServerApiCommon.ESTIMATE_FEE_NORMAL:
                            coinData.normalFee = new CoinEngine.Amount(fee, getFeeCurrency());
                            break;
                        case ServerApiCommon.ESTIMATE_FEE_PRIORITY:
                            coinData.maxFee = new CoinEngine.Amount(fee, getFeeCurrency());
                            break;
                    }

                    if (coinData.minFee != null && coinData.normalFee != null && coinData.maxFee != null) {
                        blockchainRequestsCallbacks.onComplete(true);
                    } else {
                        blockchainRequestsCallbacks.onProgress();
                    }
                }

                @Override
                public void onFail(int blockCount, String message) {
                    // TODO - add fail counter to terminate after NNN tries
                    if (blockchainRequestsCallbacks.allowAdvance()) {
                        serverApiCommon.requestBtcEstimatedFee(blockCount);
                        return;
                    }
                    ctx.setError(ctx.getContext().getString(R.string.cannot_calculate_fee_wrong_data_received_from_node));
                    blockchainRequestsCallbacks.onComplete(false);
                }
            };
            serverApiCommon.setBtcEstimatedFeeListener(estimatedFeeListener);

            serverApiCommon.requestBtcEstimatedFee(ServerApiCommon.ESTIMATE_FEE_PRIORITY);
            serverApiCommon.requestBtcEstimatedFee(ServerApiCommon.ESTIMATE_FEE_NORMAL);
            serverApiCommon.requestBtcEstimatedFee(ServerApiCommon.ESTIMATE_FEE_MINIMAL);

        }
    }

//    /blockcypher/
//    @Override
//    public void requestFee(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) throws Exception {
//        final int calcSize = calculateEstimatedTransactionSize(targetAddress, amount.toValueString());
//        Log.e(TAG, String.format("Estimated tx size %d", calcSize));
//        coinData.minFee = null;
//        coinData.maxFee = null;
//        coinData.normalFee = null;
//
//        final ServerApiBlockcypher serverApiBlockcypher = new ServerApiBlockcypher();
//
//        ServerApiBlockcypher.ResponseListener blockcypherListener = new ServerApiBlockcypher.ResponseListener() {
//            @Override
//            public void onSuccess(String method, BlockcypherResponse blockcypherResponse) {
//                Log.e(TAG, "Wrong response type for requestFee");
//                ctx.setError("Wrong response type for requestFee");
//                blockchainRequestsCallbacks.onComplete(false);
//            }
//
//            public void onSuccess(String method, BlockcypherFee blockcypherFee) {
//                Log.i(TAG, "onSuccess: " + method);
//                try {
//                    BigDecimal minByteFee = new BigDecimal(blockcypherFee.getLow_fee_per_kb()).divide(BigDecimal.valueOf(1024));
//                    BigDecimal normalByteFee = new BigDecimal(blockcypherFee.getMedium_fee_per_kb()).divide(BigDecimal.valueOf(1024));
//                    BigDecimal maxByteFee = new BigDecimal(blockcypherFee.getHigh_fee_per_kb()).divide(BigDecimal.valueOf(1024));
//
//                    CoinEngine.InternalAmount minIntAmount = new CoinEngine.InternalAmount(minByteFee.multiply(BigDecimal.valueOf(calcSize)), "satoshi");
//                    CoinEngine.InternalAmount normalIntAmount = new CoinEngine.InternalAmount(normalByteFee.multiply(BigDecimal.valueOf(calcSize)), "satoshi");
//                    CoinEngine.InternalAmount maxIntAmount = new CoinEngine.InternalAmount(maxByteFee.multiply(BigDecimal.valueOf(calcSize)), "satoshi");
//
//                    coinData.minFee = convertToAmount(minIntAmount);
//                    coinData.normalFee = convertToAmount(normalIntAmount);
//                    coinData.maxFee = convertToAmount(maxIntAmount);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    Log.e(TAG, "FAIL BLOCKCYPHER_FEE Exception");
//                }
//
//                if (serverApiBlockcypher.isRequestsSequenceCompleted()) {
//                    blockchainRequestsCallbacks.onComplete(!ctx.hasError());
//                } else {
//                    blockchainRequestsCallbacks.onProgress();
//                }
//            }
//
//            @Override
//            public void onFail(String method, String message) {
//                Log.i(TAG, "onFail: " + method + " " + message);
//                ctx.setError(message);
//                blockchainRequestsCallbacks.onComplete(false);
//            }
//        };
//
//        serverApiBlockcypher.setResponseListener(blockcypherListener);
//
//        serverApiBlockcypher.requestData(ctx.getBlockchain().getID(), ServerApiBlockcypher.BLOCKCYPHER_FEE, "", "");
//    }

    @Override
    public void requestSendTransaction(BlockchainRequestsCallbacks blockchainRequestsCallbacks, byte[] txForSend) throws Exception {
        final String txStr = BTCUtils.toHex(txForSend);
        if (useElectrum) {
            final ServerApiElectrum serverApiElectrum = new ServerApiElectrum();

            ServerApiElectrum.ResponseListener electrumListener = new ServerApiElectrum.ResponseListener() {
                @Override
                public void onSuccess(ElectrumRequest electrumRequest) {
                    if (electrumRequest.isMethod(ElectrumRequest.METHOD_SendTransaction)) {
                        try {
                            String resultString = electrumRequest.getResultString();
                            if (resultString == null || resultString.isEmpty()) {
                                ctx.setError("Rejected by node: " + electrumRequest.getError());
                                blockchainRequestsCallbacks.onComplete(false);
                            } else {
                                ctx.setError(null);
                                blockchainRequestsCallbacks.onComplete(true);
                            }
                        } catch (Exception e) {
                            if (e.getMessage() != null) {
                                ctx.setError(e.getMessage());
                                blockchainRequestsCallbacks.onComplete(false);
                            } else {
                                ctx.setError(e.getClass().getName());
                                blockchainRequestsCallbacks.onComplete(false);
                            }
                        }
                    }
                }

                @Override
                public void onFail(ElectrumRequest electrumRequest) {
                    ctx.setError(electrumRequest.getError());
                    blockchainRequestsCallbacks.onComplete(false);
                }
            };
            serverApiElectrum.setResponseListener(electrumListener);


            serverApiElectrum.requestData(ctx, ElectrumRequest.broadcast(ctx.getCoinData().getWallet(), txStr));
        } else {
            final ServerApiSoChain serverApi = new ServerApiSoChain();

            ServerApiSoChain.SendTxListener listener = new ServerApiSoChain.SendTxListener() {
                @Override
                public void onSuccess(SoChain.Response.SendTx response) {
                    try {
                        if (response.getStatus() == null || response.getData() == null) {
                            ctx.setError("Rejected by node: invalid answer received");
                            blockchainRequestsCallbacks.onComplete(false);
                        } else if (response.getStatus().equals("fail") || response.getData().getTxid() == null) {
                            ctx.setError("Rejected by node: " + response.getData());
                            blockchainRequestsCallbacks.onComplete(false);
                        } else {
                            ctx.setError(null);
                            blockchainRequestsCallbacks.onComplete(true);
                        }
                    } catch (Exception e) {
                        if (e.getMessage() != null) {
                            ctx.setError(e.getMessage());
                            blockchainRequestsCallbacks.onComplete(false);
                        } else {
                            ctx.setError(e.getClass().getName());
                            blockchainRequestsCallbacks.onComplete(false);
                        }
                    }
                }

                @Override
                public void onFail(String message) {
                    ctx.setError(message);
                    blockchainRequestsCallbacks.onComplete(false);
                }
            };
            serverApi.setSendTxListener(listener);
            serverApi.requestSendTransaction(ctx.getBlockchain(), txStr);

        }
    }

//    /blockcypher/
//    @Override
//    public void requestSendTransaction(BlockchainRequestsCallbacks blockchainRequestsCallbacks, byte[] txForSend) {
//        final ServerApiBlockcypher serverApiBlockcypher = new ServerApiBlockcypher();
//        final String txStr = BTCUtils.toHex(txForSend);
//
//        ServerApiBlockcypher.ResponseListener blockcypherListener = new ServerApiBlockcypher.ResponseListener() {
//            @Override
//            public void onSuccess(String method, BlockcypherResponse blockcypherResponse) {
//                String resultString = blockcypherResponse.toString();
//                try {
//                    if (resultString.isEmpty()) {
//                        ctx.setError("No response from node");
//                        blockchainRequestsCallbacks.onComplete(false);
//                    } else { // TODO: Make check for a valid send response
//                        ctx.setError(null);
//                        blockchainRequestsCallbacks.onComplete(true);
//                    }
//                } catch (Exception e) {
//                    if (e.getMessage() != null) {
//                        ctx.setError(e.getMessage());
//                        blockchainRequestsCallbacks.onComplete(false);
//                    } else {
//                        ctx.setError(e.getClass().getName());
//                        blockchainRequestsCallbacks.onComplete(false);
//                        Log.e(TAG, resultString);
//                    }
//                }
//            }
//
//            public void onSuccess(String method, BlockcypherFee blockcypherFee) {
//                Log.e(TAG, "Wrong response type for requestSendTransaction");
//                ctx.setError("Wrong response type for requestSendTransaction");
//                blockchainRequestsCallbacks.onComplete(false);
//            }
//
//            @Override
//            public void onFail(String method, String message) {
//                Log.i(TAG, "onFail: " + method + " " + message);
//                ctx.setError(message);
//                blockchainRequestsCallbacks.onComplete(false);
//            }
//        };
//        serverApiBlockcypher.setResponseListener(blockcypherListener);
//
//        serverApiBlockcypher.requestData(ctx.getBlockchain().getID(), ServerApiBlockcypher.BLOCKCYPHER_SEND, "", txStr);
//    }

}