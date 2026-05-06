-- ==========================================
-- Project: Bibliographic Data Integration
-- Course: Advanced Topics in Databases
-- University of Ioannina
-- ==========================================

DROP DATABASE IF EXISTS research_publications_db;

CREATE DATABASE research_publications_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE research_publications_db;

-- ==========================================
-- LOOKUP TABLE: article_types
-- 1 = journal
-- 2 = conference
-- ==========================================

CREATE TABLE article_types (
   type_id INT PRIMARY KEY,
   type_name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;


-- ==========================================
-- MAIN TABLE: articles
-- ==========================================

CREATE TABLE articles (
  article_id INT PRIMARY KEY,
  ee VARCHAR(2000),
  articlekey VARCHAR(500),
  mdate DATE,
  pages VARCHAR(50),
  title VARCHAR(1000) NOT NULL,
  year INT NOT NULL,
  type_id INT NOT NULL,
  url VARCHAR(500),

  CONSTRAINT fk_articles_type
    FOREIGN KEY (type_id)
      REFERENCES article_types(type_id)
) ENGINE=InnoDB;


-- ==========================================
-- LOOKUP TABLE: authors
-- ==========================================

CREATE TABLE authors (
    author_id INT PRIMARY KEY,
    author_name VARCHAR(255) CHARACTER SET utf8mb4
     COLLATE utf8mb4_0900_as_cs NOT NULL,
    UNIQUE KEY uq_authors_name (author_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE best_subject_areas (
    best_area_id INT PRIMARY KEY,
    area_name VARCHAR(255) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE publishers (
    publisher_id INT PRIMARY KEY,
    publisher_name VARCHAR(255) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE primaryFoR_categories (
   primaryFoR_id INT PRIMARY KEY,
   primaryFoR_name VARCHAR(255) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE journals (
  journal_id INT PRIMARY KEY,
  journal_name VARCHAR(255) NOT NULL UNIQUE,
  publisher_id INT,

  CONSTRAINT fk_journals_publisher
      FOREIGN KEY (publisher_id)
          REFERENCES publishers(publisher_id)
) ENGINE=InnoDB;

CREATE TABLE conferences (
    conference_id INT PRIMARY KEY,
    acronym VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(255),
    icore_id INT UNIQUE
) ENGINE=InnoDB;


-- ==========================================
-- RELATION TABLE: article_authors
-- Συνδέει articles <-> authors
-- ==========================================

CREATE TABLE article_authors (
    article_id INT NOT NULL,
    author_id INT NOT NULL,

    PRIMARY KEY (article_id, author_id),

    CONSTRAINT fk_article_authors_article
     FOREIGN KEY (article_id)
         REFERENCES articles(article_id)
         ON DELETE CASCADE,

    CONSTRAINT fk_article_authors_author
     FOREIGN KEY (author_id)
         REFERENCES authors(author_id)
         ON DELETE CASCADE
) ENGINE=InnoDB;


CREATE TABLE journal_articles (
    article_id INT PRIMARY KEY,
    journal_id INT NOT NULL,
    source_id INT NOT NULL,
    volume VARCHAR(50),
    number VARCHAR(50),

    CONSTRAINT fk_journal_articles_article
      FOREIGN KEY (article_id)
          REFERENCES articles(article_id)
          ON DELETE CASCADE,

    CONSTRAINT fk_journal_articles_journal
      FOREIGN KEY (journal_id)
          REFERENCES journals(journal_id)
) ENGINE=InnoDB;


CREATE TABLE conference_articles (
    article_id INT PRIMARY KEY,
    conference_id INT NOT NULL,

    CONSTRAINT fk_conference_articles_article
     FOREIGN KEY (article_id)
         REFERENCES articles(article_id)
         ON DELETE CASCADE,

    CONSTRAINT fk_conference_articles_conference
     FOREIGN KEY (conference_id)
         REFERENCES conferences(conference_id)
) ENGINE=InnoDB;

CREATE TABLE journal_rankings (
    journal_id INT PRIMARY KEY,
    ranking_position INT UNIQUE NOT NULL,
    best_area_id INT NOT NULL,
    best_quartile ENUM('Q1','Q2','Q3','Q4'),
    sjr_index DECIMAL(10,3),
    cite_score DECIMAL(10,2),
    h_index INT,
    total_docs INT,
    total_docs_3y INT,
    total_refs INT,
    total_cites_3y INT,
    citable_docs_3y INT,
    cites_per_doc_2y DECIMAL(10,3),
    refs_per_doc DECIMAL(10,3),

    CONSTRAINT fk_journal_rankings_journal
      FOREIGN KEY (journal_id)
          REFERENCES journals(journal_id),

    CONSTRAINT fk_journal_rankings_area
      FOREIGN KEY (best_area_id)
          REFERENCES best_subject_areas(best_area_id)
) ENGINE=InnoDB;


CREATE TABLE conference_rankings (
    conference_id INT PRIMARY KEY,
    rank_label VARCHAR(50) NOT NULL,
    primaryFoR_id INT,

    CONSTRAINT fk_conference_rankings_conference
     FOREIGN KEY (conference_id)
         REFERENCES conferences(conference_id),

    CONSTRAINT fk_conference_rankings_primaryFoR
     FOREIGN KEY (primaryFoR_id)
         REFERENCES primaryFoR_categories(primaryFoR_id)
) ENGINE=InnoDB;


SHOW TABLES;
DESCRIBE article_types;
DESCRIBE articles;
DESCRIBE authors;
DESCRIBE article_authors;
DESCRIBE journals;
DESCRIBE journal_articles;
DESCRIBE conferences;
DESCRIBE conference_articles;
DESCRIBE publishers;
DESCRIBE best_subject_areas;
DESCRIBE journal_rankings;
DESCRIBE primaryFoR_categories;
DESCRIBE conference_rankings;