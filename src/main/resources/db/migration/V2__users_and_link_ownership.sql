create table if not exists users (
    id bigserial primary key,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    created_at timestamptz not null
);

alter table links add column if not exists user_id bigint;
alter table links add column if not exists owner_token varchar(64);

alter table links
    add constraint fk_links_user
    foreign key (user_id) references users(id);

create index if not exists idx_links_user_id on links(user_id);
create index if not exists idx_links_owner_token on links(owner_token);
