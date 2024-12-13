package test;

import replacement.ClockReplacementPolicy;
import replacement.LRUReplacementPolicy;
import replacement.MRUReplacementPolicy;
import replacement.ReplacementPolicy;
import storage.HeapFile;

import java.io.IOException;
import java.util.Random;

/**
 * A test class for evaluating the effectiveness of different replacement policies
 * (LRU, MRU, CLOCK) in managing the buffer pool. The class examines various
 * scenarios to highlight the strengths and weaknesses of each policy.
 */
public class ReplacementPolicyBasicTest {
    public static void main(String[] args) {
        try {
            // File names for the HeapFile data and directory files
            String heapDataFilename = "hf_test_input.dat";
            String heapDirectoryFilename = "hf_test_input.pd";

            // --- Initialize HeapFile instances with different replacement policies ---
            int bufferSize = 16; // Size of the buffer pool
            ReplacementPolicy lruPolicy = new LRUReplacementPolicy(bufferSize);
            HeapFile hfLRU = new HeapFile(heapDataFilename, heapDirectoryFilename, bufferSize, lruPolicy);
            ReplacementPolicy mruPolicy = new MRUReplacementPolicy(bufferSize);
            HeapFile hfMRU = new HeapFile(heapDataFilename, heapDirectoryFilename, bufferSize, mruPolicy);

            // Initialize buffer pool content to avoid cold misses in subsequent tests
            hfLRU.searchRecord(256 * 2);
            hfMRU.searchRecord(256 * 2);

            // --- Task 1: Analyze MRU's advantages over LRU in a specific scenario ---
            System.out.println("Task 1: Why is MRU better than LRU in this case?");
            initBufferPoolStats(hfLRU);
            initBufferPoolStats(hfMRU);

            for (int i = 1; i <= 100; i++) {
                hfLRU.searchRecord(256);
                hfMRU.searchRecord(256);
            }

            printBufferPoolStats(hfLRU);
            printBufferPoolStats(hfMRU);
            System.out.println();

            // --- Task 2: Analyze LRU's advantages over MRU in a specific scenario ---
            System.out.println("Task 2: Why is LRU better than MRU in this case?");
            initBufferPoolStats(hfLRU);
            initBufferPoolStats(hfMRU);

            Random rand = new Random(0); // Random generator for access patterns
            int hotRecord = 128; // Number of hot (frequently accessed) records
            int totalAccess = 1000; // Total number of record accesses
            double hotRatio = 0.8; // Probability of accessing hot records

            for (int i = 0; i < totalAccess; i++) {
                int key;
                if (rand.nextDouble() < hotRatio) {
                    key = rand.nextInt(hotRecord); // Select from hot records
                } else {
                    key = hotRecord + rand.nextInt(1024); // Select from cold records
                }
                hfLRU.searchRecordWithHash(key);
                hfMRU.searchRecordWithHash(key);
            }

            printBufferPoolStats(hfLRU);
            printBufferPoolStats(hfMRU);
            System.out.println();

            // --- Task 3: Evaluate CLOCK as an approximation of LRU ---
            System.out.println("Task 3: Does CLOCK approximate LRU behavior?");

            ReplacementPolicy clockPolicy = new ClockReplacementPolicy(bufferSize);
            HeapFile hfClock = new HeapFile(heapDataFilename, heapDirectoryFilename, bufferSize, clockPolicy);

            // Reinitialize buffer pools for consistent comparison
            hfLRU.searchRecord(256 * 2);
            hfClock.searchRecord(256 * 2);
            hfMRU.searchRecord(256 * 2);

            initBufferPoolStats(hfLRU);
            initBufferPoolStats(hfClock);
            initBufferPoolStats(hfMRU);

            for (int i = 0; i < totalAccess; i++) {
                int key;
                if (rand.nextDouble() < hotRatio) {
                    key = rand.nextInt(hotRecord); // Select from hot records
                } else {
                    key = hotRecord + rand.nextInt(1024); // Select from cold records
                }
                hfLRU.searchRecordWithHash(key);
                hfClock.searchRecordWithHash(key);
                hfMRU.searchRecordWithHash(key);
            }

            printBufferPoolStats(hfLRU);
            printBufferPoolStats(hfClock);
            printBufferPoolStats(hfMRU);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Resets the hit and miss counters of the given HeapFile.
     *
     * @param hf the HeapFile instance to reset stats for
     */
    public static void initBufferPoolStats(HeapFile hf) {
        hf.resetHitMissCounters();
    }

    /**
     * Prints the hit, miss, and hit ratio statistics for the given HeapFile.
     *
     * @param hf the HeapFile instance whose stats are printed
     */
    public static void printBufferPoolStats(HeapFile hf) {
        System.out.println("- " + hf.getReplacementPolicyName()
                + " hit: " + hf.getHitCount()
                + ", miss: " + hf.getMissCount()
                + ", hit ratio: " + String.format("%.2f", hf.getHitRatio()));
    }
}
