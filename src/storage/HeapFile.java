package storage;

import buffer.BufferManager;
import replacement.ReplacementPolicy;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The HeapFile class manages a collection of records stored in pages,
 * providing functionality for insertion, search, deletion, and range-based queries.
 */
public class HeapFile {
    private PageDirectory pageDirectory; // Metadata for all pages
    private String dataFilename;         // Path to the data file
    private String directoryFilename;    // Path to the page directory file

    private Map<Integer, SimpleEntry<Integer, Integer>> hashIndex; // In-memory hash index for fast record lookup
    private BufferManager bufferManager; // Manages pages in memory
    private int diskReadCount = 0;       // Counts disk read operations
    private int diskWriteCount = 0;      // Counts disk write operations

    /**
     * Constructs a HeapFile instance with specified filenames and buffer manager parameters.
     *
     * @param dataFilename      Path to the data file.
     * @param directoryFilename Path to the directory file.
     * @param bufferSize        Size of the buffer pool.
     * @param policy            Replacement policy for the buffer manager.
     * @throws IOException If an I/O error occurs while initializing the directory.
     */
    public HeapFile(String dataFilename, String directoryFilename, int bufferSize, ReplacementPolicy policy) throws IOException {
        this.dataFilename = dataFilename;
        this.directoryFilename = directoryFilename;
        this.pageDirectory = readDirectoryFromDisk();
        this.bufferManager = new BufferManager(dataFilename, bufferSize, policy);
        this.hashIndex = new HashMap<>();
        rebuildHashIndex();
    }

    /**
     * Rebuilds the in-memory hash index based on the current contents of the page directory.
     *
     * @throws IOException If an I/O error occurs during page access.
     */
    private void rebuildHashIndex() throws IOException {
        List<PageInfo> pages = pageDirectory.getPages();
        for (int pageId = 0; pageId < pages.size(); pageId++) {
            PageInfo pageInfo = pages.get(pageId);
            long offset = pageInfo.getOffset();
            Page page = bufferManager.getPage(offset);

            for (int recordId = 0; recordId < Page.SLOT_COUNT; recordId++) {
                if (page.isSlotUsed(recordId)) {
                    Record record = page.getRecord(recordId);
                    hashIndex.put(record.getKey(), new SimpleEntry<>(pageId, recordId));
                }
            }
        }
    }

    /**
     * Inserts a record into the heap file. Allocates a new page if no free slots are available.
     *
     * @param record The record to insert.
     * @throws IOException If an I/O error occurs during the operation.
     */
    public void insertRecord(Record record) throws IOException {
        PageInfo pageInfo = null;
        Page page = null;

        // Find a page with free slots
        for (PageInfo info : pageDirectory.getPages()) {
            if (info.getFreeSlots() > 0) {
                pageInfo = info;
                page = bufferManager.getPage(pageInfo.getOffset());
                break;
            }
        }

        if (page == null) {
            // No page with free slots, create a new page
            page = new Page();
            long offset = pageDirectory.getPages().size() * Page.PAGE_SIZE;
            pageInfo = new PageInfo(offset, Page.SLOT_COUNT);
            pageDirectory.addPage(pageInfo);

            try (RandomAccessFile raf = new RandomAccessFile(dataFilename, "rw")) {
                raf.seek(pageInfo.getOffset());
                raf.write(page.toByteArray());
                diskWriteCount++;
            }

            page = bufferManager.getPage(offset);
            if (page == null) {
                throw new IOException("Failed to load newly created page into the buffer manager.");
            }
        }

        // Insert the record into the first available slot
        for (int i = 0; i < Page.SLOT_COUNT; i++) {
            if (!page.isSlotUsed(i)) {
                page.insertRecord(i, record);
                bufferManager.markDirty(pageInfo.getOffset());

                pageInfo.setFreeSlots(pageInfo.getFreeSlots() - 1);
                pageDirectory.updatePageInfo(pageInfo);
                writeDirectoryToDisk();

                int pageId = (int) pageInfo.getOffset() / Page.PAGE_SIZE;
                hashIndex.put(record.getKey(), new SimpleEntry<>(pageId, i));
                return;
            }
        }
    }

    /**
     * Searches for a record by its key using a full table scan.
     *
     * @param key The key of the record to search for.
     * @return The matching record, or null if not found.
     * @throws IOException If an I/O error occurs during the operation.
     */
    public Record searchRecord(int key) throws IOException {
        for (PageInfo pageInfo : pageDirectory.getPages()) {
            Page page = bufferManager.getPage(pageInfo.getOffset());
            for (int i = 0; i < Page.SLOT_COUNT; i++) {
                if (page.isSlotUsed(i) && page.getRecord(i).getKey() == key) {
                    return page.getRecord(i);
                }
            }
        }
        return null;
    }

    /**
     * Searches for a record using the in-memory hash index.
     *
     * @param key The key of the record to search for.
     * @return The matching record, or null if not found.
     * @throws IOException If an I/O error occurs during the operation.
     */
    public Record searchRecordWithHash(int key) throws IOException {
        SimpleEntry<Integer, Integer> entry = hashIndex.get(key);
        if (entry == null) {
            return null;
        }

        int pageId = entry.getKey();
        int recordId = entry.getValue();
        long offset = pageId * Page.PAGE_SIZE;
        Page page = bufferManager.getPage(offset);

        return page.isSlotUsed(recordId) ? page.getRecord(recordId) : null;
    }

    /**
     * Deletes a record by its key.
     *
     * @param key The key of the record to delete.
     * @return True if the record was successfully deleted, false otherwise.
     * @throws IOException If an I/O error occurs during the operation.
     */
    public boolean deleteRecord(int key) throws IOException {
        for (PageInfo pageInfo : pageDirectory.getPages()) {
            Page page = bufferManager.getPage(pageInfo.getOffset());
            for (int i = 0; i < Page.SLOT_COUNT; i++) {
                if (page.isSlotUsed(i) && page.getRecord(i).getKey() == key) {
                    page.deleteRecord(i);
                    bufferManager.markDirty(pageInfo.getOffset());

                    pageInfo.setFreeSlots(pageInfo.getFreeSlots() + 1);
                    pageDirectory.updatePageInfo(pageInfo);
                    writeDirectoryToDisk();
                    hashIndex.remove(key);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Performs a range search for records within the specified bounds.
     *
     * @param lowerBound The lower bound of the range (inclusive).
     * @param upperBound The upper bound of the range (inclusive).
     * @return A list of matching records.
     * @throws IOException If an I/O error occurs during the operation.
     */
    public List<Record> rangeSearch(int lowerBound, int upperBound) throws IOException {
        List<Record> result = new ArrayList<>();
        for (PageInfo pageInfo : pageDirectory.getPages()) {
            Page page = bufferManager.getPage(pageInfo.getOffset());
            for (int i = 0; i < Page.SLOT_COUNT; i++) {
                if (page.isSlotUsed(i)) {
                    Record record = page.getRecord(i);
                    if (record.getKey() >= lowerBound && record.getKey() <= upperBound) {
                        result.add(record);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Prints all pages and their records in the heap file.
     *
     * @throws IOException If an I/O error occurs during the operation.
     */
    public void printAllPages() throws IOException {
        List<PageInfo> pages = pageDirectory.getPages();
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            Page page = bufferManager.getPage(pages.get(pageIndex).getOffset());
            System.out.print("Page " + pageIndex + ": ");
            page.printAllRecords();
        }
    }

    // Helper method to read the page directory from disk
    private PageDirectory readDirectoryFromDisk() throws IOException {
        File dirFile = new File(directoryFilename);
        if (!dirFile.exists()) {
            return new PageDirectory();
        }
        try (FileInputStream fis = new FileInputStream(directoryFilename)) {
            byte[] data = fis.readAllBytes();
            diskReadCount++;
            return PageDirectory.fromByteArray(data);
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to load page directory", e);
        }
    }

    // Helper method to write the page directory to disk
    private void writeDirectoryToDisk() throws IOException {
        try (FileOutputStream fos = new FileOutputStream(directoryFilename)) {
            fos.write(pageDirectory.toByteArray());
            diskWriteCount++;
        }
    }

    public void flushAll() throws IOException {
        bufferManager.flushAll();
    }

    public int getCurrentPoolSize() {
        return bufferManager.getCurrentPoolSize();
    }

    // Methods to access disk I/O statistics
    public int getDiskReadCount() {
        return diskReadCount + bufferManager.getDiskReadCount();
    }

    public int getDiskWriteCount() {
        return diskWriteCount + bufferManager.getDiskWriteCount();
    }

    public void resetDiskIOCounters() {
        diskReadCount = 0;
        diskWriteCount = 0;
        bufferManager.resetDiskIOCounters();
    }

    // Methods to access buffer hit/miss statistics
    public double getHitRatio() {
        return bufferManager.getHitRatio();
    }

    public int getHitCount() {
        return bufferManager.getHitCount();
    }

    public int getMissCount() {
        return bufferManager.getMissCount();
    }

    public void resetHitMissCounters() {
        bufferManager.resetHitMissCounters();
    }

    // Retrieves the name of the replacement policy
    public String getReplacementPolicyName() { return bufferManager.getReplacementPolicyName(); }

    // Nested class to represent a key-value pair
    private static class SimpleEntry<K, V> {
        private K key;
        private V value;

        public SimpleEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }
}