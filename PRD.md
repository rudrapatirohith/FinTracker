# FinTracker — Product Requirements Document (PRD)
## Production-Oriented Full-Stack, Backend Architecture, Distributed Systems & AI Engineering Project

**Document Version:** 1.0  
**Status:** Proposed — Updated Architecture  
**Project:** FinTracker  
**Architecture:** Next.js + Java Spring Boot + Python FastAPI + PostgreSQL  
**Primary Objective:** Build a production-oriented financial management platform while gaining hands-on experience in senior backend engineering, system design, distributed systems, and AI engineering.

---

# 1. Executive Summary

FinTracker will evolve from an existing Supabase-powered financial tracking application into a full-stack application with a custom backend, custom database, authentication system, AI service, and production-oriented infrastructure.

The project has two equally important objectives:

1. Deliver a functional, secure, maintainable financial management product.
2. Use the product as a practical engineering environment to master backend development, system design, distributed systems, AI integration, and production engineering.

The project must go beyond implementing CRUD operations. It must demonstrate architectural decision-making, database integrity, security, concurrency, scalability, reliability, observability, testing, deployment, and failure recovery.

The existing Next.js frontend must be preserved and integrated incrementally.

# 2. Project Goals

## 2.1 Product Goals

- Replace Supabase-dependent backend functionality with a custom backend.
- Implement custom authentication and authorization.
- Support Google OAuth.
- Preserve existing financial records and their identifiers.
- Maintain accurate financial calculations and currency handling.
- Provide consistent dashboards, analytics, reports, and transaction history.
- Integrate AI-assisted financial insights.
- Maintain compatibility with the existing frontend experience.
- Support multiple users with strict financial data isolation.

## 2.2 Engineering and Career Goals

Gain demonstrable hands-on experience in:

- Senior Java backend engineering.
- Spring Boot application architecture.
- Python AI service development.
- REST, gRPC, and WebSocket communication.
- Database design and optimization.
- Distributed systems and event-driven architecture.
- Messaging, caching, and asynchronous processing.
- High availability and fault tolerance.
- Scalability and performance engineering.
- Security and production operations.
- LLM engineering and AI application development.
- Technical design documentation and interview defense.

The outcome should be a portfolio project that can be discussed in depth during senior backend, full-stack, and AI engineering interviews.

# 3. Technology Architecture

## 3.1 Frontend

**Technology:** Next.js, React, TypeScript, Tailwind CSS.

Responsibilities:

- User interface and navigation.
- Authentication screens.
- Financial data visualization.
- Dashboard and reporting interfaces.
- API communication.
- AI assistant interface.
- Form validation and client-side interaction.

Existing components and routes should be preserved wherever possible.

## 3.2 Core Backend

**Technology:** Java, Spring Boot, Maven.

Responsibilities:

- Authentication and authorization.
- User and account management.
- Income management.
- Expense management.
- Loan management.
- Loan payment management.
- International transfers.
- Scheduled payments.
- Transactions and financial history.
- Dashboard calculations.
- Analytics and reporting.
- AI request orchestration.
- Financial business rules.

The initial architecture will be a modular monolith with clearly separated domain modules.

## 3.3 AI Service

**Technology:** Python, FastAPI.

Responsibilities:

- LLM integration.
- Prompt engineering.
- Structured outputs.
- AI provider abstraction.
- Financial categorization.
- Financial summaries.
- Read-only AI tools.
- Document processing.
- Retrieval-augmented generation.
- Embedding generation.
- Model evaluation.
- AI guardrails and safety.

The AI service will not own financial business logic or become an unrestricted database client.

## 3.4 Database

**Technology:** PostgreSQL.

Responsibilities:

- User data.
- Authentication-related records.
- Financial records.
- Relationships and constraints.
- Transactional consistency.
- Reporting data.
- Audit information.

Financial amounts and exchange rates must use precise decimal representations.

## 3.5 Infrastructure and Supporting Technologies

The project will progressively incorporate:

- Docker and Docker Compose.
- Redis.
- Apache Kafka.
- RabbitMQ.
- NGINX.
- OpenTelemetry.
- Metrics and centralized logging.
- CI/CD.
- Cloud deployment.
- Database backup and recovery.

# 4. High-Level Architecture

```text
                    Next.js Frontend
                           |
                           | HTTPS / REST
                           |
                    NGINX / Gateway
                           |
                           v
                  Java Spring Boot API
                           |
          +----------------+----------------+
          |                |                |
       Auth            Financial          AI
       Module           Modules       Orchestrator
          |                |                |
          +----------------+                |
                           |                |
                      PostgreSQL            |
                           |                |
                           +----------------+
                                            |
                                            | Internal API
                                            v
                                      Python FastAPI
                                            |
                                      LLM Providers
                                            |
                                   Embeddings / Models


             Redis — Cache / Rate Limiting
             Kafka — Event Streaming
             RabbitMQ — Task Queues
             Workers — Background Processing
             OpenTelemetry — Observability
```

This is the target learning architecture, not a requirement to deploy every component immediately.

# 5. Functional Requirements

## 5.1 Authentication and User Management

- Email and password registration.
- Secure password hashing using Argon2id or an appropriately selected secure alternative.
- Login and logout.
- Google OAuth 2.0 / OpenID Connect.
- Access-token management.
- Secure refresh-token rotation.
- Session revocation.
- Email verification and password recovery.
- Account linking.
- Role-based authorization where appropriate.
- Strict user-level data isolation.

## 5.2 Income Management

- Create, retrieve, update, and delete income records.
- Support recurring income.
- Support multiple currencies.
- Preserve historical exchange-rate information.
- Provide date and category filtering.

## 5.3 Expense Management

- Create and manage expenses.
- Category-based expense tracking.
- Currency handling.
- Date-based filtering.
- Monthly and yearly aggregation.
- Expense analysis.

## 5.4 Loan Management

- Loan creation and updates.
- Outstanding balance tracking.
- Principal and interest tracking.
- Loan payment history.
- Loan payoff calculations.
- Historical payment reconciliation.

## 5.5 International Transfers

- Record sent and received amounts.
- Support multiple currencies.
- Track exchange rates and transfer fees.
- Track transfer status.
- Preserve historical conversion values.

## 5.6 Scheduled Payments

- Create and manage scheduled payments.
- Recurring payment support.
- Due-date tracking.
- Reminder functionality.
- Payment status tracking.
- Background processing where appropriate.

## 5.7 Transactions and Financial History

- Unified transaction history.
- Search and filtering.
- Pagination and sorting.
- Consistent financial event representation.
- Correct currency and transaction-type handling.
- Avoid duplicate transaction representations.

## 5.8 Dashboard and Analytics

- Income and expense summaries.
- Monthly and yearly reports.
- Financial trends.
- Category breakdowns.
- Loan summaries.
- Transfer summaries.
- Consistent calculations across dashboard, analytics, and reports.

Financial calculations must be performed by the backend, not delegated to an LLM.

## 5.9 AI Financial Assistant

- Financial summaries.
- Spending insights.
- Expense categorization.
- Savings recommendations.
- Natural-language questions.
- Read-only access to approved financial tools.
- Context-aware responses.
- Structured and validated outputs.
- AI response evaluation.

Any action that changes financial records must require explicit user approval.

# 6. Backend Engineering Curriculum — Mandatory Practical Track

Every topic below must have an implementation, experiment, test, or documented engineering exercise.

## Track A — Java and Spring Boot Backend Engineering

1. Java fundamentals relevant to backend development.
2. Collections, streams, generics, exceptions.
3. Concurrency and multithreading.
4. JVM fundamentals and memory management.
5. Spring Boot application architecture.
6. Dependency injection and inversion of control.
7. Modular monolith design.
8. Layered architecture and domain-driven design.
9. REST API design.
10. API versioning and backward compatibility.
11. Request validation and error handling.
12. Pagination, sorting, filtering.
13. DTOs and mapping.
14. Spring Data JPA and Hibernate.
15. Database transactions.
16. Transaction propagation and isolation.
17. Optimistic and pessimistic locking.
18. Idempotent APIs.
19. Concurrency control.
20. Authentication and authorization.
21. Spring Security.
22. OAuth2 and OpenID Connect.
23. Secure session management.
24. API security and OWASP risks.
25. Unit, integration, and end-to-end testing.
26. Performance optimization.

**Practical outcome:** Build and defend a secure, production-oriented Java backend for FinTracker.

## Track B — API and Communication Architecture

All three communication approaches will be studied and practiced.

### REST

- Resource-oriented design.
- HTTP methods and status codes.
- Idempotency.
- API contracts.
- OpenAPI documentation.
- Versioning.
- Pagination.
- Rate limiting.

### gRPC

- Protocol Buffers.
- Service definitions.
- Unary calls.
- Streaming.
- Inter-service communication.
- REST versus gRPC trade-offs.

### WebSockets

- Persistent connections.
- Bidirectional communication.
- Connection lifecycle.
- Reconnection.
- Authentication.
- Real-time updates.
- Backpressure.

**Practical outcome:** Implement REST as the primary application interface, gRPC as an internal service communication exercise, and WebSockets for a justified real-time feature.

## Track C — Database Engineering

- Relational database design.
- Normalization and denormalization.
- Primary and foreign keys.
- Composite constraints.
- Indexing.
- Composite indexes.
- Query optimization.
- EXPLAIN and EXPLAIN ANALYZE.
- PostgreSQL query plans.
- Transactions.
- ACID properties.
- Isolation levels.
- Deadlocks.
- Optimistic and pessimistic locking.
- Connection pooling.
- Database partitioning.
- Read replicas.
- Replication lag.
- Database backup and restore.
- Sharding strategies.
- Data migration.
- Schema evolution.

**Practical outcome:** Design, optimize, benchmark, and defend the financial database.

## Track D — Distributed Systems

This is a mandatory hands-on learning track, not an optional reading list.

### Fundamental concepts

- Monolith versus modular monolith versus microservices.
- Horizontal and vertical scaling.
- Distributed system communication.
- CAP theorem.
- Consistency models.
- Availability.
- Network partitions.
- Strong versus eventual consistency.
- Synchronous versus asynchronous communication.
- Distributed transactions.
- Eventual consistency.

### Messaging and event-driven architecture

- Event-driven architecture.
- Event producers and consumers.
- Apache Kafka.
- RabbitMQ.
- Topics, partitions, queues, and exchanges.
- Consumer groups.
- Message acknowledgment.
- Delivery guarantees.
- At-most-once, at-least-once, and effectively-once processing.
- Idempotent consumers.
- Dead-letter queues.
- Retry strategies.
- Transactional outbox.
- Event ordering.
- Schema evolution.

### Distributed transaction patterns

- Saga pattern.
- Choreography.
- Orchestration.
- Compensation actions.
- Outbox and inbox patterns.
- Failure recovery.

**Practical outcome:** Implement asynchronous financial workflows and demonstrate how the application behaves when services or message brokers fail.

Kafka and RabbitMQ will both receive hands-on implementation and comparison exercises. We will choose one as the primary broker for the final application where appropriate.

## Track E — Scalability and Performance

- NGINX reverse proxy.
- Load balancing.
- Round-robin and alternative balancing strategies.
- Health checks.
- Horizontal scaling.
- Stateless application design.
- Redis caching.
- Cache-aside pattern.
- Cache invalidation.
- Cache consistency.
- Distributed rate limiting.
- Request throttling.
- Backpressure.
- Database bottleneck analysis.
- Connection pooling.
- Performance testing.
- Capacity estimation.
- Throughput and latency.
- P95 and P99 latency.

**Practical outcome:** Benchmark the application before and after caching and load balancing, and explain the results.

## Track F — Fault Tolerance and Reliability

- Timeouts.
- Retries.
- Exponential backoff.
- Jitter.
- Circuit breakers.
- Bulkheads.
- Fallback strategies.
- Rate limiting.
- Service health checks.
- Graceful degradation.
- Failure isolation.
- Dependency failures.
- Disaster recovery.
- High availability.
- SLI, SLO, and SLA.
- Error budgets.

Resilience4j and other appropriate Java ecosystem tools will be used.

**Practical outcome:** Deliberately break dependencies and demonstrate controlled recovery instead of relying on assumptions.

## Track G — Production Engineering and Observability

- Docker.
- Docker Compose.
- Multi-stage Docker builds.
- CI/CD pipelines.
- Environment configuration.
- Secret management.
- Deployment environments.
- Cloud networking.
- Reverse proxies.
- Structured logging.
- Centralized logs.
- Metrics.
- Distributed tracing.
- OpenTelemetry.
- Alerting.
- Health and readiness checks.
- Backup and restore testing.
- Security scanning.
- Dependency vulnerability management.

**Practical outcome:** Build, deploy, monitor, troubleshoot, and recover the application.

# 7. AI Engineering Curriculum — Mandatory Practical Track

Python will be used for a separate AI service.

## AI Track A — LLM Foundations

- Transformer architecture.
- Tokens and tokenization.
- Context windows.
- Embeddings.
- Prompt engineering.
- System and user prompts.
- Structured outputs.
- JSON schema validation.
- Model selection.
- Temperature and generation parameters.
- Context management.

## AI Track B — AI Service Architecture

- Python backend development.
- FastAPI.
- Pydantic.
- Provider abstraction.
- Model routing.
- Internal service authentication.
- Request timeouts.
- Retries and fallbacks.
- Streaming responses.
- AI request history.
- Token and cost tracking.

## AI Track C — Tool Calling and Agents

- Function calling.
- Tool schemas.
- Read-only financial tools.
- Tool validation.
- Agent execution.
- Agent state.
- Guardrails.
- Human approval.
- Controlled actions.
- Prompt injection defenses.

## AI Track D — Hugging Face and Open-Source Models

- Hugging Face ecosystem.
- Transformers.
- Model loading.
- Inference.
- Model selection.
- Embedding models.
- Text classification.
- Financial categorization.
- Local versus hosted inference.

## AI Track E — Evaluation

- Model evaluation.
- Accuracy measurement.
- Hallucination detection.
- Structured output validation.
- Regression testing.
- Golden datasets.
- Latency measurement.
- Cost measurement.
- Model comparisons.
- Human evaluation.

## AI Track F — Retrieval-Augmented Generation

- RAG architecture.
- Document ingestion.
- Text chunking.
- Embeddings.
- Vector similarity.
- pgvector.
- Metadata filtering.
- Retrieval evaluation.
- Context construction.
- Source citations.
- Permission-aware retrieval.

## AI Track G — Fine-Tuning

- Fine-tuning fundamentals.
- Dataset preparation.
- Model selection.
- LoRA.
- QLoRA.
- Parameter-efficient fine-tuning.
- Evaluation before and after fine-tuning.
- Cost and performance trade-offs.

Fine-tuning will be implemented only when experiments demonstrate a meaningful reason to use it.

## AI Track H — Production AI Systems

- AI deployment.
- Model versioning.
- Model monitoring.
- Prompt versioning.
- AI observability.
- Privacy.
- Security.
- Cost optimization.
- Latency optimization.
- Provider failure handling.
- Agent guardrails.
- Human-in-the-loop workflows.

# 8. System Design Interview Preparation

System design learning must run alongside implementation.

For every major feature, produce:

1. Requirements and assumptions.
2. Functional and non-functional requirements.
3. High-level architecture.
4. Low-level design.
5. Database schema.
6. API contract.
7. Data flow.
8. Scaling strategy.
9. Reliability strategy.
10. Security considerations.
11. Failure scenarios.
12. Trade-off analysis.

Required design exercises:

- Design a financial tracking platform.
- Design an authentication and session service.
- Design a transaction history service.
- Design an analytics and reporting service.
- Design a scheduled payment and notification service.
- Design a real-time notification system.
- Design an event-driven architecture.
- Design a distributed AI assistant.
- Design a scalable multi-tenant SaaS application.
- Design a highly available backend.

Practice capacity estimation, bottleneck analysis, failure handling, and architectural defense.

# 9. Testing Strategy

Testing is mandatory at every stage.

- Unit tests.
- Integration tests.
- Repository tests.
- API tests.
- Contract tests.
- End-to-end tests.
- Security tests.
- Authorization tests.
- Cross-tenant isolation tests.
- Concurrency tests.
- Idempotency tests.
- Performance tests.
- Load tests.
- Failure injection.
- AI evaluation tests.

Every feature must have acceptance criteria and evidence that it works.

# 10. Data Migration Requirements

The existing Supabase database is the source of truth until migration is verified.

Requirements:

- Preserve original UUIDs.
- Preserve exact decimal values.
- Preserve currencies and exchange rates.
- Preserve timestamps and date semantics.
- Validate ownership relationships.
- Validate loan/payment relationships.
- Reconcile row counts.
- Reconcile financial totals.
- Check duplicate and orphan records.
- Do not invent transaction records.
- Preserve historical data.
- Maintain a rollback strategy.

The supplied dataset contains 62 records across seven populated tables, with the transactions table empty.

The Supabase authentication export is still required before safely migrating existing login identities.

# 11. Implementation Phases

## Phase 0 — Existing Application Audit
Completed.

## Phase 1 — Local Development Foundation
Completed with FastAPI and PostgreSQL starter environment.

The Python starter is retained as a learning/reference artifact. The core backend direction is now Java.

## Phase 2 — Architecture and Database Foundation
Updated direction:

- Finalize Java/Spring Boot architecture.
- Establish Java project structure.
- Configure PostgreSQL connectivity.
- Establish database migration strategy.
- Define domain boundaries.
- Create authentication and financial data models.
- Document architectural decisions.

## Phase 3 — Authentication and Authorization

- User registration and login.
- Password security.
- Google OAuth.
- Token/session lifecycle.
- Account linking.
- Tenant isolation.
- Security tests.

## Phase 4 — Financial Backend APIs

- Income.
- Expenses.
- Loans.
- Loan payments.
- Transfers.
- Scheduled payments.
- Transactions.
- Dashboard calculations.
- Reports and analytics.

## Phase 5 — Data Migration

- Backup.
- Migration scripts.
- Data import.
- Reconciliation.
- Validation.
- Rollback testing.

## Phase 6 — Frontend Integration

- Integrate the existing Next.js frontend.
- Replace Supabase dependencies incrementally.
- Preserve UI behavior.
- Test API compatibility.

## Phase 7 — Python AI Service

- FastAPI service.
- Provider abstraction.
- AI assistant.
- Read-only financial tools.
- Structured responses.
- Security boundaries.

## Phase 8 — Distributed Systems

- Redis.
- Messaging.
- Kafka.
- RabbitMQ.
- Outbox.
- Background workers.
- Saga patterns.
- Event-driven workflows.

## Phase 9 — Scalability and Reliability

- NGINX.
- Load balancing.
- Horizontal scaling.
- Caching.
- Rate limiting.
- Circuit breakers.
- Retries.
- Performance tests.
- Failure simulations.

## Phase 10 — Advanced Database Engineering

- Index optimization.
- Query plans.
- Replication.
- Partitioning.
- Sharding experiments.
- Backup and recovery.

## Phase 11 — Advanced AI Engineering

- Hugging Face.
- Evaluation.
- RAG.
- pgvector.
- Fine-tuning.
- LoRA / QLoRA.
- Controlled agents.
- AI deployment and monitoring.

## Phase 12 — Production Deployment and Interview Readiness

- CI/CD.
- Cloud deployment.
- Observability.
- Security audits.
- Disaster recovery.
- Load testing.
- Architecture documentation.
- System design presentations.
- Interview preparation.

# 12. Mandatory Learning Methodology

Every feature follows this sequence:

1. **Understand:** Learn the concept and why production systems use it.
2. **Design:** Make architecture decisions and document trade-offs.
3. **Implement:** Build working code and understand its components.
4. **Test:** Verify expected behavior and edge cases.
5. **Break:** Simulate failures, concurrency, network problems, invalid inputs, and dependency outages.
6. **Defend:** Explain the implementation and alternatives as if answering a senior engineering interview question.

Copying code without understanding it does not count as completing a milestone.

The learner must be able to modify the implementation, debug it, test it, and defend the design.

# 13. Definition of Done

A feature is not complete merely because the endpoint returns HTTP 200.

Each milestone must have:

- Working implementation.
- Documented architecture.
- Database design where applicable.
- Automated tests.
- Security and authorization validation.
- Failure handling.
- Performance considerations.
- API documentation.
- Architectural decision records where meaningful.
- A practical explanation of trade-offs.
- Relevant interview questions and answers.

# 14. Architectural Guardrails

- Start with a modular monolith for the Java core backend.
- Introduce separate services when a genuine boundary or learning objective justifies them.
- Keep PostgreSQL authoritative for financial data.
- Keep AI calculations separate from authoritative financial computations.
- Do not expose API keys in the frontend.
- Never trust client-supplied user IDs for authorization.
- Never silently alter historical financial data.
- Avoid unnecessary microservices.
- Do not introduce infrastructure without understanding its operational cost.
- Preserve frontend compatibility.
- Use measured performance evidence when making scaling decisions.
- Keep the original Supabase database unchanged until migration verification is complete.

# 15. Final Deliverables

The completed project should include:

1. Working Next.js frontend.
2. Java Spring Boot core backend.
3. Python FastAPI AI service.
4. PostgreSQL database.
5. Secure authentication and Google OAuth.
6. Complete financial domain APIs.
7. Verified data migration.
8. Automated test suites.
9. Redis caching implementation.
10. Kafka and RabbitMQ practical implementations and comparisons.
11. NGINX and load-balancing configuration.
12. Resilience and failure-testing demonstrations.
13. Database performance and scaling experiments.
14. AI assistant with evaluation.
15. RAG implementation.
16. AI model experimentation and fine-tuning exercises.
17. Docker-based local development.
18. CI/CD pipeline.
19. Deployed application.
20. Architecture diagrams and ADRs.
21. API and database documentation.
22. System design case studies.
23. Interview preparation notes and implementation explanations.

# 16. Success Criteria

The project is successful when the developer can independently:

- Design and implement a secure Spring Boot backend.
- Build and integrate a Python AI service.
- Design and optimize PostgreSQL schemas.
- Explain transaction isolation and concurrency.
- Design event-driven workflows.
- Explain Kafka and RabbitMQ trade-offs.
- Implement caching and cache invalidation.
- Design reliable distributed communication.
- Implement retries, timeouts, circuit breakers, and idempotency.
- Explain CAP theorem and consistency models.
- Design scalable services and databases.
- Troubleshoot production-style failures.
- Implement and evaluate RAG and LLM applications.
- Defend architectural choices during senior-level interviews.

**Final project principle:** FinTracker is both a real application and a structured engineering laboratory. Product quality and engineering depth must advance together.