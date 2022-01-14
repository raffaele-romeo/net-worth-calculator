CREATE TABLE users (
  id SERIAL NOT NULL,
  PRIMARY KEY (id),
  username VARCHAR UNIQUE NOT NULL,
  password VARCHAR NOT NULL,
  salt VARCHAR NOT NULL,
  role VARCHAR NOT NULL
);

CREATE TABLE assets (
  id SERIAL NOT NULL,
  PRIMARY KEY (id),
  asset_name VARCHAR NOT NULL,
  asset_type VARCHAR(10) NOT NULL,
  user_id INTEGER NOT NULL
);

CREATE TABLE transactions (
  id SERIAL NOT NULL,
  PRIMARY KEY (id),
  amount NUMERIC(6, 4) NOT NUL,
  currency VARCHAR(3) NOT NUL,
  month SMALLINT NOT NUL,
  year SMALLINT NOT NUL,
  asset_id INTEGER NOT NULL,
  user_id INTEGER NOT NULL
);

ALTER TABLE assets
ADD FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE transactions
ADD FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE transactions
ADD FOREIGN KEY (asset_id) REFERENCES assets (id);

CREATE INDEX assets_user_id_index
ON assets (user_id);

CREATE INDEX transactions_user_id_index
ON transactions (user_id);

CREATE INDEX users_username_index
ON users (username);
