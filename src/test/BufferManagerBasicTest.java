package test;

import replacement.LRUReplacementPolicy;
import replacement.ReplacementPolicy;
import storage.HeapFile;
import storage.Record;

import java.io.IOException;

/**
 * A basic test class for evaluating the Buffer Manager and its interaction
 * with the HeapFile implementation. It tests insertions, searches, eviction
 * policies, and dirty-page handling.
 */
public class BufferManagerBasicTest {
    public static void main(String[] args) {
        try {
            // File names for the HeapFile data and directory files
            String heapDataFilename = "hf_buffmgr_test.dat";
            String heapDirectoryFilename = "hf_buffmgr_test.pd";

            // Remove existing files to ensure a clean test environment
            new java.io.File(heapDataFilename).delete();
            new java.io.File(heapDirectoryFilename).delete();

            // --- Initialize the HeapFile ---
            int bufferSize = 2; // Buffer pool size
            ReplacementPolicy lruPolicy = new LRUReplacementPolicy(bufferSize);
            HeapFile heapFile = new HeapFile(heapDataFilename, heapDirectoryFilename, bufferSize, lruPolicy);

            // --- Insert records into the HeapFile ---
            // Insert 32 records, enough to fill two pages (16 records per page)
            try {
                for (int i = 1; i <= 32; i++) {
                    String data = "HeapData" + i;
                    Record record = new Record(i, data);
                    heapFile.insertRecord(record);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            // Print all records to confirm successful insertion
            System.out.println("HeapFile contents after initial record insertion:");
            heapFile.printAllPages();
            System.out.println();

            // --- Test searches ---
            heapFile.resetHitMissCounters();

            // Search for a record with key 1
            // This accesses Page 0 (expected: one hit)
            int searchKey = 1;
            Record foundRecord = heapFile.searchRecord(searchKey);
            if (foundRecord != null) {
                System.out.println("Found Record - Key: " + foundRecord.getKey() + ", Data: " + foundRecord.getData());
            } else {
                System.out.println("Record with Key " + searchKey + " not found.");
            }
            System.out.println("Buffer Hits: " + heapFile.getHitCount() + ", Misses: " + heapFile.getMissCount());
            System.out.println();

            // Search for a record with key 17
            // This accesses Page 0 and Page 1 (expected: two hits)
            searchKey = 17;
            foundRecord = heapFile.searchRecord(searchKey);
            if (foundRecord != null) {
                System.out.println("Found Record - Key: " + foundRecord.getKey() + ", Data: " + foundRecord.getData());
            } else {
                System.out.println("Record with Key " + searchKey + " not found.");
            }
            System.out.println("Buffer Hits: " + heapFile.getHitCount() + ", Misses: " + heapFile.getMissCount());
            System.out.println();

            // --- Test eviction by inserting a new record ---
            System.out.println("Inserting a new record with Key: 33");

            // Insert a new record, triggering eviction of the oldest page (Page 0)
            heapFile.insertRecord(new Record(33, "HeapData33"));

            // Check if buffer pool size exceeds the configured buffer size
            if (heapFile.getCurrentPoolSize() > bufferSize) {
                System.err.println("ERROR: Buffer pool overflow detected. Eviction policy failed.");
                return;
            }
            System.out.println("Buffer Hits: " + heapFile.getHitCount() + ", Misses: " + heapFile.getMissCount());
            System.out.println();

            // Validate eviction logic by searching for an evicted record (Key 1)
            searchKey = 1;
            foundRecord = heapFile.searchRecord(searchKey); // Access Page 0 again (expected: one miss)
            if (foundRecord != null) {
                System.out.println("Found Record - Key: " + foundRecord.getKey() + ", Data: " + foundRecord.getData());
            } else {
                System.out.println("Record with Key " + searchKey + " not found (likely evicted).");
            }
            System.out.println("Buffer Hits: " + heapFile.getHitCount() + ", Misses: " + heapFile.getMissCount());
            System.out.println();

            // --- Test eviction and dirty-page handling ---
            System.out.println("Reloading the HeapFile to test dirty-page handling:");
            HeapFile heapFile2 = new HeapFile(heapDataFilename, heapDirectoryFilename, bufferSize, lruPolicy);
            heapFile2.printAllPages();
            System.out.println();

            // Delete records with keys 8 to 16
            for (int i = 8; i <= 16; i++) {
                heapFile.deleteRecord(i);
            }

            // Flush all changes to disk and reload the HeapFile for verification
            heapFile.flushAll();
            System.out.println("Reloading the HeapFile after flushing changes:");
            heapFile = new HeapFile(heapDataFilename, heapDirectoryFilename, bufferSize, lruPolicy);
            heapFile.printAllPages();

            // Clean up test files
            new java.io.File(heapDataFilename).delete();
            new java.io.File(heapDirectoryFilename).delete();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}