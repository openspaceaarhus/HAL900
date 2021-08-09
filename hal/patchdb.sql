begin transaction;
alter table accountTransaction add column receiptSent timestamp;
commit;

begin transaction;
insert into account (type_id, id, accountName) values (4, 100001, 'OSAA kontingent');
commit;

begin transaction;
create table rfid (
       id serial primary key,
       created timestamp default now(),
       updated timestamp default now(),

       rfid integer unique not null,
       owner_id integer references member(id) not null,
       pin bigint,
       lost boolean default false
);

create table doorTransaction (
       id serial primary key,
       created timestamp default now(),
       updated timestamp default now(),

       rfid_id integer references rfid(id) not null,

       hash bigint not null, 
       kind char(1) not null
);
commit;

begin transaction;
alter table rfid add column lost boolean default false;
commit;

begin transaction;
alter table member add column lastNagMail timestamp;
commit;

begin transaction;
alter table member add column lastRFIDMail timestamp;
commit;

begin transaction;
alter table accountTransaction add column operator_id integer references member(id);
commit;

begin transaction;

alter table rfid add column name varchar;
alter table rfid alter column rfid type bigint;

update rfid set name=rfid where name is null;

create table access_event_type (
   id int primary key,
   name varchar not null
);
insert into access_event_type (id, name) values (0, 'Power up');
insert into access_event_type (id, name) values (1, 'Wiegand');
insert into access_event_type (id, name) values (3, 'GPIO');
insert into access_event_type (id, name) values (4, 'Control token');
insert into access_event_type (id, name) values (5, 'Log message');

insert into access_event_type (id, name) values (254, 'Unlocked');
insert into access_event_type (id, name) values (255, 'User timeout');

create table access_device (
   id int primary key,
   created timestamp default now() not null,
   name varchar not null,
   aesKey varchar not null
);

create table access_event (
   id serial primary key,
   created_hal timestamp default now() not null,
   created_remote timestamp unique not null,
   device_id int references access_device(id) not null,  
   access_event_type int references access_event_type(id) not null,
   event_number int not null,
   wiegand_data bigint,
   event_text varchar not null
);

commit;
begin;
update access_device set name='Raspberry Pi' where id=0 and name='Unknown'; 
update access_device set name='v4 1-relæ' where id=1 and name='Unknown'; 
update access_device set name='v4 2-relæ' where id=2 and name='Unknown'; 

update access_event_type set name='Pi start' where name = 'Unknown 253';
update access_event_type set name='Locked' where name = 'Unknown 252';
update access_event_type set name='PIN timeout' where id=255 and name = 'User timeout';
commit;

begin;
create table gpio_bit (
   device_id int references access_device(id) not null,  
   index int not null,

   name varchar not null,
   set_event varchar,
   clear_event varchar,

   primary key (device_id, index);
);

/* Outputs */
insert into gpio_bit (device_id, index, name, set_event, clear_event) values (1, 0, "Lås", "Låst op", "Låst");
insert into gpio_bit (device_id, index, name, set_event, clear_event) values (1, 1, "Sirene", "Sirene tændt", "Sirene slukket");
insert into gpio_bit (device_id, index, name, set_event, clear_event) values (1, 2, "Wiegand ok", null, null);
insert into gpio_bit (device_id, index, name, set_event, clear_event) values (1, 3, "bit 3", "bit 3 set", "bit 3 cleared");

/* inputs */
insert into gpio_bit (device_id, index, name, set_event, clear_event) values (1, 4, "Greb", "Greb nede", "Greb oppe");
insert into gpio_bit (device_id, index, name, set_event, clear_event) values (1, 5, "Rigel", "Rigel inde", "Rigel ude");
insert into gpio_bit (device_id, index, name, set_event, clear_event) values (1, 6, "Dør", "Dør åben", "Dør lukket");
insert into gpio_bit (device_id, index, name, set_event, clear_event) values (1, 7, "bit 7", "bit 7 set", "bit 7 cleared");

/* Outputs */
insert into gpio_bit (device_id, index, name, set_event, clear_event) values (2, 0, "Lås", "Låst op", "Låst");
insert into gpio_bit (device_id, index, name, set_event, clear_event) values (2, 1, "K2", "K2=1", "K2=0");
insert into gpio_bit (device_id, index, name, set_event, clear_event) values (2, 2, "Wiegand ok", null, null);
insert into gpio_bit (device_id, index, name, set_event, clear_event) values (2, 3, "bit 3", "bit 3 set", "bit 3 cleared");

/* inputs */
insert into gpio_bit (device_id, index, name, set_event, clear_event) values (2, 4, "Greb", "Greb nede", "Greb oppe");
insert into gpio_bit (device_id, index, name, set_event, clear_event) values (2, 5, "Rigel", "Rigel inde", "Rigel ude");
insert into gpio_bit (device_id, index, name, set_event, clear_event) values (2, 6, "Dør", "Dør åben", "Dør lukket");
insert into gpio_bit (device_id, index, name, set_event, clear_event) values (2, 7, "Bystrøm", "Bystrøm ok", "Bystrøm mangler");

commit;
