-- Author publications report
-- Parameters:
--   1: author_id
--   2: start_year
--   3: end_year

SELECT
    pr.article_id,
    pr.articlekey,
    pr.title,
    pr.year,
    pr.article_type,

    pr.journal_id,
    pr.journal_name,
    pr.volume,
    pr.number,

    pr.conference_id,
    pr.conference_acronym,
    pr.conference_title,

    pr.pages,
    pr.ee,
    pr.url,
    pr.author_count,
    pr.authors
FROM vw_publication_report pr
         JOIN article_authors aa
              ON pr.article_id = aa.article_id
WHERE aa.author_id = ?
  AND pr.year BETWEEN ? AND ?
ORDER BY
    pr.year,
    pr.article_type,
    pr.title;