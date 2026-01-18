

-- 0, Common: auto update update_time trigger
create or replace function set_update_time()
returns trigger as $$
begin
  new.update_time = now();
  return new;
end;
$$ language plpgsql;


-- 1, ARTICLE
create table if not exists article (
  id            bigserial primary key,

  client        int not null,
  article_type  varchar(20) not null,      -- 'Article' or 'Product'
  article_no    varchar(80) not null,
  product_no    varchar(80) not null,

  deleted       boolean not null default false,

  status1       int,
  status2       int,
  status3       int,
  status4       int,

  creation_time timestamptz not null default now(),
  update_time   timestamptz not null default now(),
  creation_user varchar(100) not null,
  update_user   varchar(100) not null,

  constraint ck_article_type check (article_type in ('Article','Product'))
);

create unique index if not exists ux_article_article_no on article(client,article_no);
create index if not exists ix_article_type_deleted on article(article_type, deleted);
create index if not exists ix_article_product_no on article(product_no);

drop trigger if exists trg_article_set_update_time on article;
create trigger trg_article_set_update_time
before update on article
for each row execute function set_update_time();



-- 2, CATEGORY_NODE (tree)
create table if not exists category_node (
  id              bigserial primary key,

  -- root of the tree: for root node, set root_node = id (self)
  root_node       bigint not null,
  node_identifier varchar(120) not null,
  parent_node     bigint,

  name            varchar(200),
  level_no        int not null default 0,
  sort_no         int not null default 0,
  hierarchy_path  varchar(2000) not null,

  deleted         boolean not null default false,

  creation_time   timestamptz not null default now(),
  update_time     timestamptz not null default now(),
  creation_user   varchar(100) not null,
  update_user     varchar(100) not null,

  -- basic sanity
  constraint ck_category_node_not_self_parent check (parent_node is null or parent_node <> id)
);


alter table category_node
  add constraint fk_category_node_root
  foreign key (root_node) references category_node(id) on delete restrict;

alter table category_node
  add constraint uq_category_node_root_id unique (root_node, id);

alter table category_node
  add constraint fk_category_node_parent_same_root
  foreign key (root_node, parent_node)
  references category_node(root_node, id)
  on delete restrict;

create unique index if not exists ux_category_node_root_identifier
  on category_node(root_node, node_identifier);

create index if not exists ix_category_node_root_path
  on category_node(root_node, hierarchy_path);

create index if not exists ix_category_node_parent
  on category_node(parent_node);

drop trigger if exists trg_category_node_set_update_time on category_node;
create trigger trg_category_node_set_update_time
before update on category_node
for each row execute function set_update_time();




-- 3， NODE_ARTICLE_REL (m:n)
create table if not exists node_article_rel (
  id            bigserial primary key,

  node_id       bigint not null references category_node(id) on delete cascade,
  article_id    bigint not null references article(id) on delete cascade,

  rel_type      varchar(30) not null default 'ASSIGNED',
  valid_from    date,
  valid_until   date,

  creation_time timestamptz not null default now(),
  update_time   timestamptz not null default now(),
  creation_user varchar(100) not null,
  update_user   varchar(100) not null
);

create unique index if not exists ux_node_article_rel
  on node_article_rel(node_id, article_id, rel_type);

create index if not exists ix_node_article_rel_article on node_article_rel(article_id);
create index if not exists ix_node_article_rel_node on node_article_rel(node_id);

drop trigger if exists trg_node_article_rel_set_update_time on node_article_rel;
create trigger trg_node_article_rel_set_update_time
before update on node_article_rel
for each row execute function set_update_time();



-- 4， ATTRIBUTE
create table if not exists attribute (
  id            bigserial primary key,

  identifier    varchar(120) not null,
  name          varchar(200),
  description   text,

  value_type    varchar(30) not null,   -- STRING/NUMBER/BOOLEAN/DATE/ENUM
  unit          varchar(30),
  is_multivalue boolean not null default false,
  is_required   boolean not null default false,
  deleted       boolean not null default false,

  creation_time timestamptz not null default now(),
  update_time   timestamptz not null default now(),
  creation_user varchar(100) not null,
  update_user   varchar(100) not null,

  constraint ck_attribute_value_type check (value_type in ('STRING','NUMBER','BOOLEAN','DATE','ENUM'))
);

create unique index if not exists ux_attribute_identifier on attribute(identifier);

drop trigger if exists trg_attribute_set_update_time on attribute;
create trigger trg_attribute_set_update_time
before update on attribute
for each row execute function set_update_time();



-- 5， ATTRIBUTE_VALUE (enum options)

create table if not exists attribute_value (
  id            bigserial primary key,

  attribute_id  bigint not null references attribute(id) on delete cascade,

  value_code    varchar(120) not null,
  value_label   varchar(200),
  sort_no       int not null default 0,
  deleted       boolean not null default false,

  creation_time timestamptz not null default now(),
  update_time   timestamptz not null default now(),
  creation_user varchar(100) not null,
  update_user   varchar(100) not null
);

create unique index if not exists ux_attribute_value
  on attribute_value(attribute_id, value_code);

drop trigger if exists trg_attribute_value_set_update_time on attribute_value;
create trigger trg_attribute_value_set_update_time
before update on attribute_value
for each row execute function set_update_time();



-- 6， ARTICLE_AV (EAV)

create table if not exists article_av (
  id                 bigserial primary key,

  client             int not null,

  article_id         bigint not null references article(id) on delete cascade,
  attribute_id       bigint not null references attribute(id) on delete cascade,

  attribute_value_id bigint references attribute_value(id) on delete restrict, -- for ENUM

  value_text         text,
  value_num          numeric(18,6),
  value_bool         boolean,
  value_date         date,

  value_index        int not null default 0, -- for multi-value
  deleted            boolean not null default false,

  creation_time      timestamptz not null default now(),
  update_time        timestamptz not null default now(),
  creation_user      varchar(100) not null,
  update_user        varchar(100) not null
);

create unique index if not exists ux_article_av
  on article_av(client, article_id, attribute_id, value_index);

create index if not exists ix_article_av_attribute on article_av(attribute_id);
create index if not exists ix_article_av_article on article_av(article_id);

drop trigger if exists trg_article_av_set_update_time on article_av;
create trigger trg_article_av_set_update_time
before update on article_av
for each row execute function set_update_time();



-- 7， USER TABLES

create table if not exists app_user (
  id            bigserial primary key,

  username      varchar(80) not null,
  display_name  varchar(120),
  email         varchar(200),

  password_hash varchar(200) not null,
  enabled       boolean not null default true,

  creation_time timestamptz not null default now(),
  update_time   timestamptz not null default now(),
  creation_user varchar(100) not null,
  update_user   varchar(100) not null
);

create unique index if not exists ux_app_user_username on app_user(username);

drop trigger if exists trg_app_user_set_update_time on app_user;
create trigger trg_app_user_set_update_time
before update on app_user
for each row execute function set_update_time();


create table if not exists user_role (
  id            bigserial primary key,
  identifier     varchar(80) not null,
  role_name     varchar(120),

  creation_time timestamptz not null default now(),
  update_time   timestamptz not null default now(),
  creation_user varchar(100) not null,
  update_user   varchar(100) not null
);

create unique index if not exists ux_user_role_identifier on user_role(identifier);

drop trigger if exists trg_user_role_set_update_time on user_role;
create trigger trg_user_role_set_update_time
before update on user_role
for each row execute function set_update_time();


create table if not exists user_right (
  id            bigserial primary key,
  identifier    varchar(120) not null,
  right_name    varchar(200),

  creation_time timestamptz not null default now(),
  update_time   timestamptz not null default now(),
  creation_user varchar(100) not null,
  update_user   varchar(100) not null
);

create unique index if not exists ux_user_right_identifier on user_right(identifier);

drop trigger if exists trg_user_right_set_update_time on user_right;
create trigger trg_user_right_set_update_time
before update on user_right
for each row execute function set_update_time();


create table if not exists user_user_role_rel (
  id            bigserial primary key,
  user_id       bigint not null references app_user(id) on delete cascade,
  role_id       bigint not null references user_role(id) on delete cascade,

  creation_time timestamptz not null default now(),
  update_time   timestamptz not null default now(),
  creation_user varchar(100) not null,
  update_user   varchar(100) not null
);

create unique index if not exists ux_user_user_role_rel on user_user_role_rel(user_id, role_id);

drop trigger if exists trg_user_user_role_rel_set_update_time on user_user_role_rel;
create trigger trg_user_user_role_rel_set_update_time
before update on user_user_role_rel
for each row execute function set_update_time();


create table if not exists user_role_user_right_rel (
  id            bigserial primary key,
  role_id       bigint not null references user_role(id) on delete cascade,
  right_id      bigint not null references user_right(id) on delete cascade,

  creation_time timestamptz not null default now(),
  update_time   timestamptz not null default now(),
  creation_user varchar(100) not null,
  update_user   varchar(100) not null
);

create unique index if not exists ux_user_role_user_right_rel on user_role_user_right_rel(role_id, right_id);

drop trigger if exists trg_user_role_user_right_rel_set_update_time on user_role_user_right_rel;
create trigger trg_user_role_user_right_rel_set_update_time
before update on user_role_user_right_rel
for each row execute function set_update_time();

--8, price
create table if not exists price (
    id               bigserial primary key,
    client           int not null,
    identifier       varchar(100) not null,   -- LIST_PRICE, SALE_PRICE
    name             varchar(255) not null,
    currency         varchar(3) not null,     -- EUR, USD
    price_type       varchar(50) not null,    -- LIST, SALE, PURCHASE, B2B

    deleted          boolean not null default false,

    creation_time    timestamptz not null default now(),
    update_time      timestamptz not null default now(),
    creation_user    varchar(100) not null,
    update_user      varchar(100) not null
    );

create unique index if not exists ux_price_client_identifier
    on price(client, identifier);

create index if not exists ix_price_client
    on price(client);

--9, article_price_rel
create table if not exists article_price_rel (
    id               bigserial primary key,
    client           int not null,
    article_id       bigint not null references article(id) on delete cascade,
    price_id         bigint not null references price(id) on delete cascade,

    amount           numeric(18,6) not null,
    valid_from       date,
    valid_until      date,

    deleted          boolean not null default false,

    creation_time    timestamptz not null default now(),
    update_time      timestamptz not null default now(),
    creation_user    varchar(100) not null,
    update_user      varchar(100) not null
    );

create unique index if not exists ux_article_price_rel
    on article_price_rel(client, article_id, price_id, valid_from);

create index if not exists ix_article_price_article
    on article_price_rel(article_id);

create index if not exists ix_article_price_price
    on article_price_rel(price_id);


