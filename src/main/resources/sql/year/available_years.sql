-- Lightweight available years query
-- Επιστρέφει μόνο τις χρονιές που έχουν έστω ένα article.
-- Δεν κάνει counts, joins ή aggregates.

SELECT DISTINCT
    a.year AS year,

    -- Τα κρατάμε για να ταιριάζει με το AvailableYearDto / YearRepository mapping.
    NULL AS total_articles,
    NULL AS total_journal_articles,
    NULL AS total_conference_articles
FROM articles a
WHERE a.year IS NOT NULL
  AND a.year BETWEEN 1900 AND 2100
ORDER BY a.year DESC;