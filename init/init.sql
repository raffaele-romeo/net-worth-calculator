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
  asset_type VARCHAR NOT NULL,
  user_id INTEGER NOT NULL
);

CREATE TABLE transactions (
  id SERIAL NOT NULL,
  PRIMARY KEY (id),
  amount MONEY,
  month SMALLINT,
  year SMALLINT,
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
