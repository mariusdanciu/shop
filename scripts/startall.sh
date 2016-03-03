mkdir -p ./logs

mongod --auth --fork --logpath ./logs/db.log --dbpath ../mongodb

./start.sh $1

