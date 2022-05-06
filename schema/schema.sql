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