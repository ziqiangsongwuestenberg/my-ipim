-- clean
-- delete from node_article_rel;
-- delete from article_av;
-- delete from attribute_value;
-- delete from attribute;
 delete from category_node;
 delete from article;


-- 1, Articles

insert into article(client, article_type, article_no, product_no, deleted, status1, status2, status3, status4, creation_time, update_time, creation_user, update_user)
values
(12,'Product', 'P-1001', 'PRD-1001', false, 1, 1, 1, 1, now(), now(), 'system', 'system'),
(12,'Article', 'A-2001', 'PRD-1001', false, 2, 1, 1, 1, now(), now(), 'system', 'system'),
(13,'Article', 'A-2002', 'PRD-1002', false, 3, 1, 1, 1, now(), now(), 'system', 'system')
on conflict (article_no) do nothing;

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


-- 3,  Category Node

-- 3.1  root node
insert into category_node(id, root_node, node_identifier, parent_node, name, level_no, sort_no, hierarchy_path, deleted, creation_time, update_time, creation_user, update_user)
values (1, 1, 'root', null, 'Root', 0, 0, '/root', false, now(), now(), 'system', 'system');

select pg_get_serial_sequence('category_node', 'id');
select setval('public.category_node_id_seq', 1, true); --then nextval() return 2

-- 3.2  root node id
-- select id from category_node where node_identifier='root' and parent_node is null;

-- 3.3  node
insert into category_node(root_node, node_identifier, parent_node, name, level_no, sort_no, hierarchy_path, deleted, creation_time, update_time, creation_user, update_user)
select r.id, 'cat_a', r.id, 'Category A', 1, 10, '/root/cat_a', false, now(), now(), 'system', 'system'
from category_node r
where r.node_identifier='root' and r.parent_node is null;

insert into category_node(root_node, node_identifier, parent_node, name, level_no, sort_no, hierarchy_path, deleted, creation_time, update_time, creation_user, update_user)
select r.id, 'cat_b', r.id, 'Category B', 1, 20, '/root/cat_b', false, now(), now(), 'system', 'system'
from category_node r
where r.node_identifier='root' and r.parent_node is null;

-- 3.4 node
insert into category_node(root_node, node_identifier, parent_node, name, level_no, sort_no, hierarchy_path, deleted, creation_time, update_time, creation_user, update_user)
select a.root_node, 'cat_a_1', a.id, 'Category A-1', 2, 10, '/root/cat_a/cat_a_1', false, now(), now(), 'system', 'system'
from category_node a
where a.node_identifier='cat_a';




