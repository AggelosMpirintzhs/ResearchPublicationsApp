import os
import sys
import time
import subprocess


# ============================================================
# Full ETL runner
# ============================================================
# Trexei ola ta epimerous ETL scripts me ti sosti seira.
#
# ============================================================



ETL_SCRIPTS = [
    "etl_journal_rankings.py",
    "etl_conferences.py",
    "etl_journals.py",
    "etl_conference_proceedings.py",
]


def check_scripts_exist(base_dir):
    missing = []

    for script in ETL_SCRIPTS:
        script_path = os.path.join(base_dir, script)
        if not os.path.isfile(script_path):
            missing.append(script)

    if missing:
        print("\nERROR: Δεν βρέθηκαν τα παρακάτω ETL scripts:")
        for script in missing:
            print(f"  - {script}")
        print("\nΒεβαιώσου ότι το run_full_etl.py βρίσκεται στον ίδιο φάκελο με τα scripts.")
        return False

    return True


def run_script(base_dir, script_name, step_no, total_steps):
    print("\n" + "=" * 80)
    print(f"STEP {step_no}/{total_steps}: Running {script_name}")
    print("=" * 80)

    start = time.perf_counter()
    script_path = os.path.join(base_dir, script_name)

    # Xrisimopoioume to idio Python executable pou trexei kai auto to runner.
    # Etsi apofeugoume problimata an sto systima yparxoun polla Python versions.
    command = [sys.executable, script_path]

    result = subprocess.run(
        command,
        cwd=base_dir,
    )

    elapsed = time.perf_counter() - start

    if result.returncode != 0:
        print("\n" + "!" * 80)
        print(f"ETL FAILED στο script: {script_name}")
        print(f"Exit code: {result.returncode}")
        print(f"Elapsed time before failure: {elapsed:.2f} seconds")
        print("Η διαδικασία σταμάτησε για να μη συνεχίσει με λάθος ή μισά δεδομένα.")
        print("!" * 80)
        return False

    print("\n" + "-" * 80)
    print(f"Completed {script_name} successfully in {elapsed:.2f} seconds.")
    print("-" * 80)

    return True


def main():
    total_start = time.perf_counter()

    # O fakelos tou runner. Ola ta relative paths twn ETL scripts tha doulepoun
    # me vasi auton ton fakelo.
    base_dir = os.path.dirname(os.path.abspath(__file__))

    print("=" * 80)
    print("FULL ETL PIPELINE STARTED")
    print("=" * 80)
    print(f"Working directory: {base_dir}")
    print(f"Python executable: {sys.executable}")

    if not check_scripts_exist(base_dir):
        sys.exit(1)

    total_steps = len(ETL_SCRIPTS)

    for index, script_name in enumerate(ETL_SCRIPTS, start=1):
        success = run_script(base_dir, script_name, index, total_steps)

        if not success:
            sys.exit(1)

    total_elapsed = time.perf_counter() - total_start

    print("\n" + "=" * 80)
    print("FULL ETL PIPELINE COMPLETED SUCCESSFULLY")
    print("=" * 80)
    print(f"Total elapsed time: {total_elapsed:.2f} seconds")

    print("\nExpected final TSV files:")
    final_files = [
        "article_types.tsv",
        "articles.tsv",
        "authors.tsv",
        "article_authors.tsv",
        "publishers.tsv",
        "journals.tsv",
        "journal_articles.tsv",
        "journal_rankings.tsv",
        "best_subject_areas.tsv",
        "conferences.tsv",
        "conference_articles.tsv",
        "conference_rankings.tsv",
        "primaryFoR_categories.tsv",
    ]

    for file_name in final_files:
        file_path = os.path.join(base_dir, file_name)
        status = "OK" if os.path.exists(file_path) else "MISSING"
        print(f"  [{status}] {file_name}")


if __name__ == "__main__":
    main()