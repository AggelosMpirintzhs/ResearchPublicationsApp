-- =========================================================
-- ARTICLES
-- Χρήσιμο για φίλτρα χρονιάς, year profile, linecharts,
-- reports με range years.
-- =========================================================

CREATE INDEX idx_articles_year
ON articles(year);

CREATE INDEX idx_articles_type_year
ON articles(type_id, year);

CREATE INDEX idx_articles_year_type
ON articles(year, type_id);


-- =========================================================
-- JOURNAL ARTICLES
-- Χρήσιμο για journal profile, journal yearly stats,
-- journal article reports.
-- =========================================================

CREATE INDEX idx_journal_articles_journal_id
ON journal_articles(journal_id);

CREATE INDEX idx_journal_articles_article_id
ON journal_articles(article_id);

CREATE INDEX idx_journal_articles_journal_article
ON journal_articles(journal_id, article_id);


-- =========================================================
-- CONFERENCE ARTICLES
-- Χρήσιμο για conference profile, conference yearly stats,
-- conference article reports.
-- =========================================================

CREATE INDEX idx_conference_articles_conference_id
ON conference_articles(conference_id);

CREATE INDEX idx_conference_articles_article_id
ON conference_articles(article_id);

CREATE INDEX idx_conference_articles_conference_article
ON conference_articles(conference_id, article_id);


-- =========================================================
-- ARTICLE AUTHORS
-- Χρήσιμο για author profile, author publications,
-- counts distinct authors, authors ανά article.
-- =========================================================

CREATE INDEX idx_article_authors_article_id
ON article_authors(article_id);

CREATE INDEX idx_article_authors_author_id
ON article_authors(author_id);

CREATE INDEX idx_article_authors_author_article
ON article_authors(author_id, article_id);

CREATE INDEX idx_article_authors_article_author
ON article_authors(article_id, author_id);

CREATE INDEX idx_articles_year_article_id
ON articles(year, article_id);

CREATE INDEX idx_articles_type_year_article_id
ON articles(type_id, year, article_id);


-- =========================================================
-- AUTHORS
-- Χρήσιμο για search authors.
-- Αν κάνεις LIKE 'text%' βοηθάει.
-- Αν κάνεις LIKE '%text%' δεν βοηθάει τόσο.
-- =========================================================

CREATE INDEX idx_authors_name
ON authors(author_name);


-- =========================================================
-- JOURNALS
-- Χρήσιμο για search journals και joins με publisher.
-- =========================================================

CREATE INDEX idx_journals_name
ON journals(journal_name);

CREATE INDEX idx_journals_publisher_id
ON journals(publisher_id);


-- =========================================================
-- CONFERENCES
-- Χρήσιμο για search conferences.
-- =========================================================

CREATE INDEX idx_conferences_acronym
ON conferences(acronym);

CREATE INDEX idx_conferences_title
ON conferences(conference_title);


-- =========================================================
-- JOURNAL RANKINGS
-- Χρήσιμο για ranking lookup ανά journal.
-- =========================================================

CREATE INDEX idx_journal_rankings_journal_id
ON journal_rankings(journal_id);

CREATE INDEX idx_journal_rankings_best_subject_area_id
ON journal_rankings(best_subject_area_id);


-- =========================================================
-- CONFERENCE RANKINGS
-- Χρήσιμο για ranking lookup ανά conference.
-- =========================================================

CREATE INDEX idx_conference_rankings_conference_id
ON conference_rankings(conference_id);

CREATE INDEX idx_conference_rankings_primary_for_id
ON conference_rankings(primaryFoR_id);


-- =========================================================
-- ANALYZE TABLES
-- =========================================================

ANALYZE TABLE articles;
ANALYZE TABLE journal_articles;
ANALYZE TABLE conference_articles;
ANALYZE TABLE article_authors;
ANALYZE TABLE authors;
ANALYZE TABLE journals;
ANALYZE TABLE conferences;
ANALYZE TABLE journal_rankings;
ANALYZE TABLE conference_rankings;