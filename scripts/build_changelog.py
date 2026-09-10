#!/usr/bin/env python3
"""
Builds landing/changelog/index.html directly from CHANGELOG.md.
Keeps the website changelog and GitHub release notes in sync from a single source.
"""

import html
import re
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
CHANGELOG_PATH = REPO_ROOT / "CHANGELOG.md"
HTML_PATH = REPO_ROOT / "landing" / "changelog" / "index.html"


def md_to_html(text: str) -> str:
    text = html.escape(text, quote=False)
    # Inline code
    text = re.sub(r"`([^`]+)`", r"<code>\1</code>", text)
    # Bold
    text = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", text)
    # Italic
    text = re.sub(r"\*([^*]+)\*", r"<em>\1</em>", text)
    # Links: [label](url)
    text = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", r'<a href="\2">\1</a>', text)
    # Quotes
    text = text.replace('"', "&quot;")
    return text


def parse_changelog(md_text: str):
    versions = []
    current_ver = None
    current_category = None

    lines = md_text.splitlines()
    for line in lines:
        line_clean = line.strip()
        # Match "## [1.7.0] - Title" or "## [1.7.0]"
        ver_match = re.match(r"^##\s+\[([^\]]+)\](?:\s*-\s*(.*))?$", line_clean)
        if ver_match:
            version_str = ver_match.group(1).strip()
            title_str = (ver_match.group(2) or "").strip()
            current_ver = {
                "version": version_str,
                "title": title_str,
                "sections": [],  # list of (category_name or None, [items])
            }
            versions.append(current_ver)
            current_category = None
            continue

        if not current_ver:
            continue

        # Match "### Category"
        cat_match = re.match(r"^###\s+(.+)$", line_clean)
        if cat_match:
            current_category = cat_match.group(1).strip()
            continue

        # Match "* Bullet" or "- Bullet"
        item_match = re.match(r"^[*-]\s+(.+)$", line_clean)
        if item_match:
            item_text = item_match.group(1).strip()
            # If we don't have a section for current_category, create it
            if not current_ver["sections"] or current_ver["sections"][-1][0] != current_category:
                current_ver["sections"].append((current_category, []))
            current_ver["sections"][-1][1].append(item_text)

    return versions


def render_html(versions: list) -> tuple[str, str, str]:
    if not versions:
        return "", "", ""

    latest_version = versions[0]["version"]

    # TOC
    toc_lines = []
    for idx, ver in enumerate(versions, 1):
        v_num = ver["version"]
        sec_id = f"v{v_num.replace('.', '-')}"
        toc_lines.append(f'          <li><a href="#{sec_id}"><span class="toc-num">{idx:02d}</span>v{v_num}</a></li>')
    toc_html = "\n".join(toc_lines)

    # Body sections
    body_sections = []
    for idx, ver in enumerate(versions, 1):
        v_num = ver["version"]
        sec_id = f"v{v_num.replace('.', '-')}"
        title = f" - {md_to_html(ver['title'])}" if ver["title"] else ""
        section_lines = [
            f'        <section id="{sec_id}">',
            f'          <h2><span class="sec-num">{idx:02d}.</span>v{v_num}{title}</h2>',
        ]

        for cat_name, items in ver["sections"]:
            if cat_name:
                section_lines.append(f'          <p><strong>{md_to_html(cat_name)}</strong></p>')
            section_lines.append('          <ul>')
            for item in items:
                section_lines.append(f'            <li>{md_to_html(item)}</li>')
            section_lines.append('          </ul>')

        section_lines.append('        </section>')
        body_sections.append("\n".join(section_lines))

    sections_html = "\n\n".join(body_sections)
    return latest_version, toc_html, sections_html


def update_changelog_html():
    if not CHANGELOG_PATH.exists():
        raise FileNotFoundError(f"Missing {CHANGELOG_PATH}")
    if not HTML_PATH.exists():
        raise FileNotFoundError(f"Missing {HTML_PATH}")

    changelog_md = CHANGELOG_PATH.read_text(encoding="utf-8")
    versions = parse_changelog(changelog_md)
    latest_ver, toc_html, sections_html = render_html(versions)

    html_content = HTML_PATH.read_text(encoding="utf-8")

    # 1. Update Latest version in doc-head-meta
    html_content = re.sub(
        r'(<span><b>Latest</b>\s*)v[^<]*(</span>)',
        rf'\g<1>v{latest_ver}\2',
        html_content,
    )

    # 2. Update <ol> inside <aside class="doc-toc">
    toc_pattern = re.compile(
        r'(<aside class="doc-toc">\s*<h4>Versions</h4>\s*<ol>)(.*?)(</ol>\s*</aside>)',
        re.DOTALL,
    )
    html_content = toc_pattern.sub(
        rf'\1\n{toc_html}\n        \3',
        html_content,
    )

    # 3. Update sections inside <div class="doc-body">
    body_pattern = re.compile(
        r'(<div class="doc-body">\s*<p class="doc-lede">.*?</p>\s*)(<section id=.*?</section>)\s*(</div>\s*</div>\s*</main>)',
        re.DOTALL,
    )
    html_content = body_pattern.sub(
        rf'\1\n{sections_html}\n\n      \3',
        html_content,
    )

    HTML_PATH.write_text(html_content, encoding="utf-8")
    print(f"Successfully updated {HTML_PATH} for version {latest_ver} ({len(versions)} versions).")


if __name__ == "__main__":
    update_changelog_html()
