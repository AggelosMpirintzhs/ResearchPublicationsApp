package query;

import java.util.List;

public record SqlQuery(
        String sql,
        List<Object> parameters
) {
}