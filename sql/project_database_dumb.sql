USE research_publications_db;

/*SET FOREIGN_KEY_CHECKS = 0;
SET UNIQUE_CHECKS = 0;
SET AUTOCOMMIT = 0;*/
SET FOREIGN_KEY_CHECKS = 0;
SET SESSION net_read_timeout = 1200;
SET SESSION net_write_timeout = 1200;
SET SESSION wait_timeout = 28800;

SET AUTOCOMMIT = 1;
SET UNIQUE_CHECKS = 1;

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

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 9.1/Uploads/article_types.tsv'
INTO TABLE article_types
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@type_id, @type_name)
SET
    type_id = NULLIF(@type_id, ''),
    type_name = NULLIF(@type_name, '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 9.1/Uploads/authors.tsv'
INTO TABLE authors
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@author_id, @author_name)
SET
    author_id = NULLIF(@author_id, ''),
    author_name = NULLIF(@author_name, '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 9.1/Uploads/publishers.tsv'
INTO TABLE publishers
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@publisher_id, @publisher_name)
SET
    publisher_id = NULLIF(@publisher_id, ''),
    publisher_name = NULLIF(@publisher_name, '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 9.1/Uploads/best_subject_areas.tsv'
INTO TABLE best_subject_areas
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@best_area_id, @area_name)
SET
    best_area_id = NULLIF(@best_area_id, ''),
    area_name = NULLIF(@area_name, '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 9.1/Uploads/primaryFoR_categories.tsv'
INTO TABLE primaryFoR_categories
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@primaryFoR_id, @primaryFoR_name)
SET
    primaryFoR_id = NULLIF(@primaryFoR_id, ''),
    primaryFoR_name = NULLIF(@primaryFoR_name, '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 9.1/Uploads/conferences.tsv'
INTO TABLE conferences
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@conference_id, @acronym, @title, @icore_id)
SET
    conference_id = NULLIF(@conference_id, ''),
    acronym = NULLIF(@acronym, ''),
    title = NULLIF(@title, ''),
    icore_id = NULLIF(@icore_id, '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 9.1/Uploads/journals.tsv'
INTO TABLE journals
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@journal_id, @journal_name, @publisher_id)
SET
    journal_id = NULLIF(@journal_id, ''),
    journal_name = NULLIF(@journal_name, ''),
    publisher_id = NULLIF(@publisher_id, '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 9.1/Uploads/articles_load.tsv'
INTO TABLE articles
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n' /*allagh*/
IGNORE 1 LINES
(@article_id, @ee, @articlekey, @mdate, @pages, @title, @year, @type_id, @url)
SET
    article_id = NULLIF(@article_id, ''),
    ee = NULLIF(@ee, ''),
    articlekey = NULLIF(@articlekey, ''),
    mdate = NULLIF(@mdate, ''),
    pages = NULLIF(@pages, ''),
    title = NULLIF(@title, ''),
    year = NULLIF(@year, ''),
    type_id = NULLIF(@type_id, ''),
    url = NULLIF(@url, '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 9.1/Uploads/journal_rankings.tsv'
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
    journal_id = NULLIF(@journal_id, ''),
    ranking_position = NULLIF(@ranking_position, ''),
    best_area_id = NULLIF(@best_area_id, ''),
    best_quartile = NULLIF(@best_quartile, ''),
    sjr_index = NULLIF(@sjr_index, ''),
    cite_score = NULLIF(@cite_score, ''),
    h_index = NULLIF(@h_index, ''),
    total_docs = NULLIF(@total_docs, ''),
    total_docs_3y = NULLIF(@total_docs_3y, ''),
    total_refs = NULLIF(@total_refs, ''),
    total_cites_3y = NULLIF(@total_cites_3y, ''),
    citable_docs_3y = NULLIF(@citable_docs_3y, ''),
    cites_per_doc_2y = NULLIF(@cites_per_doc_2y, ''),
    refs_per_doc = NULLIF(@refs_per_doc, '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 9.1/Uploads/conference_rankings.tsv'
INTO TABLE conference_rankings
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@conference_id, @rank_label, @primaryFoR_id)
SET
    conference_id = NULLIF(@conference_id, ''),
    rank_label = NULLIF(@rank_label, ''),
    primaryFoR_id = NULLIF(@primaryFoR_id, '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 9.1/Uploads/article_authors.tsv'
INTO TABLE article_authors
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@article_id, @author_id)
SET
    article_id = NULLIF(@article_id, ''),
    author_id = NULLIF(@author_id, '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 9.1/Uploads/journal_articles.tsv'
INTO TABLE journal_articles
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@article_id, @journal_id, @source_id, @volume, @number)
SET
    article_id = NULLIF(@article_id, ''),
    journal_id = NULLIF(@journal_id, ''),
    source_id = NULLIF(@source_id, ''),
    volume = NULLIF(@volume, ''),
    number = NULLIF(@number, '');

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 9.1/Uploads/conference_articles.tsv'
INTO TABLE conference_articles
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(@article_id, @conference_id)
SET
    article_id = NULLIF(@article_id, ''),
    conference_id = NULLIF(@conference_id, '');

COMMIT;

SET AUTOCOMMIT = 1;
SET UNIQUE_CHECKS = 1;
SET FOREIGN_KEY_CHECKS = 1;