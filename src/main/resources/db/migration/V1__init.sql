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
    login      varchar(64)              not null unique,
    email      varchar(64)              not null,
    -- комиссия мерчанта (сколько % забираем себе от суммы сделки)
    commission decimal                  not null check ( commission >= 0 ),
    -- можно отключить мерчанта
    enabled    bool                     not null default true,
    created    timestamp with time zone not null default now()
);

-- кто платит комиссию?
create type commission_type as enum ('CLIENT', 'MERCHANT');

-- магазины мерчанта на которых он интегрируется
create table if not exists shop
(
    id                   uuid                              default gen_random_uuid() primary key,
    merchant_id          uuid                     not null references merchant (id),
    -- имя сайта в админке
    name                 varchar(128)             not null,
    -- ссылка на сайт на которую может перейти клиент из виджета
    url                  varchar(512)             not null,
    -- уникальный ключ для обращений к API платформы
    api_key              char(64)                 not null unique,
    -- кто будет оплачивать комиссию
    commission_type      commission_type          not null,
    -- разрешить перерасчет в случае не полной оплаты клиентом
    allow_recalculation  bool                     not null default false,
    -- время жизни сделки (после завершается)
    expire_minutes       int                      not null default 30,
    -- % от суммы сделки при получении платежа в пределах которого считаем сделку завершенной
    underpayment_allowed decimal                  not null default 0.01,
    deleted              bool                     not null default false,
    created              timestamp with time zone not null default now()
);

create type invoice_status_type as enum ('NEW', 'PROCESSING', 'TERMINATED');

-- счет от мерчанта (намерение о совершении сделки)
create table if not exists invoice
(
    id                uuid                              default gen_random_uuid() not null primary key,
    -- внешний ид который передаем в виджет и ссылку
    external_id       char(32)                 not null,
    status            invoice_status_type      not null,
    shop_id           uuid                     not null references shop (id),
    -- сколько денег хочет получить/отправить мерчант за сделку
    amount            decimal                  not null check ( amount > 0 ),
    -- в какой валюте мерчант хочет получить/отправить
    currency_id       int                      not null references currency (id),
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
    -- ключ который использовался при создании счета
    api_key           char(64)                 not null,
    created           timestamp with time zone not null default now(),
    -- в пределах мерчанта order id должен быть уникален
    unique (shop_id, merchant_order_id)
);

create type order_status_type as enum ('NEW', 'PENDING', 'COMPLETED', 'CANCELLED', 'ERROR', 'MISMATCH', 'NOT_ENOUGH');

-- заказы сделанные на основе счета
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
    customer_amount_received   decimal                  not null check ( customer_amount_received >= 0 ),
    -- сколько еще ожидаем от клиента для завершения сделки
    customer_amount_pending    decimal                  not null check ( customer_amount_pending >= 0 ),
    -- сколько отдаем мерчанту в валюте сделки с учетом комиссий
    merchant_amount_by_order   decimal                  not null check ( merchant_amount_by_order > 0 ),
    -- сколько отдаем мерчанту в валюте invoice
    merchant_amount_by_invoice decimal                  not null check ( merchant_amount_by_invoice > 0 ),
    -- эталонная сумма сделки в валюте которую выбрал клиент, от которой идет расчет (invoice.amount -> exchange_rate[selected_currency_id])
    reference_amount           decimal                  not null check ( reference_amount > 0 ),
    -- курс по которому идет перерасчет валюты invoice в валюту сделки (invoice -> order) order.amount = invoice.amount * exchange_rate
    exchange_rate              decimal                  not null check ( exchange_rate > 0 ),
    -- какую валюту выбрал пользователь для оплаты счета
    selected_currency_id       int                      null references currency (id),
    -- сумма комиссии которую взимаем с мерчанта
    commission_amount          decimal                  not null check ( commission_amount >= 0 ),
    -- комиссия мерчанта на момент создания сделки (%)
    commission                 decimal                  not null check (commission >= 0 ),
    -- количество подтверждений сети при проведении транзакции
    confirmations              int                      not null check ( confirmations >= 0 ),
    created                    timestamp with time zone not null default now()
);

-- храним все обновления ордеров тут
create table if not exists order_operation_log
(
    order_id                 uuid                     not null references "order" (id),
    from_status              order_status_type        not null,
    to_status                order_status_type        not null,
    customer_amount          decimal                  not null check ( customer_amount > 0 ),
    customer_amount_received decimal                  not null check ( customer_amount_received >= 0 ),
    customer_amount_pending  decimal                  not null check ( customer_amount_pending >= 0 ),
    created                  timestamp with time zone not null default now()
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
    -- можно отключить кошелек
    -- (на него не будут создаваться новые заказы, но те что в обработке - продолжат процессится шедулером)
    enabled     bool                     not null default true,
    updated     timestamp with time zone not null default now(),
    -- на одном адресе не может быть несколько валют
    unique (address, currency_id)
);

-- актуальный курс валют
create table if not exists rate
(
    id      bigserial,
    name    varchar(32)              not null,
    rate    decimal                  not null,
    created timestamp with time zone not null default now()
);

-- test data
insert into merchant(id, login, email, commission)
values ('2a3e59ff-b549-4ca2-979c-e771c117f350', 'test_merchant', 'merchant1@email.com', 1.5);

insert into shop (id, merchant_id, name, url, api_key, commission_type, expire_minutes, underpayment_allowed)
values ('af36f972-9abb-4c98-b7cf-12bc1f9a2a79', '2a3e59ff-b549-4ca2-979c-e771c117f350', 'shop1', 'https://google.com', 'XXuMTye9BpV8yTYYtK2epB452p9PgcHgHK3CDGbLDGwc4xNmWT7y2wmVTKtGvwyZ', 'MERCHANT', 35, 0.02);

insert into blockchain_income_wallet (address, currency_id, order_id)
values ('test_address1', 3, null);
values ('test_address2', 4, null);
values ('test_address3', 5, null);
