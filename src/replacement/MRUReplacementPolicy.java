package replacement;

/**
 * Implements the Most Recently Used (MRU) replacement policy.
 * This policy evicts the most recently accessed page.
 */
public class MRUReplacementPolicy implements ReplacementPolicy {
    private long mostRecentlyUsed; // Offset of the most recently accessed page
    private boolean initialized;   // Tracks whether the policy has been initialized
    private int capacity;          // Buffer pool capacity (currently unused by this class)

    /**
     * Constructs an MRUReplacementPolicy instance with the specified capacity.
     *
     * @param capacity The maximum capacity of the buffer pool.
     */
    public MRUReplacementPolicy(int capacity) {
        this.mostRecentlyUsed = -1; // Default value for uninitialized state
        this.initialized = false;
        this.capacity = capacity;  // Not enforced by MRU, assumes BufferManager controls size
    }

    /**
     * Initializes the MRU replacement policy.
     * Resets the internal state, marking it as uninitialized.
     */
    @Override
    public void init() {
        initialized = false;
    }

    /**
     * Notifies the policy that a page has been accessed.
     * Updates the most recently used page to the given offset.
     *
     * @param offset The offset of the accessed page.
     */
    @Override
    public void notifyPageAccess(long offset) {
        mostRecentlyUsed = offset; // Set the most recently used page
        initialized = true;        // Mark the policy as initialized
    }

    /**
     * Notifies the policy that a page has been evicted.
     * If the evicted page is the most recently used page, resets the state.
     *
     * @param offset The offset of the evicted page.
     */
    @Override
    public void notifyPageEvict(long offset) {
        if (mostRecentlyUsed == offset) {
            initialized = false; // Reset if the most recently used page is evicted
        }
    }

    /**
     * Chooses a victim page to evict based on the MRU policy.
     * Evicts the most recently accessed page.
     *
     * @return The offset of the page to evict.
     * @throws IllegalStateException If no page has been accessed yet.
     */
    @Override
    public long chooseVictim() {
        if (!initialized) {
            throw new IllegalStateException("No page has been accessed yet!");
        }
        return mostRecentlyUsed; // Return the most recently accessed page
    }

    /**
     * Returns the name of this replacement policy.
     *
     * @return "MRU" as the name of the policy.
     */
    @Override
    public String getName() {
        return "MRU";
    }
}