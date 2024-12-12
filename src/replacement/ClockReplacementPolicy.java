package replacement;

// TODO: Import libraries if needed.

import java.util.ArrayList;

/**
 * Implements the CLOCK replacement policy.
 * This policy uses a circular buffer and reference bits to approximate LRU behavior.
 */
public class ClockReplacementPolicy implements ReplacementPolicy {
    private int capacity;
    private ArrayList<Long> pages;
    private ArrayList<Boolean> referenceBits;
    private int clockHand;

    /**
     * Constructs a ClockReplacementPolicy instance with the specified capacity.
     *
     * @param capacity The maximum number of pages the buffer pool can hold.
     */
    public ClockReplacementPolicy(int capacity) {
        this.capacity = capacity;
        this.pages = new ArrayList<>(capacity);
        this.referenceBits = new ArrayList<>(capacity);
        this.clockHand = 0;
    }

    /**
     * Initializes the Clock replacement policy.
     * Resets the internal state, clearing all pages and reference bits.
     */
    @Override
    public void init() {
        pages.clear();
        referenceBits.clear();
        clockHand = 0;
    }

    /**
     * Notifies the policy of a page access.
     * If the page is already in the buffer, sets its reference bit to true.
     * If not, adds the page to an empty slot in the buffer.
     *
     * @param offset The offset of the accessed page.
     */
    @Override
    public void notifyPageAccess(long offset) {
        int index = pages.indexOf(offset);
        if (index != -1) {
            referenceBits.set(index, true);
        } else if (pages.size() < capacity) {
            pages.add(offset);
            referenceBits.add(true);
        }
    }

    /**
     * Notifies the policy of a page eviction.
     * Removes the evicted page from the buffer and clears its reference bit.
     *
     * @param offset The offset of the evicted page.
     */
    @Override
    public void notifyPageEvict(long offset) {
        int index = pages.indexOf(offset);
        if (index != -1) {
            pages.remove(index);
            referenceBits.remove(index);
        }
    }

    /**
     * Chooses a victim page to evict based on the Clock policy.
     * Finds the first page with a reference bit of 0, resetting bits with value 1.
     *
     * @return The offset of the page to evict.
     */
    @Override
    public long chooseVictim() {
        while (true) {
            if (!referenceBits.get(clockHand)) {
                long victim = pages.get(clockHand);
                pages.remove(clockHand);
                referenceBits.remove(clockHand);
                return victim;
            }
            referenceBits.set(clockHand, false);
            clockHand = (clockHand + 1) % capacity;
        }
    }

    /**
     * Returns the name of this replacement policy.
     *
     * @return "CLOCK" as the name of the policy.
     */
    @Override
    public String getName() {
        return "CLOCK";
    }
}
