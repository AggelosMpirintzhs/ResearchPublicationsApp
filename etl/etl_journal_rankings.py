from __future__ import annotations

import csv
import time
from decimal import Decimal, InvalidOperation


# =========================================================
# Configuration
# =========================================================

INPUT_JOURNAL_RANKINGS = "journal_ranking_data_raw.tsv"
INPUT_BEST_SUBJECT_AREAS = "bestSubjectArea.tsv"

# Final output files pou xreiazontai gia ti vasi / ergasia.
# Den paragoume pleon extra debug/statistics TSV arxeio.
OUTPUT_PUBLISHERS = "publishers.tsv"
OUTPUT_JOURNALS = "journals.tsv"
OUTPUT_JOURNAL_RANKINGS = "journal_rankings.tsv"
OUTPUT_BEST_SUBJECT_AREAS = "best_subject_areas.tsv"

DELIMITER = "\t"

NO_MATCHING_JOURNAL_ID = 999999999
NO_MATCHING_JOURNAL_NAME = "NO MATCHING JOURNAL"


# =========================================================
# Optional progress bar
# =========================================================

try:
    from tqdm import tqdm
except ImportError:
    tqdm = None


def progress(iterable, desc: str, total: int | None = None):
    """
    Xrisimopoioume tqdm an einai egkatestimeno.
    Alliws to programma trexei kanonika xwris progress bar.
    """
    if tqdm is None:
        return iterable
    return tqdm(iterable, desc=desc, total=total)


def count_data_rows(path: str) -> int | None:
    """
    Metraei tis grammes dedomenwn gia na exei to progress bar swsto total.
    An kati paei strava, girname None kai to tqdm douleuei xwris total.
    """
    try:
        with open(path, "r", encoding="utf-8", newline="") as f:
            line_count = sum(1 for _ in f)
        return max(line_count - 1, 0)
    except OSError:
        return None


# =========================================================
# Helpers
# =========================================================

def clean_text(value: str | None) -> str | None:
    if value is None:
        return None

    value = str(value).strip()
    if not value:
        return None

    return value


def parse_int(value: str | None) -> int | None:
    value = clean_text(value)
    if value is None:
        return None

    try:
        return int(value)
    except ValueError:
        return None


def parse_decimal(value: str | None, places: int | None = None) -> str | None:
    """
    Kanei parse arithmitikes times kai epistrefei string etoimo gia TSV / MySQL.
    An places dothei, kratame stathero arithmo dekadikwn psifiwn.
    """
    value = clean_text(value)
    if value is None:
        return None

    try:
        num = Decimal(value)
        if places is not None:
            return f"{num:.{places}f}"
        return str(num)
    except (InvalidOperation, ValueError):
        return None


def normalize_quartile(value: str | None) -> str | None:
    value = clean_text(value)
    if value is None:
        return None

    value = value.upper().replace(" ", "")
    if value in {"Q1", "Q2", "Q3", "Q4"}:
        return value

    return None


def write_tsv(path: str, fieldnames: list[str], rows: list[dict]):
    """
    Grafei ena TSV me headers.
    Ta None ginontai kena strings, wste na fortwnontai pio kathara sti MySQL.
    """
    with open(path, "w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(
            f,
            fieldnames=fieldnames,
            delimiter=DELIMITER,
            extrasaction="ignore",
        )
        writer.writeheader()

        for row in progress(rows, desc=f"Writing {path}", total=len(rows)):
            clean_row = {}
            for col in fieldnames:
                val = row.get(col)
                clean_row[col] = "" if val is None else val
            writer.writerow(clean_row)


# =========================================================
# Load best subject areas
# =========================================================

def load_best_subject_areas(best_subject_area_file: str):
    """
    Diavazei to bestSubjectArea.tsv prwta, gia na dimiourgisoume stable IDs.
    Den kanoume normalization: kratame trimmed exact text, opws ston arxiko kwdika.
    """
    area_name_to_id: dict[str, int] = {}
    best_subject_areas_rows: list[dict] = []

    next_area_id = 1
    total_rows = count_data_rows(best_subject_area_file)

    with open(best_subject_area_file, "r", encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f, delimiter=DELIMITER)

        if "BestSubjectArea" not in (reader.fieldnames or []):
            raise ValueError(
                "Το αρχείο bestSubjectArea.tsv πρέπει να έχει στήλη 'BestSubjectArea'"
            )

        for row in progress(reader, desc="Loading best subject areas", total=total_rows):
            area_name = clean_text(row.get("BestSubjectArea"))
            if area_name is None:
                continue

            if area_name not in area_name_to_id:
                area_id = next_area_id
                next_area_id += 1

                area_name_to_id[area_name] = area_id
                best_subject_areas_rows.append(
                    {
                        "best_area_id": area_id,
                        "area_name": area_name,
                    }
                )

    best_subject_areas_rows.sort(key=lambda x: x["best_area_id"])
    return area_name_to_id, best_subject_areas_rows


# =========================================================
# Final stats
# =========================================================

def print_final_stats(
    publishers_rows: list[dict],
    journals_rows: list[dict],
    best_subject_areas_rows: list[dict],
    journal_rankings_rows: list[dict],
    counters: dict[str, int],
    elapsed_seconds: float,
):
    print("\n" + "=" * 70)
    print("JOURNAL RANKINGS ETL COMPLETED")
    print("=" * 70)

    print(f"Input ranking rows processed: {counters['input_rows_total']}")
    print(f"Publishers created: {len(publishers_rows)}")
    print(f"Journals created: {len(journals_rows)}")
    print(f"Best subject areas created: {len(best_subject_areas_rows)}")
    print(f"Journal rankings created: {len(journal_rankings_rows)}")

    print("\nSkipped rows:")
    print(f"  Empty journal title: {counters['skipped_empty_title']}")
    print(f"  Empty best subject area: {counters['skipped_empty_best_subject_area']}")
    print(f"  Invalid rank: {counters['skipped_invalid_rank']}")
    print(f"  Duplicate rank: {counters['skipped_duplicate_rank']}")
    print(f"  Duplicate journal title: {counters['skipped_duplicate_journal']}")

    print("\nData quality notes:")
    print(f"  New best subject areas found in raw ranking file: {counters['new_best_subject_areas_from_raw']}")
    print(f"  Rows with empty publisher name: {counters['publishers_with_null_name']}")
    print(f"  Journals with null publisher_id: {counters['journals_with_null_publisher']}")
    print(f"  Rankings with null quartile: {counters['rankings_with_null_quartile']}")
    print(f"  Rankings with null SJR-index: {counters['rankings_with_null_sjr_index']}")
    print(f"  Rankings with null CiteScore: {counters['rankings_with_null_cite_score']}")
    print(f"  Rankings with null H-index: {counters['rankings_with_null_h_index']}")
    print(f"  Rankings with null Total Docs.: {counters['rankings_with_null_total_docs']}")
    print(f"  Rankings with null Total Docs. 3y: {counters['rankings_with_null_total_docs_3y']}")
    print(f"  Rankings with null Total Refs.: {counters['rankings_with_null_total_refs']}")
    print(f"  Rankings with null Total Cites 3y: {counters['rankings_with_null_total_cites_3y']}")
    print(f"  Rankings with null Citable Docs. 3y: {counters['rankings_with_null_citable_docs_3y']}")
    print(f"  Rankings with null Cites/Doc. 2y: {counters['rankings_with_null_cites_per_doc_2y']}")
    print(f"  Rankings with null Refs./Doc.: {counters['rankings_with_null_refs_per_doc']}")

    print("\nOutput files:")
    print(f"  {OUTPUT_PUBLISHERS}")
    print(f"  {OUTPUT_JOURNALS}")
    print(f"  {OUTPUT_BEST_SUBJECT_AREAS}")
    print(f"  {OUTPUT_JOURNAL_RANKINGS}")

    print(f"\nElapsed time: {elapsed_seconds:.2f} seconds")


# =========================================================
# Main ETL
# =========================================================

def process_journal_rankings():
    start_time = time.perf_counter()

    print("Loading best subject areas...")
    area_name_to_id, best_subject_areas_rows = load_best_subject_areas(
        INPUT_BEST_SUBJECT_AREAS
    )

    publisher_name_to_id: dict[str, int] = {}
    journal_name_to_id: dict[str, int] = {}

    publishers_rows: list[dict] = []
    journals_rows: list[dict] = []
    journal_rankings_rows: list[dict] = []

    next_publisher_id = 1
    next_journal_id = 1

    seen_ranking_positions: set[int] = set()

    counters = {
        "input_rows_total": 0,
        "skipped_empty_title": 0,
        "skipped_empty_best_subject_area": 0,
        "skipped_invalid_rank": 0,
        "skipped_duplicate_rank": 0,
        "skipped_duplicate_journal": 0,
        "new_best_subject_areas_from_raw": 0,
        "publishers_with_null_name": 0,
        "journals_with_null_publisher": 0,
        "rankings_with_null_quartile": 0,
        "rankings_with_null_sjr_index": 0,
        "rankings_with_null_cite_score": 0,
        "rankings_with_null_h_index": 0,
        "rankings_with_null_total_docs": 0,
        "rankings_with_null_total_docs_3y": 0,
        "rankings_with_null_total_refs": 0,
        "rankings_with_null_total_cites_3y": 0,
        "rankings_with_null_citable_docs_3y": 0,
        "rankings_with_null_cites_per_doc_2y": 0,
        "rankings_with_null_refs_per_doc": 0,
    }

    total_ranking_rows = count_data_rows(INPUT_JOURNAL_RANKINGS)

    print("Processing journal ranking rows...")

    with open(INPUT_JOURNAL_RANKINGS, "r", encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f, delimiter=DELIMITER)

        required_columns = {
            "Rank",
            "Title",
            "SJR-index",
            "CiteScore",
            "H-index",
            "Best Quartile",
            "Best Subject Area",
            "Total Docs.",
            "Total Docs. 3y",
            "Total Refs.",
            "Total Cites 3y",
            "Citable Docs. 3y",
            "Cites/Doc. 2y",
            "Refs./Doc.",
            "Publisher",
        }

        missing = required_columns - set(reader.fieldnames or [])
        if missing:
            raise ValueError(
                f"Λείπουν υποχρεωτικές στήλες από το {INPUT_JOURNAL_RANKINGS}: {sorted(missing)}"
            )

        for row in progress(reader, desc="Journal ranking rows", total=total_ranking_rows):
            counters["input_rows_total"] += 1

            # -------------------------
            # Journal title
            # -------------------------
            journal_name = clean_text(row.get("Title"))
            if journal_name is None:
                counters["skipped_empty_title"] += 1
                continue

            # -------------------------
            # Publisher
            # -------------------------
            publisher_name = clean_text(row.get("Publisher"))

            publisher_id = None
            if publisher_name is not None:
                if publisher_name not in publisher_name_to_id:
                    publisher_id = next_publisher_id
                    next_publisher_id += 1

                    publisher_name_to_id[publisher_name] = publisher_id
                    publishers_rows.append(
                        {
                            "publisher_id": publisher_id,
                            "publisher_name": publisher_name,
                        }
                    )
                else:
                    publisher_id = publisher_name_to_id[publisher_name]
            else:
                counters["publishers_with_null_name"] += 1

            # -------------------------
            # Best subject area
            # -------------------------
            best_subject_area_name = clean_text(row.get("Best Subject Area"))
            if best_subject_area_name is None:
                counters["skipped_empty_best_subject_area"] += 1
                continue

            if best_subject_area_name not in area_name_to_id:
                new_id = max(area_name_to_id.values(), default=0) + 1
                area_name_to_id[best_subject_area_name] = new_id
                best_subject_areas_rows.append(
                    {
                        "best_area_id": new_id,
                        "area_name": best_subject_area_name,
                    }
                )
                counters["new_best_subject_areas_from_raw"] += 1

            best_area_id = area_name_to_id[best_subject_area_name]

            # -------------------------
            # Ranking position
            # -------------------------
            ranking_position = parse_int(row.get("Rank"))
            if ranking_position is None:
                counters["skipped_invalid_rank"] += 1
                continue

            if ranking_position in seen_ranking_positions:
                counters["skipped_duplicate_rank"] += 1
                continue

            # -------------------------
            # Journal lookup / create
            # To journal_id einai PRIMARY KEY sto journal_rankings,
            # opote an to idio Title emfanistei ksana, to paraleipoume.
            # -------------------------
            if journal_name in journal_name_to_id:
                counters["skipped_duplicate_journal"] += 1
                continue

            journal_id = next_journal_id
            next_journal_id += 1
            journal_name_to_id[journal_name] = journal_id

            journals_rows.append(
                {
                    "journal_id": journal_id,
                    "journal_name": journal_name,
                    "publisher_id": publisher_id,
                }
            )

            if publisher_id is None:
                counters["journals_with_null_publisher"] += 1

            seen_ranking_positions.add(ranking_position)

            # -------------------------
            # Ranking metrics
            # -------------------------
            best_quartile = normalize_quartile(row.get("Best Quartile"))
            sjr_index = parse_decimal(row.get("SJR-index"), places=3)
            cite_score = parse_decimal(row.get("CiteScore"), places=2)
            h_index = parse_int(row.get("H-index"))
            total_docs = parse_int(row.get("Total Docs."))
            total_docs_3y = parse_int(row.get("Total Docs. 3y"))
            total_refs = parse_int(row.get("Total Refs."))
            total_cites_3y = parse_int(row.get("Total Cites 3y"))
            citable_docs_3y = parse_int(row.get("Citable Docs. 3y"))
            cites_per_doc_2y = parse_decimal(row.get("Cites/Doc. 2y"), places=3)
            refs_per_doc = parse_decimal(row.get("Refs./Doc."), places=3)

            if best_quartile is None:
                counters["rankings_with_null_quartile"] += 1
            if sjr_index is None:
                counters["rankings_with_null_sjr_index"] += 1
            if cite_score is None:
                counters["rankings_with_null_cite_score"] += 1
            if h_index is None:
                counters["rankings_with_null_h_index"] += 1
            if total_docs is None:
                counters["rankings_with_null_total_docs"] += 1
            if total_docs_3y is None:
                counters["rankings_with_null_total_docs_3y"] += 1
            if total_refs is None:
                counters["rankings_with_null_total_refs"] += 1
            if total_cites_3y is None:
                counters["rankings_with_null_total_cites_3y"] += 1
            if citable_docs_3y is None:
                counters["rankings_with_null_citable_docs_3y"] += 1
            if cites_per_doc_2y is None:
                counters["rankings_with_null_cites_per_doc_2y"] += 1
            if refs_per_doc is None:
                counters["rankings_with_null_refs_per_doc"] += 1

            journal_rankings_rows.append(
                {
                    "journal_id": journal_id,
                    "ranking_position": ranking_position,
                    "best_area_id": best_area_id,
                    "best_quartile": best_quartile,
                    "sjr_index": sjr_index,
                    "cite_score": cite_score,
                    "h_index": h_index,
                    "total_docs": total_docs,
                    "total_docs_3y": total_docs_3y,
                    "total_refs": total_refs,
                    "total_cites_3y": total_cites_3y,
                    "citable_docs_3y": citable_docs_3y,
                    "cites_per_doc_2y": cites_per_doc_2y,
                    "refs_per_doc": refs_per_doc,
                }
            )

    # Deterministic ordering gia na einai stathera ta output arxeia.
    publishers_rows.sort(key=lambda x: x["publisher_id"])
    journals_rows.sort(key=lambda x: x["journal_id"])
    best_subject_areas_rows.sort(key=lambda x: x["best_area_id"])
    journal_rankings_rows.sort(key=lambda x: x["ranking_position"])

    # Sentinel journal gia error handling / fallback se unmatched journal.
    if NO_MATCHING_JOURNAL_NAME not in journal_name_to_id:
        journals_rows.append(
            {
                "journal_id": NO_MATCHING_JOURNAL_ID,
                "journal_name": NO_MATCHING_JOURNAL_NAME,
                "publisher_id": None,
            }
        )

    print("Writing final TSV files...")

    write_tsv(
        OUTPUT_PUBLISHERS,
        ["publisher_id", "publisher_name"],
        publishers_rows,
    )

    write_tsv(
        OUTPUT_JOURNALS,
        ["journal_id", "journal_name", "publisher_id"],
        journals_rows,
    )

    write_tsv(
        OUTPUT_BEST_SUBJECT_AREAS,
        ["best_area_id", "area_name"],
        best_subject_areas_rows,
    )

    write_tsv(
        OUTPUT_JOURNAL_RANKINGS,
        [
            "journal_id",
            "ranking_position",
            "best_area_id",
            "best_quartile",
            "sjr_index",
            "cite_score",
            "h_index",
            "total_docs",
            "total_docs_3y",
            "total_refs",
            "total_cites_3y",
            "citable_docs_3y",
            "cites_per_doc_2y",
            "refs_per_doc",
        ],
        journal_rankings_rows,
    )

    elapsed_seconds = time.perf_counter() - start_time

    print_final_stats(
        publishers_rows,
        journals_rows,
        best_subject_areas_rows,
        journal_rankings_rows,
        counters,
        elapsed_seconds,
    )


if __name__ == "__main__":
    process_journal_rankings()
