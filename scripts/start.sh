mkdir ./logs

java -Xms256m -Xmx700m -classpath .:./lib/* net.shop.web.StartShop -Dlog4j.configuration=./config/log4j.properties > ./logs/console.log  2>&1 &

echo $! > ./server.pid
