# AI Usage

**Tools:** Claude (Anthropic) via the Claude app, with file/shell tools.

**Used for:** scaffolding the Spring Boot API, SQL schema and seed data, the React UI, and a first draft of this README.

**What I verified:** the schema, seed data and key SQL (search aggregate, ON CONFLICT insert) were run against a real PostgreSQL 16; the frontend was built with Vite.

**Example of a correction/rejection:**
<!-- TODO (candidate): replace with your own real example before submitting, e.g. something you changed or rejected while reviewing the generated code. -->
During the design, a "check if listing exists, then insert" approach for duplicate listings was replaced by a single atomic `INSERT ... ON CONFLICT ... DO UPDATE ... WHERE active = FALSE`, because check-then-insert is racy under concurrent requests. Likewise, JPA entities with `@Version` were dropped in favour of an explicit `version` column in the UPDATE's WHERE clause to keep the locking logic visible in one place.
