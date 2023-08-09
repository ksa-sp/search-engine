package dto;

import lombok.Getter;
import lombok.Setter;
import searchengine.dto.search.SearchDataItem;

import javax.persistence.Entity;

@Entity
@Getter
@Setter
public class SearchResponseTest {
    private Boolean result;
    private String error;
    private Integer count;
    private SearchDataItem[] data;
}
