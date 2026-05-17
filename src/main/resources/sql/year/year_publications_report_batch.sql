-- Year publications batch loading
-- Optimized version: first selects only batch article_ids,
-- then loads full information only for these articles.
--
-- Parameters:
--   1: year
--   2: publication_type
--      Allowed values:
--        'ALL'
--        'JOURNAL'
--        'CONFERENCE'
--   3: journal_id       default 0 = no specific journal filter
--   4: conference_id    default 0 = no specific conference filter
--   5: author_id        default 0 = no specific author filter
--   6: last_article_id  default 0 = first batch
--   7: batch_size

WITH params AS (
    SELECT
        ? AS selected_year,
        ? AS selected_publication_type,
        ? AS selected_journal_id,
        ? AS selected_conference_id,
        ? AS selected_author_id,
        ? AS last_article_id
),
     batch_articles AS (
         SELECT
             a.article_id
         FROM articles a
                  LEFT JOIN journal_articles ja
                            ON a.article_id = ja.article_id
                  LEFT JOIN conference_articles ca
                            ON a.article_id = ca.article_id
                  CROSS JOIN params p
         WHERE a.year = p.selected_year
           AND a.article_id > p.last_article_id

           AND (
             p.selected_publication_type = 'ALL'
                 OR (
                 p.selected_publication_type = 'JOURNAL'
                     AND ja.article_id IS NOT NULL
                 )
                 OR (
                 p.selected_publication_type = 'CONFERENCE'
                     AND ca.article_id IS NOT NULL
                 )
             )

           AND (
             p.selected_journal_id = 0
                 OR ja.journal_id = p.selected_journal_id
             )

           AND (
             p.selected_conference_id = 0
                 OR ca.conference_id = p.selected_conference_id
             )

           AND (
             p.selected_author_id = 0
                 OR EXISTS (
                 SELECT 1
                 FROM article_authors aa_filter
                 WHERE aa_filter.article_id = a.article_id
                   AND aa_filter.author_id = p.selected_author_id
             )
             )
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

    ja.journal_id,
    j.journal_name,
    ja.source_id,
    ja.volume,
    ja.number,

    ca.conference_id,
    c.acronym AS conference_acronym,
    c.title AS conference_title,
    c.icore_id,

    a.pages,
    a.ee,
    a.url,
    a.mdate,

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