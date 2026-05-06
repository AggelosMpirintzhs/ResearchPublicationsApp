-- B2, B3, B4, B5:
-- Year publications table with optional filters
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

WITH params AS (
    SELECT
        ? AS selected_year,
        ? AS selected_publication_type,
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
    p.selected_publication_type = 'ALL'
        OR (
        p.selected_publication_type = 'JOURNAL'
            AND pr.journal_id IS NOT NULL
        )
        OR (
        p.selected_publication_type = 'CONFERENCE'
            AND pr.conference_id IS NOT NULL
        )
    )

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
        FROM article_authors aa
        WHERE aa.article_id = pr.article_id
          AND aa.author_id = p.selected_author_id
    )
    )
ORDER BY
    pr.article_type,
    pr.title;