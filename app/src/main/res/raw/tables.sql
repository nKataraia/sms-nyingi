drop table if exists sms;
create table sms(
id integer primary key autoincrement,
sender varchar(15) not null,
receiverPhone varchar(15) not null,
senderPhone varchar(15) not null,
"text" text not null,
topic text not null,
dateSent date not null,
status text not null
);

drop table if exists db;
create table db(
dbVersion integer not null,
codeVersion integer not null
)