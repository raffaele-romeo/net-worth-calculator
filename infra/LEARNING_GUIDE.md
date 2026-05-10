# Terraform + GCP Learning Guide

Learn Terraform and GCP by deploying the Net Worth Calculator: frontend on
Cloud Storage + Cloud CDN, backend on Cloud Run behind an internal Load
Balancer, Cloud SQL + Memorystore on a private VPC, and a GitHub Actions
CI/CD pipeline.

Each step has a **goal**, **hints**, and a **what to ask if stuck** prompt.
Steps build on each other — work through them in order. The Terraform code
is **not** in this repo; you'll write it yourself in `infra/terraform/`.

---

## Cost Reality Check (read first)

Running this stack 24/7 costs roughly **$60–80/month**:

| Service           | Approx cost                | Notes                                |
| ----------------- | -------------------------- | ------------------------------------ |
| Cloud SQL micro   | ~$10/mo                    | `db-f1-micro`, smallest tier         |
| Memorystore Redis | ~$35/mo                    | 1 GB Basic tier (no smaller option)  |
| HTTPS LBs         | ~$18/mo each (we'll have 2)| Internal + external                  |
| NAT Gateway       | ~$1/mo + traffic           |                                      |
| Cloud Run         | Free tier likely covers it | Pay per request                      |
| GCS + CDN         | Pennies                    |                                      |
| Artifact Registry | Pennies                    |                                      |

**Strategy:** do steps in sittings, run `terraform destroy` when you stop,
and `terraform apply` when you resume. The final teardown step has a
checklist. **GCP free trial gives $300 credit** which covers ~4 months.

---

## Prerequisites Cheat Sheet

| Concept                | One-liner                                                                |
| ---------------------- | ------------------------------------------------------------------------ |
| **Provider**           | A plugin that talks to one cloud (e.g., `google`)                        |
| **Resource**           | A thing Terraform creates (`google_storage_bucket`, `google_sql_...`)    |
| **Data source**        | A read-only lookup of something that already exists                      |
| **State**              | Terraform's record of what it created — JSON file (local or remote)      |
| **Backend**            | Where state lives (local file vs GCS bucket with locking)                |
| **Plan**               | Diff between desired (HCL) and actual (state + cloud)                    |
| **Apply**              | Execute the plan                                                         |
| **Drift**              | Reality has diverged from state (someone clicked in the console)         |
| **Import**             | Bring an existing cloud resource under Terraform management              |
| **Module**             | Reusable bundle of resources — like a Scala trait + impl                 |
| **Workspace**          | Named state file (e.g., `dev`, `prod`) sharing the same HCL              |
| **Variable / Output**  | Like function parameters and return values                               |
| **`for_each` / `count`** | Loops over a map/list to create N copies of a resource                 |

### Scala analogies

| Scala                              | Terraform                                              |
| ---------------------------------- | ------------------------------------------------------ |
| `case class Bucket(name: String)`  | `resource "google_storage_bucket" "x" { name = ... }`  |
| `val x = compute(...)`             | `locals { x = ... }`                                   |
| Type signature                     | `variable "x" { type = string }`                       |
| Function return                    | `output "x" { value = ... }`                           |
| `Map[K, V].map`                    | `for_each = var.things`                                |
| Implicit module wiring             | `module "network" { source = "./modules/network" }`    |
| `IO[Unit]` (description vs run)    | HCL is *desired state*; `apply` is the runtime        |

---

## Setup

### Tools to install

```bash
# Terraform (use tfenv to manage versions)
brew install tfenv
tfenv install 1.9.5
tfenv use 1.9.5

# Google Cloud CLI
brew install --cask google-cloud-sdk

# Docker (you already have it for this project)
docker --version
```

### Repository layout you'll build

```
infra/
├── LEARNING_GUIDE.md          <- this file
└── terraform/
    ├── main.tf                <- start simple, refactor later
    ├── variables.tf
    ├── outputs.tf
    ├── backend.tf             <- added in Step 7
    ├── terraform.tfvars       <- gitignored: your project ID etc.
    └── modules/               <- added in Step 28
        ├── network/
        ├── data/
        ├── runtime/
        └── cdn/
```

---

## Phase 0 — Foundations

### Step 1 — GCP project and tooling

**Concept:** A GCP **project** is the unit of billing, IAM, and resource
isolation. Everything you create lives in exactly one project.

**Goal:** Have a project, billing enabled, `gcloud` authenticated, and
Application Default Credentials (ADC) so Terraform can call GCP.

**What to do:**

1. Create a GCP account (free $300 credit). Use a personal Gmail.
2. Create a new project. Pick a globally unique ID like
   `nwc-learn-<your-initials>`. **Project IDs are immutable** — choose
   carefully.
3. Link a billing account to the project.
4. Run `gcloud auth login` (opens browser).
5. Run `gcloud auth application-default login` — this creates
   `~/.config/gcloud/application_default_credentials.json`. Terraform
   uses this automatically.
6. Set defaults: `gcloud config set project <id>` and
   `gcloud config set compute/region europe-west1` (pick the region
   closest to you).

**Key ideas:**

- ADC ≠ user login. ADC is a separate file that *libraries* (including
  Terraform) read. You ran two commands for a reason.
- Region choice cascades — Cloud SQL/Run/LB pricing varies by region.
- `gcloud config configurations` lets you have multiple profiles
  (work, personal). Useful later.

**Stuck?** Ask: "billing not linking" or "ADC isn't being picked up".

---

### Step 2 — Your first resource (local state)

**Concept:** HCL describes desired state. `terraform init` downloads
providers, `plan` diffs, `apply` executes.

**Goal:** Create one GCS bucket via Terraform with **local state**.

**What to do:**

1. In `infra/terraform/`, create `main.tf` with:
   - `terraform { required_providers { google = { ... } } }` block
   - `provider "google" { project = ... region = ... }` block
   - One `resource "google_storage_bucket" "playground"` — pick any
     globally unique name (bucket names are global!)
2. Run `terraform init`. Look at what it created:
   `.terraform/`, `.terraform.lock.hcl`.
3. Run `terraform plan`. Read the output carefully — what does the
   `+` mean? What's the difference between `(known after apply)` and
   a literal value?
4. Run `terraform apply`. Look at `terraform.tfstate` — yes, **open
   the JSON file**. This is the thing we'll obsess over later.
5. Run `terraform apply` *again* with no changes. What happens? Why?

**Key ideas:**

- The lock file pins provider versions for reproducibility — commit it.
- State is a *cache* of cloud reality. Terraform consults it before
  every plan to avoid hitting every API.
- "Globally unique bucket name" means *across all GCP customers*.
  Convention: prefix with project ID.

**Hints:**

- `required_providers` syntax: `google = { source = "hashicorp/google", version = "~> 5.0" }`.
- The `~> 5.0` operator means "any 5.x, but not 6.x".

**Break-it preview:** in Phase 2 you'll mess this state up on purpose.
For now, leave the bucket alone.

---

### Step 3 — Variables, outputs, `.tfvars`

**Concept:** Don't hardcode. Variables are inputs, outputs are return
values, `.tfvars` files supply variable values without committing them.

**Goal:** Move `project`, `region`, and the bucket name into variables.
Output the bucket's URL.

**What to do:**

1. Create `variables.tf` declaring `project_id`, `region`,
   `bucket_name`. Each gets a `type` and ideally a `description`.
2. Create `terraform.tfvars` with actual values. **Add it to
   `.gitignore`** — it can contain secrets later.
3. Create `outputs.tf` with `output "bucket_url" { value = ... }`.
4. Run `terraform plan` — same plan, no diff.
5. Run `terraform output` after apply.

**Key ideas:**

- `var.x` reads a variable; `local.x` reads a `locals { x = ... }`
  block (computed value, not user-supplied).
- Variable precedence: CLI flag > `*.auto.tfvars` > `terraform.tfvars`
  > env vars (`TF_VAR_x`) > default in `variable` block.
- Outputs are how modules return values to their caller. Even at the
  root level they double as a "what did Terraform create?" report.

**Stuck?** Ask: "how do I make a variable optional with a default?"

---

## Phase 1 — Real resources (still local state)

### Step 4 — Enable APIs as code

**Concept:** GCP requires you to *enable* an API before using it (e.g.,
`run.googleapis.com`). Click-ops works, but enabling APIs is itself
infrastructure — put it in Terraform.

**Goal:** Enable Cloud Run, Cloud SQL Admin, Memorystore, Artifact
Registry, Compute, VPC Access, IAM, and Secret Manager APIs as
Terraform resources.

**What to do:**

1. Use `google_project_service` resource. There are ~8 APIs to enable.
2. **Use `for_each`** with a `set()` of API names — don't write 8
   `resource` blocks. This is the moment you learn `for_each`.
3. Set `disable_on_destroy = false` (you'll thank yourself when
   destroying — disabling APIs while resources still exist is a mess).

**Key ideas:**

- `for_each = toset([...])` creates N resources keyed by the set
  values. Reference one as `google_project_service.apis["run.googleapis.com"]`.
- Enabling APIs takes 30+ seconds each. Don't panic during the first apply.

**Hints:**

```hcl
resource "google_project_service" "apis" {
  for_each = toset(["run.googleapis.com", "sqladmin.googleapis.com", ...])
  service  = each.value
  # ...
}
```

---

### Step 5 — Artifact Registry repo

**Concept:** Artifact Registry is GCP's Docker registry. Your Cloud Run
service pulls images from here.

**Goal:** Create a Docker repo named `nwc-backend`.

**What to do:**

1. Add a `google_artifact_registry_repository` resource.
2. `format = "DOCKER"`, give it a region.
3. Add `depends_on = [google_project_service.apis]` so Terraform waits
   for the API to be enabled. **Or** reference one of the API
   resources implicitly (we'll discuss explicit vs implicit deps).
4. Output the repo URL: format is
   `<region>-docker.pkg.dev/<project>/<repo>`.

**Key ideas:**

- Implicit dependencies (referencing `.id` of another resource) are
  preferred over `depends_on`. Use `depends_on` only when no attribute
  reference makes sense (like for API enablement).

---

### Step 6 — A starter GCS bucket (refactor)

**Goal:** Replace the playground bucket from Step 2 with two purposeful
buckets: one for **Terraform state** (used in Step 7) and one for the
**frontend static site** (used in Phase 6).

**What to do:**

1. Add two `google_storage_bucket` resources with descriptive names.
2. Set `uniform_bucket_level_access = true` (modern best practice).
3. **For the state bucket:** enable versioning
   (`versioning { enabled = true }`). This lets you recover from
   state corruption (you'll do this on purpose in Step 12).
4. **Delete the Step 2 playground bucket** — remove the resource block,
   run `plan`, observe the `-` (destroy), run `apply`.
5. Use `for_each` over a `map` if you want both buckets in one
   resource block — your call.

**Key ideas:**

- Removing a `resource` block from HCL → Terraform plans to **destroy**
  it. This is the *right* default but bites you with refactors. We'll
  fix that with `moved` blocks in Step 11.
- Bucket versioning costs almost nothing for state files. Always on.

---

## Phase 2 — State management & breaking things 🔥

This phase is the heart of the guide. You'll deliberately break state
five different ways and recover each time. Read each step before doing
it — some require you to commit state changes that need recovery.

### Step 7 — Migrate local state to GCS backend

**Concept:** Local state breaks teams (no sharing, no locking, lost
laptop = lost state). The `gcs` backend stores state in a bucket with
object-level locking.

**Goal:** Move your local `terraform.tfstate` into the state bucket
from Step 6.

**What to do:**

1. Add a `backend.tf`:
   ```hcl
   terraform {
     backend "gcs" {
       bucket = "<your-state-bucket-name>"
       prefix = "nwc/state"
     }
   }
   ```
2. Run `terraform init`. Terraform asks: "do you want to copy existing
   state to the new backend?" — say **yes**.
3. Inspect the state bucket in the GCP console. See the `default.tfstate`
   object and note the `prefix`.
4. **Delete `terraform.tfstate` and `terraform.tfstate.backup` from your
   local working directory.** They're now obsolete and dangerous to
   keep around (could overwrite remote).
5. Run `terraform plan` — should be no changes.

**Key ideas:**

- The `prefix` is a path inside the bucket. Different `prefix` =
  different state file. This is how workspaces work later.
- Locking: GCS uses object generation numbers. Two concurrent
  `terraform apply` calls will fail one of them safely.
- The `bucket` value **cannot use variables** — backend config is
  evaluated before variables are. Hardcode it (or use `-backend-config`
  CLI flags).

**Stuck?** Ask: "init says backend changed but I can't migrate".

---

### Step 8 — 🔥 Break-it #1: Drift from console

**Concept:** Someone (you) clicks something in the GCP console and
changes a resource Terraform manages. Terraform's next plan should
detect this and offer to revert it.

**Setup:** Make sure your buckets from Step 6 exist and state is remote.

**Break it:**

1. Go to the GCP console → Storage → your **frontend bucket**.
2. Add a label: key `owned-by`, value `intern-eve`.
3. Don't tell Terraform anything.

**Observe:**

4. Run `terraform plan`. Look for the bucket in the diff.
5. **What does Terraform want to do?** Revert your manual label change.
   Read the diff carefully — `~ labels = {...}`.

**Two ways to fix:**

- **Option A — accept the drift:** Update your HCL to *include* the
  label, so Terraform stops planning to remove it.
- **Option B — reject the drift:** Just `terraform apply` and let
  Terraform put the cloud back to match HCL.

**Do both:** apply once with Option A, then revert your HCL and apply
again to demonstrate Option B. Watch what changes each time.

**Key ideas:**

- Drift is *information*, not always an emergency. Labels are usually
  not worth fighting over; security settings absolutely are.
- `terraform plan -refresh-only` shows drift without proposing changes.
  Useful for audit.
- This is why teams forbid console edits to TF-managed resources.

---

### Step 9 — 🔥 Break-it #2: `terraform state rm` + reimport

**Concept:** State and HCL can disagree in a way that needs *manual*
state surgery. Removing a resource from state without removing it from
HCL is one such case.

**Break it:**

1. Pick the **frontend bucket**. Run:
   ```bash
   terraform state rm google_storage_bucket.frontend
   ```
   (Use the actual resource address. `terraform state list` shows them.)
2. Run `terraform plan`.

**Observe:**

3. Terraform now thinks the bucket doesn't exist and plans to **create**
   it. But the bucket *does* exist in GCP.
4. If you naively `apply`, GCP returns `409 Conflict` ("bucket already
   exists, and globally unique").

**Recover (the part where you learn `import`):**

5. Use the modern declarative import block (Terraform 1.5+):
   ```hcl
   import {
     to = google_storage_bucket.frontend
     id = "<your-bucket-name>"
   }
   ```
6. Run `terraform plan` — Terraform now plans to import (no destroy/create).
7. Run `terraform apply`. State is healed.
8. **Remove the `import` block** after import succeeds (it's a one-shot).

**Alternate recovery (CLI form, older but still works):**

```bash
terraform import google_storage_bucket.frontend <your-bucket-name>
```

**Key ideas:**

- `terraform state rm` is destructive to state, **harmless to cloud**.
  It's the right tool when you want to "let go" of a resource without
  destroying it.
- `import` block is declarative — code-reviewable, works in CI. The
  CLI form is imperative.
- Different resource types have different import IDs. Bucket = name.
  Cloud SQL instance = `<project>/<instance>`. The provider docs list
  the format under each resource's "Import" section.

---

### Step 10 — 🔥 Break-it #3: Out-of-band deletion

**Concept:** Someone deletes a resource directly in the console.
Terraform's state still has it. What happens?

**Break it:**

1. Pick a low-stakes resource — let's add a *new* test bucket via
   Terraform first so you don't break the state bucket. Apply it.
2. Go to the console and **delete the test bucket** by hand.
3. Run `terraform plan`.

**Observe:**

4. Terraform notices the resource is missing and plans to **recreate**
   it. (This is the inverse of Step 9.)

**You have two choices:**

- **Re-create:** `terraform apply` and let it come back.
- **Adopt the deletion:** remove the resource block from HCL, then
  `apply`. But wait — Terraform will plan to destroy something that
  doesn't exist. That's fine; it just removes from state.

**Variant — drift on a *property*:**

5. Add a versioning block to a bucket via HCL and apply.
6. In console, **disable versioning** on that bucket.
7. `terraform plan` — Terraform wants to re-enable it.
8. This is the kind of drift that *matters* (security/compliance).

**Key ideas:**

- Out-of-band deletion is detected on `plan` because Terraform
  refreshes state from cloud first.
- `terraform refresh` (deprecated) / `terraform apply -refresh-only`
  updates state to match cloud without applying any HCL changes.
  Useful when adopting cloud-side decisions.

---

### Step 11 — 🔥 Break-it #4: Renaming without churn (`moved` blocks)

**Concept:** You want to rename `google_storage_bucket.frontend` to
`google_storage_bucket.web_assets` because the new name is clearer.
Naively, this destroys and recreates the bucket — which means **data
loss** for stateful resources.

**Break it (safely):**

1. Rename the resource block in HCL.
2. Run `terraform plan`. Look at the output: **destroy + create**.
   You do *not* want this in real life.

**Recover with `moved`:**

3. Add a `moved` block:
   ```hcl
   moved {
     from = google_storage_bucket.frontend
     to   = google_storage_bucket.web_assets
   }
   ```
4. Run `terraform plan` again. Now: **just a state move**, no cloud
   change.
5. Apply. Then remove the `moved` block (it's a one-time migration,
   like a DB migration script).

**Equivalent CLI form:**

```bash
terraform state mv google_storage_bucket.frontend google_storage_bucket.web_assets
```

(But the `moved` block is reviewable in PRs and works in CI.)

**Key ideas:**

- This is the Terraform equivalent of a Scala "rename refactor" —
  identity should follow the rename, not destroy the value.
- Same pattern works when moving a resource into a module:
  `moved { from = google_x.y, to = module.network.google_x.y }`.
- Stateless resources (buckets are *almost* stateless if empty,
  databases very much aren't) make rename-as-recreate disastrous.
  Always check the plan before applying a rename.

---

### Step 12 — 🔥 Break-it #5: Stale lock and force-unlock

**Concept:** `terraform apply` takes an exclusive lock on state. If the
process dies (Ctrl-C twice, network drop, laptop sleeps), the lock can
get stuck.

**Break it:**

1. Run `terraform apply` and **type `yes`** to confirm.
2. Immediately **kill the process** (Ctrl-C twice, or
   `kill -9 <pid>` from another terminal). Try to do it during the
   apply, not before.
3. Run `terraform plan` from another shell.

**Observe:**

4. Error: `Error acquiring the state lock`. There's a lock ID printed.

**Recover:**

5. Check the bucket — there's a `default.tflock` object next to your
   state file.
6. Run `terraform force-unlock <lock-id>`. **This is dangerous in real
   life** because you might be unlocking someone else's running
   apply. Verify nobody else is applying first.

**Key ideas:**

- The lock is a tiny object holding metadata about who took it. GCS's
  generation-precondition writes implement the locking primitive.
- In CI, design pipelines so concurrent applies are impossible (queue
  the workflow, single-runner, etc.) rather than relying on
  `force-unlock`.

**Bonus — state file recovery:**

7. In the console, delete the *current* version of the state object.
8. Run `terraform plan`. Cry.
9. Recover: in the bucket, view object versions, restore the previous
   version. **This is why we enabled versioning in Step 6.**

---

### Step 13 — 🔥 Break-it #6: Adopt an unmanaged resource via `import`

**Concept:** You have infra that wasn't created by Terraform — maybe
created manually, maybe by a colleague, maybe by a different tool. You
want to bring it under Terraform management.

**Break it (or rather, set it up):**

1. In the console, create a Pub/Sub topic named
   `nwc-out-of-band-topic`. (Pub/Sub is cheap; we won't use it for
   real, but it's a good import target.)
2. Don't add anything to HCL yet.

**Adopt it:**

3. Find the right resource type in the
   [google provider docs](https://registry.terraform.io/providers/hashicorp/google/latest/docs)
   — search "pubsub topic". Note the **Import** section at the bottom
   — it tells you the ID format: `projects/<project>/topics/<name>`.
4. Add an `import` block:
   ```hcl
   import {
     to = google_pubsub_topic.out_of_band
     id = "projects/<project>/topics/nwc-out-of-band-topic"
   }
   ```
5. Add a *minimal* `resource` block matching the topic:
   ```hcl
   resource "google_pubsub_topic" "out_of_band" {
     name = "nwc-out-of-band-topic"
   }
   ```
6. Run `terraform plan`. Read the diff.

**Observe:**

7. Terraform is smart: it shows a **plan** for the import + any
   property mismatches between HCL and reality. If your HCL is
   incomplete (missing labels the cloud has), you'll see drift.
8. Iterate: update HCL until plan shows "import only, no changes".

**Apply and clean up:**

9. `terraform apply`.
10. Remove the `import` block.
11. Now you can delete this topic via Terraform (`destroy` or remove
    block) — full ownership.

**Key ideas:**

- `terraform plan -generate-config-out=imported.tf` (1.5+) generates a
  **starter HCL** for an import block. Try it on this topic to see —
  the generated code is messy but a useful starting point for big
  imports.
- For complex resources (Cloud SQL, GKE clusters), import is harder
  because there are sub-resources (users, databases, node pools) each
  needing their own import. There's a tool called `terraformer` that
  bulk-imports.

---

## Phase 3 — Networking

### Step 14 — VPC, public + private subnets, NAT

**Concept:** A VPC is a private network. Subnets carve it into ranges.
"Public" subnets have resources with external IPs; "private" subnets
have resources that egress through a NAT gateway.

**Goal:** Create:
- One VPC `nwc-vpc` (custom mode, not auto-mode)
- One **public** subnet `nwc-public` (10.10.0.0/24)
- One **private** subnet `nwc-private` (10.10.1.0/24) with **Private
  Google Access** enabled
- A Cloud Router and Cloud NAT so private resources can reach the
  internet (e.g., `apt-get` updates) without external IPs

**What to do:**

1. `google_compute_network` with `auto_create_subnetworks = false`.
2. Two `google_compute_subnetwork` resources. Look up
   `private_ip_google_access` on the private one.
3. `google_compute_router` (regional).
4. `google_compute_router_nat` — set
   `source_subnetwork_ip_ranges_to_nat = "LIST_OF_SUBNETWORKS"` and
   list only the private subnet.

**Key ideas:**

- "Public subnet" is **not a real GCP concept**. In GCP, any subnet
  can host a VM with or without an external IP. The public/private
  distinction is *what you put in it*. (AWS people: this is different
  from there.)
- Private Google Access lets resources without external IPs reach
  Google APIs (Cloud SQL, GCS, etc.) over private IPs.
- Cloud NAT is a managed NAT — no instance to run.

**Stuck?** Ask: "what CIDR ranges should I pick?"

---

### Step 15 — Serverless VPC Connector

**Concept:** Cloud Run is serverless and lives outside your VPC by
default. To reach Cloud SQL/Memorystore on private IPs, Cloud Run
egress traffic must go through a **VPC Connector** — a small managed
NAT-like bridge with its own subnet range.

**Goal:** Create a VPC connector in a dedicated `/28` range.

**What to do:**

1. Add a third subnet: `nwc-connector` with range `10.10.2.0/28`.
2. Add `google_vpc_access_connector` referencing that subnet.
3. Note the machine size — `e2-micro` is cheapest, fine for learning.

**Key ideas:**

- Connectors cost a few cents/hour while running. Destroy when not in
  use (will be a recurring theme).
- Connectors are regional. Your Cloud Run service must be in the same
  region.

---

## Phase 4 — Data layer

### Step 16 — Cloud SQL (Postgres) on private IP

**Concept:** Cloud SQL with a private IP is reached over your VPC, not
the internet. To allow this, you need a **VPC peering** between your
VPC and Google's services-VPC.

**Goal:** A `db-f1-micro` Postgres 15 instance with a private IP, a
database named `networthcalculator`, and a user.

**What to do:**

1. **Allocate an IP range** for service peering:
   `google_compute_global_address` with `purpose = "VPC_PEERING"` and
   `prefix_length = 16`.
2. **Create the peering connection:**
   `google_service_networking_connection`.
3. Create the Cloud SQL instance:
   `google_sql_database_instance`. Inside `settings.ip_configuration`,
   set `ipv4_enabled = false` and `private_network = <vpc id>`.
4. Add a `google_sql_database` for `networthcalculator`.
5. Add a `google_sql_user` — but **don't put the password in HCL**.
   Use a `random_password` resource and we'll move it to Secret
   Manager in Step 18.

**Key ideas:**

- The peering is a one-time setup per VPC, but conceptually weird:
  you're letting Google's managed-services VPC peer with yours so they
  can route private traffic.
- Cloud SQL takes 5–10 minutes to create. Get coffee.
- `db-f1-micro` is shared-core (slow) but cheap. Production uses
  `db-custom-N-Mmb` (dedicated cores).

**🔥 Break-it #7 (optional):**

After it's up, change the `tier` in the GCP console to `db-g1-small`.
Run `terraform plan`. You'll see drift. Decide: revert, or update HCL?

---

### Step 17 — Memorystore (Redis) on private IP

**Goal:** A 1 GB Basic-tier Redis instance on the same VPC.

**What to do:**

1. `google_redis_instance` with `tier = "BASIC"`,
   `memory_size_gb = 1`, `authorized_network = <vpc id>`.
2. `connect_mode = "PRIVATE_SERVICE_ACCESS"` (uses the same peering
   you set up for Cloud SQL).
3. Output the host + port.

**Key ideas:**

- "Basic" tier = no replica, no failover. Cheaper. Fine for learning.
- Memorystore is **the** expensive line item — destroy when you stop.

---

### Step 18 — Secret Manager for credentials

**Concept:** Don't put secrets in state files (state is plaintext-ish
JSON in a bucket). Don't put them in HCL. Use Secret Manager.

**Goal:** Store the SQL password and a JWT signing secret in Secret
Manager. Cloud Run will read them at runtime.

**What to do:**

1. `google_secret_manager_secret` (the secret container) +
   `google_secret_manager_secret_version` (the actual value).
2. Use `random_password` resources for the values:
   ```hcl
   resource "random_password" "db" { length = 32, special = false }
   ```
3. Grant Cloud Run's service account `roles/secretmanager.secretAccessor`.

**Key ideas:**

- `random_password` values *are* in state. State bucket needs strict
  IAM. Better than HCL, not perfect. Production teams sometimes use
  external secret tools (Vault) and Terraform only references them.
- A secret has a "container" and "versions". Rotating = adding a new
  version, updating consumers to point at it.

---

## Phase 5 — Backend on Cloud Run

### Step 19 — Manual Docker push (no Terraform)

**Concept:** Before automating, do it once by hand to understand the
moving parts. Same pattern as the frontend guide's manual `fetch`
before TanStack Query.

**Goal:** Build the Scala backend's Docker image, push it to Artifact
Registry, by hand.

**What to do:**

1. From repo root: `sbt docker:publishLocal`. Note the image name —
   probably `networthcalculator:latest`.
2. Tag it for Artifact Registry:
   ```bash
   docker tag networthcalculator:latest \
     <region>-docker.pkg.dev/<project>/nwc-backend/backend:v1
   ```
3. Configure Docker auth:
   ```bash
   gcloud auth configure-docker <region>-docker.pkg.dev
   ```
4. Push: `docker push <full-tag>`.
5. View it in the GCP console under Artifact Registry.

**Key ideas:**

- Image tag = identity. `:latest` is an anti-pattern in CI/CD; use
  immutable tags (git SHA).
- This step is throwaway in terms of automation but burns the moving
  parts into your brain.

---

### Step 20 — Cloud Run service via Terraform

**Goal:** Deploy the backend as a Cloud Run service that:
- Runs the image you pushed
- Reads DB/Redis credentials from Secret Manager
- Egresses through the VPC connector
- Has ingress restricted to LB-only

**What to do:**

1. `google_service_account` for the Cloud Run service. Grant it the
   Secret Manager and Cloud SQL Client roles.
2. `google_cloud_run_v2_service` (use **v2**, not v1):
   - `template.containers[0].image = <your image tag>`
   - `template.vpc_access.connector = <connector id>`,
     `egress = "PRIVATE_RANGES_ONLY"`
   - `template.containers[0].env` — set `JDBC_URL` etc., reading
     secrets via `value_source.secret_key_ref`
   - `template.service_account = <sa email>`
   - `ingress = "INGRESS_TRAFFIC_INTERNAL_LOAD_BALANCER"`
3. **Don't** make it publicly invokable yet — we'll route through an
   LB next step.

**Key ideas:**

- Cloud Run v2 API is much cleaner than v1. Always use v2 for new code.
- `INGRESS_TRAFFIC_INTERNAL_LOAD_BALANCER` makes the service
  unreachable from the public internet — you can't even hit it with
  `curl` from your laptop. Only the LB can.

**🔥 Break-it #8 (optional):** in console, deploy a new Cloud Run
revision with a different image tag. Run `terraform plan`. Watch
Terraform try to revert your manual deploy. Discussion: in real life,
you'd likely want CI to update `image` in TF rather than fight it.

---

### Step 21 — Internal HTTPS Load Balancer for the backend

**Concept:** An internal LB lives inside your VPC and is reachable from
your VPC (and via the external LB you build in Phase 6).

**Goal:** Internal HTTPS LB → Cloud Run backend.

**What to do:**

This is the most fiddly step in the guide. The pieces:

1. `google_compute_region_network_endpoint_group` — a "serverless NEG"
   pointing at your Cloud Run service.
2. `google_compute_region_backend_service` — references the NEG.
3. `google_compute_region_url_map` — routes paths to the backend
   service.
4. `google_compute_region_target_https_proxy` — terminates TLS.
5. `google_compute_region_ssl_certificate` — self-signed for learning,
   managed cert for prod.
6. `google_compute_forwarding_rule` — the actual "IP + port"
   listener. Must be `INTERNAL_MANAGED` scheme.
7. A proxy-only subnet for the LB (regional internal LBs need this).

**Hints:**

- Look at the
  [Internal HTTPS LB tutorial](https://cloud.google.com/load-balancing/docs/l7-internal/setting-up-l7-internal-serverless)
  — but resist copy-paste, write the HCL yourself.
- The proxy-only subnet is a *third* subnet (after public/private
  from Step 14). Range like `10.10.3.0/26` with
  `purpose = "REGIONAL_MANAGED_PROXY"`.

**Key ideas:**

- 7 resources for one LB feels excessive. It is. Google's LB model is
  composable but verbose. Modules in Step 28 will hide this.
- "Internal" = only reachable from this VPC + peered VPCs.

---

## Phase 6 — Frontend hosting

### Step 22 — GCS static site + Cloud CDN + external HTTPS LB

**Goal:** Public URL `https://<your-domain-or-IP>` serves the React
frontend from your `frontend` bucket, with CDN.

**What to do:**

1. Make sure your frontend bucket has `website { main_page_suffix = "index.html" }`.
2. `google_compute_backend_bucket` referencing the bucket, with
   `enable_cdn = true`.
3. **External** counterparts of the LB pieces from Step 21:
   - `google_compute_url_map` (global, not regional)
   - `google_compute_target_https_proxy`
   - `google_compute_managed_ssl_certificate` (or self-signed for
     learning — but managed certs are free if you have a domain)
   - `google_compute_global_forwarding_rule` (`EXTERNAL_MANAGED`)
   - `google_compute_global_address` for the public IP
4. Make the bucket objects publicly readable:
   `google_storage_bucket_iam_member` granting
   `roles/storage.objectViewer` to `allUsers`. (UBLA enabled means
   you do this on the bucket, not per-object.)
5. Manually upload a placeholder `index.html` via `gsutil cp` so you
   can test. (Phase 7's pipeline replaces this.)

**Key ideas:**

- GCS doesn't *natively* have HTTPS for buckets. The external LB +
  backend bucket combo is how you get CDN + HTTPS.
- Cache invalidation: when you re-upload `index.html`, the CDN
  doesn't auto-refresh. Step 26 covers this.

---

### Step 23 — Routing: `/` to GCS, `/api/*` to backend

**Goal:** One public URL handles both frontend and API. `/api/*`
proxies to the **internal LB**.

**Decision point:** Two ways to wire this:

- **A) External LB → both:** Add a backend service in the *external*
  URL map that points at the Cloud Run NEG directly. Skip the
  internal LB. Simpler.
- **B) External LB → Internal LB → Cloud Run:** True "edge → internal"
  pattern. More realistic for enterprise (where backend has to be
  internal-only). Requires `google_compute_region_network_endpoint_group`
  with `network_endpoint_type = "INTERNET_FQDN_PORT"` to point at the
  internal LB's IP… which is awkward.

**Recommendation for learning:** start with A, but **leave the
internal LB in place** so you've built it. Real prod uses B with a
second hop (often via a Service Mesh, Apigee, or just colocated
backends).

**What to do (option A):**

1. Add a new `google_compute_backend_service` (global) pointing at the
   Cloud Run serverless NEG (just like Step 21 but global scope).
2. Update the URL map: add a `path_matcher` rule routing `/api/*` to
   this backend service, with the rest going to the bucket backend.

**Key ideas:**

- URL maps are powerful — host + path routing, header rewrites,
  redirects. Worth reading the docs even if not used here.
- CORS would otherwise be a problem; same-origin makes it disappear.

---

## Phase 7 — CI/CD with GitHub Actions

### Step 24 — Workload Identity Federation (no static keys)

**Concept:** GitHub Actions needs to call GCP APIs. The bad way: a
service account JSON key stored as a GitHub secret. The good way:
**Workload Identity Federation (WIF)** — GitHub's OIDC token is
exchanged for short-lived GCP credentials with no static keys.

**Goal:** GHA can authenticate to your GCP project as a service
account, scoped to your specific repo.

**What to do:**

1. `google_iam_workload_identity_pool` — a logical container.
2. `google_iam_workload_identity_pool_provider` — type "OIDC", issuer
   `https://token.actions.githubusercontent.com`. Set an attribute
   condition like
   `assertion.repository == "raffaeler/net-worth-calculator"` so only
   your repo can use it.
3. `google_service_account` for GHA (e.g., `gha-deployer`).
4. `google_service_account_iam_binding` granting
   `roles/iam.workloadIdentityUser` to the GitHub principal:
   `principalSet://iam.googleapis.com/<pool>/attribute.repository/<owner/repo>`.
5. Grant the service account project roles it needs (Cloud Run admin,
   Artifact Registry writer, Storage admin, Service Account User on
   the Cloud Run runtime SA).

**Key ideas:**

- WIF feels magical but it's just OIDC token exchange. GitHub's signed
  JWT contains claims; GCP trusts them per the pool/provider config.
- Attribute conditions are your security boundary. **Without them**,
  any GitHub repo on Earth could mint your tokens.
- The `principalSet://` syntax is GCP-specific and confusing. Read
  Google's WIF docs once to lock it in.

---

### Step 25 — App pipeline: build, push, deploy backend

**Goal:** A `.github/workflows/deploy-backend.yml` that, on push to
`master`:
1. Builds the Scala Docker image
2. Pushes to Artifact Registry
3. Deploys a new Cloud Run revision

**What to do:**

1. Create the workflow YAML. Steps:
   - `actions/checkout@v4`
   - `google-github-actions/auth@v2` with WIF config (the pool +
     provider IDs from Step 24)
   - `setup-java` + `sbt docker:publishLocal`
   - `docker tag` + `docker push`
   - `google-github-actions/deploy-cloudrun@v2` to update the service
     to the new image tag (use git SHA)
2. **Don't** manage the image tag from Terraform anymore. Use a
   `lifecycle { ignore_changes = [template[0].containers[0].image] }`
   block on the Cloud Run resource so Terraform stops fighting CI.

**Key ideas:**

- `ignore_changes` is the standard pattern for "Terraform creates the
  shape, CI updates the contents". Same for Cloud Run images, GKE
  deployments, etc.
- Tag images with `${{ github.sha }}`, never `:latest`. Lets you
  roll back by redeploying an old SHA.

---

### Step 26 — Frontend pipeline: build + sync GCS + invalidate CDN

**Goal:** On push affecting `frontend/`, build the React app and
publish to GCS.

**What to do:**

1. Workflow `.github/workflows/deploy-frontend.yml`.
2. `paths: [frontend/**]` filter so it only runs on frontend changes.
3. Steps: WIF auth → `npm ci` → `npm run build` → `gsutil rsync -d -r
   frontend/dist gs://<frontend-bucket>` → `gcloud compute url-maps
   invalidate-cdn-cache <url-map> --path "/*"`.

**Key ideas:**

- `rsync -d` deletes objects that don't exist locally — important so
  old assets clean up.
- CDN invalidation is **not free** in time (~minutes) but is in cost
  (within limits). Use carefully.
- For long-term, set short cache TTL on `index.html`, long TTL on
  hashed JS/CSS — that way you barely need to invalidate.

---

### Step 27 — Infra pipeline: `plan` on PR, `apply` on merge

**Goal:** Terraform changes go through code review, with `plan`
visible in the PR and `apply` automated on merge.

**What to do:**

1. `.github/workflows/terraform.yml` with two jobs:
   - **plan** — runs on PR. WIF auth, `terraform init`, `plan -out=tfplan`.
     Post the plan output as a PR comment (use a community action or
     a script).
   - **apply** — runs on push to `master`. Same setup, then `apply`.
2. Set the GHA service account's permissions narrowly — it needs
   admin on the resources you manage, but **not** project owner.
3. Configure environments + required reviewers in GitHub settings so
   `apply` can require manual approval.

**Key ideas:**

- This is where state locking earns its keep — concurrent applies
  from different PRs would clobber state without it.
- Some teams gate `apply` behind a manual `terraform apply` step
  (Atlantis, Terraform Cloud) rather than auto-on-merge. Either is
  reasonable.

**🔥 Break-it #9:**

Push a PR that intentionally has bad HCL — say, a wrong variable
name. Watch the plan job fail. Now push a PR that's *valid* HCL but
will fail at apply (e.g., a bucket name that's already taken
globally). Plan succeeds, apply fails. Discussion: how do you make
your pipeline robust to apply-time failures?

---

## Phase 8 — Refactor and operate

### Step 28 — Split into modules

**Concept:** Like extracting a Scala trait. A module is a folder of
HCL files with its own `variables.tf` (input) and `outputs.tf`
(return values).

**Goal:** Refactor your monolithic `main.tf` into `modules/network/`,
`modules/data/`, `modules/runtime/`, `modules/cdn/`, `modules/cicd/`.

**What to do:**

1. Create `modules/network/` with `main.tf`, `variables.tf`,
   `outputs.tf`. Move the VPC + subnets + NAT + connector resources
   into it. Outputs: VPC ID, subnet IDs, connector ID.
2. Update root `main.tf` to call:
   ```hcl
   module "network" {
     source = "./modules/network"
     # ... vars
   }
   ```
3. **Use `moved` blocks** (Step 11!) to migrate state from
   `google_compute_network.foo` → `module.network.google_compute_network.foo`
   without destroy/create.
4. Repeat for the other modules.

**Key ideas:**

- Modules force you to declare interfaces. Like making a trait.
- Module outputs are the only way the caller sees inside the module.
  Like `private` members.
- `source` can be a local path, git URL, or Terraform Registry. The
  registry has high-quality community modules — read a few.

---

### Step 29 — Multiple environments

**Concept:** You'll want `dev` and `prod` eventually. Two patterns:

- **Workspaces:** same HCL, different state files keyed by workspace
  name. Quick but encourages bad habits (dev/prod diverge subtly,
  hard to see).
- **Per-environment directories:** `environments/dev/`,
  `environments/prod/`, each calls the same modules with different
  vars. More files, but explicit and reviewable.

**Industry preference:** directories. Workspaces are fine for a
truly identical dev/prod, but production environments inevitably grow
their own quirks.

**Goal:** Move your existing setup to `environments/dev/`. Don't
build prod for real (cost!), but lay out the directory.

**What to do:**

1. Create `environments/dev/` containing thin `main.tf`,
   `variables.tf`, `terraform.tfvars`, `backend.tf`. Each calls the
   modules.
2. Adjust your backend config: different `prefix` per environment.
3. Use `moved` blocks again to migrate state from old root to new.

**Key ideas:**

- Variables that differ between envs: project ID, region, tier sizes,
  domain. Keep modules vanilla; variables differentiate.
- `environments/prod/` would mirror `dev/` with different
  `terraform.tfvars` and different state prefix.

---

### Step 30 — Teardown

**Goal:** Destroy everything. Verify the bill stops.

**What to do:**

1. Run `terraform destroy` from `environments/dev/` (or root, if you
   skipped Step 29).
2. **Cloud SQL has `deletion_protection = true` by default in newer
   provider versions.** You'll need to set it `false` and apply,
   *then* destroy. This is intentional friction.
3. Memorystore has no equivalent flag — it'll just go.
4. **Watch out for:** the state bucket itself! If you put it in
   Terraform, it'll try to destroy the bucket holding its own state.
   Solutions:
   - Don't manage the state bucket via Terraform (manual one-time
     creation), **or**
   - Manage it in a separate "bootstrap" config with its own local
     state.
5. Disable APIs (optional). If you set `disable_on_destroy = false`
   in Step 4, they stay enabled. Doesn't cost anything to leave on.
6. Check the GCP Billing console next day — costs should drop.

**Cost forensics:**

- Billing → Reports → group by service. Anything still costing money
  after destroy means a leftover resource. Common culprits: forgotten
  static IPs ($), persistent disks, log buckets.

---

## Chaos Appendix — extra breaking exercises

After you've finished, try these for further state surgery practice:

1. **`terraform state pull` / `push`:** download state, edit JSON by
   hand (e.g., change a resource's `id` field), push it back. This is
   nuclear-level dangerous in real life. Practice in dev.
2. **Two roots, one resource:** add the same resource to two separate
   Terraform configurations (different state files). Apply both.
   Watch them fight. Diagnose with `terraform state list` from each.
3. **Simulate a corrupted state:** in the state bucket, download the
   state file, randomly delete a resource entry, upload it back. Run
   `plan`. Recover from versioning.
4. **Provider version mismatch:** downgrade your provider version in
   `required_providers` to one too old to support a feature you use.
   Watch init or plan break. Learn the upgrade path.
5. **Race a manual change:** start `terraform apply` modifying a
   bucket. While it's running, edit the bucket in the console.
   Observe what wins.

---

## When You're Stuck

- Read Terraform's plan output character by character. The diff tells
  the truth.
- `terraform state list` and `terraform state show <addr>` are your
  best debugging tools.
- The
  [google provider docs](https://registry.terraform.io/providers/hashicorp/google/latest/docs)
  are excellent — every resource has examples + import instructions.
- For "why is this resource taking 10 minutes", check `gcloud
  <service> operations list` to see GCP-side progress.
- When asking me for help: tell me which **step** you're on and paste
  the **exact error or plan output**, not a paraphrase.

Good luck. The state-breaking phase is the most useful — don't skim it.
