import csv
import os
import re
import time
import unicodedata
from collections import Counter

try:
    from tqdm import tqdm
except ImportError:
    # Fallback oste to script na trexei kai an den einai egkatestimeno to tqdm.
    # Gia progress bar: pip install tqdm
    def tqdm(iterable, **kwargs):
        return iterable


# =========================================================
# Configuration
# =========================================================

INPUT_PROCEEDINGS = "input_inproceedings.tsv"

ARTICLES_FILE = "articles.tsv"
ARTICLE_TYPES_FILE = "article_types.tsv"
AUTHORS_FILE = "authors.tsv"
ARTICLE_AUTHORS_FILE = "article_authors.tsv"
CONFERENCES_FILE = "conferences.tsv"
CONFERENCE_ARTICLES_FILE = "conference_articles.tsv"

DELIMITER = "\t"
NO_AUTHOR_ID = 999999999
NO_AUTHOR_NAME = "NO AUTHOR"

# True  -> grafei pano sta arxika TSV.
# False -> dimiourgei *_updated.tsv, xwris na peiraksei ta arxika arxeia.
OVERWRITE_OUTPUTS = True
OUTPUT_SUFFIX = "_updated"


# =========================================================
# Helpers
# =========================================================

def clean_value(value):
    if value is None:
        return ""
    return str(value).strip()


def is_empty_title(value):
    value = clean_value(value)
    return value == "" or value.upper() == "NULL" or value == r"\N"


def normalize_text(text):
    """Kanoume kanonikopoiisi gia na tairiazoume idia onomata authors."""
    text = clean_value(text)
    if not text:
        return ""

    text = unicodedata.normalize("NFKD", text)
    text = "".join(ch for ch in text if not unicodedata.combining(ch))
    text = text.casefold()
    text = re.sub(r"[^\w\s]", " ", text, flags=re.UNICODE)
    text = re.sub(r"_+", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def normalize_title_for_matching(text):
    """
    Kanonikopoiisi titlou/acronym conference gia matching me to booktitle.
    Kratame tin idia logiki me tin debugging ekdosi tou script.
    """
    text = clean_value(text)
    if not text:
        return ""

    text = unicodedata.normalize("NFKD", text)
    text = "".join(ch for ch in text if not unicodedata.combining(ch))
    text = text.casefold()

    text = re.sub(r"\([^)]*\)", " ", text)
    text = text.replace("@", " at ")
    text = text.replace("&", " and ")
    text = re.sub(r"[^\w\s]", " ", text)

    weak_words = {
        "proceedings", "conference", "symposium", "workshop",
        "international", "annual", "on", "of", "the", "and",
        "in", "for"
    }

    tokens = [tok for tok in text.split() if tok not in weak_words]
    text = " ".join(tokens)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def read_tsv(path):
    with open(path, "r", encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f, delimiter=DELIMITER)
        rows = list(reader)
        fieldnames = reader.fieldnames
    return fieldnames, rows


def write_tsv(path, fieldnames, rows):
    with open(path, "w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(
            f,
            fieldnames=fieldnames,
            delimiter=DELIMITER,
            extrasaction="ignore"
        )
        writer.writeheader()
        writer.writerows(rows)


def output_path(original_path):
    if OVERWRITE_OUTPUTS:
        return original_path
    base, ext = os.path.splitext(original_path)
    return f"{base}{OUTPUT_SUFFIX}{ext}"


def to_int(value, default=None):
    value = clean_value(value)
    if value == "":
        return default
    try:
        return int(value)
    except Exception:
        return default


def validate_columns(file_name, actual_columns, required_columns):
    actual_columns = set(actual_columns or [])
    missing = required_columns - actual_columns
    if missing:
        raise ValueError(f"Missing columns in {file_name}: {sorted(missing)}")


# =========================================================
# Main ETL
# =========================================================

def main():
    start_time = time.time()
    stats = Counter()
    match_stats = Counter()

    print("Loading existing TSV files...")

    article_types_fields, article_types_rows = read_tsv(ARTICLE_TYPES_FILE)
    articles_fields, articles_rows = read_tsv(ARTICLES_FILE)
    authors_fields, authors_rows = read_tsv(AUTHORS_FILE)
    article_authors_fields, article_authors_rows = read_tsv(ARTICLE_AUTHORS_FILE)
    conferences_fields, conferences_rows = read_tsv(CONFERENCES_FILE)

    if os.path.exists(CONFERENCE_ARTICLES_FILE):
        conference_articles_fields, conference_articles_rows = read_tsv(CONFERENCE_ARTICLES_FILE)
    else:
        conference_articles_fields = ["article_id", "conference_id"]
        conference_articles_rows = []

    proceedings_fields, proceedings_rows = read_tsv(INPUT_PROCEEDINGS)

    print("Validating TSV columns...")

    validate_columns(
        INPUT_PROCEEDINGS,
        proceedings_fields,
        {"id", "author", "booktitle", "ee", "key", "mdate", "pages", "title", "url", "year"}
    )
    validate_columns(
        ARTICLES_FILE,
        articles_fields,
        {"article_id", "ee", "articlekey", "mdate", "pages", "title", "year", "type_id", "url"}
    )
    validate_columns(
        AUTHORS_FILE,
        authors_fields,
        {"author_id", "author_name"}
    )
    validate_columns(
        ARTICLE_AUTHORS_FILE,
        article_authors_fields,
        {"article_id", "author_id"}
    )
    validate_columns(
        CONFERENCES_FILE,
        conferences_fields,
        {"conference_id", "acronym", "title"}
    )
    validate_columns(
        CONFERENCE_ARTICLES_FILE,
        conference_articles_fields,
        {"article_id", "conference_id"}
    )

    print("Preparing indexes...")

    existing_article_ids = {
        clean_value(r["article_id"])
        for r in articles_rows
        if clean_value(r["article_id"]) != ""
    }

    existing_article_author_pairs = {
        (clean_value(r["article_id"]), clean_value(r["author_id"]))
        for r in article_authors_rows
    }

    existing_conference_pairs = {
        (clean_value(r["article_id"]), clean_value(r["conference_id"]))
        for r in conference_articles_rows
    }

    # To NO AUTHOR theloume na meinei sto telos tou authors.tsv.
    authors_last_special_row = None
    authors_body_rows = authors_rows[:]

    if authors_rows:
        last_row = authors_rows[-1]
        if (
            to_int(last_row.get("author_id")) == NO_AUTHOR_ID
            or clean_value(last_row.get("author_name")).upper() == NO_AUTHOR_NAME
        ):
            authors_last_special_row = last_row
            authors_body_rows = authors_rows[:-1]

    author_name_to_id = {}
    max_author_id = 0

    for row in authors_body_rows:
        author_id = to_int(row["author_id"])
        author_name = clean_value(row["author_name"])
        if author_id is not None:
            max_author_id = max(max_author_id, author_id)
        if author_name:
            author_name_to_id[normalize_text(author_name)] = author_id

    if authors_last_special_row:
        author_name_to_id[
            normalize_text(clean_value(authors_last_special_row["author_name"]))
        ] = NO_AUTHOR_ID

    type_name_to_id = {}
    max_type_id = 0

    for row in article_types_rows:
        type_id = to_int(row["type_id"])
        type_name = clean_value(row["type_name"]).casefold()
        if type_id is not None:
            type_name_to_id[type_name] = type_id
            max_type_id = max(max_type_id, type_id)

    conference_match_index = {}
    conference_id_to_row = {}

    for row in conferences_rows:
        conf_id = clean_value(row["conference_id"])
        acronym = clean_value(row.get("acronym"))
        title = clean_value(row.get("title"))

        conference_id_to_row[conf_id] = row

        if acronym:
            key = normalize_title_for_matching(acronym)
            if key:
                conference_match_index.setdefault(key, []).append(conf_id)

        if title:
            key = normalize_title_for_matching(title)
            if key:
                conference_match_index.setdefault(key, []).append(conf_id)

    print("Ensuring article type 'conference' exists...")

    if "conference" in type_name_to_id:
        conference_type_id = type_name_to_id["conference"]
        article_types_added = 0
    else:
        conference_type_id = max_type_id + 1
        article_types_rows.append({
            "type_id": conference_type_id,
            "type_name": "conference"
        })
        type_name_to_id["conference"] = conference_type_id
        article_types_added = 1

    def match_conference_id(booktitle):
        raw = clean_value(booktitle)
        if not raw:
            return {
                "conference_id": None,
                "match_method": "missing_booktitle",
                "matched_on": "",
                "matched_value": "",
                "normalized_booktitle": "",
            }

        norm = normalize_title_for_matching(raw)
        if not norm:
            return {
                "conference_id": None,
                "match_method": "empty_after_normalization",
                "matched_on": "",
                "matched_value": "",
                "normalized_booktitle": "",
            }

        # 1. Exact match sto kanonikopoiimeno acronym/title.
        ids = conference_match_index.get(norm)
        if ids:
            ids_unique = sorted(set(ids))
            conf_id = ids_unique[0]
            return {
                "conference_id": conf_id,
                "match_method": "normalized_exact" if len(ids_unique) == 1 else "normalized_exact_ambiguous",
                "matched_on": "acronym_or_title",
                "matched_value": norm,
                "normalized_booktitle": norm,
            }

        # 2. Exact match afairontas to kommati mesa se parentheseis.
        raw_before_paren = re.sub(r"\([^)]*\)", "", raw).strip()
        norm_before_paren = normalize_title_for_matching(raw_before_paren)
        if norm_before_paren:
            ids = conference_match_index.get(norm_before_paren)
            if ids:
                ids_unique = sorted(set(ids))
                conf_id = ids_unique[0]
                return {
                    "conference_id": conf_id,
                    "match_method": "normalized_before_parenthesis_exact" if len(ids_unique) == 1 else "normalized_before_parenthesis_ambiguous",
                    "matched_on": "acronym_or_title",
                    "matched_value": norm_before_paren,
                    "normalized_booktitle": norm,
                }

        # 3. Containment match, gia periptoseis pou to ena keimeno periexei to allo.
        for key, ids in conference_match_index.items():
            if not key:
                continue
            if norm in key or key in norm:
                ids_unique = sorted(set(ids))
                conf_id = ids_unique[0]
                return {
                    "conference_id": conf_id,
                    "match_method": "containment_match" if len(ids_unique) == 1 else "containment_match_ambiguous",
                    "matched_on": "acronym_or_title",
                    "matched_value": key,
                    "normalized_booktitle": norm,
                }

        # 4. Exact acronym token match mesa sto booktitle.
        norm_tokens = set(norm.split())
        for row in conferences_rows:
            acronym_norm = normalize_title_for_matching(row.get("acronym", ""))
            if acronym_norm and acronym_norm in norm_tokens:
                return {
                    "conference_id": clean_value(row["conference_id"]),
                    "match_method": "acronym_token_match",
                    "matched_on": "acronym",
                    "matched_value": acronym_norm,
                    "normalized_booktitle": norm,
                }

        return {
            "conference_id": None,
            "match_method": "not_matched",
            "matched_on": "",
            "matched_value": "",
            "normalized_booktitle": norm,
        }

    print("Processing proceedings...")

    new_articles_rows = []
    new_article_authors_rows = []
    new_conference_articles_rows = []
    new_authors_rows = []

    for row in tqdm(proceedings_rows, desc="Proceedings rows", unit="row", dynamic_ncols=True):
        proc_id = clean_value(row["id"])
        if proc_id == "":
            stats["rows_skipped_missing_id"] += 1
            continue

        title = clean_value(row["title"])
        if is_empty_title(title):
            stats["rows_skipped_missing_title"] += 1
            continue

        ee = clean_value(row["ee"])
        articlekey = clean_value(row["key"])
        mdate = clean_value(row["mdate"])
        pages = clean_value(row["pages"])
        url = clean_value(row["url"])
        year = clean_value(row["year"])
        booktitle = clean_value(row["booktitle"])
        raw_authors = clean_value(row["author"])

        # Add article mono an den yparxei idi sto articles.tsv.
        if proc_id not in existing_article_ids:
            new_articles_rows.append({
                "article_id": proc_id,
                "ee": ee,
                "articlekey": articlekey,
                "mdate": mdate,
                "pages": pages,
                "title": title,
                "year": year,
                "type_id": conference_type_id,
                "url": url
            })
            existing_article_ids.add(proc_id)
            stats["articles_added"] += 1
        else:
            stats["articles_already_existing"] += 1

        # Syndesi article -> conference me vasi to booktitle.
        match_result = match_conference_id(booktitle)
        conf_id = match_result["conference_id"]
        match_method = match_result["match_method"]
        match_stats[match_method] += 1

        if conf_id is not None:
            stats["conference_matched"] += 1
            pair = (proc_id, conf_id)
            if pair not in existing_conference_pairs:
                new_conference_articles_rows.append({
                    "article_id": proc_id,
                    "conference_id": conf_id
                })
                existing_conference_pairs.add(pair)
                stats["conference_articles_added"] += 1
            else:
                stats["conference_articles_already_existing"] += 1
        else:
            stats["conference_not_matched"] += 1

        # Authors: split me '|', reuse existing author_id an yparxei, alliws neo id.
        if raw_authors == "":
            author_ids_for_article = [NO_AUTHOR_ID]
            stats["articles_with_empty_author_field"] += 1
        else:
            split_authors = [a.strip() for a in raw_authors.split("|")]
            split_authors = [a for a in split_authors if a != ""]

            if not split_authors:
                author_ids_for_article = [NO_AUTHOR_ID]
                stats["articles_with_empty_author_field"] += 1
            else:
                author_ids_for_article = []

                for author_name in split_authors:
                    norm_author = normalize_text(author_name)

                    if norm_author in author_name_to_id:
                        author_id = author_name_to_id[norm_author]
                        stats["author_matches_existing"] += 1
                    else:
                        max_author_id += 1
                        author_id = max_author_id
                        author_name_to_id[norm_author] = author_id
                        new_authors_rows.append({
                            "author_id": author_id,
                            "author_name": author_name
                        })
                        stats["authors_added"] += 1

                    author_ids_for_article.append(author_id)

        # Afairoume diplous authors gia to idio article, xwris na allazoume ti seira tous.
        author_ids_for_article = list(dict.fromkeys(author_ids_for_article))

        for author_id in author_ids_for_article:
            pair = (proc_id, str(author_id))
            if pair not in existing_article_author_pairs:
                new_article_authors_rows.append({
                    "article_id": proc_id,
                    "author_id": author_id
                })
                existing_article_author_pairs.add(pair)
                stats["article_authors_added"] += 1
            else:
                stats["article_authors_already_existing"] += 1

    print("Building final output rows...")

    final_article_types_rows = article_types_rows
    final_articles_rows = articles_rows + new_articles_rows

    final_authors_rows = authors_body_rows + new_authors_rows
    if authors_last_special_row is not None:
        final_authors_rows.append(authors_last_special_row)
    elif normalize_text(NO_AUTHOR_NAME) not in author_name_to_id:
        final_authors_rows.append({
            "author_id": NO_AUTHOR_ID,
            "author_name": NO_AUTHOR_NAME
        })
        stats["no_author_row_added_to_authors"] += 1

    final_article_authors_rows = article_authors_rows + new_article_authors_rows
    final_conference_articles_rows = conference_articles_rows + new_conference_articles_rows

    output_files = [
        (ARTICLE_TYPES_FILE, article_types_fields, final_article_types_rows),
        (ARTICLES_FILE, articles_fields, final_articles_rows),
        (AUTHORS_FILE, authors_fields, final_authors_rows),
        (ARTICLE_AUTHORS_FILE, article_authors_fields, final_article_authors_rows),
        (CONFERENCES_FILE, conferences_fields, conferences_rows),
        (CONFERENCE_ARTICLES_FILE, conference_articles_fields, final_conference_articles_rows),
    ]

    print("Writing final TSV files...")
    for file_name, fieldnames, rows in tqdm(output_files, desc="Writing TSV files", unit="file", dynamic_ncols=True):
        write_tsv(output_path(file_name), fieldnames, rows)

    elapsed_seconds = time.time() - start_time

    print_final_stats(
        stats=stats,
        match_stats=match_stats,
        elapsed_seconds=elapsed_seconds,
        proceedings_count=len(proceedings_rows),
        article_types_added=article_types_added,
        final_counts={
            ARTICLE_TYPES_FILE: len(final_article_types_rows),
            ARTICLES_FILE: len(final_articles_rows),
            AUTHORS_FILE: len(final_authors_rows),
            ARTICLE_AUTHORS_FILE: len(final_article_authors_rows),
            CONFERENCES_FILE: len(conferences_rows),
            CONFERENCE_ARTICLES_FILE: len(final_conference_articles_rows),
        },
        output_files=[output_path(file_name) for file_name, _, _ in output_files]
    )


def print_final_stats(stats, match_stats, elapsed_seconds, proceedings_count, article_types_added, final_counts, output_files):
    print("\n" + "=" * 70)
    print("PROCESS COMPLETED")
    print("=" * 70)

    print(f"Elapsed time: {elapsed_seconds:.2f} seconds")
    print(f"Proceedings input rows: {proceedings_count}")
    print(f"Article types added: {article_types_added}")
    print(f"Articles added: {stats['articles_added']}")
    print(f"Articles already existing: {stats['articles_already_existing']}")
    print(f"Authors added: {stats['authors_added']}")
    print(f"Existing author matches: {stats['author_matches_existing']}")
    print(f"Article-author relations added: {stats['article_authors_added']}")
    print(f"Article-author relations already existing: {stats['article_authors_already_existing']}")
    print(f"Conference matched: {stats['conference_matched']}")
    print(f"Conference not matched: {stats['conference_not_matched']}")
    print(f"Conference-article relations added: {stats['conference_articles_added']}")
    print(f"Conference-article relations already existing: {stats['conference_articles_already_existing']}")
    print(f"Articles with empty author field: {stats['articles_with_empty_author_field']}")
    print(f"Rows skipped due to missing id: {stats['rows_skipped_missing_id']}")
    print(f"Rows skipped due to missing title: {stats['rows_skipped_missing_title']}")

    if stats["no_author_row_added_to_authors"] > 0:
        print(f"NO AUTHOR row added to authors file: {stats['no_author_row_added_to_authors']}")

    print("\nConference match methods:")
    if match_stats:
        for method, count in sorted(match_stats.items(), key=lambda x: (-x[1], x[0])):
            print(f"  {method}: {count}")
    else:
        print("  No conference matching was performed.")

    print("\nFinal row counts:")
    for file_name, row_count in final_counts.items():
        print(f"  {file_name}: {row_count}")

    print("\nOutput files:")
    for file_name in output_files:
        print(f"  {file_name}")


if __name__ == "__main__":
    main()
