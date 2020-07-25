# Omni Chat

_Trusted, Extensible, Better Chat_

![Cover](branding/facebook_cover_photo_2.png)

For people who need to communicate via instant messaging, Omni Chat is a free and open core chat system that can replace every existing chat app. Unlike other chat apps, our product brings together all the useful features of existing services, while leaving out their annoying parts.

The [spec](docs/spec.md) explains how Omni Chat differentiates itself from existing services, and which features have been implemented so far. This repo is for the backend API. There's no frontend UI yet.

To view a previous version's docs, go to `https://github.com/neelkamath/omni-chat/tree/<VERSION>`, where `<VERSION>` is the release tag (e.g., `v0.1.1`).

## Installation

1. Download the `rest-api.html` asset from a [release](https://github.com/neelkamath/omni-chat/releases).
1. Download [`dockerize`](docker/dockerize).
1. Download [`docker-compose.yml`](docs/docker-compose.yml).
1. Install [Docker](https://docs.docker.com/get-docker/).
1. [Configure](docs/config.md).
1. Optionally, generate a wrapper for the GraphQL API using [GraphQL Code Generator](https://graphql-code-generator.com/) on [`schema.graphqls`](src/main/resources/schema.graphqls).
1. Optionally, generate a wrapper for the REST API using [OpenAPI Generator](https://openapi-generator.tech/) on [`openapi.yaml`](docs/openapi.yaml).

## Usage

- [Docs](docs/api.md)
- [Changelog](docs/CHANGELOG.md)
- [Branding assets](branding)

### Running the Application

1. Start the server on http://localhost:80: `docker-compose up -d`
1. [Set up the auth system](docs/auth_setup.md) if you haven't already.
1. To shut down: `docker-compose down`

- The auth system's admin panel runs on http://localhost:80/auth/admin. The username is `admin`, and the password is the value of `KEYCLOAK_PASSWORD` in the `.env` file. You can perform any operation in the admin panel except creating and deleting users because that would cause the chat DB to fall out of sync.
- The `chat` service can be scaled freely.
- The `chat` service is dependent on other services, such as `auth`. Please see the docs of the other services to operate them in production. For example, the `auth` service uses the [Keycloak](https://hub.docker.com/r/jboss/keycloak) image, which documents how to back up, restore, etc. it.
- When restoring backups for the `chat-db` and `auth` services, make sure the backups had been taken at the same time because they're dependent on each other's state.

### Migrating to a Newer Version

Since the application is still pre-release software quality, DB migrations aren't provided. This means you must delete the existing databases when you migrate to a newer version.

## [Contributing](docs/CONTRIBUTING.md)

## Credits

[`dockerize`](docker/dockerize) was taken from [jwilder](https://github.com/jwilder/dockerize).

## License

This project is under the [MIT License](LICENSE).
