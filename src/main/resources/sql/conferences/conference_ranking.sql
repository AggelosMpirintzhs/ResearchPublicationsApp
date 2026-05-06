-- A6: Conference ranking
-- Parameters:
--   1: conference_id

SELECT
    conference_id,
    acronym,
    conference_title,
    icore_id,

    rank_label,
    primaryFoR_id,
    primaryFoR_name
FROM vw_conference_ranking_metrics
WHERE conference_id = ?;