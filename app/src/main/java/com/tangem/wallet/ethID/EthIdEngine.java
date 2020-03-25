package com.tangem.wallet.ethID;

import android.net.Uri;
import android.text.InputFilter;
import android.util.Log;

import com.tangem.data.Blockchain;
import com.tangem.data.network.ServerApiBlockcypher;
import com.tangem.data.network.ServerApiInfura;
import com.tangem.data.network.model.BlockcypherFee;
import com.tangem.data.network.model.BlockcypherResponse;
import com.tangem.data.network.model.BlockcypherTx;
import com.tangem.data.network.model.BlockcypherTxref;
import com.tangem.data.network.model.InfuraResponse;
import com.tangem.tangem_card.data.TangemCard;
import com.tangem.tangem_card.reader.CardProtocol;
import com.tangem.tangem_card.tasks.SignTask;
import com.tangem.tangem_card.util.Util;
import com.tangem.util.CryptoUtil;
import com.tangem.util.DecimalDigitsInputFilter;
import com.tangem.wallet.BTCUtils;
import com.tangem.wallet.BalanceValidator;
import com.tangem.wallet.CoinData;
import com.tangem.wallet.CoinEngine;
import com.tangem.wallet.ECDSASignatureETH;
import com.tangem.wallet.EthTransaction;
import com.tangem.wallet.Keccak256;
import com.tangem.wallet.R;
import com.tangem.wallet.TangemContext;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.SignatureDecodeException;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;

public class EthIdEngine extends CoinEngine {

    private static final String TAG = com.tangem.wallet.eth.EthEngine.class.getSimpleName();
    public EthIdData coinData = null;
    private byte[] approvalPubKey = Util.hexToBytes("04EAD74FEEE4061044F46B19EB654CEEE981E9318F0C8FE99AF5CDB9D779D2E52BB51EA2D14545E0B323F7A90CF4CC72753C973149009C10DB2D83DCEC28487729"); //TODO
    public String approvalAddress = calculateAddress(approvalPubKey); //TODO

    public EthIdEngine(TangemContext ctx) throws Exception {
        super(ctx);
        if (ctx.getCoinData() == null) {
            coinData = new EthIdData();
            ctx.setCoinData(coinData);
        } else if (ctx.getCoinData() instanceof EthIdData) {
            coinData = (EthIdData) ctx.getCoinData();
        } else {
            throw new Exception("Invalid type of Blockchain data for " + this.getClass().getSimpleName());
        }
    }

    public EthIdEngine() {
        super();
    }

    private static int getDecimals() {
        return 18;
    }

    private int getChainIdNum() {
        return EthTransaction.ChainEnum.Mainnet.getValue();
    }

    @Override
    public boolean awaitingConfirmation() {
        return false;
    }

    @Override
    public Amount getBalance() {
        return null;
    }

    @Override
    public String getBalanceHTML() {
        if (coinData != null) {
            if (coinData.isHasApprovalTx()) {
                return "VERIFIED";
            } else {
                return "NOT REGISTERED";
            }
        } else {
            return "NOT REGISTERED";
        }
    }

    @Override
    public String getBalanceCurrency() {
        return Blockchain.Ethereum.getCurrency();
    }

    @Override
    public boolean isBalanceNotZero() {
        return true;
    }

    @Override
    public String getFeeCurrency() {
        return Blockchain.Ethereum.getCurrency();
    }

    public boolean isNeedCheckNode() {
        return false;
    }


    @Override
    public CoinData createCoinData() {
        return new EthIdData();
    }

    @Override
    public String getUnspentInputsDescription() {
        return "";
    }

    @Override
    public boolean validateAddress(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }

        if (!address.startsWith("0x") && !address.startsWith("0X")) {
            return false;
        }

        if (address.length() != 42) {
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
        BigDecimal d = internalAmount.divide(new BigDecimal("1000000000000000000"), getDecimals(), RoundingMode.DOWN);
        return new Amount(d, ctx.getBlockchain().getCurrency());
    }

    @Override
    public Amount convertToAmount(String strAmount, String currency) {
        return new Amount(strAmount, currency);
    }

    @Override
    public InternalAmount convertToInternalAmount(Amount amount) {
        return new InternalAmount(amount.multiply(new BigDecimal("1000000000000000000")), "wei");
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
        return true;
    }

    @Override
    public Uri getShareWalletUri() {
        if (ctx.getCard().getDenomination() != null) {
            return Uri.parse(ctx.getCoinData().getWallet());// + "?value=" + mCard.getDenomination() +"e18");
        } else {
            return Uri.parse(ctx.getCoinData().getWallet());
        }
    }

    @Override
    public Uri getWalletExplorerUri() {
        if (ctx.getBlockchain() == Blockchain.EthereumTestNet)
            return Uri.parse("https://rinkeby.etherscan.io/address/" + ctx.getCoinData().getWallet());
        else
            return Uri.parse("https://etherscan.io/address/" + ctx.getCoinData().getWallet());
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
        return true;
    }

    @Override
    public boolean checkNewTransactionAmountAndFee(Amount amount, Amount fee, Boolean isFeeIncluded) {
        return true;
    }

    @Override
    public boolean validateBalance(BalanceValidator balanceValidator) {
        if (coinData != null && coinData.isHasApprovalTx()) {
            balanceValidator.setScore(100);
            balanceValidator.setFirstLine(R.string.id_verified);
            balanceValidator.setSecondLine(R.string.balance_validator_second_line_unverified_balance);
            return false;
        } else {
            balanceValidator.setScore(0);
            balanceValidator.setFirstLine(R.string.id_not_registered);
            balanceValidator.setSecondLine(R.string.balance_validator_second_line_unverified_balance);
            return false;
        }
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
    public String calculateAddress(byte[] pkUncompressed) {
        Keccak256 kec = new Keccak256();
        int lenPk = pkUncompressed.length;
        if (lenPk < 2) {
            throw new IllegalArgumentException("Uncompress public key length is invalid");
        }
        byte[] cleanKey = new byte[lenPk - 1];
        for (int i = 0; i < cleanKey.length; ++i) {
            cleanKey[i] = pkUncompressed[i + 1];
        }
        byte[] r = kec.digest(cleanKey);

        byte[] address = new byte[20];
        for (int i = 0; i < 20; ++i) {
            address[i] = r[i + 12];
        }

        return String.format("0x%s", BTCUtils.toHex(address));
    }

    @Override
    public SignTask.TransactionToSign constructTransaction(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) {

        final EthTransaction tx = constructIdTxForSign(coinData.getApprovalAddressNonce());

        return new SignTask.TransactionToSign() {
            @Override
            public boolean isSigningMethodSupported(TangemCard.SigningMethod signingMethod) {
                return signingMethod == TangemCard.SigningMethod.Sign_Hash;
            }

            @Override
            public byte[][] getHashesToSign() {
                byte[][] hashesForSign = new byte[1][];
                hashesForSign[0] = tx.getRawHash();
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
                byte[] for_hash = tx.getRawHash();
                BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, 0, 32));
                BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, 32, 64));
                s = CryptoUtil.toCanonicalised(s);

                boolean f = ECKey.verify(for_hash, new ECKey.ECDSASignature(r, s), approvalPubKey);

                if (!f) {
                    Log.e(this.getClass().getSimpleName() + "-CHECK", "sign Failed.");
                }

                tx.signature = new ECDSASignatureETH(r, s);
                int v = tx.BruteRecoveryID2(tx.signature, for_hash, approvalPubKey);
                if (v != 27 && v != 28) {
                    Log.e(this.getClass().getSimpleName(), "invalid v");
                    throw new Exception("Error in " + this.getClass().getSimpleName() + " - invalid v");
                }
                tx.signature.v = (byte) v;
                Log.e(this.getClass().getSimpleName(), " V: " + String.valueOf(v));

                byte[] txForSend = tx.getEncoded();
                notifyOnNeedSendTransaction(txForSend);
                return txForSend;
            }
        };
    }

    @Override
    public void requestBalanceAndUnspentTransactions(BlockchainRequestsCallbacks blockchainRequestsCallbacks) {

        final ServerApiBlockcypher serverApiBlockcypher = new ServerApiBlockcypher();

        ServerApiBlockcypher.ResponseListener blockcypherListener = new ServerApiBlockcypher.ResponseListener() {
            @Override
            public void onSuccess(String method, BlockcypherResponse blockcypherResponse) {
                Log.i(TAG, "onSuccess: " + method);
                try {
                    //TODO: change request logic, 2000 tx max
                    if (blockcypherResponse.getTxrefs() != null) {
                        for (BlockcypherTxref txref : blockcypherResponse.getTxrefs()) {
                            serverApiBlockcypher.requestData("ETH", ServerApiBlockcypher.BLOCKCYPHER_TXS, "", txref.getTx_hash());
                        }

                        if (serverApiBlockcypher.isRequestsSequenceCompleted()) {
                            blockchainRequestsCallbacks.onComplete(!ctx.hasError());//serverApiElectrum.isErrorOccurred(), serverApiElectrum.getError());
                        } else {
                            blockchainRequestsCallbacks.onProgress();
                        }
                    } else {
                        blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "FAIL BLOCKCYPHER_ADDRESS Exception");
                }
            }

            public void onSuccess(String method, BlockcypherFee blockcypherFee) {
                Log.e(TAG, "Wrong response type for requestBalanceAndUnspentTransactions");
            }

            @Override
            public void onFail(String method, String message) {
                Log.i(TAG, "onFail: " + method + " " + message);
            }
        };

        ServerApiBlockcypher.TxResponseListener txListener = new ServerApiBlockcypher.TxResponseListener() {
            @Override
            public void onSuccess(BlockcypherTx response) {
                Log.i(TAG, "onSuccess: BlockcypherTx");
                try {
                    String address = approvalAddress;
                    if (address.startsWith("0x") || address.startsWith("0X")) {
                        address = address.substring(2);
                    }
                    if (response.getAddesses().contains(address)) {
                        coinData.setHasApprovalTx(true);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "onFail: BlockcypherTx" + response);
                }
                if (serverApiBlockcypher.isRequestsSequenceCompleted()) {
                    blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                } else {
                    blockchainRequestsCallbacks.onProgress();
                }
            }

            @Override
            public void onFail(String message) {
                Log.i(TAG, "onFail: BlockcypherTx " + message);
                if (serverApiBlockcypher.isRequestsSequenceCompleted()) {
                    blockchainRequestsCallbacks.onComplete(!ctx.hasError());//serverApiElectrum.isErrorOccurred(), serverApiElectrum.getError());
                } else {
                    blockchainRequestsCallbacks.onProgress();
                }
            }
        };

        serverApiBlockcypher.setResponseListener(blockcypherListener);
        serverApiBlockcypher.setTxResponseListener(txListener);

        serverApiBlockcypher.requestData("ETH", ServerApiBlockcypher.BLOCKCYPHER_ADDRESS, ctx.getCoinData().getWallet(), "");
        requestApprovalAddressNonce();
    }

    @Override
    public void requestFee(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) {
        blockchainRequestsCallbacks.onComplete(true);
    }

    @Override
    public void requestSendTransaction(BlockchainRequestsCallbacks blockchainRequestsCallbacks, byte[] txForSend) {

        String txStr = String.format("0x%s", BTCUtils.toHex(txForSend));

        ServerApiInfura serverApiInfura = new ServerApiInfura();
        // request requestData eth gasPrice listener
        ServerApiInfura.ResponseListener responseListener = new ServerApiInfura.ResponseListener() {
            @Override
            public void onSuccess(String method, InfuraResponse infuraResponse) {
                if (method.equals(ServerApiInfura.INFURA_ETH_SEND_RAW_TRANSACTION)) {
                    if (infuraResponse.getResult() == null || infuraResponse.getResult().isEmpty()) {
                        ctx.setError("Rejected by node: " + infuraResponse.getError());
                        blockchainRequestsCallbacks.onComplete(false);
                    } else {
                        ctx.setError(null);
                        blockchainRequestsCallbacks.onComplete(true);
                    }
                }
            }

            @Override
            public void onFail(String method, String message) {
                if (method.equals(ServerApiInfura.INFURA_ETH_SEND_RAW_TRANSACTION)) {
                    ctx.setError(message);
                    blockchainRequestsCallbacks.onComplete(false);
                }
            }
        };

        serverApiInfura.setResponseListener(responseListener);

        serverApiInfura.requestData(ServerApiInfura.INFURA_ETH_SEND_RAW_TRANSACTION, 67, coinData.getWallet(), "", txStr);
    }

    public int pendingTransactionTimeoutInSeconds() {
        return 10;
    }

    @Override
    public boolean needMultipleLinesForBalance() {
        return true;
    }

    @Override
    public void defineWallet() throws CardProtocol.TangemException {
        try {
            String wallet = calculateAddress(calculateCKDpub(ctx.getCard().getIdHash()));
            ctx.getCoinData().setWallet(wallet);
        } catch (Exception e) {
            ctx.getCoinData().setWallet("ERROR");
            throw new CardProtocol.TangemException("Can't define wallet address");
        }

    }

    private byte[] calculateCKDpub(byte[] idHash) {
        DeterministicKey masterPubKey = HDKeyDerivation.createMasterPubKeyFromBytes(ctx.getCard().getWalletPublicKeyRar(), idHash);
        DeterministicKey childPubKey = HDKeyDerivation.deriveChildKey(masterPubKey, new ChildNumber(1, false));
        return childPubKey.getPubKey();
    }

    private EthTransaction constructIdTxForSign(long nonce) {
        BigInteger nonceValue = BigInteger.valueOf(nonce);

        BigInteger gasPrice = BigInteger.valueOf(10000000000L);
        BigInteger gasLimit = BigInteger.valueOf(21000);

        BigInteger weiAmount = gasPrice.multiply(gasLimit).add(BigInteger.ONE);

        Integer chainId = this.getChainIdNum();

        String to = coinData.getWallet();

        if (to.startsWith("0x") || to.startsWith("0X")) {
            to = to.substring(2);
        }

        return EthTransaction.create(to, weiAmount, nonceValue, gasPrice, gasLimit, chainId);
    }

    private boolean checkSignature(byte[] signature, long nonce) throws SignatureDecodeException {
        byte[] hashForSign = constructIdTxForSign(nonce).getRawHash();
//        new Secp256k1Context();
        return ECKey.verify(hashForSign, signature, coinData.getCKDpub());
    }

    private void requestApprovalAddressNonce() {
        final ServerApiInfura serverApiInfura = new ServerApiInfura();

        ServerApiInfura.ResponseListener responseListener = new ServerApiInfura.ResponseListener() {
            @Override
            public void onSuccess(String method, InfuraResponse infuraResponse) {
                String pending = infuraResponse.getResult();
                pending = pending.substring(2);
//                BigInteger count = new BigInteger(pending, 16);
                Long count = Long.valueOf(pending, 16);
                coinData.setApprovalAddressNonce(count);
            }

            @Override
            public void onFail(String method, String message) {
                Log.e(TAG, "onFail: " + method + " " + message);
            }
        };
        serverApiInfura.setResponseListener(responseListener);

        serverApiInfura.requestData(ServerApiInfura.INFURA_ETH_GET_PENDING_COUNT, 67, approvalAddress, "", "");
    }
}
