import argparse
import csv
import os
import re
import sys
import time
import unicodedata
from collections import Counter, defaultdict

try:
    from tqdm import tqdm
except ImportError:  # fallback αν δεν υπάρχει εγκατεστημένο το tqdm
    def tqdm(iterable=None, **kwargs):
        return iterable if iterable is not None else []


# ============================================================
# Default configuration
# ============================================================

DEFAULT_INPUT_ARTICLES_FILE = "input_article.tsv"
DEFAULT_JOURNALS_FILE = "journals.tsv"
DEFAULT_JOURNAL_ALIASES_FILE = "journal_aliases.tsv"  # optional
DEFAULT_OUTPUT_DIR = "."

OUTPUT_AUTHORS = "authors.tsv"
OUTPUT_ARTICLE_TYPES = "article_types.tsv"
OUTPUT_ARTICLES = "articles.tsv"
OUTPUT_ARTICLE_AUTHORS = "article_authors.tsv"
OUTPUT_JOURNAL_ARTICLES = "journal_articles.tsv"

TYPE_ID_JOURNAL = 1
TYPE_NAME_JOURNAL = "journal"

UNMATCHED_JOURNAL_ID = 999999999
NO_AUTHOR_ID = 999999999
NO_AUTHOR_NAME = "NO AUTHORS"
DEFAULT_SOURCE_ID = 1

ENCODING = "utf-8"
# Safe CSV field size limit for Windows.
# sys.maxsize can be too large for csv.field_size_limit() on Windows,
# so we reduce it until Python accepts the value.
max_csv_field_size = sys.maxsize

while True:
    try:
        csv.field_size_limit(max_csv_field_size)
        break
    except OverflowError:
        max_csv_field_size = int(max_csv_field_size / 10)

SERIES_PATTERNS = [
    r"\bser\.?\s+[ab]\b",
    r"\bseries\s+[ab]\b",
]

LEADING_GENERIC_PATTERNS = [
    r"^\s*journal\s+of\s+",
    r"^\s*j\.?\s+of\s+",
    r"^\s*j\.?\s+",
    r"^\s*international\s+journal\s+of\s+",
    r"^\s*international\s+journal\s+",
    r"^\s*i\.?\s*j\.?\s+of\s+",
    r"^\s*i\.?\s*j\.?\s+",
]

STOPWORDS = {
    "of", "on", "and", "the", "for", "in", "to", "a", "an",
    "de", "la", "et", "with", "und", "der", "den", "das"
}


# ============================================================
# General helpers
# ============================================================

def clean_value(value):
    if value is None:
        return ""
    return str(value).strip()


def safe_int(value, default=None):
    value = clean_value(value)
    if not value:
        return default
    try:
        return int(value)
    except Exception:
        return default


def read_tsv(path):
    with open(path, "r", encoding=ENCODING, newline="") as f:
        reader = csv.DictReader(f, delimiter="\t")
        rows = list(reader)
        return rows, reader.fieldnames or []


def write_tsv(path, fieldnames, rows):
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with open(path, "w", encoding=ENCODING, newline="") as f:
        writer = csv.DictWriter(
            f,
            fieldnames=fieldnames,
            delimiter="\t",
            extrasaction="ignore"
        )
        writer.writeheader()
        for row in rows:
            writer.writerow(row)


def detect_column(fieldnames, candidates, required=True):
    lower_map = {f.casefold(): f for f in fieldnames}
    for candidate in candidates:
        if candidate.casefold() in lower_map:
            return lower_map[candidate.casefold()]

    if required:
        raise ValueError(
            f"Missing required column. Expected one of {candidates}. "
            f"Available columns: {fieldnames}"
        )

    return None


def should_skip_article_title(title):
    title = clean_value(title)

    if not title:
        return True, "missing_title"

    if title == "…":
        return True, "ellipsis_title"

    if len(title) >= 2 and title[0] == "(" and title[-1] == ")":
        return True, "parenthesized_title"

    return False, ""


def output_path(output_dir, filename):
    return os.path.join(output_dir, filename)


# ============================================================
# Text normalization helpers
# ============================================================

def normalize_unicode(text):
    text = unicodedata.normalize("NFKD", text)
    return "".join(ch for ch in text if not unicodedata.combining(ch))


def strip_subtitle(text):
    text = clean_value(text)
    if ":" in text:
        text = text.split(":", 1)[0]
    return text.strip()


def extract_series_letter(text):
    text = clean_value(text)
    if not text:
        return ""

    match = re.search(r"\b(?:ser\.?|series)\s+([ab])\b", text, flags=re.IGNORECASE)
    if match:
        return match.group(1).upper()

    return ""


def strip_series_only(text):
    text = clean_value(text)
    if not text:
        return ""

    text = re.sub(r"\b(?:ser\.?|series)\s+[ab]\b", " ", text, flags=re.IGNORECASE)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def strip_leading_journal_of(text):
    text = clean_value(text)
    for pattern in LEADING_GENERIC_PATTERNS:
        text = re.sub(pattern, "", text, flags=re.IGNORECASE)
    return text.strip()


def keep_left_side_of_slash(text):
    text = clean_value(text)
    text = re.sub(r"\b([A-Za-z0-9]+)\s*/\s*([A-Za-z0-9]+)\b", r"\1", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def replace_symbols_for_tokenization(text):
    text = clean_value(text)
    replacements = {
        "&": " ",
        "/": " ",
        "-": " ",
        ",": " ",
        ";": " ",
        ":": " ",
        "'": "",
        '"': "",
        "+": " ",
        "=": " ",
        "*": " ",
        "!": " ",
        "?": " ",
        "(": " ",
        ")": " ",
        "[": " ",
        "]": " ",
        "{": " ",
        "}": " ",
    }

    for old, new in replacements.items():
        text = text.replace(old, new)

    text = re.sub(r"\s+", " ", text).strip()
    return text


def extract_words(text):
    text = clean_value(text)
    if not text:
        return []

    words = []
    for raw in text.split():
        token = re.sub(r"^[^\w]+|[^\w.]+$", "", raw)
        token = token.strip()
        if token:
            words.append(token)

    return words


def is_capitalized_word(word):
    if not word:
        return False
    first_char = word[0]
    return first_char.isalpha() and first_char.isupper()


def extract_capitalized_words(text):
    return [word for word in extract_words(text) if is_capitalized_word(word)]


def normalize_token_basic(token):
    token = clean_value(token)
    if not token:
        return ""

    token = normalize_unicode(token)
    token = token.casefold()
    token = token.strip(".")
    token = re.sub(r"[^a-z0-9]+", "", token)

    if not token:
        return ""

    if token in STOPWORDS:
        return ""

    return token


def strip_leading_allcaps_prefixes(text):
    """
    Afairoume prefix typou 'IEEE', 'ACM' klp mono otan akolouthei kanonikos titlos.
    Etsi den xalaei ena journal pou einai olo all-caps.
    """
    text = clean_value(text)
    if not text:
        return ""

    words = extract_words(text)
    if len(words) < 2:
        return text

    def is_allcaps_piece(piece):
        piece = re.sub(r"[^A-Za-z0-9\-]", "", piece)
        if not piece:
            return False

        letters = [ch for ch in piece if ch.isalpha()]
        return bool(letters) and all(ch.isupper() for ch in letters)

    def is_allcaps_token(word):
        word = clean_value(word)
        if not word:
            return False

        parts = [part for part in word.split("/") if part]
        if not parts:
            return False

        return all(is_allcaps_piece(part) for part in parts)

    i = 0
    while i < len(words) and is_allcaps_token(words[i]):
        i += 1

    if i == 0 or i == len(words):
        return text

    remaining = words[i:]
    if not any(not is_allcaps_token(word) for word in remaining):
        return text

    return " ".join(remaining).strip()


def preprocess_phase_text(text, remove_prefix=False):
    text = clean_value(text)
    if not text:
        return ""

    if remove_prefix:
        text = strip_subtitle(text)
        text = strip_leading_journal_of(text)
        text = strip_leading_allcaps_prefixes(text)

    text = keep_left_side_of_slash(text)
    text = replace_symbols_for_tokenization(text)
    return text


# ============================================================
# Normalization variants
# ============================================================

def fast_normalize_tokens(text, remove_prefix=False):
    text = preprocess_phase_text(text, remove_prefix=remove_prefix)
    if not text:
        return [], "", ""

    tokens = []
    initials = []

    for word in extract_words(text):
        token = normalize_token_basic(word)
        if token:
            tokens.append(token)
            initials.append(token[0])

    return tokens, " ".join(tokens), "".join(initials)


def build_fast_variants(text):
    variants = []

    for remove_prefix in (False, True):
        tokens, norm_key, initials = fast_normalize_tokens(text, remove_prefix=remove_prefix)
        if tokens:
            variants.append({
                "remove_prefix": remove_prefix,
                "tokens": tokens,
                "norm_key": norm_key,
                "initials": initials,
            })

    # Kratame mono monadika variants gia na min kanoume diplous elegxous.
    seen = set()
    unique_variants = []
    for variant in variants:
        key = (
            variant["remove_prefix"],
            tuple(variant["tokens"]),
            variant["norm_key"],
            variant["initials"],
        )
        if key not in seen:
            seen.add(key)
            unique_variants.append(variant)

    return unique_variants


# ============================================================
# Acronym handling
# ============================================================

def extract_acronym_candidate(text):
    raw = clean_value(text)
    if not raw:
        return ""

    raw = raw.replace(".", "").replace("-", "").replace(" ", "").strip()
    if not raw:
        return ""

    if re.fullmatch(r"[A-Z0-9]{2,15}", raw):
        return raw

    return ""


def build_acronym_from_capitalized_words(text, remove_prefix=False):
    preprocessed = preprocess_phase_text(text, remove_prefix=remove_prefix)
    capitalized_words = extract_capitalized_words(preprocessed)

    if not capitalized_words:
        return "", []

    acronym = "".join(
        word[0].upper()
        for word in capitalized_words
        if word and word[0].isalpha()
    )

    return acronym, capitalized_words


def acronym_matches_exact_word_count(article_journal_raw, journal_title, remove_prefix=False):
    acronym = extract_acronym_candidate(article_journal_raw)
    if not acronym:
        return False, "", []

    journal_acronym, capitalized_words = build_acronym_from_capitalized_words(
        journal_title,
        remove_prefix=remove_prefix
    )

    if not journal_acronym:
        return False, "", capitalized_words

    if len(capitalized_words) != len(acronym):
        return False, journal_acronym, capitalized_words

    if journal_acronym.upper() != acronym.upper():
        return False, journal_acronym, capitalized_words

    return True, journal_acronym, capitalized_words


# ============================================================
# Embedded acronym handling
# ============================================================

def is_allcaps_token_strict(token):
    token = clean_value(token)
    if not token:
        return False

    token = token.replace(".", "").strip()

    if not re.fullmatch(r"[A-Z0-9]{2,15}", token):
        return False

    return any(ch.isalpha() for ch in token)


def build_mixed_tokens_for_embedded_acronym(text, remove_prefix=False):
    text = preprocess_phase_text(text, remove_prefix=remove_prefix)
    if not text:
        return []

    tokens = []
    for word in extract_words(text):
        raw = clean_value(word).strip()
        raw_no_dots = raw.replace(".", "")

        if is_allcaps_token_strict(raw_no_dots):
            tokens.append(raw_no_dots.upper())
            continue

        token = normalize_token_basic(raw)
        if token:
            tokens.append(token)

    return tokens


def token_prefix_match(a, b):
    if not a or not b:
        return False

    shorter = a if len(a) <= len(b) else b
    longer = b if len(a) <= len(b) else a
    return longer.startswith(shorter)


def contains_embedded_allcaps_among_mixed(tokens):
    if not tokens:
        return False

    has_allcaps = any(is_allcaps_token_strict(token) for token in tokens)
    has_normal = any(not is_allcaps_token_strict(token) for token in tokens)
    return has_allcaps and has_normal


def embedded_acronym_sequence_match(article_tokens, journal_tokens):
    if not article_tokens or not journal_tokens:
        return False

    i = 0
    j = 0

    while i < len(article_tokens) and j < len(journal_tokens):
        article_token = article_tokens[i]

        if is_allcaps_token_strict(article_token):
            letters = list(article_token.casefold())
            needed_count = len(letters)

            if j + needed_count > len(journal_tokens):
                return False

            candidate = journal_tokens[j:j + needed_count]
            initials = [token[0].casefold() for token in candidate if token]

            if len(initials) != needed_count:
                return False

            if initials != letters:
                return False

            i += 1
            j += needed_count
            continue

        journal_token = journal_tokens[j]
        if token_prefix_match(article_token, journal_token):
            i += 1
            j += 1
            continue

        return False

    return i == len(article_tokens) and j == len(journal_tokens)


# ============================================================
# Token comparison
# ============================================================

def compare_tokens_prefixwise(tokens1, tokens2):
    if not tokens1 or not tokens2:
        return False

    if len(tokens1) != len(tokens2):
        return False

    for a, b in zip(tokens1, tokens2):
        if not token_prefix_match(a, b):
            return False

    return True


# ============================================================
# Input loaders
# ============================================================

def load_journals(path):
    rows, fieldnames = read_tsv(path)

    col_id = detect_column(fieldnames, ["journal_id", "id"])
    col_name = detect_column(fieldnames, ["journal_name", "name"])
    col_publisher = detect_column(fieldnames, ["publisher_id"], required=False)

    journals = []
    stats = Counter()

    for row in tqdm(rows, desc="Loading journals", unit="row"):
        journal_id = safe_int(row.get(col_id))
        journal_name = clean_value(row.get(col_name))
        publisher_id = clean_value(row.get(col_publisher)) if col_publisher else ""

        if journal_id is None:
            stats["skipped_missing_id"] += 1
            continue

        if not journal_name:
            stats["skipped_missing_name"] += 1
            continue

        journals.append({
            "journal_id": journal_id,
            "journal_name": journal_name,
            "publisher_id": publisher_id,
        })

    stats["loaded"] = len(journals)
    return journals, stats


def load_articles(path):
    rows, fieldnames = read_tsv(path)

    required_candidates = {
        "article_id": ["id", "article_id"],
        "author": ["author"],
        "ee": ["ee"],
        "articlekey": ["key", "articlekey"],
        "mdate": ["mdate"],
        "pages": ["pages"],
        "title": ["title"],
        "year": ["year"],
        "url": ["url"],
        "journal": ["journal"],
        "volume": ["volume"],
        "number": ["number"],
    }

    cols = {}
    for logical_name, candidates in required_candidates.items():
        cols[logical_name] = detect_column(
            fieldnames,
            candidates,
            required=(logical_name in {"article_id", "title", "year"})
        )

    articles = []
    stats = Counter()

    for row in tqdm(rows, desc="Loading articles", unit="row"):
        article_id = safe_int(row.get(cols["article_id"]))
        if article_id is None:
            stats["skipped_missing_id"] += 1
            continue

        title = clean_value(row.get(cols["title"]))
        skip_title, reason = should_skip_article_title(title)
        if skip_title:
            stats[f"skipped_{reason}"] += 1
            continue

        articles.append({
            "article_id": article_id,
            "author": clean_value(row.get(cols["author"])),
            "ee": clean_value(row.get(cols["ee"])),
            "articlekey": clean_value(row.get(cols["articlekey"])),
            "mdate": clean_value(row.get(cols["mdate"])),
            "pages": clean_value(row.get(cols["pages"])),
            "title": title,
            "year": safe_int(row.get(cols["year"])),
            "url": clean_value(row.get(cols["url"])),
            "journal": clean_value(row.get(cols["journal"])),
            "volume": clean_value(row.get(cols["volume"])),
            "number": clean_value(row.get(cols["number"])),
        })

    stats["loaded"] = len(articles)
    return articles, stats


def load_aliases(path, journals_by_id, journals_by_name_exact):
    if not path or not os.path.exists(path):
        return [], Counter({"loaded": 0, "file_missing": 1})

    rows, fieldnames = read_tsv(path)
    if not fieldnames:
        return [], Counter({"loaded": 0, "empty_file": 1})

    lower_map = {field.casefold(): field for field in fieldnames}

    col_journal_id = lower_map.get("journal_id") or lower_map.get("id")
    col_journal_name = lower_map.get("journal_name")
    col_alias = (
        lower_map.get("alias")
        or lower_map.get("journal_alias")
        or lower_map.get("alias_name")
    )
    col_name = lower_map.get("name")

    # Υποστήριξη για απλό TSV 2 στηλών χωρίς αυστηρά ονόματα headers.
    if col_alias is None and len(fieldnames) == 2:
        first, second = fieldnames[0], fieldnames[1]
        first_lower, second_lower = first.casefold(), second.casefold()

        if first_lower in ("journal_id", "id"):
            col_journal_id = first
            col_alias = second
        elif second_lower in ("journal_id", "id"):
            col_journal_id = second
            col_alias = first
        elif first_lower in ("journal_name", "name"):
            col_journal_name = first
            col_alias = second
        elif second_lower in ("journal_name", "name"):
            col_journal_name = second
            col_alias = first

    aliases = []
    stats = Counter()

    for row in tqdm(rows, desc="Loading aliases", unit="row"):
        alias_text = ""
        journal_id = None
        journal_name = ""

        if col_alias:
            alias_text = clean_value(row.get(col_alias))
        elif col_name and not col_journal_name:
            alias_text = clean_value(row.get(col_name))

        if col_journal_id:
            journal_id = safe_int(row.get(col_journal_id))

        if col_journal_name:
            journal_name = clean_value(row.get(col_journal_name))

        if journal_id is None and journal_name:
            journal_id = journals_by_name_exact.get(journal_name.casefold())

        if journal_id is None and col_name and col_alias and not journal_name:
            possible_name = clean_value(row.get(col_name))
            journal_id = journals_by_name_exact.get(possible_name.casefold())

        if journal_id is None:
            stats["skipped_missing_journal_id"] += 1
            continue

        if journal_id not in journals_by_id:
            stats["skipped_unknown_journal_id"] += 1
            continue

        if not alias_text:
            stats["skipped_missing_alias"] += 1
            continue

        aliases.append({
            "journal_id": journal_id,
            "alias": alias_text,
        })

    stats["loaded"] = len(aliases)
    return aliases, stats


# ============================================================
# Journal matcher
# ============================================================

class JournalMatcher:
    def __init__(self, journals, aliases=None):
        self.journals = journals
        self.aliases = aliases or []

        self.journals_by_id = {
            journal["journal_id"]: journal
            for journal in journals
        }
        self.journals_by_name_exact = {
            clean_value(journal["journal_name"]).casefold(): journal["journal_id"]
            for journal in journals
        }

        self.used_journal_ids = set()
        self.cache = {}

        self.alias_exact = {}
        self.normkey_index = defaultdict(set)
        self.initials_index = defaultdict(set)
        self.token_count_index = defaultdict(set)
        self.journal_repr = defaultdict(list)
        self.acronym_index_before = defaultdict(set)
        self.acronym_index_after = defaultdict(set)

        self._build_indices()

    def _add_text(self, journal_id, text, is_alias=False):
        raw_key = clean_value(text).casefold()
        if is_alias and raw_key:
            self.alias_exact[raw_key] = journal_id

        for variant in build_fast_variants(text):
            tokens = variant["tokens"]
            norm_key = variant["norm_key"]
            initials = variant["initials"]
            remove_prefix = variant["remove_prefix"]

            self.normkey_index[(remove_prefix, norm_key)].add(journal_id)
            self.initials_index[(remove_prefix, initials, len(tokens))].add(journal_id)
            self.token_count_index[(remove_prefix, len(tokens))].add(journal_id)
            self.journal_repr[journal_id].append({
                "remove_prefix": remove_prefix,
                "tokens": tokens,
            })

        acronym_before, words_before = build_acronym_from_capitalized_words(
            text,
            remove_prefix=False
        )
        if acronym_before and words_before:
            self.acronym_index_before[acronym_before].add(journal_id)

        acronym_after, words_after = build_acronym_from_capitalized_words(
            text,
            remove_prefix=True
        )
        if acronym_after and words_after:
            self.acronym_index_after[acronym_after].add(journal_id)

    def _build_indices(self):
        for journal in tqdm(self.journals, desc="Indexing journals", unit="journal"):
            self._add_text(journal["journal_id"], journal["journal_name"], is_alias=False)

        for alias in tqdm(self.aliases, desc="Indexing aliases", unit="alias"):
            self._add_text(alias["journal_id"], alias["alias"], is_alias=True)

    def _result(self, matched, journal_raw, method, journal_id=None, journal_name="", score="", details=""):
        return {
            "matched": matched,
            "journal_raw": journal_raw,
            "journal_id": journal_id if journal_id is not None else UNMATCHED_JOURNAL_ID,
            "journal_name": journal_name,
            "method": method,
            "score": score,
            "details": details,
        }

    def _acronym_match_pass(self, raw_text, remove_prefix):
        acronym = extract_acronym_candidate(raw_text)
        if not acronym:
            return None

        index = self.acronym_index_after if remove_prefix else self.acronym_index_before
        candidate_ids = index.get(acronym, set())
        valid_ids = set()

        for journal_id in candidate_ids:
            title = self.journals_by_id[journal_id]["journal_name"]
            ok, _built, _cap_words = acronym_matches_exact_word_count(
                raw_text,
                title,
                remove_prefix=remove_prefix
            )
            if ok:
                valid_ids.add(journal_id)

        if len(valid_ids) == 1:
            journal_id = next(iter(valid_ids))
            self.used_journal_ids.add(journal_id)
            return self._result(
                True,
                raw_text,
                "acronym_match_after_prefix_removal" if remove_prefix else "acronym_match_before_prefix_removal",
                journal_id=journal_id,
                journal_name=self.journals_by_id[journal_id]["journal_name"],
                score=0.98,
                details=f"acronym={acronym}"
            )

        if len(valid_ids) > 1:
            return self._result(
                False,
                raw_text,
                "ambiguous_acronym_match_after_prefix_removal" if remove_prefix else "ambiguous_acronym_match_before_prefix_removal",
                details=f"acronym={acronym}; candidates={self._format_candidates(valid_ids)}"
            )

        return None

    def _series_sensitive_match_pass(self, raw_text, remove_prefix):
        article_series = extract_series_letter(raw_text)
        if not article_series:
            return None

        article_base = strip_series_only(raw_text)
        article_variants = [
            variant
            for variant in build_fast_variants(article_base)
            if variant["remove_prefix"] == remove_prefix
        ]
        if not article_variants:
            return None

        found_ids = set()

        for journal_id, journal in self.journals_by_id.items():
            journal_name = journal["journal_name"]
            journal_series = extract_series_letter(journal_name)

            if journal_series != article_series:
                continue

            journal_base = strip_series_only(journal_name)
            journal_variants = [
                variant
                for variant in build_fast_variants(journal_base)
                if variant["remove_prefix"] == remove_prefix
            ]

            found = False
            for article_variant in article_variants:
                for journal_variant in journal_variants:
                    if compare_tokens_prefixwise(article_variant["tokens"], journal_variant["tokens"]):
                        found = True
                        break
                if found:
                    break

            if found:
                found_ids.add(journal_id)

        if len(found_ids) == 1:
            journal_id = next(iter(found_ids))
            self.used_journal_ids.add(journal_id)
            return self._result(
                True,
                raw_text,
                "series_sensitive_match_after_prefix_removal" if remove_prefix else "series_sensitive_match_before_prefix_removal",
                journal_id=journal_id,
                journal_name=self.journals_by_id[journal_id]["journal_name"],
                score=0.997,
                details=f"series={article_series}"
            )

        if len(found_ids) > 1:
            return self._result(
                False,
                raw_text,
                "ambiguous_series_sensitive_match_after_prefix_removal" if remove_prefix else "ambiguous_series_sensitive_match_before_prefix_removal",
                details=f"series={article_series}; candidates={self._format_candidates(found_ids)}"
            )

        return None

    def _normalized_key_match_pass(self, raw_text, remove_prefix):
        variants = [
            variant
            for variant in build_fast_variants(raw_text)
            if variant["remove_prefix"] == remove_prefix
        ]

        found_ids = set()
        found_norm_keys = set()

        for variant in variants:
            norm_key = variant["norm_key"]
            ids = self.normkey_index.get((remove_prefix, norm_key), set())

            if ids:
                found_ids.update(ids)
                found_norm_keys.add(norm_key)

        if len(found_ids) == 1:
            journal_id = next(iter(found_ids))
            self.used_journal_ids.add(journal_id)
            return self._result(
                True,
                raw_text,
                "normalized_exact_key_after_prefix_removal" if remove_prefix else "normalized_exact_key_before_prefix_removal",
                journal_id=journal_id,
                journal_name=self.journals_by_id[journal_id]["journal_name"],
                score=0.99,
                details=f"norm_key={', '.join(sorted(found_norm_keys))}"
            )

        if len(found_ids) > 1:
            return self._result(
                False,
                raw_text,
                "ambiguous_normalized_exact_key_after_prefix_removal" if remove_prefix else "ambiguous_normalized_exact_key_before_prefix_removal",
                details=f"candidates={self._format_candidates(found_ids)}"
            )

        return None

    def _embedded_acronym_match_pass_phase2(self, raw_text):
        article_tokens = build_mixed_tokens_for_embedded_acronym(raw_text, remove_prefix=True)
        if not article_tokens:
            return None

        if not contains_embedded_allcaps_among_mixed(article_tokens):
            return None

        approx_len = 0
        for token in article_tokens:
            if is_allcaps_token_strict(token):
                approx_len += len(token)
            else:
                approx_len += 1

        candidate_ids = set(self.token_count_index.get((True, approx_len), set()))
        if not candidate_ids:
            for delta in (1, 2):
                candidate_ids = set()
                candidate_ids.update(self.token_count_index.get((True, approx_len - delta), set()))
                candidate_ids.update(self.token_count_index.get((True, approx_len + delta), set()))
                if candidate_ids:
                    break

        found_ids = set()

        for journal_id in candidate_ids:
            for representation in self.journal_repr.get(journal_id, []):
                if representation["remove_prefix"] is not True:
                    continue

                journal_tokens = representation["tokens"]
                if embedded_acronym_sequence_match(article_tokens, journal_tokens):
                    found_ids.add(journal_id)
                    break

        if len(found_ids) == 1:
            journal_id = next(iter(found_ids))
            self.used_journal_ids.add(journal_id)
            return self._result(
                True,
                raw_text,
                "embedded_allcaps_acronym_match_after_prefix_removal",
                journal_id=journal_id,
                journal_name=self.journals_by_id[journal_id]["journal_name"],
                score=0.965,
                details="phase2 mixed-allcaps sequence match"
            )

        if len(found_ids) > 1:
            return self._result(
                False,
                raw_text,
                "ambiguous_embedded_allcaps_acronym_match_after_prefix_removal",
                details=f"candidates={self._format_candidates(found_ids)}"
            )

        return None

    def _prefixwise_match_pass(self, raw_text, remove_prefix):
        variants = [
            variant
            for variant in build_fast_variants(raw_text)
            if variant["remove_prefix"] == remove_prefix
        ]

        found_ids = set()

        for variant in variants:
            tokens = variant["tokens"]
            initials = variant["initials"]
            token_count = len(tokens)

            candidate_ids = self.initials_index.get((remove_prefix, initials, token_count), set())
            if not candidate_ids:
                candidate_ids = self.token_count_index.get((remove_prefix, token_count), set())

            for journal_id in candidate_ids:
                for representation in self.journal_repr.get(journal_id, []):
                    if representation["remove_prefix"] != remove_prefix:
                        continue

                    journal_tokens = representation["tokens"]
                    if compare_tokens_prefixwise(tokens, journal_tokens):
                        found_ids.add(journal_id)
                        break

        if len(found_ids) == 1:
            journal_id = next(iter(found_ids))
            self.used_journal_ids.add(journal_id)
            return self._result(
                True,
                raw_text,
                "prefixwise_match_after_prefix_removal" if remove_prefix else "prefixwise_match_before_prefix_removal",
                journal_id=journal_id,
                journal_name=self.journals_by_id[journal_id]["journal_name"],
                score=0.95,
            )

        if len(found_ids) > 1:
            return self._result(
                False,
                raw_text,
                "ambiguous_prefixwise_match_after_prefix_removal" if remove_prefix else "ambiguous_prefixwise_match_before_prefix_removal",
                details=f"candidates={self._format_candidates(found_ids)}"
            )

        return None

    def _format_candidates(self, journal_ids, limit=5):
        return ", ".join(
            f"{journal_id}:{self.journals_by_id[journal_id]['journal_name']}"
            for journal_id in sorted(journal_ids)[:limit]
        )

    def match(self, raw_text):
        raw_text = clean_value(raw_text)

        if raw_text in self.cache:
            return self.cache[raw_text]

        if not raw_text:
            result = self._result(False, raw_text, "empty_journal", details="Empty journal field")
            self.cache[raw_text] = result
            return result

        raw_lower = raw_text.casefold()

        if raw_lower in self.alias_exact:
            journal_id = self.alias_exact[raw_lower]
            self.used_journal_ids.add(journal_id)
            result = self._result(
                True,
                raw_text,
                "alias_exact_raw",
                journal_id=journal_id,
                journal_name=self.journals_by_id[journal_id]["journal_name"],
                score=1.0,
            )
            self.cache[raw_text] = result
            return result

        if raw_lower in self.journals_by_name_exact:
            journal_id = self.journals_by_name_exact[raw_lower]
            self.used_journal_ids.add(journal_id)
            result = self._result(
                True,
                raw_text,
                "official_exact_raw",
                journal_id=journal_id,
                journal_name=self.journals_by_id[journal_id]["journal_name"],
                score=1.0,
            )
            self.cache[raw_text] = result
            return result

        passes = [
            lambda: self._acronym_match_pass(raw_text, remove_prefix=False),
            lambda: self._series_sensitive_match_pass(raw_text, remove_prefix=False),
            lambda: self._normalized_key_match_pass(raw_text, remove_prefix=False),
            lambda: self._prefixwise_match_pass(raw_text, remove_prefix=False),
            lambda: self._acronym_match_pass(raw_text, remove_prefix=True),
            lambda: self._series_sensitive_match_pass(raw_text, remove_prefix=True),
            lambda: self._normalized_key_match_pass(raw_text, remove_prefix=True),
            lambda: self._embedded_acronym_match_pass_phase2(raw_text),
            lambda: self._prefixwise_match_pass(raw_text, remove_prefix=True),
        ]

        for match_pass in passes:
            result = match_pass()
            if result is not None:
                self.cache[raw_text] = result
                return result

        result = self._result(False, raw_text, "no_match", details="No reliable candidate found")
        self.cache[raw_text] = result
        return result


# ============================================================
# ETL builders
# ============================================================

def split_authors(author_field):
    author_field = clean_value(author_field)
    if not author_field:
        return []

    return [clean_value(part) for part in author_field.split("|") if clean_value(part)]


def build_journal_match_map(articles, matcher):
    unique_journals = sorted({clean_value(article["journal"]) for article in articles})
    journal_match_map = {}

    for raw_journal in tqdm(unique_journals, desc="Matching unique journals", unit="journal"):
        journal_match_map[raw_journal] = matcher.match(raw_journal)

    return journal_match_map


def build_outputs(articles, journal_match_map, include_no_author_link=False):
    authors_rows = []
    article_types_rows = [{"type_id": TYPE_ID_JOURNAL, "type_name": TYPE_NAME_JOURNAL}]
    articles_rows = []
    article_authors_rows = []
    journal_articles_rows = []

    author_to_id = {}
    next_author_id = 1

    stats = Counter()
    unmatched_reason_counter = Counter()
    match_method_counter = Counter()

    for article in tqdm(articles, desc="Building output rows", unit="article"):
        article_id = article["article_id"]
        raw_journal = clean_value(article["journal"])
        match_result = journal_match_map[raw_journal]

        articles_rows.append({
            "article_id": article_id,
            "ee": article["ee"],
            "articlekey": article["articlekey"],
            "mdate": article["mdate"],
            "pages": article["pages"],
            "title": article["title"],
            "year": article["year"] if article["year"] is not None else "",
            "type_id": TYPE_ID_JOURNAL,
            "url": article["url"],
        })

        article_authors = split_authors(article["author"])
        if not article_authors:
            stats["articles_without_authors"] += 1
            if include_no_author_link:
                article_authors_rows.append({
                    "article_id": article_id,
                    "author_id": NO_AUTHOR_ID,
                })
        else:
            seen_authors_for_article = set()
            for author_name in article_authors:
                if author_name not in author_to_id:
                    author_to_id[author_name] = next_author_id
                    authors_rows.append({
                        "author_id": next_author_id,
                        "author_name": author_name,
                    })
                    next_author_id += 1

                author_id = author_to_id[author_name]
                if author_id not in seen_authors_for_article:
                    article_authors_rows.append({
                        "article_id": article_id,
                        "author_id": author_id,
                    })
                    seen_authors_for_article.add(author_id)

        journal_articles_rows.append({
            "article_id": article_id,
            "journal_id": match_result["journal_id"],
            "source_id": DEFAULT_SOURCE_ID,
            "volume": article["volume"],
            "number": article["number"],
        })

        match_method_counter[match_result["method"]] += 1

        if match_result["matched"]:
            stats["matched_articles"] += 1
        else:
            stats["unmatched_articles"] += 1
            unmatched_reason_counter[match_result["method"]] += 1

    # Kratame to NO AUTHORS row gia symvatotita me ta import scripts / schema.
    authors_rows.append({
        "author_id": NO_AUTHOR_ID,
        "author_name": NO_AUTHOR_NAME,
    })

    stats["authors"] = len(authors_rows)
    stats["articles"] = len(articles_rows)
    stats["article_author_links"] = len(article_authors_rows)
    stats["journal_article_links"] = len(journal_articles_rows)
    stats["unique_raw_journals"] = len(journal_match_map)

    return {
        "authors_rows": authors_rows,
        "article_types_rows": article_types_rows,
        "articles_rows": articles_rows,
        "article_authors_rows": article_authors_rows,
        "journal_articles_rows": journal_articles_rows,
        "stats": stats,
        "unmatched_reason_counter": unmatched_reason_counter,
        "match_method_counter": match_method_counter,
    }


def write_outputs(outputs, output_dir):
    files_written = []

    output_specs = [
        (
            OUTPUT_AUTHORS,
            ["author_id", "author_name"],
            outputs["authors_rows"],
        ),
        (
            OUTPUT_ARTICLE_TYPES,
            ["type_id", "type_name"],
            outputs["article_types_rows"],
        ),
        (
            OUTPUT_ARTICLES,
            ["article_id", "ee", "articlekey", "mdate", "pages", "title", "year", "type_id", "url"],
            outputs["articles_rows"],
        ),
        (
            OUTPUT_ARTICLE_AUTHORS,
            ["article_id", "author_id"],
            outputs["article_authors_rows"],
        ),
        (
            OUTPUT_JOURNAL_ARTICLES,
            ["article_id", "journal_id", "source_id", "volume", "number"],
            outputs["journal_articles_rows"],
        ),
    ]

    for filename, fieldnames, rows in tqdm(output_specs, desc="Writing TSV files", unit="file"):
        path = output_path(output_dir, filename)
        write_tsv(path, fieldnames, rows)
        files_written.append((filename, path, len(rows)))

    return files_written


# ============================================================
# Validation / summary
# ============================================================

def ensure_input_files(input_articles_file, journals_file):
    missing = []
    for path in [input_articles_file, journals_file]:
        if not os.path.exists(path):
            missing.append(path)

    if missing:
        raise FileNotFoundError(f"Missing required input files: {missing}")


def print_counter(counter, title, limit=None):
    if not counter:
        print(f"{title}: none")
        return

    print(title)
    print("-" * 80)
    items = counter.most_common(limit)
    for key, value in items:
        print(f"{key:55s} {value}")


def print_final_summary(
    elapsed_seconds,
    journals,
    journal_stats,
    article_stats,
    alias_stats,
    matcher,
    outputs,
    files_written,
):
    stats = outputs["stats"]
    total_articles = stats["articles"]
    matched_articles = stats["matched_articles"]
    unmatched_articles = stats["unmatched_articles"]
    match_pct = (matched_articles / total_articles * 100.0) if total_articles else 0.0

    print("\n" + "=" * 80)
    print("ETL SUMMARY")
    print("=" * 80)
    print(f"Elapsed time                         : {elapsed_seconds:.2f} sec")
    print(f"Journals loaded                      : {journal_stats['loaded']}")
    print(f"Articles loaded after filtering      : {article_stats['loaded']}")
    print(f"Aliases loaded                       : {alias_stats['loaded']}")
    print(f"Unique raw journal strings           : {stats['unique_raw_journals']}")
    print(f"Matcher cache entries                : {len(matcher.cache)}")
    print(f"Used journals                        : {len(matcher.used_journal_ids)} / {len(journals)}")
    print(f"Matched articles                     : {matched_articles} ({match_pct:.2f}%)")
    print(f"Unmatched articles                   : {unmatched_articles}")
    print(f"Articles without authors             : {stats['articles_without_authors']}")

    print("\nInput filtering")
    print("-" * 80)
    print(f"Articles skipped missing id          : {article_stats['skipped_missing_id']}")
    print(f"Articles skipped missing title       : {article_stats['skipped_missing_title']}")
    print(f"Articles skipped title='...'         : {article_stats['skipped_ellipsis_title']}")
    print(f"Articles skipped title='(...)'       : {article_stats['skipped_parenthesized_title']}")
    print(f"Journals skipped missing id          : {journal_stats['skipped_missing_id']}")
    print(f"Journals skipped missing name        : {journal_stats['skipped_missing_name']}")

    print("\nOutput row counts")
    print("-" * 80)
    print(f"{OUTPUT_AUTHORS:30s} {stats['authors']}")
    print(f"{OUTPUT_ARTICLE_TYPES:30s} {len(outputs['article_types_rows'])}")
    print(f"{OUTPUT_ARTICLES:30s} {stats['articles']}")
    print(f"{OUTPUT_ARTICLE_AUTHORS:30s} {stats['article_author_links']}")
    print(f"{OUTPUT_JOURNAL_ARTICLES:30s} {stats['journal_article_links']}")

    print("\nFiles written")
    print("-" * 80)
    for filename, path, row_count in files_written:
        print(f"{filename:30s} rows={row_count:<10d} path={path}")

    print("\nTop match methods")
    print("-" * 80)
    for method, count in outputs["match_method_counter"].most_common(10):
        print(f"{method:55s} {count}")

    if outputs["unmatched_reason_counter"]:
        print("\nTop unmatched reasons")
        print("-" * 80)
        for reason, count in outputs["unmatched_reason_counter"].most_common(10):
            print(f"{reason:55s} {count}")
    else:
        print("\nTop unmatched reasons: none")

    print("=" * 80)


def parse_args():
    parser = argparse.ArgumentParser(
        description="Final ETL script for journal articles TSV files."
    )
    parser.add_argument(
        "--articles",
        default=DEFAULT_INPUT_ARTICLES_FILE,
        help=f"Input articles TSV file. Default: {DEFAULT_INPUT_ARTICLES_FILE}",
    )
    parser.add_argument(
        "--journals",
        default=DEFAULT_JOURNALS_FILE,
        help=f"Input journals TSV file. Default: {DEFAULT_JOURNALS_FILE}",
    )
    parser.add_argument(
        "--aliases",
        default=DEFAULT_JOURNAL_ALIASES_FILE,
        help=f"Optional journal aliases TSV file. Default: {DEFAULT_JOURNAL_ALIASES_FILE}",
    )
    parser.add_argument(
        "--output-dir",
        default=DEFAULT_OUTPUT_DIR,
        help=f"Directory where output TSV files will be written. Default: {DEFAULT_OUTPUT_DIR}",
    )
    parser.add_argument(
        "--link-no-author",
        action="store_true",
        help="Create article_authors rows with author_id=999999999 for articles with no author.",
    )
    return parser.parse_args()


# ============================================================
# Main
# ============================================================

def main():
    args = parse_args()
    start = time.perf_counter()

    ensure_input_files(args.articles, args.journals)
    os.makedirs(args.output_dir, exist_ok=True)

    print("Starting journal articles ETL...")
    print(f"Input articles : {args.articles}")
    print(f"Input journals : {args.journals}")
    print(f"Input aliases  : {args.aliases if args.aliases else 'disabled'}")
    print(f"Output dir     : {args.output_dir}")
    print()

    journals, journal_stats = load_journals(args.journals)
    articles, article_stats = load_articles(args.articles)

    journals_by_id = {journal["journal_id"]: journal for journal in journals}
    journals_by_name_exact = {
        journal["journal_name"].casefold(): journal["journal_id"]
        for journal in journals
    }

    aliases, alias_stats = load_aliases(
        args.aliases,
        journals_by_id,
        journals_by_name_exact,
    )

    matcher = JournalMatcher(journals, aliases=aliases)
    journal_match_map = build_journal_match_map(articles, matcher)

    outputs = build_outputs(
        articles,
        journal_match_map,
        include_no_author_link=args.link_no_author,
    )

    files_written = write_outputs(
        outputs,
        args.output_dir,
    )

    elapsed = time.perf_counter() - start
    print_final_summary(
        elapsed,
        journals,
        journal_stats,
        article_stats,
        alias_stats,
        matcher,
        outputs,
        files_written,
    )


if __name__ == "__main__":
    main()
