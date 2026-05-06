package query;

public record Pagination(
        int limit,
        int offset
) {

    public static Pagination defaultPagination() {
        return new Pagination(100, 0);
    }

    public Pagination {
        if (limit <= 0) {
            limit = 100;
        }

        if (limit > 500) {
            limit = 500;
        }

        if (offset < 0) {
            offset = 0;
        }
    }
}