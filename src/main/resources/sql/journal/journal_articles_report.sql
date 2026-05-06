-- A13, A14: Journal articles report
-- Parameters:
--   1: journal_id
--   2: start_year
--   3: end_year

SELECT
    article_id,
    articlekey,
    title,
    year,
    article_type,

    journal_id,
    journal_name,
    source_id,
    volume,
    number,

    pages,
    ee,
    url,
    mdate,

    author_count,
    authors
FROM vw_publication_report
WHERE journal_id = ?
  AND year BETWEEN ? AND ?
ORDER BY
    year,
    title;