CREATE SCHEMA IF NOT EXISTS reqres;

CREATE TABLE IF NOT EXISTS reqres.users (
    email                varchar PRIMARY KEY,
    id                      bigint,
    first_name              varchar,
    last_name               varchar
);

ALTER TABLE reqres.users ADD PRIMARY KEY (email);

INSERT INTO reqres.users (email, id, first_name, last_name)
       VALUES ('rafael3@gmail.com', 3, 'rafael', 'k')