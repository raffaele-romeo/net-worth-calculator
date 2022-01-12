CREATE TABLE users (
  id SERIAL NOT NULL,
  PRIMARY KEY (id),
  username VARCHAR UNIQUE NOT NULL,
  password VARCHAR NOT NULL,
  salt VARCHAR NOT NULL,
  role VARCHAR NOT NULL
);

CREATE TABLE accounts (
  id SERIAL NOT NULL,
  PRIMARY KEY (id),
  account_name VARCHAR NOT NULL,
  account_type VARCHAR NOT NULL,
  user_id INTEGER NOT NULL
);

CREATE TABLE transactions (
  id SERIAL NOT NULL,
  PRIMARY KEY (id),
  amount MONEY,
  month SMALLINT,
  year SMALLINT,
  account_id INTEGER NOT NULL,
  user_id INTEGER NOT NULL
);

ALTER TABLE accounts
ADD FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE transactions
ADD FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE transactions
ADD FOREIGN KEY (account_id) REFERENCES accounts (id);

CREATE INDEX accounts_user_id_index
ON accounts (user_id);

CREATE INDEX transactions_user_id_index
ON transactions (user_id);

CREATE INDEX users_username_index
ON users (username);
