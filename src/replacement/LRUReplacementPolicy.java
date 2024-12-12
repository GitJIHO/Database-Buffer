package replacement;

import java.util.ArrayList;

/**
 * Implements the Least Recently Used (LRU) replacement policy.
 * This policy evicts the page that has not been accessed for the longest time.
 */
public class LRUReplacementPolicy implements ReplacementPolicy {
    private ArrayList<Long> pages; // Stores the access order of page offsets
    private int capacity;          // Buffer pool capacity (currently unused by this class)

    /**
     * Constructs an LRUReplacementPolicy instance with the specified capacity.
     *
     * @param capacity The maximum capacity of the buffer pool.
     */
    public LRUReplacementPolicy(int capacity) {
        this.pages = new ArrayList<>();
        this.capacity = capacity; // Not enforced by LRU, assumes BufferManager controls size
    }

    /**
     * Initializes the LRU replacement policy.
     * Clears the internal list of pages to reset the state.
     */
    @Override
    public void init() {
        pages.clear();
    }

    /**
     * Notifies the policy that a page has been accessed.
     * Updates the access order by moving the accessed page to the end of the list.
     *
     * @param offset The offset of the accessed page.
     */
    @Override
    public void notifyPageAccess(long offset) {
        pages.remove(offset); // Remove the page if it already exists (O(n))
        pages.add(offset);    // Add the page to the end of the list
    }

    /**
     * Notifies the policy that a page has been evicted.
     * Removes the page from the internal list of pages.
     *
     * @param offset The offset of the evicted page.
     */
    @Override
    public void notifyPageEvict(long offset) {
        pages.remove(offset); // Remove the evicted page (O(n))
    }

    /**
     * Chooses a victim page to evict based on the LRU policy.
     * Evicts the page that was accessed least recently (at the beginning of the list).
     *
     * @return The offset of the page to evict.
     * @throws IllegalStateException If there are no pages to evict.
     */
    @Override
    public long chooseVictim() {
        if (pages.isEmpty()) {
            throw new IllegalStateException("No pages to evict!");
        }
        return pages.get(0); // The least recently used page is at the front
    }

    /**
     * Returns the name of this replacement policy.
     *
     * @return "LRU" as the name of the policy.
     */
    @Override
    public String getName() {
        return "LRU";
    }
}