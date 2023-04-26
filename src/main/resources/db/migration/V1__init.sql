create schema if not exists demo_webflux;

create table if not exists demo_webflux.account
(
    id   uuid default gen_random_uuid() primary key,
    "name" varchar(255),
    description text
)