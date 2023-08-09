package searchengine.dto.indexing;

import lombok.Getter;

import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

/**
 * Lemma occurrence in text offset data.
 */
@Getter
public class LemmaOffset {
    final int start;            // First char index
    final int end;              // Last char index + 1

    /**
     * Constructor initiates this object data with data of apache lucene object.
     *
     * @param offsetAttribute Apache lucene offset object.
     */
    public LemmaOffset(OffsetAttribute offsetAttribute) {
        start = offsetAttribute.startOffset();
        end = offsetAttribute.endOffset();
    }
}
