mkdir ./logs

java -Xms256m -Xmx700m -classpath .:./lib/* net.shop.web.StartShop -Dlog4j.configuration=./config/log4j.properties 2>&1 | tee ./logs/server.out &

echo $! > ./server.pid
