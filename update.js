
print("Update DB")

db.categories.find({}).forEach( function(item) { 
    var n = item.title.ro.toLowerCase().replace(/['"]+/g, '');
	item.name = n;

	db.categories.save(item);
  }
);


db.products.find({}).forEach( function(item) { 
    var n = item.title.ro.toLowerCase().replace(/['"]+/g, '');
	item.name = n;
	
	db.products.save(item);
  }
);
