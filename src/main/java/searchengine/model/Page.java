package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.persistence.*;

import java.util.Arrays;
import java.util.Set;

/**
 * Page table entity class.
 */
@Getter
@Setter
@Entity
@Table(indexes = @javax.persistence.Index(columnList = "code"))
public class Page {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "site_id")
    private Integer siteId;

    @Column(columnDefinition = "TEXT NOT NULL, FULLTEXT KEY (path)")
    private String path;

    @Column(nullable = false)
    private Integer code = FATAL_ERROR_CODE;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;         // Must have no default value

    @OneToMany(cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JoinColumn(name = "page_id", foreignKey = @ForeignKey(name = "page_to_index"))
    private Set<Index> indexes;

    // Static methods

    @Setter
    private static JdbcTemplate jdbcTemplate = null;

    /**
     * Unexpected error while page downloading.
     * <br>
     * Page code constant.
     */
    public static final int FATAL_ERROR_CODE = 0;

    /**
     * Not indexed site objects, stored in database to prevent repeated downloading.
     * <br>
     * Page code constant.
     */
    public static final int NOT_A_PAGE_CODE = 1;

    /**
     * Removes all page records of a site.
     *
     * @param siteId Site id.
     */
    public static void delete(Integer siteId) {
        String sql = "DELETE FROM `page` WHERE site_id ";

        if (siteId == null) {
            sql += "IS NULL";
        } else {
            sql += " = " + siteId;
        }

        jdbcTemplate.execute(sql);
    }

    /**
     * Whether database provide cascade deleting page records on delete parent site record.
     *
     * @return true if database removes child records,<br>false if database restricts the deletion.
     */
    private static boolean isOnDeleteCascade() {
        String response = jdbcTemplate.queryForList("show create table `page`").toString();
        return response.replaceAll("\\s", " ")
                .matches(".+ FOREIGN KEY .+ ON DELETE CASCADE.+");
    }

    /**
     * Rebuilds foreign indexes to activate cascade delete page records.
     */
    public static void setOnDeleteCascade() {
        if (!isOnDeleteCascade()) {
            String[] sql = {
                    "DROP CONSTRAINT site_to_page"
                    , "ADD CONSTRAINT site_to_page FOREIGN KEY (`site_id`) REFERENCES `site` (`id`) ON DELETE CASCADE"
            };
            Arrays.stream(sql).forEach(s -> jdbcTemplate.execute("ALTER TABLE `page` " + s));
        }
    }
}
