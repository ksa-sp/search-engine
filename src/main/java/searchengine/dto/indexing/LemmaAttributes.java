package searchengine.dto.indexing;

import lombok.Getter;

import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Attributes of a lemma found in a text.
 */
@Getter
public class LemmaAttributes {
    private final Set<LemmaOffset> offsetList;

    /**
     * Constructor adds first set of attributes of a lemma.
     *
     * @param offsetAttribute Offset data of the first occurrence of the lemma in the text.
     */
    public LemmaAttributes(OffsetAttribute offsetAttribute) {
        offsetList = new TreeSet<>(
            Comparator.comparingInt(LemmaOffset::getStart)
        );
        offsetList.add(new LemmaOffset(offsetAttribute));
    }

    /**
     * Adds next set of attributes of a lemma.
     *
     * @param attribute Offset data of the next occurrence of the lemma in the text.
     *
     * @return This object.
     */
    public LemmaAttributes add(OffsetAttribute attribute) {
        offsetList.add(new LemmaOffset(attribute));

        return this;
    }

    /**
     * Returns number of occurrences of the lemma in the text.
     *
     * @return The lemma rank.
     */
    public float getRank() {
        return offsetList.size();
    }
}
