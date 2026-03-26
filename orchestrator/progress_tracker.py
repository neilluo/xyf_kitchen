"""
Progress tracker module.

Handles logging, progress.md updates, and learnings.md appends.
"""

from __future__ import annotations

import logging
from datetime import datetime
from pathlib import Path

from .task_parser import Phase, Task


def setup_logging(log_dir: Path) -> logging.Logger:
    """Set up structured logging to file and console."""
    log_dir.mkdir(parents=True, exist_ok=True)

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    log_file = log_dir / f"orchestrator_{timestamp}.log"

    logger = logging.getLogger("orchestrator")
    logger.setLevel(logging.DEBUG)

    # File handler - detailed
    fh = logging.FileHandler(log_file, encoding="utf-8")
    fh.setLevel(logging.DEBUG)
    fh.setFormatter(logging.Formatter(
        "%(asctime)s | %(levelname)-8s | %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    ))

    # Console handler - concise
    ch = logging.StreamHandler()
    ch.setLevel(logging.INFO)
    ch.setFormatter(logging.Formatter(
        "%(asctime)s | %(message)s",
        datefmt="%H:%M:%S",
    ))

    logger.addHandler(fh)
    logger.addHandler(ch)

    return logger


def append_progress(progress_file: Path, task: Task, status: str, note: str = "") -> None:
    """Append a line to .ai/progress.md.

    Format: | date | task_id | title | status | note |
    """
    progress_file.parent.mkdir(parents=True, exist_ok=True)

    now = datetime.now().strftime("%Y-%m-%d %H:%M")
    status_emoji = {"completed": "done", "blocked": "BLOCKED", "failed": "FAILED"}.get(status, status)
    note_text = f" {note}" if note else ""

    line = f"| {now} | {task.task_id} | {task.title} | {status_emoji} |{note_text} |\n"

    with open(progress_file, "a", encoding="utf-8") as f:
        f.write(line)


def append_learning(learnings_file: Path, learning: str) -> None:
    """Append a learning entry to .ai/learnings.md."""
    learnings_file.parent.mkdir(parents=True, exist_ok=True)

    now = datetime.now().strftime("%Y-%m-%d")
    line = f"- [{now}] {learning}\n"

    with open(learnings_file, "a", encoding="utf-8") as f:
        f.write(line)


def print_phase_summary(phase: Phase) -> None:
    """Print a summary of a phase's progress."""
    total = phase.total_tasks
    done = phase.completed_tasks
    blocked = phase.blocked_tasks
    pending = phase.pending_tasks

    bar_width = 30
    done_chars = int(done / total * bar_width) if total else 0
    blocked_chars = int(blocked / total * bar_width) if total else 0
    pending_chars = bar_width - done_chars - blocked_chars

    bar = f"[{'#' * done_chars}{'!' * blocked_chars}{'.' * pending_chars}]"

    print(f"  {phase.name}: {bar} {done}/{total} done, {blocked} blocked, {pending} pending")


def print_overall_summary(phases: list[Phase]) -> None:
    """Print overall progress across all phases."""
    total = sum(p.total_tasks for p in phases)
    done = sum(p.completed_tasks for p in phases)
    blocked = sum(p.blocked_tasks for p in phases)
    pending = total - done - blocked

    print(f"\n{'='*60}")
    print(f"  Grace Orchestrator - Overall Progress")
    print(f"{'='*60}")
    print(f"  Total: {total} tasks | Done: {done} | Blocked: {blocked} | Pending: {pending}")
    print(f"{'='*60}")

    for phase in phases:
        print_phase_summary(phase)

    print()
