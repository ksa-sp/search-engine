package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.persistence.*;

import java.util.Date;
import java.util.Set;

/**
 * Site table entity class.
 */
@Getter
@Setter
@Entity
@Table(indexes = {
        @javax.persistence.Index(columnList = "status")
        , @javax.persistence.Index(columnList = "url")
})
public class Site {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = IndexingStatus.SQL_ENUM, nullable = false)
    private IndexingStatus status;

    @Column(nullable = false)
    private Date statusTime;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String name;

    @OneToMany(cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JoinColumn(name = "site_id", foreignKey = @ForeignKey(name = "site_to_page"))
    private Set<Page> pages;

    @OneToMany(cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JoinColumn(name = "site_id", foreignKey = @ForeignKey(name = "site_to_lemma"))
    private Set<Lemma> lemmas;

    // Static methods

    @Setter
    private static JdbcTemplate jdbcTemplate = null;

    /**
     * Removes site records with id provided.
     *
     * @param siteId Site id or null.
     */
    public static void delete(Integer siteId) {
        String sql = "DELETE FROM `site` WHERE id ";

        if (siteId == null) {
            sql += "IS NULL";
        } else {
            sql += " = " + siteId;
        }

        jdbcTemplate.execute(sql);
    }
}
