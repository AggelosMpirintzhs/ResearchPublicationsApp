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
     total_publications AS (
         SELECT
             sp.publisher_id,
             COUNT(DISTINCT ja.article_id) AS total_publications
         FROM selected_publisher sp
                  LEFT JOIN journals j
                            ON j.publisher_id = sp.publisher_id
                  LEFT JOIN journal_articles ja
                            ON ja.journal_id = j.journal_id
         GROUP BY
             sp.publisher_id
     ),
     quartile_publications AS (
         SELECT
             sp.publisher_id,
             jr.best_quartile AS quartile,
             COUNT(DISTINCT ja.article_id) AS publication_count
         FROM selected_publisher sp
                  JOIN journals j
                       ON j.publisher_id = sp.publisher_id
                  JOIN journal_articles ja
                       ON ja.journal_id = j.journal_id
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
    COALESCE(qp.publication_count, 0) AS publication_count,
    COALESCE(tp.total_publications, 0) AS total_publications
FROM selected_publisher sp
         CROSS JOIN quartiles q
         LEFT JOIN quartile_publications qp
                   ON qp.publisher_id = sp.publisher_id
                       AND qp.quartile = q.quartile
         LEFT JOIN total_publications tp
                   ON tp.publisher_id = sp.publisher_id
ORDER BY
    FIELD(q.quartile, 'Q1', 'Q2', 'Q3', 'Q4');