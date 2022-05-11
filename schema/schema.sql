drop schema if exists omac;

create schema omac;

use omac;

create table addresses (
	S_NO int AUTO_INCREMENT not null,
    BLK_NO varchar(8),
    ROAD_NAME varchar(128),
    BUILDING varchar(128),
    FULL_ADDRESS varchar(256),
    POSTAL_CODE int not null,
    DATE_UPDATED date default (current_date),

    primary key (S_NO)
);

create table users (
	email varchar(32) not null,
    password varchar(256) not null,

    primary key (email)
);

insert into users (email, password)
	values ('omacapp@outlook.com', sha1(12345));
    
create table files (
	email varchar(32),
    filename varchar(8) not null,

    primary key (filename),
    
	constraint fk_email
        foreign key(email) 
        references users(email) 
        on delete cascade
);