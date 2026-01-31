insert into currency (id, ISO_Code, currency) values (1, 'ZWL', 'ZWL $');
insert into entity_type_group (id, description) values (1, 'ICEcash');
insert into entity_type (id, description, entity_type_group_id) values (1, 'Card', 1);
insert into entity (id, first_name, last_name, legacy_account_id, entity_type_id, pin_key, pvv, kyc_status_id, status) values (1, 'ZimPark', null, 0, 1, '1', '1', 1, 'ACTIVE');
insert into entity (id, first_name, last_name, legacy_account_id, entity_type_id, pin_key, pvv, kyc_status_id, status) values (2, 'FBC', null, 0, 1, '2', '2', 1, 'ACTIVE');
insert into entity (id, first_name, last_name, legacy_account_id, entity_type_id, pin_key, pvv, kyc_status_id, status) values (3, 'CBZ Issuing Clearing Account', null, 0, 1, '3', '3', 1, 'ACTIVE');
insert into entity (id, first_name, last_name, legacy_account_id, entity_type_id, pin_key, pvv, kyc_status_id, status) values (4, 'IMT Control', null, 0, 1, '4', '4', 1, 'ACTIVE');
insert into initiator_type (id, description, active, entity_id) values (1, 'card', 1, 3);
insert into account_type (id, currency_id, description, legacy_wallet_id, active) VALUES (1, 1, 'Zim Dollar', '1', true);
insert into account (id, entity_id, account_type_id, created_date, account_number, account_status) VALUES (1, 1, 1, now(), '30000000010', 'ACTIVE');
insert into account (id, entity_id, account_type_id, created_date, account_number, account_status) VALUES (2, 2, 1, now(), '30000000020', 'ACTIVE');
insert into account (id, entity_id, account_type_id, created_date, account_number, account_status) VALUES (3, 3, 1, now(), '30000000030', 'ACTIVE');
insert into account (id, entity_id, account_type_id, created_date, account_number, account_status) VALUES (4, 4, 1, now(), '30000000040', 'ACTIVE');
insert into transaction_code (id, code, kyc_required, description, active) values (1, 'PAY', 0, 'Payment', true);
insert into transaction_code (id, code, kyc_required, description, active) values (2, 'IMT', 0, 'IMT', true);
insert into fee (id, transaction_code_id, currency_id, dr_entity_account_id, cr_entity_account_id, process_order, src_amount_fee_id, charge_type, amount, min_charge, max_charge, active)
    values (10, 1, 1, 2, 1, 1, null, 'ORIGINAL', 0.00, 0.00, 0.00, 1);
insert into fee (id, transaction_code_id, currency_id, dr_entity_account_id, cr_entity_account_id, process_order, src_amount_fee_id, charge_type, amount, min_charge, max_charge, active)
    values (2, 1, 1, 2, 1, 2, 10, 'FIXED', 2.00, 0.00, 100.00, 1);
insert into fee (id, transaction_code_id, currency_id, dr_entity_account_id, cr_entity_account_id, process_order, src_amount_fee_id, charge_type, amount, min_charge, max_charge, active)
    values (3, 1, 1, 2, 4, 3, 2, 'PERCENT', 0.02, 0.00, 100.00, 1);
insert into transaction_lines (id, transaction_id, transaction_code_id, entity_account_id, description, amount) values (1, 1, 1, 2, 'line', 20.0);
