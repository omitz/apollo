FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/node:lts-alpine

# make the 'code' folder the current working directory
WORKDIR /code

# copy both 'package.json' and 'package-lock.json'
COPY ./edge-enroll-ui/package*.json ./

# install project dependencies
RUN npm install
RUN npm install -g @vue/cli

ENV PATH "$PATH:/code/node_modules/.bin"

# copy project files and folders to the current working directory
COPY ./edge-enroll-ui/ /code/

# build app for production with minification
RUN npm run build

EXPOSE 4000
CMD [ "npm", "run", "serve", "--", "--port", "4000"]