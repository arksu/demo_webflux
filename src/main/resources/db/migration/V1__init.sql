create schema if not exists demo_webflux;

create type blockchain_type as enum ('TRON', 'TRON-NILE', 'TRON-SHASTA', 'ETH');

create table if not exists currency
(
    id                     int generated always as identity primary key,
    -- код валюты (краткое наименование)
    name                   varchar(32)     not null,
    -- тип блокчейна
    blockchain             blockchain_type not null,
    -- как в точности называется токен в блокчейне. проверяем это в транзакции
    token                  varchar(64)     null,
    -- адрес токена в блокчейне
    contract_address       varchar(128)    null,
    -- сколько подтверждений ждем в блокчейне
    confirmations_required integer         not null,
    -- имя которое показываем в виджете
    display_name           varchar(64)     not null,
    enabled                bool default true
);

insert into currency (name, blockchain, display_name, token, contract_address, confirmations_required, enabled)
values ('USDT-TRC20', 'TRON', 'USDT TRC-20', 'USDT', 'TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t', 25, true),
       ('TRX', 'TRON', 'Tron', null, null, 25, false),
       ('USDT-TRC20-NILE', 'TRON-NILE', 'TEST Nile Tether TRC-20', 'USDT', 'TXLAQ63Xg1NAzckPwKHvzw7CSEmLMEqcdj', 25, false),
       ('USDD-TRC20-NILE', 'TRON-NILE', 'TEST Nile USDD TRC-20', 'USDD', 'THJ6CYd8TyNzHFrdLTYQ1iAAZDrf5sEsZU', 25, false),
       ('TRX-NILE', 'TRON-NILE', 'TEST Nile Tron', null, null, 25, false),
       ('USDT-ERC20', 'ETH', 'Tether ERC-20', 'USDT', '0xdac17f958d2ee523a2206206994597c13d831ec7', 25, false),
       ('ETH', 'ETH', 'Ethereum', null, null, 25, false);

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
    secret_key           char(64)                 not null,
    -- кто будет оплачивать комиссию
    commission_type      commission_type          not null,
    -- разрешить перерасчет в случае не полной оплаты клиентом
    allow_recalculation  bool                     not null default false,
    -- время жизни сделки (после завершается)
    expire_minutes       int                      not null default 30,
    -- % от суммы сделки при получении платежа в пределах которого считаем сделку завершенной (в процентах)
    underpayment_allowed decimal                  not null default 1,
    -- урл куда шлем вебхуки по заказам
    webhook_url          varchar(512)             not null,
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
    success_url       varchar(512)             null,
    -- куда отправим в случае ошибки
    fail_url          varchar(512)             null,
    -- ключ который использовался при создании счета
    api_key           char(64)                 not null,
    -- дата в которую протухает счет на оплату - закрываем его
    deadline          timestamp with time zone not null,
    -- дата создания счета на оплату
    created           timestamp with time zone not null default now(),
    -- в пределах мерчанта order id должен быть уникален
    unique (shop_id, merchant_order_id)
);

create type order_status_type as enum ('NEW', 'PENDING', 'COMPLETED', 'EXPIRED', 'ERROR', 'OVERPAID', 'NOT_ENOUGH');

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
    id           uuid                              default gen_random_uuid() not null primary key,
    -- блокчейн адрес
    address      varchar(512)             not null,
    -- приватный ключ
    key          varchar(255)             null,
    -- кошелек сгенерирован? если да то не очищаем поле order_id в кошельке при завершении ордера
    is_generated bool                     not null,
    -- валюта которую принимаем
    currency_id  int                      not null references currency (id),
    -- кошелек занят приемом? ожидаем на него поступления средств,
    -- тогда впишем сюда ид заказа по которому ожидаем оплату на кошелек
    -- null - если кошелек свободен
    order_id     uuid                     null references "order" (id),
    -- можно отключить кошелек
    -- (на него не будут создаваться новые заказы, но те что в обработке - продолжат процессится шедулером)
    enabled      bool                     not null default true,
    updated      timestamp with time zone not null default now(),
    -- на одном адресе не может быть несколько валют
    unique (address, currency_id)
);

-- транзакции из блокчейна
create table if not exists blockchain_transaction
(
    id           char(64)                 not null primary key,
    amount       decimal                  not null,
    from_address char(64)                 not null,
    to_address   char(64)                 not null,
    wallet_id    uuid                     not null references blockchain_income_wallet (id),
    currency_id  int                      not null references currency (id),
    blockchain   blockchain_type          not null,
    created      timestamp with time zone not null default now()
);

-- транзакции в ожидании подтверждения
create table if not exists blockchain_transaction_pending
(
    id            char(64)                     not null primary key,
    blockchain    blockchain_type              not null,
    confirmed     boolean                      not null,
    completed     boolean                      not null,
    order_id      uuid references "order" (id) not null,
    -- количество подтверждений сети при проведении транзакции
    confirmations int                          not null check ( confirmations >= 0 ),
    created       timestamp with time zone     not null default now(),
    completed_at  timestamp with time zone     null,
    updated_at    timestamp with time zone     null
);

create type rate_source as enum ('BINANCE');
-- актуальный курс валют
create table if not exists rate
(
    id      bigserial primary key,
    name    varchar(32)              not null,
    rate    decimal                  not null,
    source  rate_source              not null,
    created timestamp with time zone not null default now()
);

create type webhook_status as enum ('NEW', 'DONE', 'RETRY', 'ERROR');

-- отправка вебхуков мерчанту
create table if not exists webhook
(
    id           bigserial primary key,
    shop_id      uuid references shop (id) not null,
    url          varchar(512)              not null,
    request_body varchar(4096)             not null,
    status       webhook_status            not null,
    -- сколько попыток уже было сделано
    try_count    integer                   not null default 0 check ( try_count >= 0 ),
    error_count  integer                   not null default 0 check ( error_count >= 0 ),
    -- подпись вебхука которую отправляем мерчанту
    signature    varchar(512)              not null,
    created      timestamp with time zone  not null default now()
);

-- ответы мерчанта на вебхуки
create table if not exists webhook_result
(
    id            bigserial primary key,
    -- ид вебхука который должны были отправить
    webhook_id    bigint references webhook (id) not null,
    -- тело ответа
    response_body varchar(4096)                  null,
    -- код ответа от мерчанта
    response_code integer                        null,
    -- текст ошибки (исключения) если была
    error         varchar(1024)                  null,
    -- номер попытки
    try_num       integer                        not null,
    -- длительность отправки запроса (получения ответа)
    response_time integer                        not null,
    -- время отправки
    created       timestamp with time zone       not null default now()
);

-- test data
insert into merchant(id, login, email, commission)
values ('2a3e59ff-b549-4ca2-979c-e771c117f350', 'test_merchant', 'merchant1@email.com', 1.5);

insert into shop (id, merchant_id, name, url, api_key, secret_key, commission_type, expire_minutes, webhook_url, underpayment_allowed)
values ('af36f972-9abb-4c98-b7cf-12bc1f9a2a79', '2a3e59ff-b549-4ca2-979c-e771c117f350', 'shop1', 'https://google.com',
        'XXuMTye9BpV8yTYYtK2epB452p9PgcHgHK3CDGbLDGwc4xNmWT7y2wmVTKtGvwyZ',
        'ACSkij3FIpXR78vVXVslG2g7nuccInJyRGrlQvDCgZuxXCypG3lqs2jL02rnIq1O',
        'MERCHANT',
        35, 'http://localhost:8080', 2);

insert into blockchain_income_wallet (address, currency_id, is_generated, order_id)
values ('TFMyJ4fxtCttadUTYGSqKy9iwKhFNWqEhv', 3, false, null);
values ('TFMyJ4fxtCttadUTYGSqKy9iwKhFNWqEhv', 4, false, null);
values ('TFMyJ4fxtCttadUTYGSqKy9iwKhFNWqEhv', 5, false, null);
