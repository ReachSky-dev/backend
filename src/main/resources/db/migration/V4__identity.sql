CREATE TABLE user_profiles (
    id           uuid         NOT NULL,
    subject      varchar(255) NOT NULL,
    display_name varchar(255) NOT NULL,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT user_profiles_pk PRIMARY KEY (id)
);

CREATE UNIQUE INDEX user_profiles_subject_uidx ON user_profiles (subject);
