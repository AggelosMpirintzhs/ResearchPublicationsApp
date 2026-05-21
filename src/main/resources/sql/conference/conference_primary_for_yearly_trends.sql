SELECT
    p.primaryFoR_name AS category,
    a.year AS year,
    COUNT(DISTINCT c.conference_id) AS count
FROM conference_rankings cr
    JOIN primaryFoR_categories p
ON p.primaryFoR_id = cr.primaryFoR_id
    JOIN conferences c
    ON c.conference_id = cr.conference_id
    JOIN conference_articles ca
    ON ca.conference_id = c.conference_id
    JOIN articles a
    ON a.article_id = ca.article_id
WHERE
    (? = '' OR CAST(cr.primaryFoR_id AS CHAR) = ?)
  AND a.year BETWEEN ? AND ?
GROUP BY
    p.primaryFoR_name,
    a.year
ORDER BY
    a.year ASC,
    count DESC,
    p.primaryFoR_name ASC;