-- Baseline schema (previously created directly in Supabase), generated from the JPA entities.

CREATE TABLE public.activities (
    estimated_cost numeric(10,2) NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    order_index integer NOT NULL,
    version integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    end_time timestamp(6) with time zone NOT NULL,
    start_time timestamp(6) with time zone NOT NULL,
    created_by uuid NOT NULL,
    id uuid NOT NULL,
    itinerary_day_id uuid NOT NULL,
    location character varying(200) NOT NULL,
    title character varying(200) NOT NULL,
    description character varying(1000)
);

CREATE TABLE public.audit_logs (
    created_at timestamp(6) with time zone NOT NULL,
    entity_id uuid NOT NULL,
    id uuid NOT NULL,
    trip_id uuid,
    user_id uuid NOT NULL,
    action character varying(100) NOT NULL,
    entity_type character varying(100) NOT NULL,
    metadata character varying(2000)
);

CREATE TABLE public.chat_messages (
    created_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    sender_id uuid NOT NULL,
    trip_id uuid NOT NULL,
    message character varying(2000) NOT NULL
);

CREATE TABLE public.documents (
    created_at timestamp(6) with time zone NOT NULL,
    file_size_bytes bigint NOT NULL,
    id uuid NOT NULL,
    trip_id uuid NOT NULL,
    uploaded_by_id uuid NOT NULL,
    document_type character varying(50) NOT NULL,
    file_name character varying(200) NOT NULL,
    file_url character varying(500) NOT NULL,
    CONSTRAINT documents_document_type_check CHECK (((document_type)::text = ANY ((ARRAY['FLIGHT_TICKET'::character varying, 'HOTEL_BOOKING'::character varying, 'PASSPORT'::character varying, 'VISA'::character varying, 'INSURANCE'::character varying, 'OTHER'::character varying])::text[])))
);

CREATE TABLE public.expense_splits (
    split_amount numeric(38,2) NOT NULL,
    expense_id uuid NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL
);

CREATE TABLE public.expenses (
    amount numeric(12,2) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    paid_by uuid NOT NULL,
    trip_id uuid NOT NULL,
    title character varying(200) NOT NULL,
    description character varying(1000),
    category character varying(255) NOT NULL,
    CONSTRAINT expenses_category_check CHECK (((category)::text = ANY ((ARRAY['FOOD'::character varying, 'TRANSPORT'::character varying, 'HOTEL'::character varying, 'SHOPPING'::character varying, 'ACTIVITY'::character varying, 'OTHER'::character varying])::text[])))
);

CREATE TABLE public.idempotency_keys (
    id uuid NOT NULL,
    resource_id uuid NOT NULL,
    key character varying(255) NOT NULL,
    operation_type character varying(255) NOT NULL
);

CREATE TABLE public.itinerary_days (
    date date NOT NULL,
    day_number integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    trip_id uuid NOT NULL,
    title character varying(200) NOT NULL,
    notes character varying(1000)
);

CREATE TABLE public.notifications (
    is_read boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    trip_id uuid,
    user_id uuid NOT NULL,
    type character varying(50) NOT NULL,
    title character varying(200) NOT NULL,
    action_url character varying(500),
    message character varying(1000) NOT NULL,
    CONSTRAINT notifications_type_check CHECK (((type)::text = ANY ((ARRAY['TRIP_INVITATION'::character varying, 'EXPENSE_ADDED'::character varying, 'EXPENSE_SETTLED'::character varying, 'DOCUMENT_UPLOADED'::character varying, 'CHAT_MESSAGE'::character varying, 'TRIP_UPDATED'::character varying, 'GENERAL'::character varying])::text[])))
);

CREATE TABLE public.oauth_accounts (
    created_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    provider character varying(30) NOT NULL,
    provider_user_id character varying(255) NOT NULL,
    CONSTRAINT oauth_accounts_provider_check CHECK (((provider)::text = 'GOOGLE'::text))
);

CREATE TABLE public.refresh_tokens (
    is_revoked boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    token_hash character varying(1000) NOT NULL
);

CREATE TABLE public.settlements (
    amount numeric(12,2) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    paid_by uuid NOT NULL,
    paid_to uuid NOT NULL,
    trip_id uuid NOT NULL
);

CREATE TABLE public.trip_invitations (
    is_revoked boolean NOT NULL,
    usage_count integer NOT NULL,
    usage_limit integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    invited_by uuid NOT NULL,
    trip_id uuid NOT NULL,
    role character varying(30) NOT NULL,
    token_hash character varying(255) NOT NULL,
    CONSTRAINT trip_invitations_role_check CHECK (((role)::text = ANY ((ARRAY['OWNER'::character varying, 'ADMIN'::character varying, 'MEMBER'::character varying])::text[])))
);

CREATE TABLE public.trip_media (
    created_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    trip_id uuid NOT NULL,
    uploaded_by_id uuid NOT NULL,
    caption character varying(500),
    file_url character varying(1000) NOT NULL,
    media_type character varying(255) NOT NULL,
    CONSTRAINT trip_media_media_type_check CHECK (((media_type)::text = ANY ((ARRAY['PHOTO'::character varying, 'VIDEO'::character varying])::text[])))
);

CREATE TABLE public.trip_members (
    joined_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    trip_id uuid NOT NULL,
    user_id uuid NOT NULL,
    role character varying(255) NOT NULL,
    CONSTRAINT trip_members_role_check CHECK (((role)::text = ANY ((ARRAY['OWNER'::character varying, 'ADMIN'::character varying, 'MEMBER'::character varying])::text[])))
);

CREATE TABLE public.trips (
    active boolean NOT NULL,
    end_date date,
    start_date date,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid NOT NULL,
    id uuid NOT NULL,
    destination character varying(100) NOT NULL,
    title character varying(150) NOT NULL,
    description character varying(1000)
);

CREATE TABLE public.users (
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    status character varying(20) NOT NULL,
    name character varying(100) NOT NULL,
    email character varying(255) NOT NULL,
    password_hash character varying(255),
    profile_image_url character varying(255),
    CONSTRAINT users_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'SUSPENDED'::character varying])::text[])))
);

ALTER TABLE ONLY public.activities
    ADD CONSTRAINT activities_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.chat_messages
    ADD CONSTRAINT chat_messages_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT documents_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.expense_splits
    ADD CONSTRAINT expense_splits_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT expenses_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.idempotency_keys
    ADD CONSTRAINT idempotency_keys_key_key UNIQUE (key);

ALTER TABLE ONLY public.idempotency_keys
    ADD CONSTRAINT idempotency_keys_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.itinerary_days
    ADD CONSTRAINT itinerary_days_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.itinerary_days
    ADD CONSTRAINT itinerary_days_trip_id_day_number_key UNIQUE (trip_id, day_number);

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.oauth_accounts
    ADD CONSTRAINT oauth_accounts_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_token_hash_key UNIQUE (token_hash);

ALTER TABLE ONLY public.settlements
    ADD CONSTRAINT settlements_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.trip_invitations
    ADD CONSTRAINT trip_invitations_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.trip_invitations
    ADD CONSTRAINT trip_invitations_token_hash_key UNIQUE (token_hash);

ALTER TABLE ONLY public.trip_media
    ADD CONSTRAINT trip_media_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.trip_members
    ADD CONSTRAINT trip_members_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.trips
    ADD CONSTRAINT trips_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.oauth_accounts
    ADD CONSTRAINT uk_oauth_provider_user UNIQUE (provider, provider_user_id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

CREATE INDEX idx_oauth_user_id ON public.oauth_accounts USING btree (user_id);

CREATE INDEX idx_users_email ON public.users USING btree (email);

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT fk1lih5y2npsf8u5o3vhdb9y0os FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.trip_invitations
    ADD CONSTRAINT fk2f5o9awoa5v3ylj3srh8m2ae1 FOREIGN KEY (invited_by) REFERENCES public.users(id);

ALTER TABLE ONLY public.chat_messages
    ADD CONSTRAINT fk2fpwfy5uwobsgaet7qhdindge FOREIGN KEY (trip_id) REFERENCES public.trips(id);

ALTER TABLE ONLY public.trip_members
    ADD CONSTRAINT fk652lcpk0gdigjfm28bhgu24y2 FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.activities
    ADD CONSTRAINT fk6qc6poqy57n1ive0p1l0cgy1o FOREIGN KEY (itinerary_day_id) REFERENCES public.itinerary_days(id);

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT fk78f2ncm6n2x9cslini85w53p5 FOREIGN KEY (trip_id) REFERENCES public.trips(id);

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT fk9twvm79qw1voae3xgtn1xq5y9 FOREIGN KEY (trip_id) REFERENCES public.trips(id);

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fk9y21adhxn0ayjhfocscqox7bh FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.oauth_accounts
    ADD CONSTRAINT fk_oauth_user FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT fka8xyugvg1b1gjr07r5057pn5 FOREIGN KEY (uploaded_by_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.settlements
    ADD CONSTRAINT fkauhpy2xtr3d3edt1tmvgd4x53 FOREIGN KEY (paid_by) REFERENCES public.users(id);

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT fkawch26o8lkciv8k73mki0gnp8 FOREIGN KEY (trip_id) REFERENCES public.trips(id);

ALTER TABLE ONLY public.itinerary_days
    ADD CONSTRAINT fkb3p1v2mgkl6qgqkfgabpt9kag FOREIGN KEY (trip_id) REFERENCES public.trips(id);

ALTER TABLE ONLY public.trips
    ADD CONSTRAINT fkc4x3abgpcmxs8xcr9f238fpby FOREIGN KEY (created_by) REFERENCES public.users(id);

ALTER TABLE ONLY public.activities
    ADD CONSTRAINT fkexq3sblvj77fqtr4ucioqowsc FOREIGN KEY (created_by) REFERENCES public.users(id);

ALTER TABLE ONLY public.trip_invitations
    ADD CONSTRAINT fkfiqptshmtmolwrcvdc2d2k2af FOREIGN KEY (trip_id) REFERENCES public.trips(id);

ALTER TABLE ONLY public.chat_messages
    ADD CONSTRAINT fkgiqeap8ays4lf684x7m0r2729 FOREIGN KEY (sender_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fkh08ws8vxl5kxfocg09i7hylyi FOREIGN KEY (trip_id) REFERENCES public.trips(id);

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT fkiwqbj8x69qhvj7a41f5r3xy10 FOREIGN KEY (paid_by) REFERENCES public.users(id);

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT fkjs4iimve3y0xssbtve5ysyef0 FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.trip_members
    ADD CONSTRAINT fklrwxwvuaj3iddljvck3c3gtqr FOREIGN KEY (trip_id) REFERENCES public.trips(id);

ALTER TABLE ONLY public.settlements
    ADD CONSTRAINT fkn3aps2chck2bmuqlyf8su1epg FOREIGN KEY (paid_to) REFERENCES public.users(id);

ALTER TABLE ONLY public.expense_splits
    ADD CONSTRAINT fknikv7drxxkj9x862du3r4ruam FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.settlements
    ADD CONSTRAINT fkp9plagybpxm3g5d2dd3kcjo5j FOREIGN KEY (trip_id) REFERENCES public.trips(id);

ALTER TABLE ONLY public.trip_media
    ADD CONSTRAINT fkqasdw722yj7n2wpjbks77htog FOREIGN KEY (trip_id) REFERENCES public.trips(id);

ALTER TABLE ONLY public.expense_splits
    ADD CONSTRAINT fkqigr3qe91wgifpl6mak0xcdso FOREIGN KEY (expense_id) REFERENCES public.expenses(id);

ALTER TABLE ONLY public.trip_media
    ADD CONSTRAINT fksgx5a7q88p9vm3uy1efae5o43 FOREIGN KEY (uploaded_by_id) REFERENCES public.users(id);

