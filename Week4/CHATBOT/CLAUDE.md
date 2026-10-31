# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

This is the Caramel AI Tech Support Chat Assistant, and it is pre-implementation. The only files are `prd.md` and `.mcp.json`. No frontend or backend exists yet, so there are no build, lint, or test commands to document. Once the projects are scaffolded, add their real commands here instead of guessing them.

`prd.md` is the source of truth for scope, functional requirements (FR-1 to FR-7), and open questions. Check it before making product decisions.

## Planned architecture

- **Frontend:** React + TypeScript chat UI. It sends `POST /api/chat` with `{ "sessionId", "message" }` and renders the `{ "reply" }` response. The `sessionId` identifies one browser session's conversation.
- **Backend:** Java + Spring Boot with Spring AI. A `ChatClient` sends requests to `gemma-4-26b-a4b-it` on Google AI Studio, with a system prompt that defines the Caramel AI support role and scope. Spring AI chat memory (message window) keeps the last N messages for each `sessionId`.

## Constraints that shape the code

- **Secrets:** `GOOGLE_AI_API_KEY` lives in `.env` and is read only by the backend. It must never reach the browser bundle or git. `.env` is already ignored by the repo-root `.gitignore`.
- **Scope:** The assistant answers only LLM inferencing support questions and politely declines everything else (FR-3). Login, account lookup, billing, refunds, voice, and image input are out of scope for v1.
- **Cost control:** History is capped at a fixed number of recent messages to keep requests small.
- **Errors:** If Google AI Studio is unavailable or rate-limited, the backend returns a friendly error instead of failing silently. The UI shows a loading indicator while waiting and a clear message on errors (FR-5).
- **Hand-off:** When the assistant cannot resolve an issue, its reply must tell the user how to reach human support (FR-4). The escalation channel is still undecided, so do not invent a support email or ticket URL.
- **Streaming:** Replies should stream to the UI (FR-6, P1). The first contract is the single `{ "reply" }` response, and streaming belongs to the hardening milestone.

## Open questions

These are listed in `prd.md` §12 and are not decided. Ask the user before hard-coding any of them: the v1 support topics, the escalation path, the tone and brand voice, and the Google AI Studio rate limits and quotas for `gemma-4-26b-a4b-it`.

## Secrets in `.mcp.json`

`.mcp.json` contains a plaintext GitHub token in its `Authorization` header. Never copy that value into code, docs, commits, or chat output.
