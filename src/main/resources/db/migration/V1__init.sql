create schema if not exists demo_webflux;

create table if not exists currency
(
    id           int generated always as identity primary key,
    name         varchar(32) not null,
    display_name varchar(64) not null,
    enabled      bool default true
);

insert into currency (name, display_name, enabled)
values ('USDT-TRC20', 'Tether TRC-20', true),
       ('USDT-ERC20', 'Tether ERC-20', false),
       ('ETH', 'Ethereum', false),
       ('TRX', 'Tron', false);

create table if not exists merchant
(
    id         uuid                              default gen_random_uuid() primary key,
    name       varchar(64)              not null,
    email      varchar(64)              null,
    commission decimal                  not null check ( commission >= 0 ),
    created    timestamp with time zone not null default now()
);

create table if not exists invoice
(
    id                uuid                              default gen_random_uuid() not null primary key,
    customer_id       varchar(64)              not null,
    merchant_id       uuid                     not null,
    merchant_order_id varchar(512)             not null,
    currency_id       int                      not null references currency (id),
    amount            decimal                  not null check ( amount > 0 ),
    commission        decimal                  not null check ( commission >= 0 ),
    description       varchar(255)             null,
    success_url       varchar(512)             not null,
    fail_url          varchar(512)             not null,
    created           timestamp with time zone not null default now()
);

create type order_status_type as enum ('NEW', 'PENDING', 'COMPLETED', 'CANCELLED', 'ERROR', 'MISMATCH', 'EXPIRED');

create table if not exists "order"
(
    id                uuid                              default gen_random_uuid() not null primary key,
    invoice_id        uuid                     not null references invoice (id),
    status            order_status_type        not null default 'NEW',
    confirmations     int                      not null check ( confirmations >= 0 ),
    selected_currency int                      null references currency (id),
    amount            decimal                  not null check ( amount > 0 ),
    created           timestamp with time zone not null
);

insert into merchant(id, name, email, commission)
values ('2a3e59ff-b549-4ca2-979c-e771c117f350', 'merch1', 'some@email.com', 1.5);

create table if not exists account
(
    id          uuid default gen_random_uuid() primary key,
    name        varchar(255),
    description text
);
insert into account(id, name, description)
VALUES ('a1f18428-cc07-4c4b-8cb2-bbf86ce8d6d7', 'foo', 'bar');
