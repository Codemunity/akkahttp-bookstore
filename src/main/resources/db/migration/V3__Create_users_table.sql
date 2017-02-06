CREATE TABLE "users" (
  "id"          BIGSERIAL PRIMARY KEY,
  "name"        VARCHAR NOT NULL,
  "email"       VARCHAR NOT NULL UNIQUE,
  "password"    VARCHAR NOT NULL
);