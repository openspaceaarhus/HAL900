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

update rfid set name=rfid;

/* Convert existing wg26 tags to wg34 */
update rfid
set rfid=((rfid*2)::bit(34)|b'1000000000000000000000000000000000')::bigint;

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
