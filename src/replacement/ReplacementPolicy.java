package replacement;

/**
 * Defines the interface for a page replacement policy.
 * Replacement policies manage which pages to evict from the buffer pool
 * when space is needed for new pages.
 */
public interface ReplacementPolicy {

    /**
     * Initializes the replacement policy.
     * This method is called once before the policy is used.
     */
    void init();

    /**
     * Notifies the replacement policy of a page access.
     * This method should update the internal state to reflect that
     * the page with the given offset was accessed.
     *
     * @param offset The offset of the accessed page in the file.
     */
    void notifyPageAccess(long offset);

    /**
     * Notifies the replacement policy of a page eviction.
     * This method should update the internal state to reflect that
     * the page with the given offset was removed from the buffer.
     *
     * @param offset The offset of the evicted page in the file.
     */
    void notifyPageEvict(long offset);

    /**
     * Chooses a victim page to evict based on the policy's rules.
     *
     * @return The offset of the page to be evicted.
     */
    long chooseVictim();

    /**
     * Returns the name of the replacement policy.
     *
     * @return The name of the policy as a String.
     */
    String getName();
}