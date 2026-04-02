create table if not exists links (
    id bigserial primary key,
    slug varchar(16) not null unique,
    original_url varchar(2048) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    is_active boolean not null default true,
    click_count bigint not null default 0
);

create index if not exists idx_links_created_at on links(created_at);

create table if not exists click_events (
    id bigserial primary key,
    link_id bigint not null,
    clicked_at timestamptz not null,
    ip_address varchar(64),
    user_agent varchar(1024),
    referer varchar(1024),
    country varchar(64),
    device_type varchar(64),
    browser varchar(64),
    os varchar(64),
    constraint fk_click_events_link foreign key (link_id) references links(id)
);

create index if not exists idx_click_events_link_id on click_events(link_id);
create index if not exists idx_click_events_clicked_at on click_events(clicked_at);
create index if not exists idx_click_events_link_id_clicked_at on click_events(link_id, clicked_at);
