-- A15, A16: Conference articles report
-- Parameters:
--   1: conference_id
--   2: start_year
--   3: end_year

SELECT
    article_id,
    articlekey,
    title,
    year,
    article_type,

    conference_id,
    conference_acronym,
    conference_title,
    icore_id,

    pages,
    ee,
    url,
    mdate,

    author_count,
    authors
FROM vw_publication_report
WHERE conference_id = ?
  AND year BETWEEN ? AND ?
ORDER BY
    year,
    title;