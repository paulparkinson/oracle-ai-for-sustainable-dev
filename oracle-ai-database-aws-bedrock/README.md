# Oracle + Bedrock RAG smoke test

This small integration test calls real services with three synthetic documents:

1. Amazon Titan Text Embeddings V2 embeds each document and the question.
2. Oracle AI Database performs cosine vector retrieval with `VECTOR_DISTANCE`.
3. Amazon Nova Lite answers using only the retrieved documents.
4. The test checks that Oracle ranked the expected document first and that the
   answer contains the expected fact (`37 hours`) and `[TRANSFER_POLICY]` citation.

Oracle receives the fixture as a bound JSON document, uses `JSON_TABLE` and
`TO_VECTOR` to perform retrieval, and returns the top two chunks. This deliberately
tests RAG **without creating tables or changing existing data**. It is not a
persistent ingestion/index benchmark and does not read your business documents.
No Bedrock Knowledge Base is needed. See [the blog](blog.html) for the larger design.

## Verification status (September 22, 2026)

Python syntax/CLI checks and five local checks with mocked services passed,
covering the full orchestration, wrong-account/database rejection, bad retrieval,
and invalid embeddings/answers. These checks do not validate Oracle SQL execution
or real model output.

Direct AWS CloudShell calls using the renewed console session in account
`054037143469`, region `us-east-1`, returned `AccessDeniedException` for both:

- `InvokeModel` on `amazon.titan-embed-text-v2:0`.
- `Converse` on `amazon.nova-lite-v1:0` (requires `bedrock:InvokeModel`).

Both errors said no identity-based policy allows `bedrock:InvokeModel` for
`AWSReservedSSO_Field-Engineering-Standard_7d5eeaaa25bfd1d3`.
The full RAG test has **not passed against live services**. The database wallet and
local database credentials are now configured; the encrypted PEM key still needs
its wallet password. This machine also needs an authenticated AWS SDK session.
The browser session alone does not supply credentials to local Python.

Suggested admin request (not sent):

> Please update and reprovision the Field-Engineering-Standard IAM Identity Center
> permission set for account 054037143469 (ODBZ Demo - 12). My renewed session as
> paul.parkinson@oracle.com still receives AccessDeniedException for
> bedrock:InvokeModel on amazon.titan-embed-text-v2:0 and amazon.nova-lite-v1:0 in
> us-east-1. Please allow invocation of these models for the Oracle RAG smoke test,
> or provide the approved execution role and model/inference-profile IDs. The
> attached bedrock-invoke-policy.json specifies the two direct model resources.

The [proposed policy](bedrock-invoke-policy.json) is narrowly scoped to direct
invocation of the default models. It does not change IAM automatically. An approved
inference profile needs corresponding profile and destination-model permissions;
provider onboarding and organization policies may impose additional requirements.

## Setup

Use Python 3.10+ and run from this directory:

```sh
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
cp .env.example .env
chmod 600 .env
```

Fill in `.env` locally. Use **paulparkdbaws**, the database created in AWS account
`054037143469`, region `us-east-1`. Copy its exact service alias from the downloaded
wallet's `tnsnames.ora`; the example alias is not a discovered connection string.
The existing database requires mTLS. Python Thin mode needs the extracted wallet
directory including `ewallet.pem` and `tnsnames.ora`, plus the wallet password if
the PEM key is encrypted. The wallet password is distinct from the database-user
password. A user able to connect and run the SELECT expressions is sufficient;
no `CREATE TABLE` privilege or administrator account is required.

The test uses `RAG_DB_*` variables to avoid accidentally using another project's
`ORACLE_*` settings. It also accepts this project's existing `DB_USERNAME`,
`DB_PASSWORD`, `DB_WALLET_DIR` (or `TNS_ADMIN`), and `DB_WALLET_PASSWORD`
(or `WALLET_PASSWORD`) settings;
`RAG_DB_*` takes precedence. Set `RAG_DB_DSN=paulparkdbaws_low` for the verified
low service alias in the `Wallet_paulparkdbaws` wallet.
It verifies the AWS account and Oracle database/service name
before the RAG model calls. Override the `RAG_EXPECTED_*` settings only when
intentionally testing a different target. No passwords, wallets, or tokens belong
in Git or chat.

## AWS authentication

An AWS browser session does not authenticate local Python. Use an existing AWS SDK
profile, temporary environment credentials, or an AWS workload role. For AWS CLI v2
SSO setup:

```sh
aws configure sso --profile oracle-bedrock
aws sso login --profile oracle-bedrock
export AWS_PROFILE=oracle-bedrock
```

Select the account and permission set authorized for this database's Bedrock use.
The role must allow `bedrock:InvokeModel` for the embedding and generation models.
Converse uses that same inference permission. This script does not need model-list,
Knowledge Base creation, or streaming permissions. AWS STS is used to identify the
account. If Nova Lite direct inference is unavailable, set `RAG_TEXT_MODEL` to an
approved Converse-compatible model or inference-profile ID. Cross-region profiles
may route processing to other regions and require permissions for those models.
`RAG_EMBED_MODEL` must be Titan V2-compatible and return 1,024 float dimensions.

## Run

```sh
# Optional: isolate Bedrock permissions and connectivity (not an Oracle/RAG test).
python rag_smoke_test.py --bedrock-only

# Full smoke test: 4 embedding invocations and 1 generation invocation.
python rag_smoke_test.py
```

Model invocations incur normal Bedrock charges. The fixture is small and uses
synthetic data only. Success ends with:

```text
PASS: Bedrock embeddings -> Oracle vector retrieval -> grounded Bedrock answer.
No database objects or rows were created or changed.
```

This is expected output, not a claim that your account has passed. A failure exits
nonzero. `--bedrock-only` explicitly reports that Oracle was not tested. The test
prints document IDs, distances, and the answer; it does not print credentials or
document embeddings. The fixed fact/citation check is a smoke assertion, not a
comprehensive evaluation of groundedness or authorization.

For a separate environment file:

```sh
python rag_smoke_test.py --env-file /secure/path/bedrock-rag.env
```

## Troubleshooting

- **NoCredentialsError / expired SSO:** authenticate the local SDK profile again.
- **AccessDeniedException:** check model invocation permissions, organization
  restrictions, and model/provider onboarding; console access alone is insufficient.
- **ValidationException:** check the chosen model ID, region, and inference-profile
  requirements. The generation model must support Converse and its parameters.
- **Oracle TLS / connection error:** verify this database's wallet, service alias,
  credentials, firewall/VPN route, and the wallet's PEM password.
- **Database-name mismatch:** confirm that the DSN selects `paulparkdbaws`. Do not
  weaken the expected-name check simply to reuse another project's connection.
- **Retrieval assertion:** ensure all embeddings use the same model/dimensions and
  inspect the reported ranking; no approximate index is used in this tiny test.

References: [Titan embedding parameters](https://docs.aws.amazon.com/bedrock/latest/userguide/model-parameters-titan-embed-text.html),
[Converse](https://docs.aws.amazon.com/bedrock/latest/APIReference/API_runtime_Converse.html),
[Oracle vector binding](https://python-oracledb.readthedocs.io/en/stable/user_guide/vector_data_type.html).
