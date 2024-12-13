package buffer;

import storage.Page;

/**
 * Represents a frame in the buffer pool, containing a page, its offset in the file,
 * and a dirty flag to indicate if the page has been modified.
 */
public class PageFrame {
    private long offset; // The offset of the page in the file
    private Page page;   // The page stored in this frame
    private boolean dirty; // Indicates whether the page has been modified

    /**
     * Constructs a PageFrame with the given offset and page.
     *
     * @param offset The offset of the page in the file.
     * @param page   The page to be stored in this frame.
     */
    public PageFrame(long offset, Page page) {
        this.offset = offset;
        this.page = page;
        this.dirty = false; // Pages are not dirty by default upon creation
    }

    /**
     * Retrieves the offset of the page in the file.
     *
     * @return The offset of the page.
     */
    public long getOffset() {
        return offset;
    }

    /**
     * Sets the offset of the page in the file.
     *
     * @param offset The new offset of the page.
     */
    public void setOffset(long offset) {
        this.offset = offset;
    }

    /**
     * Retrieves the page stored in this frame.
     *
     * @return The Page object.
     */
    public Page getPage() {
        return page;
    }

    /**
     * Checks if the page in this frame is dirty.
     *
     * @return True if the page is dirty, false otherwise.
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Sets the dirty flag for this page.
     *
     * @param dirty True if the page has been modified, false otherwise.
     */
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
