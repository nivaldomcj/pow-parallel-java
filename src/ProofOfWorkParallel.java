import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Code based on ProofOfWork.java
 */
public class ProofOfWorkParallel {

    public static class ProofOfWorkExecutor {

        // https://en.bitcoin.it/wiki/Block_hashing_algorithm
        private final String hashPrevBlock;   // 256-bit hash of previous block header
        private final String hashMerkleRoot;  // 256-bit hash based on all of the transactions in the block
        private final String difficulty;      // amount of zeros that hash should begin with
        private final long timestamp;         // current block timestamp as seconds
        private final int version;            // block version number
        private final int bits;               // target - changes every 2016 blocks

        // number of threads that will be created
        private int numThreads;

        // holds if solved the hash or not
        private boolean solved;

        public ProofOfWorkExecutor(String difficulty, String hashPrevBlock, String hashMerkleRoot, String timestamp) {
            this.hashPrevBlock = hashPrevBlock;
            this.hashMerkleRoot = hashMerkleRoot;
            this.difficulty = difficulty;
            this.timestamp = Long.parseLong(timestamp);
            this.version = 2;
            this.bits = 419520339;

            setNumThreads(Runtime.getRuntime().availableProcessors());
        }

        public void setNumThreads(int numThreads) {
            int availableThreads = Runtime.getRuntime().availableProcessors();

            if (numThreads > availableThreads) {
                System.out.println("You're setting more threads than available...");
            }

            this.numThreads = numThreads;
            System.out.println("Set " + this.numThreads + " threads of " + availableThreads + " available threads!");
        }

        public void execute() throws InterruptedException {
            this.solved = false;

            // base block header (message)
            final String blockHeader = version
                    + new String(Utils.reverseBytes(hashPrevBlock.getBytes()))
                    + new String(Utils.reverseBytes(hashMerkleRoot.getBytes()))
                    + timestamp
                    + bits;

            long initialNonce = 0;              // from where nonce starts
            long increaseBy = 100000;           // number of `pieces` of nonce to be sent to threads

            while (!solved) {
                ExecutorService es = Executors.newFixedThreadPool(numThreads);

                // each thread will have a minimum and maximum nonce to attempt
                // this two variables controls the range of those
                long nonceFrom = initialNonce;
                long nonceUntil = nonceFrom + increaseBy;

                for (int t = 0; t < numThreads; t++) {
                    final long threadNonceFrom = nonceFrom;
                    final long threadNonceUntil = nonceUntil;

                    es.execute(new Runnable() {
                        @Override
                        public void run() {
                            // try instantiate a sha256 digest
                            MessageDigest digest = null;
                            try {
                                digest = MessageDigest.getInstance("SHA-256");
                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
                            }

                            System.out.printf(
                                    "Thread %d of %d is working from nonce %d to %d\n",
                                    (Thread.currentThread().getId() % numThreads) + 1,
                                    numThreads,
                                    threadNonceFrom,
                                    threadNonceUntil
                            );

                            String strHash;
                            byte[] rawHash;

                            for (long nonce = threadNonceFrom; nonce < threadNonceUntil; nonce++) {
                                // we can't continue if sha256 digest is null
                                if (digest == null)
                                    break;

                                rawHash = blockHeader.concat(Long.toString(nonce)).getBytes(StandardCharsets.UTF_8);
                                rawHash = digest.digest(digest.digest(rawHash));
                                strHash = Utils.bytesToHex(Utils.reverseBytes(rawHash));

//                                System.out.printf(
//                                        "(Thread #%s) [%d -> %d] Nonce: %s, Hash: %s\n",
//                                        Thread.currentThread().getName(),
//                                        threadNonceFrom, threadNonceUntil,
//                                        Long.toString(nonce), strHash
//                                );

                                if (strHash.startsWith(difficulty)) {
                                    solved = true;
                                    System.out.println("------------------------");
                                    System.out.println("Block mined!");
                                    System.out.println("Nonce: " + nonce);
                                    System.out.println("Block hash: " + strHash);
                                    break;
                                }
                            }
                        }
                    });

                    // increment nonce range for the next thread
                    nonceFrom += increaseBy;
                    nonceUntil += increaseBy;
                }

                // lock this thread pool and wait until these finishes
                // guess will not be more than 1 hour...
                es.shutdown();
                es.awaitTermination(1, TimeUnit.HOURS);

                // move initial to the new `piece` of nonce (100K * threads)
                initialNonce += (numThreads * increaseBy);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting~");
        System.out.println("------------------------");

        long startTime = System.nanoTime();
        {
            ProofOfWorkExecutor pow = new ProofOfWorkExecutor(args[0], args[1], args[2], args[3]);

            if (args.length > 4)
                pow.setNumThreads(Integer.parseInt(args[4]));

            pow.execute();
        }
        long elapsedNanos = System.nanoTime() - startTime;

        System.out.println("Finished~");
        System.out.println("------------------------");
        System.out.printf("Approximate elapsed time: %.2f seconds\n", (elapsedNanos / 1e+9));
    }
}
