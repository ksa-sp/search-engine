package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.persistence.*;

import java.util.Arrays;

/**
 * Index table entity class.
 */
@Getter
@Setter
@Entity
public class Index {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "page_id")
    private Integer pageId;

    @Column(name = "lemma_id")
    private Integer lemmaId;

    @Column(nullable = false)
    private Float rank = 0f;

    // Static methods

    @Setter
    private static JdbcTemplate jdbcTemplate = null;

    /**
     * Removes all index records of a page.
     *
     * @param pageId Site id.
     */
    public static void delete(Integer pageId) {
        String sql = "DELETE FROM `index` WHERE page_id ";

        if (pageId == null) {
            sql += "IS NULL";
        } else {
            sql += " = " + pageId;
        }

        jdbcTemplate.execute(sql);
    }

    /**
     * Removes all index records with no parent page or lemma records.
     */
    public static void clean() {
        jdbcTemplate.execute("DELETE FROM `index` WHERE page_id IS NULL OR lemma_id IS NULL");
    }

    /**
     * Whether database provide cascade deleting index records on delete parent page or lemma record.
     *
     * @return true if database removes child records,<br>false if database restricts the deletion.
     */
    private static boolean isOnDeleteCascade() {
        String response = jdbcTemplate.queryForList("show create table `index`").toString();
        return response.replaceAll("\\s", " ")
                .matches(".+ FOREIGN KEY .+ ON DELETE CASCADE.+ FOREIGN KEY .+ ON DELETE CASCADE.+");
    }

    /**
     * Rebuilds foreign indexes to activate cascade delete index records.
     */
    public static void setOnDeleteCascade() {
        if (!isOnDeleteCascade()) {
            String[] sql = {
                    "DROP CONSTRAINT lemma_to_index"
                    , "ADD CONSTRAINT lemma_to_index FOREIGN KEY (`lemma_id`) REFERENCES `lemma` (`id`) ON DELETE CASCADE"
                    , "DROP CONSTRAINT page_to_index"
                    , "ADD CONSTRAINT page_to_index FOREIGN KEY (`page_id`) REFERENCES `page` (`id`) ON DELETE CASCADE"
            };
            Arrays.stream(sql).forEach(s -> jdbcTemplate.execute("ALTER TABLE `index` " + s));
        }
    }
}
