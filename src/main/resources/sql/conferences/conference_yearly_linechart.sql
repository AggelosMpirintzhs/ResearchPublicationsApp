-- A4: Conference yearly linechart
-- Parameters:
--   1: conference_id
--   2: start_year
--   3: end_year

SELECT
    conference_id,
    acronym,
    conference_title,
    year,
    total_articles,
    total_author_occurrences,
    distinct_authors,
    avg_authors_per_article
FROM vw_conference_year_stats
WHERE conference_id = ?
  AND year BETWEEN ? AND ?
ORDER BY year;