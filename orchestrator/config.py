"""
Orchestrator configuration module.

Provides OrchestratorConfig dataclass and CLI argument parsing.
"""

from __future__ import annotations

import argparse
import os
from dataclasses import dataclass, field
from pathlib import Path


@dataclass
class OrchestratorConfig:
    """Orchestrator runtime configuration."""

    # Project root directory (where AGENTS.md lives)
    project_dir: Path = field(default_factory=lambda: Path.cwd())

    # CLI tool: "opencode" or "qoder"
    cli_tool: str = "opencode"

    # Model to use (e.g. "anthropic/claude-sonnet-4-20250514")
    model: str | None = None

    # Max retries per task before marking as blocked
    max_retries: int = 3

    # Timeout per session in seconds (default 30 min)
    session_timeout: int = 1800

    # Cooldown between sessions in seconds
    cooldown: int = 10

    # Dry-run mode: parse and print tasks without executing
    dry_run: bool = False

    # Start from a specific phase (e.g. "phase-2")
    start_phase: str | None = None

    # Start from a specific task ID (e.g. "P2-05")
    start_task: str | None = None

    # Only run a single phase then stop
    single_phase: bool = False

    # Log directory
    log_dir: Path = field(default_factory=lambda: Path.cwd() / "orchestrator" / "logs")

    @property
    def tasks_dir(self) -> Path:
        return self.project_dir / ".ai" / "tasks"

    @property
    def progress_file(self) -> Path:
        return self.project_dir / ".ai" / "progress.md"

    @property
    def learnings_file(self) -> Path:
        return self.project_dir / ".ai" / "learnings.md"


def parse_args(argv: list[str] | None = None) -> OrchestratorConfig:
    """Parse CLI arguments into an OrchestratorConfig."""
    parser = argparse.ArgumentParser(
        prog="grace-orchestrator",
        description="Grace Platform AI coding orchestrator - automates agent sessions task by task.",
    )

    parser.add_argument(
        "--dir",
        type=Path,
        default=Path.cwd(),
        help="Project root directory (default: current directory)",
    )
    parser.add_argument(
        "--cli",
        choices=["opencode", "qoder"],
        default="opencode",
        help="CLI tool to use (default: opencode)",
    )
    parser.add_argument(
        "--model",
        type=str,
        default=None,
        help="Model identifier (e.g. anthropic/claude-sonnet-4-20250514)",
    )
    parser.add_argument(
        "--max-retries",
        type=int,
        default=3,
        help="Max retries per task before marking blocked (default: 3)",
    )
    parser.add_argument(
        "--timeout",
        type=int,
        default=1800,
        help="Session timeout in seconds (default: 1800 = 30min)",
    )
    parser.add_argument(
        "--cooldown",
        type=int,
        default=10,
        help="Cooldown between sessions in seconds (default: 10)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Parse and display all tasks without executing",
    )
    parser.add_argument(
        "--start-phase",
        type=str,
        default=None,
        help="Start from a specific phase file (e.g. phase-2-video-context)",
    )
    parser.add_argument(
        "--start-task",
        type=str,
        default=None,
        help="Start from a specific task ID (e.g. P2-05)",
    )
    parser.add_argument(
        "--single-phase",
        action="store_true",
        help="Only run the current/specified phase then stop",
    )
    parser.add_argument(
        "--log-dir",
        type=Path,
        default=None,
        help="Log directory (default: <project>/orchestrator/logs)",
    )

    args = parser.parse_args(argv)

    log_dir = args.log_dir or (args.dir / "orchestrator" / "logs")

    return OrchestratorConfig(
        project_dir=args.dir.resolve(),
        cli_tool=args.cli,
        model=args.model,
        max_retries=args.max_retries,
        session_timeout=args.timeout,
        cooldown=args.cooldown,
        dry_run=args.dry_run,
        start_phase=args.start_phase,
        start_task=args.start_task,
        single_phase=args.single_phase,
        log_dir=log_dir.resolve(),
    )
