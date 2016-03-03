mkdir -p ./logs

java -Xms256m -Xmx700m -classpath .:./lib/* -Dlog4j.configuration=./config/log4j.properties net.shop.web.StartShop $1 > ./logs/console.log  2>&1 &

echo $! > ./server.pid
