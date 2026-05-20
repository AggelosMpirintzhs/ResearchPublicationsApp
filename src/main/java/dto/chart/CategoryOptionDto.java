package dto.chart;

public record CategoryOptionDto(
        String id,
        String name
) {
    public String displayText() {
        if (id == null || id.isBlank()) {
            return name;
        }

        return id + " - " + name;
    }

    @Override
    public String toString() {
        return displayText();
    }
}