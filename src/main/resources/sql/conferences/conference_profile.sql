-- A10, A11, A12, A18: Conference profile statistics
-- Parameters:
--   1: conference_id
--   2: start_year
--   3: end_year

SELECT
    c.conference_id,
    c.acronym,
    c.title AS conference_title,
    c.icore_id,

    MIN(a.year) AS first_year,
    MAX(a.year) AS last_year,
    COUNT(DISTINCT a.year) AS active_years,

    COUNT(DISTINCT a.article_id) AS total_articles,
    COUNT(aa.author_id) AS total_author_occurrences,
    COUNT(DISTINCT aa.author_id) AS distinct_authors,

    COUNT(DISTINCT a.article_id) / NULLIF(COUNT(DISTINCT a.year), 0) AS avg_articles_per_year,
    COUNT(aa.author_id) / NULLIF(COUNT(DISTINCT a.year), 0) AS avg_author_occurrences_per_year,
    COUNT(aa.author_id) / NULLIF(COUNT(DISTINCT a.article_id), 0) AS avg_authors_per_article
FROM conferences c
         JOIN conference_articles ca
              ON c.conference_id = ca.conference_id
         JOIN articles a
              ON ca.article_id = a.article_id
         LEFT JOIN article_authors aa
                   ON a.article_id = aa.article_id
WHERE c.conference_id = ?
  AND a.year BETWEEN ? AND ?
GROUP BY
    c.conference_id,
    c.acronym,
    c.title,
    c.icore_id;