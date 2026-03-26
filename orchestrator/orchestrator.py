"""
Main orchestrator module.

Implements the core loop: discover phases -> find next task -> run session ->
check result -> retry or advance -> repeat.
"""

from __future__ import annotations

import signal
import sys
import time
from pathlib import Path

from .config import OrchestratorConfig, parse_args
from .progress_tracker import (
    append_progress,
    print_overall_summary,
    setup_logging,
)
from .session_runner import SessionResult, run_session
from .task_parser import (
    Phase,
    Task,
    discover_phases,
    find_next_task,
    parse_phase_file,
    update_task_status,
)


class GracefulShutdown:
    """Handle SIGINT/SIGTERM for graceful shutdown."""

    def __init__(self):
        self.should_stop = False
        signal.signal(signal.SIGINT, self._handler)
        signal.signal(signal.SIGTERM, self._handler)

    def _handler(self, signum, frame):
        if self.should_stop:
            # Second signal: force exit
            print("\nForce shutdown.")
            sys.exit(1)
        print("\nGraceful shutdown requested. Will stop after current task...")
        self.should_stop = True


def run_dry_run(config: OrchestratorConfig) -> None:
    """Parse and display all tasks without executing."""
    phases = discover_phases(config.tasks_dir)

    if not phases:
        print(f"No phase files found in {config.tasks_dir}")
        return

    print_overall_summary(phases)

    print("\nDetailed task list:")
    print("-" * 80)

    for phase in phases:
        print(f"\n## {phase.name} ({phase.total_tasks} tasks)")
        for task in phase.tasks:
            status_icon = {
                "completed": "[x]",
                "blocked": "[!]",
                "pending": "[ ]",
            }.get(task.status, "[?]")

            deps = ", ".join(task.dependencies) if task.dependencies else "none"
            print(f"  {status_icon} {task.task_id}: {task.title}")
            print(f"       deps: {deps} | verify: {task.verify_command or 'none'}")
            if task.output_files:
                for f in task.output_files[:3]:  # Show first 3 files
                    print(f"       -> {f}")
                if len(task.output_files) > 3:
                    print(f"       ... and {len(task.output_files) - 3} more files")

    # Find next task
    next_result = find_next_task(phases, config.start_task)
    if next_result:
        phase, task = next_result
        print(f"\n>> Next task: {task.task_id} ({task.title}) in {phase.name}")
    else:
        print("\n>> All tasks completed or blocked!")


def run_orchestrator(config: OrchestratorConfig) -> None:
    """Main orchestrator loop."""
    shutdown = GracefulShutdown()
    logger = setup_logging(config.log_dir)

    logger.info(f"Grace Orchestrator starting")
    logger.info(f"Project dir: {config.project_dir}")
    logger.info(f"CLI tool: {config.cli_tool}")
    logger.info(f"Max retries: {config.max_retries}")
    logger.info(f"Session timeout: {config.session_timeout}s")

    retry_counts: dict[str, int] = {}  # task_id -> retry count
    current_phase_name: str | None = None

    while not shutdown.should_stop:
        # Re-discover phases each iteration (files may have been updated by agent)
        phases = discover_phases(config.tasks_dir)

        if not phases:
            logger.error(f"No phase files found in {config.tasks_dir}")
            break

        # Print summary
        print_overall_summary(phases)

        # Filter to start phase if specified
        if config.start_phase:
            phases = [p for p in phases if config.start_phase in p.name]
            if not phases:
                logger.error(f"Phase '{config.start_phase}' not found")
                break

        # Find next task
        next_result = find_next_task(phases, config.start_task)

        if next_result is None:
            logger.info("All tasks completed or blocked. Orchestrator done.")
            break

        phase, task = next_result

        # Single-phase mode: stop if we moved to a new phase
        if config.single_phase:
            if current_phase_name is None:
                current_phase_name = phase.name
            elif phase.name != current_phase_name:
                logger.info(f"Single-phase mode: completed {current_phase_name}, stopping.")
                break

        # Clear start_task after first use
        config.start_task = None

        # Check retry count
        retries = retry_counts.get(task.task_id, 0)
        if retries >= config.max_retries:
            logger.warning(
                f"Task {task.task_id} exceeded max retries ({config.max_retries}). "
                f"Marking as blocked."
            )
            update_task_status(phase.file_path, task.task_id, "blocked")
            append_progress(
                config.progress_file,
                task,
                "blocked",
                f"Exceeded {config.max_retries} retries",
            )
            continue

        # Run the session
        logger.info(f"{'='*60}")
        logger.info(f"Task {task.task_id}: {task.title} (attempt {retries + 1}/{config.max_retries})")
        logger.info(f"Phase: {phase.name}")
        logger.info(f"{'='*60}")

        result = run_session(task, phase, config, logger)

        if result.success:
            logger.info(f"Task {task.task_id} completed successfully!")

            # Re-verify by re-reading the phase file
            updated_phase = parse_phase_file(phase.file_path)
            task_updated = False
            for t in updated_phase.tasks:
                if t.task_id == task.task_id and t.status == "completed":
                    task_updated = True
                    break

            if not task_updated:
                # Agent didn't mark task as done - mark it ourselves
                logger.info(f"Marking {task.task_id} as completed (agent didn't update status)")
                update_task_status(phase.file_path, task.task_id, "completed")
                append_progress(config.progress_file, task, "completed")

            # Reset retry count
            retry_counts.pop(task.task_id, None)

        else:
            retries += 1
            retry_counts[task.task_id] = retries

            if result.timed_out:
                logger.warning(f"Task {task.task_id} timed out (attempt {retries}/{config.max_retries})")
            else:
                logger.warning(
                    f"Task {task.task_id} failed (attempt {retries}/{config.max_retries}), "
                    f"exit code: {result.exit_code}"
                )

            if retries >= config.max_retries:
                logger.error(f"Task {task.task_id} blocked after {retries} attempts")
                update_task_status(phase.file_path, task.task_id, "blocked")
                append_progress(
                    config.progress_file,
                    task,
                    "blocked",
                    f"Failed {retries} times. Last exit code: {result.exit_code}",
                )

        # Cooldown between sessions
        if not shutdown.should_stop:
            logger.debug(f"Cooling down for {config.cooldown}s...")
            time.sleep(config.cooldown)

    # Final summary
    phases = discover_phases(config.tasks_dir)
    if phases:
        print_overall_summary(phases)

    logger.info("Orchestrator stopped.")


def main(argv: list[str] | None = None) -> None:
    """Entry point."""
    config = parse_args(argv)

    if config.dry_run:
        run_dry_run(config)
    else:
        run_orchestrator(config)


if __name__ == "__main__":
    main()
