# FinTracker — Phase 1 Backend Foundation

This starter adds only the backend foundation and a local PostgreSQL service.
It does **not** migrate data, create financial tables, replace Supabase, or
change the frontend yet.

## Prerequisites (Windows)

- Python 3.12
- Docker Desktop running
- Git
- The existing FinTracker repository cloned locally

## 1. Copy the starter files

Extract this ZIP into the root of your existing FinTracker repository.
Merge the files; do not delete or overwrite your existing frontend files.

If `.gitignore.additions.txt` contains entries missing from your current
`.gitignore`, append those entries to the existing file. Do not replace the
existing `.gitignore`.

## 2. Create your local environment file

Open PowerShell at the repository root:

```powershell
Copy-Item .env.example .env
```

The password in `.env.example` is only a local-development placeholder.
Change it before using this database for anything beyond local practice.
Never commit `.env` or share its contents.

## 3. Start PostgreSQL

From the repository root:

```powershell
docker compose up -d db
docker compose ps
```

Wait until the database reports healthy.

## 4. Create the Python environment

```powershell
Set-Location backend
py -3.12 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements.txt
```

If PowerShell blocks virtual-environment activation, use this for the current
terminal only, if permitted by your machine's policy:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\.venv\Scripts\Activate.ps1
```

## 5. Start the API

Run this from the `backend` directory with the virtual environment activated:

```powershell
uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```

Open:

- http://127.0.0.1:8000/ — service status
- http://127.0.0.1:8000/docs — interactive API documentation
- http://127.0.0.1:8000/api/v1/health/live — process liveness
- http://127.0.0.1:8000/api/v1/health/ready — PostgreSQL readiness

## 6. Run tests

Open a second PowerShell terminal, go to `backend`, activate the same virtual
environment, and run:

```powershell
pytest -q
```

The tests verify the root and liveness endpoints. The readiness endpoint
checks the actual PostgreSQL connection; verify it manually in the browser.

## What success looks like

- Docker reports the `db` service as healthy.
- `/` returns JSON with `status: running`.
- `/api/v1/health/live` returns `status: alive`.
- `/api/v1/health/ready` returns `status: ready` and `database: connected`.
- `pytest -q` reports 2 passed.

## Important safety boundary

This PostgreSQL instance is a new, empty local development database. Do not
point it at the Supabase production database. Do not run old SQL setup scripts
against it. We will design the target schema and Alembic migrations only after
reconciling the latest eight-table export with the frontend's actual data
requirements.

When reporting results, share only the success/failure output and error
messages. Do not share `.env`, passwords, OAuth secrets, API keys, or tokens.
