package buffer;

import replacement.ReplacementPolicy;
import storage.Page;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

/**
 * The BufferManager class manages pages in memory using a buffer pool.
 * It interacts with replacement policies and handles page I/O with the disk.
 */
public class BufferManager {
    private int poolSize; // Maximum number of pages in the buffer pool
    private Map<Long, PageFrame> pageTable; // Maps offsets to PageFrame objects
    private ReplacementPolicy replacementPolicy; // Replacement policy for page eviction
    private String dataFilename; // Path to the data file

    private int diskReadCount = 0; // Tracks the number of disk read operations
    private int diskWriteCount = 0; // Tracks the number of disk write operations
    private int hitCount = 0; // Tracks buffer hits
    private int missCount = 0; // Tracks buffer misses

    /**
     * Constructs a BufferManager instance.
     *
     * @param dataFilename   Path to the data file.
     * @param poolSize       Maximum number of pages in the buffer pool.
     * @param policy         Replacement policy to manage the buffer pool.
     */
    public BufferManager(String dataFilename, int poolSize, ReplacementPolicy policy) {
        this.dataFilename = dataFilename;
        this.poolSize = poolSize;
        this.replacementPolicy = policy;
        this.replacementPolicy.init();
        this.pageTable = new HashMap<>();
    }

    /**
     * Retrieves a page from the buffer pool, loading it from disk if necessary.
     *
     * @param offset The offset of the page in the file.
     * @return The Page object.
     * @throws IOException If an I/O error occurs.
     */
    public Page getPage(long offset) throws IOException {
        if (pageTable.containsKey(offset)) {
            hitCount++;
            replacementPolicy.notifyPageAccess(offset);
            return pageTable.get(offset).getPage();
        }
        missCount++;
        if (pageTable.size() >= poolSize) {
            long victimOffset = replacementPolicy.chooseVictim();
            PageFrame victimFrame = pageTable.remove(victimOffset);
            if (victimFrame.isDirty()) {
                writePageToDisk(victimFrame.getPage(), victimOffset);
            }
            replacementPolicy.notifyPageEvict(victimOffset);
        }
        Page newPage = readPageFromDisk(offset);
        PageFrame newFrame = new PageFrame(offset, newPage);
        pageTable.put(offset, newFrame);
        replacementPolicy.notifyPageAccess(offset);
        return newPage;
    }

    /**
     * Marks a page in the buffer as dirty, indicating it has been modified.
     *
     * @param offset The offset of the page in the file.
     */
    public void markDirty(long offset) {
        if (pageTable.containsKey(offset)) {
            pageTable.get(offset).setDirty(true);
        }
    }

    /**
     * Writes all dirty pages in the buffer pool to disk.
     *
     * @throws IOException If an I/O error occurs.
     */
    public void flushAll() throws IOException {
        for (Map.Entry<Long, PageFrame> entry : pageTable.entrySet()) {
            if (entry.getValue().isDirty()) {
                writePageToDisk(entry.getValue().getPage(), entry.getKey());
                entry.getValue().setDirty(false);
            }
        }
    }


    /**
     * Reads a page from disk into memory.
     *
     * @param offset The offset of the page in the file.
     * @return The Page object.
     * @throws IOException If an I/O error occurs.
     */
    private Page readPageFromDisk(long offset) throws IOException {
        byte[] bytes = new byte[Page.PAGE_SIZE];
        try (RandomAccessFile raf = new RandomAccessFile(dataFilename, "r")) {
            raf.seek(offset);
            raf.readFully(bytes);
        }
        diskReadCount++;
        return Page.fromByteArray(bytes);
    }

    /**
     * Writes a page from memory to disk.
     *
     * @param page   The Page object to write.
     * @param offset The offset of the page in the file.
     * @throws IOException If an I/O error occurs.
     */
    private void writePageToDisk(Page page, long offset) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(dataFilename, "rw")) {
            raf.seek(offset);
            raf.write(page.toByteArray());
        }
        diskWriteCount++;
    }

    /**
     * Returns the current number of pages in the buffer pool.
     *
     * @return The current pool size.
     */
    public int getCurrentPoolSize() {
        return pageTable.size();
    }

    // Methods to access disk I/O statistics
    public int getDiskReadCount() {
        return diskReadCount;
    }

    public int getDiskWriteCount() {
        return diskWriteCount;
    }

    public void resetDiskIOCounters() {
        diskReadCount = 0;
        diskWriteCount = 0;
    }

    // Methods to access buffer hit/miss statistics
    public int getHitCount() {
        return hitCount;
    }

    public int getMissCount() {
        return missCount;
    }

    public double getHitRatio() {
        int total = hitCount + missCount;
        return total > 0 ? (double) hitCount / total : 0.0;
    }

    public void resetHitMissCounters() {
        hitCount = 0;
        missCount = 0;
    }

    // Retrieves the name of the replacement policy
    public String getReplacementPolicyName() {
        return replacementPolicy.getName();
    }
}
