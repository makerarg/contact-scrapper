CREATE TABLE IF NOT EXISTS `third_party_contact` (
    `id` varchar(255) NOT NULL,
    `store_name` varchar(255),
    `name` varchar(255),
    `website` varchar(255),
    `source` varchar(255),

    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE IF NOT EXISTS `contact_email_address` (
    `id` varchar(255) NOT NULL,
    `contact_id` varchar(255),
    `email_address` varchar(255),

    PRIMARY KEY (`id`),
    UNIQUE(`email_address`),
    FOREIGN KEY (`contact_id`) REFERENCES `third_party_contact`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE IF NOT EXISTS `contact_phone_number` (
    `id` varchar(255) NOT NULL,
    `contact_id` varchar(255),
    `phone_number` varchar(255),

    PRIMARY KEY (`id`),
    UNIQUE(`phone_number`),
FOREIGN KEY (`contact_id`) REFERENCES `third_party_contact`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE IF NOT EXISTS `contact_location` (
    `id` varchar(255) NOT NULL,
    `contact_id` varchar(255),
    `address` varchar(255),
    `city` varchar(255),
    `state` varchar(255),
    `country` varchar(255),
    `zip_code` varchar(255),
    `latitude` decimal,
    `longitude` decimal,

    PRIMARY KEY (`id`),
    FOREIGN KEY (`contact_id`) REFERENCES `third_party_contact`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
