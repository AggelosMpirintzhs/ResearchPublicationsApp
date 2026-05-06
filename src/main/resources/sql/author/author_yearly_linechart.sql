-- C2: Author yearly linechart
-- Parameters:
--   1: author_id
--   2: start_year
--   3: end_year

SELECT
    year,
    total_articles
FROM vw_author_year_stats
WHERE author_id = ?
  AND year BETWEEN ? AND ?
ORDER BY year;