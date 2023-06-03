create schema if not exists demo_webflux;

create type blockchain_type as enum ('TRON', 'TRON-NILE', 'TRON-SHASTA', 'ETH');

create table if not exists currency
(
    id           int generated always as identity primary key,
    name         varchar(32)     not null,
    blockchain   blockchain_type not null,
    display_name varchar(64)     not null,
    enabled      bool default true
);

insert into currency (name, blockchain, display_name, enabled)
values ('USDT-TRC20', 'TRON', 'Tether TRC-20', false),
       ('TRX', 'TRON', 'Tron', false),
       ('USDT-TRC20-NILE', 'TRON-NILE', 'TEST Nile Tether TRC-20', true),
       ('USDD-TRC20-NILE', 'TRON-NILE', 'TEST Nile USDD TRC-20', true),
       ('TRX-NILE', 'TRON-NILE', 'TEST Nile Tron', true),
       ('USDT-ERC20', 'ETH', 'Tether ERC-20', false),
       ('ETH', 'ETH', 'Ethereum', false);

create table if not exists merchant
(
    id         uuid                              default gen_random_uuid() primary key,
    login      varchar(64)              not null,
    email      varchar(64)              null,
    api_key    char(64)                 not null,
    -- комиссия мерчанта (сколько % забираем себе от суммы сделки)
    commission decimal                  not null check ( commission >= 0 ),
    enabled    bool                     not null default true,
    created    timestamp with time zone not null default now()
);

-- кто платит комиссию?
create type commission_type as enum ('CLIENT', 'MERCHANT');

create type invoice_status_type as enum ('NEW', 'PROCESSING', 'TERMINATED');

create table if not exists invoice
(
    id                uuid                              default gen_random_uuid() not null primary key,
    -- внешний ид который передаем в виджет и ссылку
    external_id       char(32)                 not null,
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
    -- ключ который использовался при создании счета
    api_key           char(64)                 not null,
    -- в пределах мерчанта order id должен быть уникален
    unique (merchant_id, merchant_order_id)
);

create type order_status_type as enum ('NEW', 'PENDING', 'COMPLETED', 'CANCELLED', 'ERROR', 'MISMATCH', 'NOT_ENOUGH');

create table if not exists "order"
(
    id                    uuid                              default gen_random_uuid() not null primary key,
    -- на 1 счет можем создать только один order
    invoice_id            uuid unique              not null references invoice (id),
    status                order_status_type        not null,
    -- какая была сумма по счету на момент создания ордера
    invoice_amount        decimal                  not null,
    -- сколько берем с клиента
    customer_amount       decimal                  not null check ( customer_amount > 0 ),
    -- сколько фактически пришло от клиента (может он отправил больше чем надо)
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
    -- сумма комиссии которую взимаем с мерчанта
    commission_amount     decimal                  not null check ( commission_amount >= 0 ),
    -- комиссия мерчанта на момент создания сделки (%)
    commission            decimal                  not null check (commission >= 0 ),
    -- количество подтверждений сети при проведении транзакции
    confirmations         int                      not null check ( confirmations >= 0 ),
    -- кошелек на который принимаем
    income_wallet_id      uuid                     not null,
    created               timestamp with time zone not null default now()
);

-- пул кошельков на который будем принимать средства от клиента
create table if not exists blockchain_income_wallet
(
    id          uuid                              default gen_random_uuid() not null primary key,
    -- блокчейн адрес
    address     varchar(512)             not null,
    -- валюта которую принимаем
    currency_id int                      not null references currency (id),
    -- кошелек занят приемом? ожидаем на него поступления средств,
    -- тогда впишем сюда ид заказа по которому ожидаем оплату на кошелек
    -- null - если кошелек свободен
    order_id    uuid                     null references "order" (id),
    updated     timestamp with time zone not null default now(),
    -- на одном адресе не может быть несколько валют
    unique (address, currency_id)
);

-- test data
insert into merchant(id, login, email, api_key, commission)
values ('2a3e59ff-b549-4ca2-979c-e771c117f350', 'test_merchant', 'merchant1@email.com', 'XXuMTye9BpV8yTYYtK2epB452p9PgcHgHK3CDGbLDGwc4xNmWT7y2wmVTKtGvwyZ', 1.5);


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
