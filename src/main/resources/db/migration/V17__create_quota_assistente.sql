CREATE TABLE quota_assistente (
    data_quota DATE NOT NULL,
    escopo VARCHAR(80) NOT NULL,
    utilizado INT NOT NULL DEFAULT 0,
    PRIMARY KEY (data_quota, escopo)
);
