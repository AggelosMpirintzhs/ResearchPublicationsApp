import csv
import time
from collections import Counter


# =========================================================
# Configuration
# =========================================================

INPUT_FILE = "iCore26_KilledColumnsForLoading.tsv"

OUTPUT_CONFERENCES = "conferences.tsv"
OUTPUT_CONFERENCE_RANKINGS = "conference_rankings.tsv"
OUTPUT_PRIMARY_FOR = "primaryFoR_categories.tsv"

DELIMITER = "\t"


# =========================================================
# Fixed Primary FoR mappings
# =========================================================
# Ta mappings einai stathera gia to dataset mas kai den ta paragoume
# dynamika apo to raw file. Etsi to primaryFoR_categories.tsv exei panta
# kathara kai sosta onomata, anti gia genika "FoR 4601", "FoR 4602" ktl.

PRIMARY_FOR_CATEGORIES = {
    46: "Information and Computing Sciences",
    4601: "Applied Computing",
    4602: "Artificial Intelligence",
    4603: "Computer Vision and Multimedia Computation",
    4604: "Cybersecurity and Privacy",
    4605: "Data Management and Data Science",
    4606: "Distributed Computing and Systems Software",
    4607: "Graphics, Augmented Reality and Games",
    4608: "Human-Centred Computing",
    4611: "Machine Learning",
    4612: "Software Engineering",
    4613: "Theory of Computation",
}


# =========================================================
# Optional progress bar
# =========================================================

try:
    from tqdm import tqdm
except ImportError:
    def tqdm(iterable=None, **kwargs):
        return iterable if iterable is not None else []


# =========================================================
# Helpers
# =========================================================

def stored_value(value):
    if value is None:
        return None

    value = str(value).strip()
    if value == "":
        return None

    return value


def match_key(value):
    value = stored_value(value)
    if value is None:
        return None

    # Kanonikopoioume mono gia matching/deduplication.
    # Den allazoume tin timi pou tha graftei sto TSV.
    return " ".join(value.lower().split())


def parse_int(value):
    value = stored_value(value)
    if value is None:
        return None

    try:
        return int(value)
    except ValueError:
        return None


def normalize_header(header):
    if header is None:
        return None

    return " ".join(str(header).strip().split())


def count_data_rows(path):
    with open(path, "r", encoding="utf-8-sig", newline="") as f:
        # Afairoume mono to header apo to total, gia na einai sosto to progress bar.
        total_lines = sum(1 for _ in f)

    return max(total_lines - 1, 0)


def write_tsv(path, fieldnames, rows):
    with open(path, "w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(
            f,
            fieldnames=fieldnames,
            delimiter=DELIMITER,
            extrasaction="ignore",
        )
        writer.writeheader()

        for row in rows:
            out = {}
            for field in fieldnames:
                val = row.get(field)
                out[field] = "" if val is None else val
            writer.writerow(out)


def build_primary_for_rows():
    return [
        {
            "primaryFoR_id": primary_for_id,
            "primaryFoR_name": primary_for_name,
        }
        for primary_for_id, primary_for_name in sorted(PRIMARY_FOR_CATEGORIES.items())
    ]


# =========================================================
# Main ETL
# =========================================================

def process_conferences():
    start_time = time.time()

    acronym_to_conference_id = {}
    conferences_by_id = {}
    rankings_by_conference_id = {}

    next_conference_id = 1

    counters = Counter()
    total_input_rows = count_data_rows(INPUT_FILE)

    print("Processing conference rows...")

    with open(INPUT_FILE, "r", encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f, delimiter=DELIMITER)

        if reader.fieldnames is None:
            raise ValueError("Το input file δεν έχει header row.")

        normalized_fieldnames = [normalize_header(h) for h in reader.fieldnames]
        reader.fieldnames = normalized_fieldnames

        required_columns = {"ID", "Title", "Acronym", "Rank", "PrimaryFoR"}
        actual_columns = set(reader.fieldnames or [])
        missing = required_columns - actual_columns

        if missing:
            raise ValueError(
                f"Λείπουν υποχρεωτικές στήλες από το input file: {sorted(missing)}. "
                f"Βρέθηκαν: {reader.fieldnames}"
            )

        for line_no, row in enumerate(
            tqdm(reader, total=total_input_rows, desc="Conference rows", unit="rows"),
            start=2,
        ):
            counters["input_rows_total"] += 1

            raw_id = row.get("ID")
            raw_title = row.get("Title")
            raw_acronym = row.get("Acronym")
            raw_rank = row.get("Rank")
            raw_primary_for = row.get("PrimaryFoR")

            icore_id = parse_int(raw_id)
            title = stored_value(raw_title)
            acronym = stored_value(raw_acronym)
            rank_label = stored_value(raw_rank)
            raw_primary_for_id = parse_int(raw_primary_for)

            acronym_key = match_key(raw_acronym)

            # -----------------------------------------------------
            # Basic validation
            # -----------------------------------------------------
            if acronym is None or acronym_key is None:
                counters["rows_skipped_missing_acronym"] += 1
                continue

            if rank_label is None:
                counters["rows_skipped_missing_rank"] += 1
                continue

            if raw_primary_for_id is None:
                primary_for_id = None
                counters["rankings_with_null_primary_for"] += 1
            elif raw_primary_for_id in PRIMARY_FOR_CATEGORIES:
                primary_for_id = raw_primary_for_id
            else:
                # Den grafoume agnosto code sto conference_rankings.tsv,
                # giati den tha uparxei sto primaryFoR_categories.tsv.
                primary_for_id = None
                counters["rankings_with_unknown_primary_for"] += 1

            # -----------------------------------------------------
            # Conference deduplication by acronym
            # -----------------------------------------------------
            if acronym_key not in acronym_to_conference_id:
                conference_id = next_conference_id
                next_conference_id += 1

                acronym_to_conference_id[acronym_key] = conference_id

                conferences_by_id[conference_id] = {
                    "conference_id": conference_id,
                    "acronym": acronym,
                    "title": title,
                    "icore_id": icore_id,
                }

                counters["conferences_added"] += 1
            else:
                conference_id = acronym_to_conference_id[acronym_key]
                existing_conf = conferences_by_id[conference_id]

                counters["duplicate_conference_acronym_rows"] += 1

                # Sumplironoume mono times pou eleipan apo tin proti emfanisi.
                if existing_conf["title"] is None and title is not None:
                    existing_conf["title"] = title
                    counters["conference_titles_filled_from_duplicate"] += 1

                if existing_conf["icore_id"] is None and icore_id is not None:
                    existing_conf["icore_id"] = icore_id
                    counters["conference_icore_ids_filled_from_duplicate"] += 1

                # Ta conflicts den stamataνε to ETL. Apla ta kratame sta stats.
                if (
                    existing_conf["icore_id"] is not None
                    and icore_id is not None
                    and existing_conf["icore_id"] != icore_id
                ):
                    counters["conflicting_icore_id_for_same_acronym"] += 1

                if (
                    existing_conf["title"] is not None
                    and title is not None
                    and existing_conf["title"] != title
                ):
                    counters["conflicting_title_for_same_acronym"] += 1

            # -----------------------------------------------------
            # Ranking deduplication by conference_id
            # -----------------------------------------------------
            if conference_id not in rankings_by_conference_id:
                rankings_by_conference_id[conference_id] = {
                    "conference_id": conference_id,
                    "rank_label": rank_label,
                    "primaryFoR_id": primary_for_id,
                }

                counters["conference_rankings_added"] += 1
            else:
                existing_rank = rankings_by_conference_id[conference_id]

                same_rank = existing_rank["rank_label"] == rank_label
                same_for = existing_rank["primaryFoR_id"] == primary_for_id

                if same_rank and same_for:
                    counters["duplicate_ranking_rows_same_values"] += 1
                    continue

                # Sumplironoume mono ean sto yparxon row eixe meinei keno.
                if existing_rank["rank_label"] is None and rank_label is not None:
                    existing_rank["rank_label"] = rank_label
                    counters["ranking_labels_filled_from_duplicate"] += 1

                if existing_rank["primaryFoR_id"] is None and primary_for_id is not None:
                    existing_rank["primaryFoR_id"] = primary_for_id
                    counters["ranking_primary_for_filled_from_duplicate"] += 1

                rank_conflict = (
                    existing_rank["rank_label"] is not None
                    and rank_label is not None
                    and existing_rank["rank_label"] != rank_label
                )

                primary_for_conflict = (
                    existing_rank["primaryFoR_id"] is not None
                    and primary_for_id is not None
                    and existing_rank["primaryFoR_id"] != primary_for_id
                )

                if rank_conflict or primary_for_conflict:
                    counters["conflicting_ranking_for_same_acronym"] += 1

    # =========================================================
    # Build final rows
    # =========================================================

    conferences_rows = sorted(
        conferences_by_id.values(),
        key=lambda x: x["conference_id"],
    )

    conference_rankings_rows = sorted(
        rankings_by_conference_id.values(),
        key=lambda x: x["conference_id"],
    )

    primary_for_rows = build_primary_for_rows()

    # =========================================================
    # Write final TSV files
    # =========================================================

    print("Writing output TSV files...")

    output_jobs = [
        (
            OUTPUT_CONFERENCES,
            ["conference_id", "acronym", "title", "icore_id"],
            conferences_rows,
        ),
        (
            OUTPUT_CONFERENCE_RANKINGS,
            ["conference_id", "rank_label", "primaryFoR_id"],
            conference_rankings_rows,
        ),
        (
            OUTPUT_PRIMARY_FOR,
            ["primaryFoR_id", "primaryFoR_name"],
            primary_for_rows,
        ),
    ]

    for path, fieldnames, rows in tqdm(output_jobs, desc="Writing TSV files", unit="file"):
        write_tsv(path, fieldnames, rows)

    elapsed_time = time.time() - start_time

    # =========================================================
    # Final stats
    # =========================================================

    print("\n" + "=" * 70)
    print("CONFERENCE ETL COMPLETED")
    print("=" * 70)

    print(f"Input rows processed: {counters['input_rows_total']}")
    print(f"Rows skipped due to missing acronym: {counters['rows_skipped_missing_acronym']}")
    print(f"Rows skipped due to missing rank: {counters['rows_skipped_missing_rank']}")

    print("\nRows created:")
    print(f"  {OUTPUT_CONFERENCES}: {len(conferences_rows)}")
    print(f"  {OUTPUT_CONFERENCE_RANKINGS}: {len(conference_rankings_rows)}")
    print(f"  {OUTPUT_PRIMARY_FOR}: {len(primary_for_rows)}")

    print("\nDeduplication / data quality stats:")
    print(f"  Conferences added: {counters['conferences_added']}")
    print(f"  Duplicate conference acronym rows: {counters['duplicate_conference_acronym_rows']}")
    print(f"  Conference rankings added: {counters['conference_rankings_added']}")
    print(f"  Duplicate ranking rows with same values: {counters['duplicate_ranking_rows_same_values']}")
    print(f"  Conflicting iCORE IDs for same acronym: {counters['conflicting_icore_id_for_same_acronym']}")
    print(f"  Conflicting titles for same acronym: {counters['conflicting_title_for_same_acronym']}")
    print(f"  Conflicting rankings for same acronym: {counters['conflicting_ranking_for_same_acronym']}")

    print("\nPrimary FoR stats:")
    print(f"  Fixed Primary FoR categories written: {len(primary_for_rows)}")
    print(f"  Rankings with empty PrimaryFoR: {counters['rankings_with_null_primary_for']}")
    print(f"  Rankings with unknown PrimaryFoR: {counters['rankings_with_unknown_primary_for']}")

    print("\nOutput files:")
    print(f"  {OUTPUT_CONFERENCES}")
    print(f"  {OUTPUT_CONFERENCE_RANKINGS}")
    print(f"  {OUTPUT_PRIMARY_FOR}")

    print(f"\nElapsed time: {elapsed_time:.2f} seconds")


if __name__ == "__main__":
    process_conferences()
