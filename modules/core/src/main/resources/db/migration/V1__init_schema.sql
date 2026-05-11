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
  user_id INTEGER NOT NULL,
  UNIQUE (asset_name, asset_type, user_id)
);

CREATE TABLE transactions (
  id SERIAL NOT NULL,
  PRIMARY KEY (id),
  amount NUMERIC NOT NULL,
  currency VARCHAR(3) NOT NULL,
  month SMALLINT NOT NULL,
  year SMALLINT NOT NULL,
  asset_id INTEGER NOT NULL,
  user_id INTEGER NOT NULL,
  UNIQUE (month, year, currency, asset_id, user_id)
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
