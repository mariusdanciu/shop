(function() {
	$(function() {

	});

})();

var admin = {

	deleteProduct : function(id, f) {
		$.ajax({
			cache : false,
			url : "/product/" + id,
			timeout : 3000,
			type : "DELETE",
			statusCode : {
				200 : function(data) {
					f(data.href);
				}
			}
		});
	},

	getCategory : function(id, categoryFunc) {
		$.ajax({
			cache : false,
			url : "/category/" + id,
			timeout : 3000,
			type : "GET",
			dataType : "json"
		}).success(categoryFunc);
	},

	deleteCategory : function(id, f) {
		$.ajax({
			cache : false,
			url : "/categories/" + id,
			timeout : 3000,
			type : "DELETE",
		}).success(f());
	}

};