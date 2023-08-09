package searchengine.model;

/**
 * Indexing status enumeration.
 */
public enum IndexingStatus {
    INDEXING,
    INDEXED,
    FAILED;

    // Static methods

    /**
     * Indexing status database field sql declaration.
     */
    public static final String SQL_ENUM = "ENUM('INDEXING', 'INDEXED', 'FAILED')";
}
