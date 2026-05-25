USE research_publications_db;

SET FOREIGN_KEY_CHECKS = 0;
SET UNIQUE_CHECKS = 0;
SET AUTOCOMMIT = 0;

TRUNCATE TABLE article_authors;
TRUNCATE TABLE journal_articles;
TRUNCATE TABLE conference_articles;
TRUNCATE TABLE journal_rankings;
TRUNCATE TABLE conference_rankings;

TRUNCATE TABLE articles;
TRUNCATE TABLE authors;
TRUNCATE TABLE journals;
TRUNCATE TABLE conferences;

TRUNCATE TABLE article_types;
TRUNCATE TABLE publishers;
TRUNCATE TABLE best_subject_areas;
TRUNCATE TABLE primaryFoR_categories;

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/article_types.tsv'
INTO TABLE article_types
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@type_id, @type_name)
SET
    type_id = NULLIF(TRIM(BOTH '\r' FROM @type_id), ''),
    type_name = NULLIF(TRIM(BOTH '\r' FROM @type_name), '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/authors.tsv'
INTO TABLE authors
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@author_id, @author_name)
SET
    author_id = NULLIF(TRIM(BOTH '\r' FROM @author_id), ''),
    author_name = LEFT(NULLIF(TRIM(BOTH '\r' FROM @author_name), ''), 255);

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/publishers.tsv'
INTO TABLE publishers
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@publisher_id, @publisher_name)
SET
    publisher_id = NULLIF(TRIM(BOTH '\r' FROM @publisher_id), ''),
    publisher_name = NULLIF(TRIM(BOTH '\r' FROM @publisher_name), '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/best_subject_areas.tsv'
INTO TABLE best_subject_areas
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@best_area_id, @area_name)
SET
    best_area_id = NULLIF(TRIM(BOTH '\r' FROM @best_area_id), ''),
    area_name = NULLIF(TRIM(BOTH '\r' FROM @area_name), '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/primaryFoR_categories.tsv'
INTO TABLE primaryFoR_categories
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@primaryFoR_id, @primaryFoR_name)
SET
    primaryFoR_id = NULLIF(TRIM(BOTH '\r' FROM @primaryFoR_id), ''),
    primaryFoR_name = NULLIF(TRIM(BOTH '\r' FROM @primaryFoR_name), '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/conferences.tsv'
INTO TABLE conferences
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@conference_id, @acronym, @title, @icore_id)
SET
    conference_id = NULLIF(TRIM(BOTH '\r' FROM @conference_id), ''),
    acronym = NULLIF(TRIM(BOTH '\r' FROM @acronym), ''),
    title = NULLIF(TRIM(BOTH '\r' FROM @title), ''),
    icore_id = NULLIF(TRIM(BOTH '\r' FROM @icore_id), '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/journals.tsv'
INTO TABLE journals
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@journal_id, @journal_name, @publisher_id)
SET
    journal_id = NULLIF(TRIM(BOTH '\r' FROM @journal_id), ''),
    journal_name = NULLIF(TRIM(BOTH '\r' FROM @journal_name), ''),
    publisher_id = NULLIF(TRIM(BOTH '\r' FROM @publisher_id), '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/articles.tsv'
INTO TABLE articles
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@article_id, @ee, @articlekey, @mdate, @pages, @title, @year, @type_id, @url)
SET
    article_id = NULLIF(TRIM(BOTH '\r' FROM @article_id), ''),
    ee = NULLIF(TRIM(BOTH '\r' FROM @ee), ''),
    articlekey = NULLIF(TRIM(BOTH '\r' FROM @articlekey), ''),
    mdate = NULLIF(TRIM(BOTH '\r' FROM @mdate), ''),
    pages = NULLIF(TRIM(BOTH '\r' FROM @pages), ''),
    title = NULLIF(TRIM(BOTH '\r' FROM @title), ''),
    year = NULLIF(TRIM(BOTH '\r' FROM @year), ''),
    type_id = NULLIF(TRIM(BOTH '\r' FROM @type_id), ''),
    url = NULLIF(TRIM(BOTH '\r' FROM @url), '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/journal_rankings.tsv'
INTO TABLE journal_rankings
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@journal_id, @ranking_position, @best_area_id, @best_quartile, @sjr_index,
 @cite_score, @h_index, @total_docs, @total_docs_3y, @total_refs,
 @total_cites_3y, @citable_docs_3y, @cites_per_doc_2y, @refs_per_doc)
SET
    journal_id = NULLIF(TRIM(BOTH '\r' FROM @journal_id), ''),
    ranking_position = NULLIF(TRIM(BOTH '\r' FROM @ranking_position), ''),
    best_area_id = NULLIF(TRIM(BOTH '\r' FROM @best_area_id), ''),
    best_quartile = NULLIF(TRIM(BOTH '\r' FROM @best_quartile), ''),
    sjr_index = NULLIF(TRIM(BOTH '\r' FROM @sjr_index), ''),
    cite_score = NULLIF(TRIM(BOTH '\r' FROM @cite_score), ''),
    h_index = NULLIF(TRIM(BOTH '\r' FROM @h_index), ''),
    total_docs = NULLIF(TRIM(BOTH '\r' FROM @total_docs), ''),
    total_docs_3y = NULLIF(TRIM(BOTH '\r' FROM @total_docs_3y), ''),
    total_refs = NULLIF(TRIM(BOTH '\r' FROM @total_refs), ''),
    total_cites_3y = NULLIF(TRIM(BOTH '\r' FROM @total_cites_3y), ''),
    citable_docs_3y = NULLIF(TRIM(BOTH '\r' FROM @citable_docs_3y), ''),
    cites_per_doc_2y = NULLIF(TRIM(BOTH '\r' FROM @cites_per_doc_2y), ''),
    refs_per_doc = NULLIF(TRIM(BOTH '\r' FROM @refs_per_doc), '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/conference_rankings.tsv'
INTO TABLE conference_rankings
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@conference_id, @rank_label, @primaryFoR_id)
SET
    conference_id = NULLIF(TRIM(BOTH '\r' FROM @conference_id), ''),
    rank_label = NULLIF(TRIM(BOTH '\r' FROM @rank_label), ''),
    primaryFoR_id = NULLIF(TRIM(BOTH '\r' FROM @primaryFoR_id), '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/article_authors.tsv'
INTO TABLE article_authors
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@article_id, @author_id)
SET
    article_id = NULLIF(TRIM(BOTH '\r' FROM @article_id), ''),
    author_id = NULLIF(TRIM(BOTH '\r' FROM @author_id), '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/journal_articles.tsv'
INTO TABLE journal_articles
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@article_id, @journal_id, @source_id, @volume, @number)
SET
    article_id = NULLIF(TRIM(BOTH '\r' FROM @article_id), ''),
    journal_id = NULLIF(TRIM(BOTH '\r' FROM @journal_id), ''),
    source_id = NULLIF(TRIM(BOTH '\r' FROM @source_id), ''),
    volume = NULLIF(TRIM(BOTH '\r' FROM @volume), ''),
    number = NULLIF(TRIM(BOTH '\r' FROM @number), '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/conference_articles.tsv'
INTO TABLE conference_articles
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@article_id, @conference_id)
SET
    article_id = NULLIF(TRIM(BOTH '\r' FROM @article_id), ''),
    conference_id = NULLIF(TRIM(BOTH '\r' FROM @conference_id), '');

COMMIT;

SET AUTOCOMMIT = 1;
SET UNIQUE_CHECKS = 1;
SET FOREIGN_KEY_CHECKS = 1;