import csv
from pathlib import Path

def detect_delimiter_from_header(file_path: Path) -> str:
    with open(file_path, "r", encoding="utf-8-sig", newline="") as f:
        for line in f:
            line = line.strip()
            if line:
                commas = line.count(",")
                semicolons = line.count(";")
                if semicolons > commas:
                    return ";"
                return ","
    return ","

for csv_file in Path(".").glob("*.csv"):
    tsv_file = csv_file.with_suffix(".tsv")
    delimiter = detect_delimiter_from_header(csv_file)

    with open(csv_file, "r", encoding="utf-8-sig", newline="") as infile, \
         open(tsv_file, "w", encoding="utf-8", newline="") as outfile:

        reader = csv.reader(infile, delimiter=delimiter)
        writer = csv.writer(outfile, delimiter="\t", quoting=csv.QUOTE_MINIMAL)

        for row in reader:
            writer.writerow(row)

    print(f"{csv_file} ({delimiter}) -> {tsv_file}")