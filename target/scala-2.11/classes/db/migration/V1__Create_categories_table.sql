CREATE TABLE "categories" (
  "id"          BIGSERIAL PRIMARY KEY,
  "title"       VARCHAR NOT NULL
);
ALTER TABLE categories ADD CONSTRAINT categories_unique_title UNIQUE (title);
