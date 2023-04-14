create table author
(
    id     serial primary key,
    name   varchar not null,
    created_date timestamp DEFAULT now()
);

ALTER TABLE budget
    ADD COLUMN author_id INTEGER,
    ADD CONSTRAINT author_id FOREIGN KEY (author_id) REFERENCES author (id);