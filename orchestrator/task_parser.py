"""
Task parser module.

Parses .ai/tasks/phase-*.md files to extract atomic tasks,
their statuses, dependencies, and metadata.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path


@dataclass
class Task:
    """Represents a single atomic task from a phase file."""

    task_id: str  # e.g. "P2-01"
    title: str  # e.g. "Create Video enums and value objects"
    status: str  # "pending" | "completed" | "blocked"
    reference_docs: list[str] = field(default_factory=list)
    output_files: list[str] = field(default_factory=list)
    verify_command: str = ""
    dependencies: list[str] = field(default_factory=list)
    notes: str = ""
    phase_file: str = ""  # which phase file this task belongs to
    raw_section: str = ""  # raw markdown section for prompt building


@dataclass
class Phase:
    """Represents a phase file with its tasks."""

    name: str  # e.g. "phase-2-video-context"
    file_path: Path
    tasks: list[Task] = field(default_factory=list)

    @property
    def total_tasks(self) -> int:
        return len(self.tasks)

    @property
    def completed_tasks(self) -> int:
        return sum(1 for t in self.tasks if t.status == "completed")

    @property
    def blocked_tasks(self) -> int:
        return sum(1 for t in self.tasks if t.status == "blocked")

    @property
    def pending_tasks(self) -> int:
        return sum(1 for t in self.tasks if t.status == "pending")

    @property
    def is_complete(self) -> bool:
        return all(t.status in ("completed", "blocked") for t in self.tasks)

    def next_pending_task(self) -> Task | None:
        """Return the first pending task, skipping completed/blocked."""
        for task in self.tasks:
            if task.status == "pending":
                return task
        return None


# Regex patterns
_TASK_HEADER_RE = re.compile(r"^###\s+(P\d+-\d+):\s+(.+)$", re.MULTILINE)
_STATUS_RE = re.compile(r"^\-\s+\*\*状态\*\*:\s+\[(.)\]", re.MULTILINE)
_FIELD_RE = re.compile(r"^\-\s+\*\*(.+?)\*\*:\s*(.+)$", re.MULTILINE)
_LIST_ITEM_RE = re.compile(r"^\s+-\s+`(.+?)`", re.MULTILINE)


def _parse_status_char(char: str) -> str:
    """Convert status char to semantic status."""
    if char == "x":
        return "completed"
    elif char == "!":
        return "blocked"
    else:
        return "pending"


def _split_task_sections(content: str) -> list[str]:
    """Split markdown content into per-task sections."""
    # Split on "---" horizontal rules between tasks
    sections = []
    current = []
    lines = content.split("\n")
    in_task = False

    for line in lines:
        if _TASK_HEADER_RE.match(line):
            if in_task and current:
                sections.append("\n".join(current))
                current = []
            in_task = True
        if in_task:
            current.append(line)

    if current:
        sections.append("\n".join(current))

    return sections


def _parse_task_section(section: str, phase_file: str) -> Task | None:
    """Parse a single task section into a Task object."""
    header_match = _TASK_HEADER_RE.search(section)
    if not header_match:
        return None

    task_id = header_match.group(1)
    title = header_match.group(2).strip()

    # Parse status
    status_match = _STATUS_RE.search(section)
    status = _parse_status_char(status_match.group(1)) if status_match else "pending"

    # Parse fields
    reference_docs: list[str] = []
    output_files: list[str] = []
    verify_command = ""
    dependencies: list[str] = []
    notes = ""

    for field_match in _FIELD_RE.finditer(section):
        field_name = field_match.group(1).strip()
        field_value = field_match.group(2).strip()

        if field_name == "参考文档":
            reference_docs = [field_value]
        elif field_name == "验证命令":
            verify_command = field_value.strip("`")
        elif field_name == "依赖":
            dependencies = [d.strip() for d in field_value.split(",")]
        elif field_name == "注意":
            notes = field_value

    # Parse output files (listed as indented ` - `path` ` items after **产出文件**)
    output_section = re.search(
        r"\*\*产出文件\*\*:\s*\n((?:\s+-\s+`.+`\n?)+)", section
    )
    if output_section:
        output_files = _LIST_ITEM_RE.findall(output_section.group(1))
    else:
        # Single-line output
        single_output = re.search(r"\*\*产出文件\*\*:\s*`(.+?)`", section)
        if single_output:
            output_files = [single_output.group(1)]
        else:
            # Check for "无新增文件" or "无"
            no_output = re.search(r"\*\*产出文件\*\*:\s*(无|无新增文件)", section)
            if no_output:
                output_files = []

    return Task(
        task_id=task_id,
        title=title,
        status=status,
        reference_docs=reference_docs,
        output_files=output_files,
        verify_command=verify_command,
        dependencies=dependencies,
        notes=notes,
        phase_file=phase_file,
        raw_section=section.strip(),
    )


def parse_phase_file(file_path: Path) -> Phase:
    """Parse a phase markdown file into a Phase object with its tasks."""
    content = file_path.read_text(encoding="utf-8")
    name = file_path.stem  # e.g. "phase-2-video-context"

    sections = _split_task_sections(content)
    tasks = []
    for section in sections:
        task = _parse_task_section(section, phase_file=name)
        if task:
            tasks.append(task)

    return Phase(name=name, file_path=file_path, tasks=tasks)


def _phase_sort_key(path: Path) -> int:
    """Extract phase number for sorting (phase-1-xxx -> 1, phase-10-xxx -> 10)."""
    match = re.match(r"phase-(\d+)", path.stem)
    return int(match.group(1)) if match else 999


def discover_phases(tasks_dir: Path) -> list[Phase]:
    """Discover and parse all phase files in numerical order."""
    phase_files = sorted(tasks_dir.glob("phase-*.md"), key=_phase_sort_key)
    phases = []
    for pf in phase_files:
        phase = parse_phase_file(pf)
        phases.append(phase)
    return phases


def find_next_task(phases: list[Phase], start_task: str | None = None) -> tuple[Phase, Task] | None:
    """Find the next pending task across all phases.

    If start_task is given, skip until that task ID is found.
    Returns (phase, task) tuple or None if all tasks are done.
    """
    found_start = start_task is None

    for phase in phases:
        for task in phase.tasks:
            if not found_start:
                if task.task_id == start_task:
                    found_start = True
                    if task.status == "pending":
                        return (phase, task)
                continue

            if task.status == "pending":
                # Check dependencies are satisfied
                if _dependencies_met(task, phases):
                    return (phase, task)

    return None


def _dependencies_met(task: Task, phases: list[Phase]) -> bool:
    """Check if all dependencies of a task are completed."""
    if not task.dependencies or task.dependencies == ["无"]:
        return True

    all_tasks: dict[str, Task] = {}
    for phase in phases:
        for t in phase.tasks:
            all_tasks[t.task_id] = t

    for dep_id in task.dependencies:
        dep_id = dep_id.strip()
        if not dep_id or dep_id == "无":
            continue
        dep_task = all_tasks.get(dep_id)
        if dep_task and dep_task.status != "completed":
            return False

    return True


def update_task_status(file_path: Path, task_id: str, new_status: str) -> None:
    """Update a task's status in its phase file.

    new_status: "completed" -> [x], "blocked" -> [!], "pending" -> [ ]
    """
    content = file_path.read_text(encoding="utf-8")

    status_char_map = {
        "completed": "x",
        "blocked": "!",
        "pending": " ",
    }
    new_char = status_char_map.get(new_status, " ")

    # Find the task section and update its status field
    # Pattern: after `### P{n}-{nn}:` find `- **状态**: [.]`
    pattern = re.compile(
        rf"(###\s+{re.escape(task_id)}:.+?)"
        rf"(\-\s+\*\*状态\*\*:\s+\[).\]",
        re.DOTALL,
    )

    new_content = pattern.sub(rf"\g<1>\g<2>{new_char}]", content, count=1)

    if new_content != content:
        file_path.write_text(new_content, encoding="utf-8")
