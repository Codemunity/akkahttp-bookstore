CREATE TABLE "orders" (
  "id"                      BIGSERIAL PRIMARY KEY,
  "order_date"              TIMESTAMP NOT NULL,
  "user_id"                 INTEGER REFERENCES users,
  "total_price_usd"         DECIMAL NOT NULL
);

CREATE TABLE "books_by_order" (
  "id"                      BIGSERIAL PRIMARY KEY,
  "order_id"                INTEGER REFERENCES orders,
  "book_id"                 INTEGER REFERENCES books,
  "unit_price_usd"          DECIMAL NOT NULL,
  "quantity"                DECIMAL NOT NULL
);
