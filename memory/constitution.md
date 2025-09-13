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
[PRINCIPLE_4_DESCRIPTION]
I've seen the AI write down decisions that were made but I never made those decisions.  Do not let the AI hallucinate any decisions, we are keeping a record of prompts so the AI can run through these when generating the research.md file and use them to generate the decisions made.  Each decision file should be numbered so we can see the sequence of the conversation.  These files should list naturally in a directly listing in the order they are numbered.  So we should avoid situations like 10 and 1 being grouped together.  Let's plan on a maximum of 999 decisions per spec so use 3 digits.

### [PRINCIPLE_5_NAME]
Boilerplate go away
[PRINCIPLE_5_DESCRIPTION]
Even though the AI is writing the majority of this code I still want to keep boilerplate to a minimum.  Use Lombok in all entity and data transfer classes.  We're already using Spring for a huge majority of boilerplate so let's keep it low.

## [PRINCIPLE_6_NAME]
We have checks for a reason
[PRINCIPLE_6_DESCRIPTION]
We have put our checks in place to keep the code clean, make sure you are not skipping checks just to see if things compile.  The code style is part of the code compilation and these checks should never be skipped.

## [PRINCIPLE_7_NAME]
Test Data will be structured properly or else responses will be failures
[PRINCIPLE_7_DESCRIPTION]
We need to make sure that our parsing code works so our test data should be structured in a valid format.  If we are testing responses for invalid formats then the responses should be handled appropriately and failures should be expected.

## [PRINCIPLE_8_NAME]
Mock services conservatively
[PRINCIPLE_8_DESCRIPTION]
During integration tests, services should never be mocked, only external systems should be mocked such as Repository interactions or External System interactions through RestClient, WebClient or RestTemplate.  Services can be mocked in unit testing when testing service to service interactions where you want the other service to always return a given result without relying on it's implementation being correct atm.

## [SECTION_1_NAME]
Login with Google and Login with Facebook will be used

[SECTION_1_CONTENT]
This app is meant for family and they either have a google account or facebook account.  This app only needs their name, nothing more.

## Governance
Constitution supersedes all other practices; Amendments require documentation, approval, migration plan

[GOVERNANCE_RULES]


**Version**: 1.2.0 | **Ratified**: 2025-09-13 | **Last Amended**: 2025-09-13