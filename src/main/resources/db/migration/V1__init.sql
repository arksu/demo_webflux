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
    login      varchar(64)              not null,
    email      varchar(64)              null,
    -- комиссия мерчанта (сколько % забираем себе от суммы сделки)
    commission decimal                  not null check ( commission >= 0 ),
    created    timestamp with time zone not null default now()
);

-- кто платит комиссию?
create type commission_type as enum ('CLIENT', 'MERCHANT');

create type invoice_status_type as enum ('NEW', 'PROCESSING', 'TERMINATED');

create table if not exists invoice
(
    id                uuid                              default gen_random_uuid() not null primary key,
    merchant_id       uuid                     not null,
    -- сколько денег хочет получить/отправить мерчант за сделку
    amount            decimal                  not null check ( amount > 0 ),
    -- в какой валюте мерчант хочет получить/отправить
    currency_id       int                      not null references currency (id),
    -- кто будет оплачивать комиссию
    commission_type   commission_type          not null,
    -- внутренний ид клиента на стороне мерчанта
    customer_id       varchar(64)              not null,
    -- внутренний ид сделки на стороне мерчанта
    merchant_order_id varchar(512)             not null check ( merchant_order_id <> '' ),
    -- описание к счету
    description       varchar(255)             null,
    -- куда отправим пользователя при успешном завершении сделки
    success_url       varchar(512)             not null,
    -- куда отправим в случае ошибки
    fail_url          varchar(512)             not null,
    created           timestamp with time zone not null default now()
);

create type order_status_type as enum ('NEW', 'PENDING', 'COMPLETED', 'CANCELLED', 'ERROR', 'MISMATCH', 'EXPIRED');

create table if not exists "order"
(
    id                    uuid                              default gen_random_uuid() not null primary key,
    -- на 1 счет можем создать только один order
    invoice_id            uuid unique              not null references invoice (id),
    status                order_status_type        not null default 'NEW',
    -- сколько берем с клиента
    customer_amount       decimal                  not null check ( customer_amount > 0 ),
    -- сколько фактически пришло от клиента
    customer_amount_fact  decimal                  not null check ( customer_amount_fact >= 0 ),
    -- сколько отдаем мерчанту в валюте сделки с учетом комиссий
    merchant_amount_order decimal                  not null check ( merchant_amount_order > 0 ),
    -- сколько отдаем мерчанту в валюте invoice
    merchant_amount       decimal                  not null check ( merchant_amount > 0 ),
    -- эталонная сумма сделки в валюте которую выбрал клиент, от которой идет расчет (invoice.amount -> exchange_rate[selected_currency_id])
    reference_amount      decimal                  not null check ( reference_amount > 0 ),
    -- курс по которому идет перерасчет валюты invoice в валюту сделки (invoice -> order) order.amount = invoice.amount * exchange_rate
    exchange_rate         decimal                  not null check ( exchange_rate > 0 ),
    -- какую валюту выбрал пользователь для оплаты счета
    selected_currency_id  int                      null references currency (id),
    -- количество подтверждений сети при проведении транзакции
    confirmations         int                      not null check ( confirmations >= 0 ),
    -- сумма комиссии которую взимаем с мерчанта
    merchant_commission   decimal                  not null check ( merchant_commission >= 0 ),
    -- комиссия системы
    system_commission     decimal                  not null check ( system_commission >= 0 ),
    created               timestamp with time zone not null default now()
);

insert into merchant(id, login, email, commission)
values ('2a3e59ff-b549-4ca2-979c-e771c117f350', 'merchant1', 'merchant1@email.com', 1.5);

create table if not exists account
(
    id          uuid default gen_random_uuid() primary key,
    name        varchar(255),
    description text
);
insert into account(id, name, description)
VALUES ('a1f18428-cc07-4c4b-8cb2-bbf86ce8d6d7', 'foo', 'bar');
