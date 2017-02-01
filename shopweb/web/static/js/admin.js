(function() {
	$(function() {

	});

})();

var admin = {

	deleteUser : function(email, okFunc) {
		$.ajax({
			url : "delete/user/" + email,
			type : "DELETE",
			cache : false,
			timeout : 3000,
			statusCode : {
				200 : function(msg) {
					okFunc();
				},

				403 : function(msg) {
					var data = JSON.parse(msg.responseText);
					if (data.errors) {
						window.common.showFormErrors(data.errors);
					}
				}
			}
		});
	},

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
			url : "/category/delete/" + id,
			timeout : 3000,
			type : "DELETE",
		}).success(f());
	}

};