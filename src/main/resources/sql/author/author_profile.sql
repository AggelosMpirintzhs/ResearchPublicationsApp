-- C1: Author profile
-- Parameters:
--   1: author_id
--   2: start_year
--   3: end_year

SELECT
    au.author_id,
    au.author_name,

    MIN(pr.year) AS first_year,
    MAX(pr.year) AS last_year,
    COUNT(DISTINCT pr.year) AS active_years,

    COUNT(DISTINCT pr.article_id) AS total_articles,
    COUNT(DISTINCT CASE WHEN pr.journal_id IS NOT NULL THEN pr.article_id END) AS total_journal_articles,
    COUNT(DISTINCT CASE WHEN pr.conference_id IS NOT NULL THEN pr.article_id END) AS total_conference_articles,

    COUNT(DISTINCT pr.journal_id) AS distinct_journals,
    COUNT(DISTINCT pr.conference_id) AS distinct_conferences,

    COUNT(DISTINCT pr.article_id) / NULLIF(COUNT(DISTINCT pr.year), 0) AS avg_articles_per_year
FROM authors au
         JOIN article_authors aa
              ON au.author_id = aa.author_id
         JOIN vw_publication_report pr
              ON aa.article_id = pr.article_id
WHERE au.author_id = ?
  AND pr.year BETWEEN ? AND ?
GROUP BY
    au.author_id,
    au.author_name;