# Name Draw Constitution

## Core Principles

### [PRINCIPLE_1_NAME]
2 Modules
[PRINCIPLE_1_DESCRIPTION]
Our application consists of 2 modules, a Java Spring Boot backend and a react + vite frontend

### [PRINCIPLE_2_NAME]
Test-First
[PRINCIPLE_2_DESCRIPTION]
TDD mandatory: Tests written → User approved → Tests fail → Then implement; Red-Green-Refactor cycle strictly enforced

### [PRINCIPLE_3_NAME]
Audit the AI
[PRINCIPLE_3_DESCRIPTION]
As part of this process the AI will build up a 'research.md' that seems to hold data about decisions that were made.  We need to actually track when decisions are made throughout the interaction process so keep a record of the decisions made inside the spec's folder inside a memory folder.  The memory folder will hold a file per prompt, I want what the user asked and then the final response from the model.  The filename will be a reasonable name summarizing the prompt input.

### [PRINCIPLE_4_NAME]
AI will not hallucinate any decisions
[PRINCIPLE_3_DESCRIPTION]
I've seen the AI write down decisions that were made but I never made those decisions.  Do not let the AI hallucinate any decisions, we are keeping a record of prompts so the AI can run through these when generating the research.md file and use them to generate the decisions made.  Each decision file should be numbered so we can see the sequence of the conversation.  These files should list naturally in a directly listing in the order they are numbered.  So we should avoid situations like 10 and 1 being grouped together.  Let's plan on a maximum of 999 decisions per spec so use 3 digits.

## [SECTION_1_NAME]
Login with Google and Login with Facebook will be used

[SECTION_1_CONTENT]
This app is meant for family and they either have a google account or facebook account.  This app only needs their name, nothing more.

## Governance
Constitution supersedes all other practices; Amendments require documentation, approval, migration plan

[GOVERNANCE_RULES]


**Version**: 1.0.0 | **Ratified**: 2025-09-08 | **Last Amended**: 2025-09-08