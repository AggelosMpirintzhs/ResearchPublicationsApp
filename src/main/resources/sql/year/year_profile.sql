-- B1: Year profile
-- Parameters:
--   1: year

SELECT
    year,
    total_articles,
    total_journal_articles,
    total_conference_articles,
    distinct_journals,
    distinct_conferences,
    total_author_occurrences,
    distinct_authors,
    avg_authors_per_article
FROM vw_year_profile_stats
WHERE year = ?;-- B2, B3, B4, B5: Year publications report with optional filters
-- Parameters:
--   1: year
--   2: journal_id       default 0 = no journal filter
--   3: conference_id    default 0 = no conference filter
--   4: author_id        default 0 = no author filter

WITH params AS (
    SELECT
        ? AS selected_year,
        ? AS selected_journal_id,
        ? AS selected_conference_id,
        ? AS selected_author_id
)
SELECT
    pr.article_id,
    pr.articlekey,
    pr.title,
    pr.year,
    pr.article_type,

    pr.journal_id,
    pr.journal_name,
    pr.source_id,
    pr.volume,
    pr.number,

    pr.conference_id,
    pr.conference_acronym,
    pr.conference_title,
    pr.icore_id,

    pr.pages,
    pr.ee,
    pr.url,
    pr.mdate,

    pr.author_count,
    pr.authors
FROM vw_publication_report pr
         CROSS JOIN params p
WHERE pr.year = p.selected_year

  AND (
    p.selected_journal_id = 0
        OR pr.journal_id = p.selected_journal_id
    )

  AND (
    p.selected_conference_id = 0
        OR pr.conference_id = p.selected_conference_id
    )

  AND (
    p.selected_author_id = 0
        OR EXISTS (
        SELECT 1
        FROM article_authors aa_filter
        WHERE aa_filter.article_id = pr.article_id
          AND aa_filter.author_id = p.selected_author_id
    )
    )
ORDER BY
    pr.article_type,
    pr.title;