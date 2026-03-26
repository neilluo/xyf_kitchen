"""
Session runner module.

Builds prompts and executes agent CLI sessions (opencode / qoder).
"""

from __future__ import annotations

import json
import subprocess
import time
from dataclasses import dataclass
from pathlib import Path

from .config import OrchestratorConfig
from .task_parser import Phase, Task


@dataclass
class SessionResult:
    """Result of a single agent session."""

    task_id: str
    success: bool
    exit_code: int
    stdout: str
    stderr: str
    duration_seconds: float
    timed_out: bool = False


def build_prompt(task: Task, phase: Phase, config: OrchestratorConfig) -> str:
    """Build the prompt that will be sent to the agent CLI."""

    prompt_parts = [
        f"You are working on the Grace Platform project.",
        f"",
        f"## Current Task",
        f"",
        f"Phase: {phase.name}",
        f"Task: {task.task_id} - {task.title}",
        f"",
        f"## Task Details",
        f"",
        task.raw_section,
        f"",
        f"## Instructions",
        f"",
        f"1. Read the AGENTS.md file first to understand the project structure and coding conventions.",
        f"2. Read the reference documentation specified in the task.",
        f"3. Implement the code files listed in the output files.",
        f"4. Run the verification command to ensure correctness.",
        f"5. If verification passes, commit the changes using Conventional Commits format.",
        f"6. Update .ai/progress.md with the completion record.",
        f"7. If you encounter non-obvious issues, add a note to .ai/learnings.md.",
        f"8. Mark the task status as [x] in the phase file: .ai/tasks/{phase.name}.md",
        f"",
        f"IMPORTANT: Only work on this single task ({task.task_id}). Do not proceed to other tasks.",
        f"IMPORTANT: If verification fails after 3 attempts, mark the task as [!] (blocked) and record the failure reason.",
    ]

    return "\n".join(prompt_parts)


def run_session(
    task: Task,
    phase: Phase,
    config: OrchestratorConfig,
    logger,
) -> SessionResult:
    """Execute a single agent session for one task."""

    prompt = build_prompt(task, phase, config)

    if config.cli_tool == "opencode":
        cmd = _build_opencode_command(prompt, config)
    else:
        cmd = _build_qoder_command(prompt, config)

    logger.info(f"Starting session for {task.task_id}: {task.title}")
    logger.debug(f"Command: {' '.join(cmd[:5])}...")

    start_time = time.time()
    timed_out = False

    try:
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=config.session_timeout,
            cwd=str(config.project_dir),
            env=_build_env(),
        )
        exit_code = result.returncode
        stdout = result.stdout
        stderr = result.stderr

    except subprocess.TimeoutExpired as e:
        timed_out = True
        exit_code = -1
        stdout = e.stdout or "" if isinstance(e.stdout, str) else (e.stdout.decode("utf-8", errors="replace") if e.stdout else "")
        stderr = e.stderr or "" if isinstance(e.stderr, str) else (e.stderr.decode("utf-8", errors="replace") if e.stderr else "")
        logger.warning(f"Session timed out after {config.session_timeout}s for {task.task_id}")

    except Exception as e:
        exit_code = -2
        stdout = ""
        stderr = str(e)
        logger.error(f"Session failed with exception: {e}")

    duration = time.time() - start_time

    # Determine success
    success = exit_code == 0 and not timed_out

    # Additionally check if the task was marked as completed in the file
    if success:
        success = _verify_task_completed(task, phase, config)

    logger.info(
        f"Session ended for {task.task_id}: "
        f"{'SUCCESS' if success else 'FAILED'} "
        f"(exit={exit_code}, {duration:.0f}s)"
    )

    return SessionResult(
        task_id=task.task_id,
        success=success,
        exit_code=exit_code,
        stdout=stdout,
        stderr=stderr,
        duration_seconds=duration,
        timed_out=timed_out,
    )


def _build_opencode_command(prompt: str, config: OrchestratorConfig) -> list[str]:
    """Build the opencode CLI command."""
    cmd = [
        "opencode",
        "run",
        prompt,
        "--dir", str(config.project_dir),
    ]

    if config.model:
        cmd.extend(["--model", config.model])

    return cmd


def _build_qoder_command(prompt: str, config: OrchestratorConfig) -> list[str]:
    """Build a qoder-compatible command.

    Note: Qoder is an Electron app and doesn't have a clean headless CLI.
    This is a fallback that writes the prompt to a temp file and attempts
    to use qoder with file-based input.
    """
    # Write prompt to a temp file
    prompt_file = config.log_dir / "current_prompt.md"
    prompt_file.parent.mkdir(parents=True, exist_ok=True)
    prompt_file.write_text(prompt, encoding="utf-8")

    cmd = [
        "qoder",
        "--file", str(prompt_file),
        "--dir", str(config.project_dir),
    ]

    return cmd


def _build_env() -> dict[str, str]:
    """Build environment variables for subprocess."""
    import os
    env = os.environ.copy()
    # Ensure non-interactive mode
    env["TERM"] = "dumb"
    env["CI"] = "true"
    return env


def _verify_task_completed(task: Task, phase: Phase, config: OrchestratorConfig) -> bool:
    """Check if the task was marked as completed in the phase file.

    Re-reads the phase file to check the task's status.
    """
    try:
        from .task_parser import parse_phase_file

        updated_phase = parse_phase_file(phase.file_path)
        for t in updated_phase.tasks:
            if t.task_id == task.task_id:
                return t.status == "completed"
    except Exception:
        pass

    return False
