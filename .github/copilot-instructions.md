# Copilot Instructions for MallPlus

## Project context
- App: MallPlus (mall-swarm-based commerce MVP)
- Goal: Java backend interview project with multi-level cache governance and dual-layer cart storage
- Primary docs: [AGENTS.md](../AGENTS.md) and [agent_docs/](../agent_docs/)
- Current phase: MVP code development

## Always read first
1. Read [AGENTS.md](../AGENTS.md) before making changes.
2. Read the relevant file under [agent_docs/](../agent_docs/) when implementing a feature or validating behavior.
3. Keep [MEMORY.md](../MEMORY.md) in sync with current task status and blockers.

## Project rules
- Do not change the base framework versions or rewrite the existing Spring Boot 3 / Spring Cloud Alibaba setup.
- Do not change the original RabbitMQ-based order delay flow.
- Do not add tables or schema changes unless the task explicitly requires it.
- Keep business logic in service/impl layers; controllers should only handle request/response routing.
- Centralize cache keys and RocketMQ topic/tag constants instead of hardcoding them.
- Explain why a new pattern is needed before implementation.
- Prefer one bounded feature at a time and verify after it.

## Workflow
- Plan briefly, then implement the narrow slice.
- Validate with the project command set in [agent_docs/testing.md](../agent_docs/testing.md).
- If verification fails, fix the root cause before proceeding.

## Constraints
- Do not delete files without explicit confirmation.
- Do not add features outside the active phase.
- Do not skip tests or static checks for "simple" changes.
- Prefer existing project conventions and avoid unnecessary dependencies.

## Scope for this repo
- Cache governance: Caffeine + Redis, lock, Bloom filter, random expiration, cache evict messaging
- Cart design: Cookie for guest cart, Redis Hash for logged-in cart, async merge via RocketMQ
- Maintain current code style and modular structure across mall-common, mall-portal, mall-admin, and related modules
