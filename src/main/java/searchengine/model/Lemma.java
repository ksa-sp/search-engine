package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.persistence.*;

import java.util.Arrays;
import java.util.Set;

/**
 * Lemma table entity class.
 */
@Getter
@Setter
@Entity
@Table(indexes = {
        @javax.persistence.Index(columnList = "lemma")
        , @javax.persistence.Index(columnList = "frequency")
})
public class Lemma {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "site_id")
    private Integer siteId;

    @Column(nullable = false)
    private String lemma;

    @Column(nullable = false)
    private Integer frequency = 0;

    @OneToMany(cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JoinColumn(name = "lemma_id", foreignKey = @ForeignKey(name = "lemma_to_index"))
    private Set<Index> indexes;

    // Static methods

    @Setter
    private static JdbcTemplate jdbcTemplate = null;

    /**
     * Removes all lemma records of a site.
     *
     * @param siteId Site id.
     */
    public static void delete(Integer siteId) {
        String sql = "DELETE FROM `lemma` WHERE site_id ";

        if (siteId == null) {
            sql += "IS NULL";
        } else {
            sql += " = " + siteId;
        }

        jdbcTemplate.execute(sql);
    }

    /**
     * Whether database provide cascade deleting lemma records on delete parent site record.
     *
     * @return true if database removes child records,<br>false if database restricts the deletion.
     */
    private static boolean isOnDeleteCascade() {
        String response = jdbcTemplate.queryForList("show create table `lemma`").toString();
        return response.replaceAll("\\s", " ")
                .matches(".+ FOREIGN KEY .+ ON DELETE CASCADE.+");
    }

    /**
     * Rebuilds foreign indexes to activate cascade delete lemma records.
     */
    public static void setOnDeleteCascade() {
        if (!isOnDeleteCascade()) {
            String[] sql = {
                    "DROP CONSTRAINT site_to_lemma"
                    , "ADD CONSTRAINT site_to_lemma FOREIGN KEY (`site_id`) REFERENCES `site` (`id`) ON DELETE CASCADE"
            };
            Arrays.stream(sql).forEach(s -> jdbcTemplate.execute("ALTER TABLE `lemma` " + s));
        }
    }
}
