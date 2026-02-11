-- clean
-- delete from node_article_rel;
-- delete from article_av;
-- delete from attribute_value;
-- delete from attribute;
-- delete from category_node;
-- delete from article;


-- 1, Articles

insert into article(client, article_type, article_no, product_no, deleted, status1, status2, status3, status4, creation_time, update_time, creation_user, update_user)
values
    (12,'Product','P-1001','PRD-1001',false,1,1,1,1,now(),now(),'system','system'),
    (12,'Product','P-1002','PRD-1002',false,1,1,1,1,now(),now(),'system','system'),
    (12,'Product','P-1003','PRD-1003',false,1,1,1,1,now(),now(),'system','system'),
    (12,'Product','P-1004','PRD-1004',false,2,1,1,1,now(),now(),'system','system'),
    (12,'Product','P-1005','PRD-1005',false,3,1,1,1,now(),now(),'system','system'),
    (12,'Product','P-1006','PRD-1006',false,1,1,1,1,now(),now(),'system','system'),
    (12,'Product','P-1007','PRD-1007',false,1,1,1,1,now(),now(),'system','system'),
    (12,'Product','P-1008','PRD-1008',false,1,1,1,1,now(),now(),'system','system'),
    (12,'Product','P-1009','PRD-1009',false,1,1,1,1,now(),now(),'system','system'),
    (12,'Product','P-1010','PRD-1010',false,1,1,1,1,now(),now(),'system','system'),

    (12,'Article','A-100101','PRD-1001',false,2,1,1,1,now(),now(),'system','system'),
    (12,'Article','A-100102','PRD-1001',false,2,1,1,1,now(),now(),'system','system'),

    (12,'Article','A-100201','PRD-1002',false,2,1,1,1,now(),now(),'system','system'),
    (12,'Article','A-100202','PRD-1002',false,2,1,1,1,now(),now(),'system','system'),

    (12,'Article','A-100301','PRD-1003',false,2,1,1,1,now(),now(),'system','system'),
    (12,'Article','A-100302','PRD-1003',false,2,1,1,1,now(),now(),'system','system'),

    (12,'Article','A-100401','PRD-1004',false,2,1,1,1,now(),now(),'system','system'),
    (12,'Article','A-100402','PRD-1004',false,2,1,1,1,now(),now(),'system','system'),

    (12,'Article','A-100501','PRD-1005',false,2,1,1,1,now(),now(),'system','system'),
    (12,'Article','A-100502','PRD-1005',false,2,1,1,1,now(),now(),'system','system'),

    (12,'Article','A-100601','PRD-1006',false,2,1,1,1,now(),now(),'system','system'),
    (12,'Article','A-100602','PRD-1006',false,2,1,1,1,now(),now(),'system','system'),

    (12,'Article','A-100701','PRD-1007',false,2,1,1,1,now(),now(),'system','system'),
    (12,'Article','A-100702','PRD-1007',false,2,1,1,1,now(),now(),'system','system'),

    (12,'Article','A-100801','PRD-1008',false,2,1,1,1,now(),now(),'system','system'),
    (12,'Article','A-100802','PRD-1008',false,2,1,1,1,now(),now(),'system','system'),

    (12,'Article','A-100901','PRD-1009',false,2,1,1,1,now(),now(),'system','system'),
    (12,'Article','A-100902','PRD-1009',false,2,1,1,1,now(),now(),'system','system'),

    (12,'Article','A-101001','PRD-1010',false,2,1,1,1,now(),now(),'system','system'),
    (12,'Article','A-101002','PRD-1010',false,2,1,1,1,now(),now(),'system','system')
    on conflict (article_no, client) do nothing;

-- 2, Attribute

insert into attribute
(identifier, name, description, value_type, unit, is_multivalue, is_required, deleted, creation_user, update_user)
values
-- Basic identification
('brand',                'Brand',                 'Vehicle manufacturer brand',                'STRING',  null,     false, true,  false, 'seed', 'seed'),
('model',                'Model',                 'Vehicle model name',                         'STRING',  null,     false, true,  false, 'seed', 'seed'),
('trim',                 'Trim',                  'Trim level or version',                     'STRING',  null,     false, false, false, 'seed', 'seed'),
('model_year',           'Model Year',            'Production model year',                     'NUMBER',  'year',   false, false, false, 'seed', 'seed'),
('vin',                  'VIN',                   'Vehicle Identification Number',             'STRING',  null,     false, false, false, 'seed', 'seed'),

-- Dimensions
('length_mm',            'Length',                'Vehicle length',                             'NUMBER',  'mm',     false, false, false, 'seed', 'seed'),
('width_mm',             'Width',                 'Vehicle width',                              'NUMBER',  'mm',     false, false, false, 'seed', 'seed'),
('height_mm',            'Height',                'Vehicle height',                             'NUMBER',  'mm',     false, false, false, 'seed', 'seed'),

('currency',             'Currency',               'Price currency',                            'STRING',  null,     false, false, false, 'seed', 'seed'),
('available_from',       'Available From',         'Market availability start date',            'DATE',    null,     false, false, false, 'seed', 'seed'),
('available_to',         'Available To',           'Market availability end date',              'DATE',    null,     false, false, false, 'seed', 'seed'),

-- Multi-value attributes
('features',             'Features',               'Vehicle feature list',                      'STRING',  null,     true,  false, false, 'seed', 'seed'),
('option_codes',         'Option Codes',            'Optional equipment codes',                  'STRING',  null,     true,  false, false, 'seed', 'seed')
    on conflict (identifier) do nothing;



--3, article_av (here I change use a function to fill the data, just for showing another way. In real projects I will maintain consistency.)
create or replace function fill_default_article_av(
    p_client int,
    p_user varchar default 'system'
)
returns void
language plpgsql
as $$
declare
    v_attr_vin          bigint;
    v_attr_available_to bigint;
    v_attr_length_mm    bigint;
begin
    --  resolve attribute ids once
select id into v_attr_vin
from attribute
where identifier = 'vin'
  and deleted = false;

select id into v_attr_available_to
from attribute
where identifier = 'available_to'
  and deleted = false;

select id into v_attr_length_mm
from attribute
where identifier = 'length_mm'
  and deleted = false;

if v_attr_vin is null
       or v_attr_available_to is null
       or v_attr_length_mm is null then
        raise exception 'Required attributes missing: vin=% available_to=% length_mm=%',
            v_attr_vin, v_attr_available_to, v_attr_length_mm;
end if;

    --  VIN (STRING -> value_text)
insert into article_av(
    client,
    article_id,
    attribute_id,
    value_index,
    value_text,
    deleted,
    creation_time,
    update_time,
    creation_user,
    update_user
)
select
    a.client,
    a.id,
    v_attr_vin,
    0,
    'VIN-' || a.article_no,
    false,
    now(),
    now(),
    p_user,
    p_user
from article a
where a.client = p_client
  and a.deleted = false
    on conflict (client, article_id, attribute_id, value_index) do nothing;

--  available_to (DATE -> value_date)
insert into article_av(
    client,
    article_id,
    attribute_id,
    value_index,
    value_date,
    deleted,
    creation_time,
    update_time,
    creation_user,
    update_user
)
select
    a.client,
    a.id,
    v_attr_available_to,
    0,
    (current_date + 365),   -- 你也可以改成 current_date + interval '1 year'
    false,
    now(),
    now(),
    p_user,
    p_user
from article a
where a.client = p_client
  and a.deleted = false
    on conflict (client, article_id, attribute_id, value_index) do nothing;

--  length_mm (NUMBER -> value_num)
insert into article_av(
    client,
    article_id,
    attribute_id,
    value_index,
    value_num,
    deleted,
    creation_time,
    update_time,
    creation_user,
    update_user
)
select
    a.client,
    a.id,
    v_attr_length_mm,
    0,
    (4500 + (a.id % 300))::numeric(18,6),
    false,
    now(),
    now(),
    p_user,
    p_user
from article a
where a.client = p_client
  and a.deleted = false
    on conflict (client, article_id, attribute_id, value_index) do nothing;
end;
$$;

select fill_default_article_av(12);


-- 4,  Category Node

-- 4.1  root node
insert into category_node(id, root_node, node_identifier, parent_node, name, level_no, sort_no, hierarchy_path, deleted, creation_time, update_time, creation_user, update_user)
values (1, 1, 'root', null, 'Root', 0, 0, '/root', false, now(), now(), 'system', 'system') on conflict (root_node, node_identifier) do nothing;

select pg_get_serial_sequence('category_node', 'id');
select setval('public.category_node_id_seq', 1, true); --then nextval() return 2

-- 4.2  root node id
-- select id from category_node where node_identifier='root' and parent_node is null;

-- 4.3  node
insert into category_node(root_node, node_identifier, parent_node, name, level_no, sort_no, hierarchy_path, deleted, creation_time, update_time, creation_user, update_user)
select r.id, 'cat_a', r.id, 'Category A', 1, 10, '/root/cat_a', false, now(), now(), 'system', 'system'
from category_node r
where r.node_identifier='root' and r.parent_node is null
    on conflict (root_node, node_identifier) do nothing;

insert into category_node(root_node, node_identifier, parent_node, name, level_no, sort_no, hierarchy_path, deleted, creation_time, update_time, creation_user, update_user)
select r.id, 'cat_b', r.id, 'Category B', 1, 20, '/root/cat_b', false, now(), now(), 'system', 'system'
from category_node r
where r.node_identifier='root' and r.parent_node is null
    on conflict (root_node, node_identifier) do nothing;

-- 4.4 node
insert into category_node(root_node, node_identifier, parent_node, name, level_no, sort_no, hierarchy_path, deleted, creation_time, update_time, creation_user, update_user)
select a.root_node, 'cat_a_1', a.id, 'Category A-1', 2, 10, '/root/cat_a/cat_a_1', false, now(), now(), 'system', 'system'
from category_node a
where a.node_identifier='cat_a'
    on conflict (root_node, node_identifier) do nothing;



-- 5 price
--5.1
create or replace function seed_prices_for_articles(p_client int)
returns void
language plpgsql
as $$
declare
v_price_list_id bigint;
  v_price_sale_id bigint;
begin
  -- 1) upsert: price master data
insert into price(client, identifier, name, currency, price_type, deleted,
                  creation_time, update_time, creation_user, update_user)
values
    (p_client, 'LIST_PRICE', 'List Price', 'EUR', 'LIST', false, now(), now(), 'system', 'system'),
    (p_client, 'SALE_PRICE', 'Sale Price', 'EUR', 'SALE', false, now(), now(), 'system', 'system')
    on conflict (client, identifier) do update
                                            set name = excluded.name,
                                            currency = excluded.currency,
                                            price_type = excluded.price_type,
                                            update_time = now(),
                                            update_user = 'system';
end;
$$;

select seed_prices_for_articles(12);

--5.2 article_price_rel
create or replace function seed_article_price_rel_simple(p_client int)
returns void
language plpgsql
as $$
declare
v_list_price_id bigint;
    v_sale_price_id bigint;
begin
-- 1. get price id
select id into v_list_price_id
from price
where client = p_client and identifier = 'LIST_PRICE';

select id into v_sale_price_id
from price
where client = p_client and identifier = 'SALE_PRICE';

if v_list_price_id is null or v_sale_price_id is null then
        raise exception 'price not initialized for client %', p_client;
end if;

-- 2.
insert into article_price_rel (
    client, article_id, price_id,
    amount, valid_from, valid_until,
    deleted, creation_time, update_time, creation_user, update_user
)
select
    p_client,
    a.id,
    v.price_id,
    v.amount,
    current_date,
    null,
    false,
    now(), now(), 'system', 'system'
from article a
         join (
    values
        ('A-100101', v_list_price_id,  99.99),
        ('A-100101', v_sale_price_id,  89.99),
        ('A-100102', v_list_price_id, 109.99),
        ('A-100102', v_sale_price_id,  95.99),

        ('A-100201', v_list_price_id, 119.00),
        ('A-100201', v_sale_price_id,  99.00),
        ('A-100202', v_list_price_id, 129.00),
        ('A-100202', v_sale_price_id, 109.00),

        ('A-100301', v_list_price_id, 139.00),
        ('A-100301', v_sale_price_id, 149.10),
        ('A-100302', v_list_price_id, 139.00),
        ('A-100302', v_sale_price_id, 149.20),

        ('A-100401', v_list_price_id, 149.00),
        ('A-100401', v_sale_price_id, 159.10),
        ('A-100402', v_list_price_id, 169.00),
        ('A-100402', v_sale_price_id, 179.20),

        ('A-100501', v_list_price_id, 139.00),
        ('A-100501', v_sale_price_id, 149.10),
        ('A-100502', v_list_price_id, 189.00),
        ('A-100502', v_sale_price_id, 149.90),

        ('A-100601', v_list_price_id, 139.70),
        ('A-100601', v_sale_price_id, 149.40),
        ('A-100602', v_list_price_id, 139.80),
        ('A-100602', v_sale_price_id, 149.20),

        ('A-100701', v_list_price_id, 139.80),
        ('A-100701', v_sale_price_id, 149.90),
        ('A-100702', v_list_price_id, 139.50),
        ('A-100702', v_sale_price_id, 149.40),

        ('A-100801', v_list_price_id, 139.30),
        ('A-100801', v_sale_price_id, 149.30),
        ('A-100802', v_list_price_id, 139.50),
        ('A-100802', v_sale_price_id, 149.60),

        ('A-100901', v_list_price_id, 139.30),
        ('A-100901', v_sale_price_id, 149.60),
        ('A-100902', v_list_price_id, 139.40),
        ('A-100902', v_sale_price_id, 149.60),

        ('A-101001', v_list_price_id, 169.00),
        ('A-101001', v_sale_price_id, 142.10),
        ('A-101002', v_list_price_id, 133.00),
        ('A-101002', v_sale_price_id, 142.20)

) as v(article_no, price_id, amount)
              on v.article_no = a.article_no
where a.client = p_client
  and a.article_type = 'Article'
  and a.deleted = false
    on conflict (client, article_id, price_id, valid_from) do nothing;

end;
$$;

select seed_article_price_rel_simple(12);


