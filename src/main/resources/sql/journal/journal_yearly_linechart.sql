-- A3: Journal yearly linechart
-- Parameters:
--   1: journal_id
--   2: start_year
--   3: end_year

SELECT
    journal_id,
    journal_name,
    year,
    total_articles,
    total_author_occurrences,
    distinct_authors,
    avg_authors_per_article
FROM vw_journal_year_stats
WHERE journal_id = ?
  AND year BETWEEN ? AND ?
ORDER BY year;