-- Author publications report - batch loading
--
-- Parameters:
--   1: author_id
--   2: start_year
--   3: end_year
--   4: last_article_id
--   5: batch_size

WITH batch_articles AS (
    SELECT
        a.article_id
    FROM article_authors aa
             JOIN articles a
                  ON aa.article_id = a.article_id
    WHERE aa.author_id = ?
      AND a.year BETWEEN ? AND ?
      AND a.article_id > ?
    ORDER BY a.article_id
    LIMIT ?
    ),
    batch_authors AS (
SELECT
    aa.article_id,
    COUNT(DISTINCT au.author_id) AS author_count,
    GROUP_CONCAT(DISTINCT au.author_name ORDER BY au.author_name SEPARATOR ', ') AS authors
FROM batch_articles ba
    JOIN article_authors aa
ON ba.article_id = aa.article_id
    JOIN authors au
    ON aa.author_id = au.author_id
GROUP BY aa.article_id
    )
SELECT
    a.article_id,
    a.articlekey,
    a.title,
    a.year,
    at.type_name AS article_type,

    j.journal_id,
    j.journal_name,
    ja.volume,
    ja.number,

    c.conference_id,
    c.acronym AS conference_acronym,
    c.title AS conference_title,

    a.pages,
    a.ee,
    a.url,

    COALESCE(bauth.author_count, 0) AS author_count,
    COALESCE(bauth.authors, '') AS authors
FROM batch_articles ba
         JOIN articles a
              ON ba.article_id = a.article_id
         JOIN article_types at
ON a.type_id = at.type_id
    LEFT JOIN journal_articles ja
    ON a.article_id = ja.article_id
    LEFT JOIN journals j
    ON ja.journal_id = j.journal_id
    LEFT JOIN conference_articles ca
    ON a.article_id = ca.article_id
    LEFT JOIN conferences c
    ON ca.conference_id = c.conference_id
    LEFT JOIN batch_authors bauth
    ON a.article_id = bauth.article_id
ORDER BY
    a.article_id;