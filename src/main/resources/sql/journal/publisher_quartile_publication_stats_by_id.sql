-- Publisher quartile JOURNAL stats by publisher id
-- Parameters:
--   1: publisher_id
--
-- NOTE:
-- The aliases remain publication_count and total_publications
-- so that the existing Java code does not break.
-- Semantically, they now represent journal counts.

WITH selected_publisher AS (
    SELECT
        p.publisher_id,
        p.publisher_name
    FROM publishers p
    WHERE p.publisher_id = ?
),
     quartiles AS (
         SELECT 'Q1' AS quartile
         UNION ALL SELECT 'Q2'
         UNION ALL SELECT 'Q3'
         UNION ALL SELECT 'Q4'
     ),
     total_journals AS (
         SELECT
             sp.publisher_id,
             COUNT(DISTINCT j.journal_id) AS total_journals
         FROM selected_publisher sp
                  LEFT JOIN journals j
                            ON j.publisher_id = sp.publisher_id
         GROUP BY sp.publisher_id
     ),
     quartile_journals AS (
         SELECT
             sp.publisher_id,
             jr.best_quartile AS quartile,
             COUNT(DISTINCT j.journal_id) AS journal_count
         FROM selected_publisher sp
                  JOIN journals j
                       ON j.publisher_id = sp.publisher_id
                  JOIN journal_rankings jr
                       ON jr.journal_id = j.journal_id
         WHERE jr.best_quartile IN ('Q1', 'Q2', 'Q3', 'Q4')
         GROUP BY
             sp.publisher_id,
             jr.best_quartile
     )
SELECT
    sp.publisher_id,
    sp.publisher_name AS publisher_name,
    q.quartile,

    COALESCE(qj.journal_count, 0) AS publication_count,
    COALESCE(tj.total_journals, 0) AS total_publications

FROM selected_publisher sp
         CROSS JOIN quartiles q
         LEFT JOIN quartile_journals qj
                   ON qj.publisher_id = sp.publisher_id
                       AND qj.quartile = q.quartile
         LEFT JOIN total_journals tj
                   ON tj.publisher_id = sp.publisher_id
ORDER BY
    FIELD(q.quartile, 'Q1', 'Q2', 'Q3', 'Q4');