CREATE TABLE "users" (
  "id"           UUID PRIMARY KEY,
  "email"        VARCHAR NOT NULL,
  "first_name"   VARCHAR NOT NULL,
  "last_name"    VARCHAR NOT NULL,
  "created"      TIMESTAMP NOT NULL,
  "seq_number"   BIGINT NOT NULL
);
