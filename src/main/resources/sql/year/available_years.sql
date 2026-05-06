-- Helper: Available years for UI dropdown

SELECT
    year,
    total_articles,
    total_journal_articles,
    total_conference_articles
FROM vw_year_profile_stats
ORDER BY year DESC;