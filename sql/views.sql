-- =====================================================
-- A. CONFERENCE / JOURNAL PROFILE
-- =====================================================

-- A1. JOURNAL YEARLY STATISTICS FOR LINECHARTS

CREATE OR REPLACE VIEW vw_journal_year_stats AS
SELECT
    ja.journal_id,
    a.year,
    COUNT(DISTINCT a.article_id) AS total_articles,
    COUNT(aa.author_id) AS total_author_occurrences,
    COUNT(DISTINCT aa.author_id) AS distinct_authors
FROM journal_articles ja
    JOIN articles a
        ON ja.article_id = a.article_id
    LEFT JOIN article_authors aa
        ON a.article_id = aa.article_id
GROUP BY ja.journal_id, a.year;

-- A2. CONFERENCE YEARLY STATISTICS FOR LINECHARTS

CREATE OR REPLACE VIEW vw_conference_year_stats AS
SELECT
    ca.conference_id,
    a.year,
    COUNT(DISTINCT a.article_id) AS total_articles,
    COUNT(aa.author_id) AS total_author_occurrences,
    COUNT(DISTINCT aa.author_id) AS distinct_authors
FROM conference_articles ca
    JOIN articles a
        ON ca.article_id = a.article_id
    LEFT JOIN article_authors aa
        ON a.article_id = aa.article_id
GROUP BY ca.conference_id, a.year;