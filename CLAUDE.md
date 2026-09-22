# CLAUDE.md — Claude Code Configuration for MallPlus

## Project Context
**App:** MallPlus
**Stack:** Java 17, Spring Boot 3.x, Spring Cloud Alibaba, MySQL, Redis, Caffeine, Guava, RocketMQ, Maven
**Stage:** MVP development
**User Level:** C-type learner / developer

## Directives
1. **Master Plan:** Always read `AGENTS.md` first. It contains the current phase and tasks.
2. **Documentation:** Read relevant files under `agent_docs/` before changing code or validating behavior.
3. **Plan-First:** Briefly outline the approach, then wait for approval before implementing the next bounded slice.
4. **Incremental Build:** Work one feature at a time and verify after each slice.
5. **Learning Friendly:** Explain the "why" behind each design choice and trade-off.

## What Not To Do
- Do not delete files without explicit confirmation.
- Do not change the framework versions or original RabbitMQ order-delay flow.
- Do not add tables or schema changes without explicit approval.
- Do not add features outside the active phase.
- Do not skip tests or static checks for "simple" changes.
- Do not hardcode cache keys or RocketMQ topic/tag names.

## Relevant Files
- [AGENTS.md](AGENTS.md)
- [MEMORY.md](MEMORY.md)
- [agent_docs/project_brief.md](agent_docs/project_brief.md)
- [agent_docs/tech_stack.md](agent_docs/tech_stack.md)
- [agent_docs/testing.md](agent_docs/testing.md)
- [document/PRD-MallPlus-MVP.md](document/PRD-MallPlus-MVP.md)
- [document/TechDesign-MallPlus-MVP.md](document/TechDesign-MallPlus-MVP.md)
