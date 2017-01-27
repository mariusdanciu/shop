
print("Update DB")

db.categories.find({}).forEach( function(item) { 
	item.name = item.title.ro.toLowerCase();
	db.categories.save(item);
  }
);


db.products.find({}).forEach( function(item) { 
	item.name = item.title.ro.toLowerCase();
	db.products.save(item);
  }
);
