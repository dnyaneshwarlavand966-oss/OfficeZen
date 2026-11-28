# OfficeZen Firebase Read-Only Auditor – Phase A

## Safety contract

- The auditor uses GET requests only.
- It lists Firestore database metadata and reads published Firestore Rules release/ruleset source.
- It never reads Firestore documents or collections.
- It never creates, updates, deletes, imports, exports, restores, migrates, or deploys anything.
- It does not change IAM, Firebase configuration, or GitHub secrets.
- Both database IDs are checked independently. Neither is assumed authoritative.

## Targets

- Google Cloud project: `officezen-d42ad`
- Project number: `147077394585`
- GitHub repository: `dnyaneshwarlavand966-oss/OfficeZen`
- Branch permitted by the workflow: `main`
- Databases: `officezen-db` and `ai-studio-android-officeze-7dd93b37-6073-4473-9d46-73f59ebdea79`

## Required Google Cloud setup (not performed by this commit)

Create a Workload Identity Pool named `officezen-gh-pool` in location `global`, and an OIDC provider named `officezen-gh-provider`.

Provider issuer:
`https://token.actions.githubusercontent.com`

Attribute mapping:
```
google.subject=assertion.sub
attribute.repository=assertion.repository
attribute.ref=assertion.ref
attribute.workflow_ref=assertion.workflow_ref
```

Attribute condition:
```
assertion.repository == 'dnyaneshwarlavand966-oss/OfficeZen' &&
assertion.ref == 'refs/heads/main' &&
assertion.workflow_ref == 'dnyaneshwarlavand966-oss/OfficeZen/.github/workflows/firebase-readonly-audit.yml@refs/heads/main'
```

The workflow uses direct Workload Identity Federation. No service-account JSON key and no repository secret are required. Grant the resulting repository principal only the custom role below at project scope. Do not grant Owner, Editor, Firebase Rules Admin, or Firestore Viewer.

## Custom role

Create a project-level custom role from `iam/officezen-auditor-role.yaml` with role ID `officezenFirebaseReadOnlyAuditor`.

The role contains only:
- `datastore.databases.list`
- `datastore.databases.getMetadata`
- `firebaserules.releases.list`
- `firebaserules.rulesets.get`

Then bind this role to the Workload Identity principal set for the exact repository attribute. Use the pool project number and pool ID in the principal URI. Do not grant any entity/document permissions.

## Workflow

`.github/workflows/firebase-readonly-audit.yml` runs daily at 03:30 UTC (09:00 India Standard Time) and supports manual dispatch. The job runs only on `main`.

## Important verification caveats

1. This workflow is intentionally not expected to run successfully until the Google Cloud pool, provider, custom role, and principal binding are created.
2. The Rules release naming convention must be verified against the actual Firebase Rules API response for each named database. If no matching release is returned, the report says `NOT_VERIFIED`; it does not guess.
3. Rule pattern checks are heuristic. They do not replace semantic review or emulator tests.
4. PITR and backup schedule verification are not yet included in this first auditor version. They require separately validated read-only permissions and API coverage.
5. No PDF is generated in this first version; JSON and HTML are produced. PDF can be added after the report schema is reviewed.
6. The current repository's separate Rules deployment workflow is not changed by this feature. Do not run that deployment workflow as part of this audit.

## How to review

After the PR is reviewed and merged, configure the Google Cloud identity and role, then run the workflow manually on `main`. Inspect the sanitized JSON/HTML artifact. A missing permission, API failure, or missing release is reported as `NOT_VERIFIED`.
