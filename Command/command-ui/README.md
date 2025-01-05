# Command User Interface

## Project setup

#### Specify analytic containers

To use your local containers, create a `.env` file in `command-ui` with `VUE_APP_ANALYTIC_ENV=local`. 

Otherwise, the UI will use the deployed `flask-apollo-processor` and analytics.

#### Build/up

```bash
docker-compose build ui
docker-compose up -d ui
```

#### Log in

Log in with the "susan" admin account specified in `flask-apollo-processor/api/__init__.py`. Once logged in as an admin, you can create a user account. (This is a temporary solution and will change once we have a way to create admin users.)


## Background

Background on how project was set up initially.

### Installing NVM/NPM

```bash
$ wget -qO- https://raw.githubusercontent.com/creationix/nvm/v0.35.3/install.sh | bash
$ nvm install --lts
```

### Install Vue CLI

Vue CLI is installed globally and not part of this project.

```bash
$ npm install -g @vue/cli
```

### Create Vue Project

Vue CLI will create the folder with an initial structure.

```bash
$ cd apollo/Command
$ vue create command-ui
```

### Install Vue dependencies

Note: Still a little confused on when to use vue and when to use npm.

```bash
$ vue add vuetify # installed as a plugin
$ npm install --save vue-router
$ npm install --save vuex
```

### Install dependency for the video player plugin

    npm install vue-core-video-player --save

### Audit and fix package dependencies

    npm audit
    npm audit fix

After `npm audit fix` you may still see `found 1 high severity vulnerability in 1447 scanned packages` due to package `serialize-javascript`, a dependency of `@vue/cli-service`. See https://github.com/vuejs/vue-cli/issues/5782.


## References

https://vuejs.org/

https://vuejs.org/v2/guide/

https://cli.vuejs.org/

https://router.vuejs.org/

https://vuex.vuejs.org/