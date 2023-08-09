package dto;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

@Entity
@Getter
@Setter
public class IndexingResponseTest {
    private boolean result;
    private String error;
}
