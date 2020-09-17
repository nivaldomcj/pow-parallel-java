import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Code based on ProofOfWork.java
 */
public class ProofOfWorkRange {

    public static void main(String[] args) throws NoSuchAlgorithmException {
        //Block header
        //See more https://en.bitcoin.it/wiki/Block_hashing_algorithm
        //Set the difficulty by typing the amount of zeros that you want to the hash to begin
        String difficulty = args[0];
        String previousBlock = args[1];
        String merkelRoot = args[2];
        long timestamp = Long.parseLong(args[3]);

        // Used to keep track of protocol upgrades
        long version = 2;

        // Target. Changes every 2016 blocks
        long bits = 419520339;

        // Nonce ranges (start/stop)
        long nonce = Long.parseLong(args[4]);
        long nonceStop = Long.parseLong(args[5]);

        String message = version
                + new String(Utils.reverseBytes(previousBlock.getBytes()))
                + new String(Utils.reverseBytes(merkelRoot.getBytes()))
                + timestamp
                + bits;
        String hashTest = null;

        while (true) {
            if (nonce == nonceStop)
                break;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(digest.digest(message.concat(Long.toString(nonce)).getBytes(StandardCharsets.UTF_8)));
            hashTest = Utils.bytesToHex(Utils.reverseBytes(hash));
            System.out.println("Nonce: " + nonce + " | Hash: " + hashTest);

            if (hashTest.substring(0, difficulty.length()).equals(difficulty)) {
                System.out.println("------------------------");
                System.out.println("Block mined!");
                System.out.println("Nonce: " + nonce);
                System.out.println("Block hash: " + hashTest);
                break;
            }

            nonce++;
        }


    }

}