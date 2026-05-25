-- =====================================================
-- views.sql
-- Project: Bibliographic Data Integration
-- Database: research_publications_db
-- MySQL 8.0+
--
-- Περιέχει τα βασικά views που απλοποιούν τα queries
-- για profiles, yearly charts, reports, authors, rankings,
-- bar charts και scatter plots.
-- =====================================================

USE research_publications_db;

-- =====================================================
-- 1. ARTICLE AUTHORS AGGREGATION
--    Ένα row ανά άρθρο με πλήθος και ονόματα συγγραφέων.
--    Χρήσιμο για reports χωρίς να επαναλαμβάνεται GROUP_CONCAT.
-- =====================================================

CREATE OR REPLACE VIEW vw_article_authors_agg AS
SELECT
    a.article_id,
    COUNT(DISTINCT au.author_id) AS author_count,
    GROUP_CONCAT(DISTINCT au.author_id ORDER BY au.author_id SEPARATOR ',') AS author_ids,
    GROUP_CONCAT(DISTINCT au.author_name ORDER BY au.author_name SEPARATOR ', ') AS authors
FROM articles a
         LEFT JOIN article_authors aa
                   ON a.article_id = aa.article_id
         LEFT JOIN authors au
                   ON aa.author_id = au.author_id
GROUP BY a.article_id;

-- =====================================================
-- 2. FULL PUBLICATION REPORT
--    Ενιαίο view για όλα τα άρθρα, είτε είναι journal είτε conference.
--    Καλύπτει reports άρθρων ανά έτος, περιοδικό, συνέδριο, συγγραφέα.
-- =====================================================

CREATE OR REPLACE VIEW vw_publication_report AS
SELECT
    a.article_id,
    a.articlekey,
    a.title,
    a.year,
    a.pages,
    a.ee,
    a.url,
    a.mdate,

    at.type_id,
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

    aaa.author_count,
    aaa.author_ids,
    aaa.authors
FROM articles a
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
    LEFT JOIN vw_article_authors_agg aaa
    ON a.article_id = aaa.article_id;

-- =====================================================
-- 3. JOURNAL YEARLY STATISTICS
--    Βασικό view για linecharts περιοδικών και yearly aggregations.
-- =====================================================

CREATE OR REPLACE VIEW vw_journal_year_stats AS
SELECT
    j.journal_id,
    j.journal_name,
    j.publisher_id,
    p.publisher_name,
    a.year,

    COUNT(DISTINCT a.article_id) AS total_articles,
    COUNT(aa.author_id) AS total_author_occurrences,
    COUNT(DISTINCT aa.author_id) AS distinct_authors,
    COUNT(aa.author_id) / NULLIF(COUNT(DISTINCT a.article_id), 0) AS avg_authors_per_article
FROM journals j
         LEFT JOIN publishers p
                   ON j.publisher_id = p.publisher_id
         JOIN journal_articles ja
              ON j.journal_id = ja.journal_id
         JOIN articles a
              ON ja.article_id = a.article_id
         LEFT JOIN article_authors aa
                   ON a.article_id = aa.article_id
GROUP BY
    j.journal_id,
    j.journal_name,
    j.publisher_id,
    p.publisher_name,
    a.year;

-- =====================================================
-- 4. CONFERENCE YEARLY STATISTICS
--    Βασικό view για linecharts συνεδρίων και yearly aggregations.
-- =====================================================

CREATE OR REPLACE VIEW vw_conference_year_stats AS
SELECT
    c.conference_id,
    c.acronym,
    c.title AS conference_title,
    c.icore_id,
    a.year,

    COUNT(DISTINCT a.article_id) AS total_articles,
    COUNT(aa.author_id) AS total_author_occurrences,
    COUNT(DISTINCT aa.author_id) AS distinct_authors,
    COUNT(aa.author_id) / NULLIF(COUNT(DISTINCT a.article_id), 0) AS avg_authors_per_article
FROM conferences c
         JOIN conference_articles ca
              ON c.conference_id = ca.conference_id
         JOIN articles a
              ON ca.article_id = a.article_id
         LEFT JOIN article_authors aa
                   ON a.article_id = aa.article_id
GROUP BY
    c.conference_id,
    c.acronym,
    c.title,
    c.icore_id,
    a.year;

-- =====================================================
-- 5. JOURNAL PROFILE STATISTICS
--    Ένα row ανά περιοδικό με all-time profile στοιχεία.
-- =====================================================

CREATE OR REPLACE VIEW vw_journal_profile_stats AS
SELECT
    j.journal_id,
    j.journal_name,
    j.publisher_id,
    p.publisher_name,

    MIN(a.year) AS first_year,
    MAX(a.year) AS last_year,
    COUNT(DISTINCT a.year) AS active_years,

    COUNT(DISTINCT a.article_id) AS total_articles,
    COUNT(aa.author_id) AS total_author_occurrences,
    COUNT(DISTINCT aa.author_id) AS distinct_authors_all_time,

    COUNT(DISTINCT a.article_id) / NULLIF(COUNT(DISTINCT a.year), 0) AS avg_articles_per_year,
    COUNT(aa.author_id) / NULLIF(COUNT(DISTINCT a.year), 0) AS avg_author_occurrences_per_year,
    COUNT(aa.author_id) / NULLIF(COUNT(DISTINCT a.article_id), 0) AS avg_authors_per_article
FROM journals j
         LEFT JOIN publishers p
                   ON j.publisher_id = p.publisher_id
         LEFT JOIN journal_articles ja
                   ON j.journal_id = ja.journal_id
         LEFT JOIN articles a
                   ON ja.article_id = a.article_id
         LEFT JOIN article_authors aa
                   ON a.article_id = aa.article_id
GROUP BY
    j.journal_id,
    j.journal_name,
    j.publisher_id,
    p.publisher_name;

-- =====================================================
-- 6. CONFERENCE PROFILE STATISTICS
--    Ένα row ανά συνέδριο με all-time profile στοιχεία.
-- =====================================================

CREATE OR REPLACE VIEW vw_conference_profile_stats AS
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
    COUNT(DISTINCT aa.author_id) AS distinct_authors_all_time,

    COUNT(DISTINCT a.article_id) / NULLIF(COUNT(DISTINCT a.year), 0) AS avg_articles_per_year,
    COUNT(aa.author_id) / NULLIF(COUNT(DISTINCT a.year), 0) AS avg_author_occurrences_per_year,
    COUNT(aa.author_id) / NULLIF(COUNT(DISTINCT a.article_id), 0) AS avg_authors_per_article
FROM conferences c
         LEFT JOIN conference_articles ca
                   ON c.conference_id = ca.conference_id
         LEFT JOIN articles a
                   ON ca.article_id = a.article_id
         LEFT JOIN article_authors aa
                   ON a.article_id = aa.article_id
GROUP BY
    c.conference_id,
    c.acronym,
    c.title,
    c.icore_id;

-- =====================================================
-- 7. YEAR PROFILE STATISTICS
--    Ένα row ανά χρονιά. Καλύπτει το προφίλ χρονιάς.
-- =====================================================

CREATE OR REPLACE VIEW vw_year_profile_stats AS
SELECT
    a.year,

    COUNT(DISTINCT a.article_id) AS total_articles,
    COUNT(DISTINCT ja.article_id) AS total_journal_articles,
    COUNT(DISTINCT ca.article_id) AS total_conference_articles,

    COUNT(DISTINCT ja.journal_id) AS distinct_journals,
    COUNT(DISTINCT ca.conference_id) AS distinct_conferences,

    COUNT(aa.author_id) AS total_author_occurrences,
    COUNT(DISTINCT aa.author_id) AS distinct_authors,
    COUNT(aa.author_id) / NULLIF(COUNT(DISTINCT a.article_id), 0) AS avg_authors_per_article
FROM articles a
         LEFT JOIN journal_articles ja
                   ON a.article_id = ja.article_id
         LEFT JOIN conference_articles ca
                   ON a.article_id = ca.article_id
         LEFT JOIN article_authors aa
                   ON a.article_id = aa.article_id
GROUP BY a.year;

-- =====================================================
-- 8. AUTHOR PROFILE STATISTICS
--    Ένα row ανά συγγραφέα με profile στοιχεία.
-- =====================================================

CREATE OR REPLACE VIEW vw_author_profile_stats AS
SELECT
    au.author_id,
    au.author_name,

    MIN(a.year) AS first_year,
    MAX(a.year) AS last_year,
    COUNT(DISTINCT a.year) AS active_years,

    COUNT(DISTINCT a.article_id) AS total_articles,
    COUNT(DISTINCT ja.article_id) AS total_journal_articles,
    COUNT(DISTINCT ca.article_id) AS total_conference_articles,

    COUNT(DISTINCT ja.journal_id) AS distinct_journals,
    COUNT(DISTINCT ca.conference_id) AS distinct_conferences,

    COUNT(DISTINCT a.article_id) / NULLIF(COUNT(DISTINCT a.year), 0) AS avg_articles_per_year
FROM authors au
         JOIN article_authors aa
              ON au.author_id = aa.author_id
         JOIN articles a
              ON aa.article_id = a.article_id
         LEFT JOIN journal_articles ja
                   ON a.article_id = ja.article_id
         LEFT JOIN conference_articles ca
                   ON a.article_id = ca.article_id
GROUP BY
    au.author_id,
    au.author_name;

-- =====================================================
-- 9. AUTHOR YEARLY STATISTICS
--    Linechart συγγραφέα ανά χρονιά, με διάκριση journal/conference.
-- =====================================================

CREATE OR REPLACE VIEW vw_author_year_stats AS
SELECT
    au.author_id,
    au.author_name,
    a.year,

    COUNT(DISTINCT a.article_id) AS total_articles,
    COUNT(DISTINCT ja.article_id) AS total_journal_articles,
    COUNT(DISTINCT ca.article_id) AS total_conference_articles,

    COUNT(DISTINCT ja.journal_id) AS distinct_journals,
    COUNT(DISTINCT ca.conference_id) AS distinct_conferences
FROM authors au
         JOIN article_authors aa
              ON au.author_id = aa.author_id
         JOIN articles a
              ON aa.article_id = a.article_id
         LEFT JOIN journal_articles ja
                   ON a.article_id = ja.article_id
         LEFT JOIN conference_articles ca
                   ON a.article_id = ca.article_id
GROUP BY
    au.author_id,
    au.author_name,
    a.year;

-- =====================================================
-- 10. JOURNAL RANKING METRICS
--     Ένα view με όλα τα ranking/scatter metrics περιοδικών.
-- =====================================================

CREATE OR REPLACE VIEW vw_journal_ranking_metrics AS
SELECT
    j.journal_id,
    j.journal_name,
    j.publisher_id,
    p.publisher_name,

    jr.ranking_position,
    jr.best_quartile,
    jr.sjr_index,
    jr.cite_score,
    jr.h_index,
    jr.total_docs,
    jr.total_docs_3y,
    jr.total_refs,
    jr.total_cites_3y,
    jr.citable_docs_3y,
    jr.cites_per_doc_2y,
    jr.refs_per_doc,

    b.best_area_id,
    b.area_name AS best_subject_area
FROM journals j
         LEFT JOIN publishers p
                   ON j.publisher_id = p.publisher_id
         LEFT JOIN journal_rankings jr
                   ON j.journal_id = jr.journal_id
         LEFT JOIN best_subject_areas b
                   ON jr.best_area_id = b.best_area_id;

-- =====================================================
-- 11. CONFERENCE RANKING METRICS
--     Ένα view με ranking/PrimaryFoR στοιχεία συνεδρίων.
-- =====================================================

CREATE OR REPLACE VIEW vw_conference_ranking_metrics AS
SELECT
    c.conference_id,
    c.acronym,
    c.title AS conference_title,
    c.icore_id,

    cr.rank_label,
    cr.primaryFoR_id,
    p.primaryFoR_name
FROM conferences c
         LEFT JOIN conference_rankings cr
                   ON c.conference_id = cr.conference_id
         LEFT JOIN primaryFoR_categories p
                   ON cr.primaryFoR_id = p.primaryFoR_id;

-- =====================================================
-- 12. BEST SUBJECT AREA PER YEAR
--     Για linecharts BestSubjectArea περιοδικών ανά χρονιά.
-- =====================================================

CREATE OR REPLACE VIEW vw_journal_area_year_stats AS
SELECT
    b.best_area_id,
    b.area_name AS best_subject_area,
    a.year,

    COUNT(DISTINCT j.journal_id) AS distinct_journals,
    COUNT(DISTINCT a.article_id) AS total_articles,
    COUNT(aa.author_id) AS total_author_occurrences,
    COUNT(DISTINCT aa.author_id) AS distinct_authors
FROM best_subject_areas b
         JOIN journal_rankings jr
              ON b.best_area_id = jr.best_area_id
         JOIN journals j
              ON jr.journal_id = j.journal_id
         JOIN journal_articles ja
              ON j.journal_id = ja.journal_id
         JOIN articles a
              ON ja.article_id = a.article_id
         LEFT JOIN article_authors aa
                   ON a.article_id = aa.article_id
GROUP BY
    b.best_area_id,
    b.area_name,
    a.year;

-- =====================================================
-- 13. PRIMARY FOR CATEGORY PER YEAR
--     Για linecharts PrimaryFoR συνεδρίων ανά χρονιά.
-- =====================================================

CREATE OR REPLACE VIEW vw_conference_for_year_stats AS
SELECT
    p.primaryFoR_id,
    p.primaryFoR_name,
    a.year,

    COUNT(DISTINCT c.conference_id) AS distinct_conferences,
    COUNT(DISTINCT a.article_id) AS total_articles,
    COUNT(aa.author_id) AS total_author_occurrences,
    COUNT(DISTINCT aa.author_id) AS distinct_authors
FROM primaryFoR_categories p
         JOIN conference_rankings cr
              ON p.primaryFoR_id = cr.primaryFoR_id
         JOIN conferences c
              ON cr.conference_id = c.conference_id
         JOIN conference_articles ca
              ON c.conference_id = ca.conference_id
         JOIN articles a
              ON ca.article_id = a.article_id
         LEFT JOIN article_authors aa
                   ON a.article_id = aa.article_id
GROUP BY
    p.primaryFoR_id,
    p.primaryFoR_name,
    a.year;

-- =====================================================
-- 14. PUBLISHER JOURNAL COUNTS
--     Για bar charts publishers με πλήθος περιοδικών.
-- =====================================================

CREATE OR REPLACE VIEW vw_publisher_journal_counts AS
SELECT
    p.publisher_id,
    p.publisher_name,
    COUNT(DISTINCT j.journal_id) AS total_journals
FROM publishers p
         LEFT JOIN journals j
                   ON p.publisher_id = j.publisher_id
GROUP BY
    p.publisher_id,
    p.publisher_name;

-- =====================================================
-- 15. PUBLISHER QUARTILE COUNTS
--     Για stacked/grouped bar chart Q1-Q4 ανά publisher.
-- =====================================================

CREATE OR REPLACE VIEW vw_publisher_quartile_counts AS
SELECT
    p.publisher_id,
    p.publisher_name,

    COUNT(DISTINCT j.journal_id) AS total_journals,
    SUM(CASE WHEN jr.best_quartile = 'Q1' THEN 1 ELSE 0 END) AS q1_count,
    SUM(CASE WHEN jr.best_quartile = 'Q2' THEN 1 ELSE 0 END) AS q2_count,
    SUM(CASE WHEN jr.best_quartile = 'Q3' THEN 1 ELSE 0 END) AS q3_count,
    SUM(CASE WHEN jr.best_quartile = 'Q4' THEN 1 ELSE 0 END) AS q4_count,
    SUM(CASE WHEN jr.best_quartile IS NULL THEN 1 ELSE 0 END) AS unranked_count
FROM publishers p
         LEFT JOIN journals j
                   ON p.publisher_id = j.publisher_id
         LEFT JOIN journal_rankings jr
                   ON j.journal_id = jr.journal_id
GROUP BY
    p.publisher_id,
    p.publisher_name;

-- =====================================================
-- 16. JOURNAL YEARLY SCATTER METRICS
--     Ένα row ανά περιοδικό και χρονιά για scatter plots τύπου
--     articles/year vs avg authors/article.
-- =====================================================

CREATE OR REPLACE VIEW vw_journal_year_scatter_stats AS
SELECT
    journal_id,
    journal_name,
    publisher_id,
    publisher_name,
    year,
    total_articles AS articles_per_year,
    total_author_occurrences,
    distinct_authors,
    avg_authors_per_article
FROM vw_journal_year_stats
WHERE total_articles > 0;

-- =====================================================
-- 17. CONFERENCE YEARLY SCATTER METRICS
--     Ένα row ανά συνέδριο και χρονιά για scatter plots τύπου
--     articles/year vs avg authors/article.
-- =====================================================

CREATE OR REPLACE VIEW vw_conference_year_scatter_stats AS
SELECT
    conference_id,
    acronym,
    conference_title,
    icore_id,
    year,
    total_articles AS articles_per_year,
    total_author_occurrences,
    distinct_authors,
    avg_authors_per_article
FROM vw_conference_year_stats
WHERE total_articles > 0;
