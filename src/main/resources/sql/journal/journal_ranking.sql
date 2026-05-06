-- A5: Journal ranking
-- Parameters:
--   1: journal_id

SELECT
    journal_id,
    journal_name,
    publisher_id,
    publisher_name,

    ranking_position,
    best_quartile,
    sjr_index,
    cite_score,
    h_index,

    total_docs,
    total_docs_3y,
    total_refs,
    total_cites_3y,
    citable_docs_3y,
    cites_per_doc_2y,
    refs_per_doc,

    best_area_id,
    best_subject_area
FROM vw_journal_ranking_metrics
WHERE journal_id = ?;