-- Scatter plot for journal ranking metrics
-- Parameters:
--   Optional 1: limit, only when Java injects LIMIT ?
--
-- Every returned row is one journal point.
-- x_value and y_value are injected only from Java whitelist columns.

SELECT
    journal_id AS id,
    journal_name AS label,
    %s AS x_value,
    %s AS y_value
FROM vw_journal_ranking_metrics
WHERE %s IS NOT NULL
  AND %s IS NOT NULL
ORDER BY (%s + %s) DESC
    %s;