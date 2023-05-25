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
       ('USDT-TRC20-NILE', 'TEST Nile Tether TRC-20', true),
       ('USDD-TRC20-NILE', 'TEST Nile USDD TRC-20', true),
       ('TRX-NILE', 'TEST Nile Tron', true),
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
    status            invoice_status_type      not null,
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
    created           timestamp with time zone not null default now(),
    -- в пределах мерчанта order id должен быть уникален
    unique (merchant_id, merchant_order_id)
);

create type order_status_type as enum ('NEW', 'PENDING', 'COMPLETED', 'CANCELLED', 'ERROR', 'MISMATCH', 'NOT_ENOUGH');

create table if not exists "order"
(
    id                         uuid                              default gen_random_uuid() not null primary key,
    -- на 1 счет можем создать только один order
    invoice_id                 uuid unique              not null references invoice (id),
    status                     order_status_type        not null,
    -- какая была сумма по счету на момент создания ордера
    invoice_amount             decimal                  not null,
    -- сколько берем с клиента
    customer_amount            decimal                  not null check ( customer_amount > 0 ),
    -- сколько фактически пришло от клиента (может он отправил больше чем надо)
    customer_amount_fact       decimal                  not null check ( customer_amount_fact >= 0 ),
    -- сколько отдаем мерчанту в валюте сделки с учетом комиссий
    merchant_amount_order      decimal                  not null check ( merchant_amount_order > 0 ),
    -- сколько отдаем мерчанту в валюте invoice
    merchant_amount            decimal                  not null check ( merchant_amount > 0 ),
    -- эталонная сумма сделки в валюте которую выбрал клиент, от которой идет расчет (invoice.amount -> exchange_rate[selected_currency_id])
    reference_amount           decimal                  not null check ( reference_amount > 0 ),
    -- курс по которому идет перерасчет валюты invoice в валюту сделки (invoice -> order) order.amount = invoice.amount * exchange_rate
    exchange_rate              decimal                  not null check ( exchange_rate > 0 ),
    -- какую валюту выбрал пользователь для оплаты счета
    selected_currency_id       int                      null references currency (id),
    -- количество подтверждений сети при проведении транзакции
    confirmations              int                      not null check ( confirmations >= 0 ),
    -- сумма комиссии которую взимаем с мерчанта
    merchant_commission_amount decimal                  not null check ( merchant_commission_amount >= 0 ),
    created                    timestamp with time zone not null default now()
);

create type wallet_type as enum ('TRON', 'ETH');
create table if not exists blockchain_income_wallet
(
    address        varchar(512)             not null primary key,
    type           wallet_type              not null,
    network        varchar(255)             not null,
    is_wait_income bool                     not null default false,
    updated        timestamp with time zone not null default now()
);

-- test data
insert into merchant(id, login, email, commission)
values ('2a3e59ff-b549-4ca2-979c-e771c117f350', 'test_merchant', 'merchant1@email.com', 1.5);


-- debug
create table if not exists account
(
    id          uuid default gen_random_uuid() primary key,
    name        varchar(255),
    description text
);
insert into account(id, name, description)
VALUES ('a1f18428-cc07-4c4b-8cb2-bbf86ce8d6d7', 'foo', 'bar'),
       ('a1f18428-cc07-4c4b-8cb2-bbf86ce8d6d2', 'fo22o', 'b22ar');
